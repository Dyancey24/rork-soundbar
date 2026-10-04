// functions/index.ts — Soundbar cloud sync.
//
// One Durable Object instance per signed-in account ("user:<uuid>") holds that
// account's shelf snapshot. The client sends its Supabase Auth bearer token;
// this worker verifies it with Supabase (/auth/v1/user) and stamps
// X-Rork-User-Id for the DOs downstream. Without a valid token, requests are
// guests and get a 401. Writes are last-write-wins on a millisecond stamp.
// Cloud sync v1: GET/PUT /shelf, one snapshot per account.
// Leaderboard v1: a global DO holds players (with friend codes), friendships,
// and ratcheting scores for the friends leaderboard.

import { DurableObject } from "cloudflare:workers";

export { Leaderboard } from "./leaderboard";

type Env = {
  DO: Fetcher;
  SUPABASE_URL?: string;
  EXPO_PUBLIC_SUPABASE_URL?: string;
  SUPABASE_ANON_KEY?: string;
  EXPO_PUBLIC_SUPABASE_ANON_KEY?: string;
};

/** Public Supabase credentials — safe to embed; env overrides keep it flexible. */
const SUPABASE_URL =
  "https://lqttmgwoslgksvenpjjl.supabase.co";
const SUPABASE_ANON_KEY = "sb_publishable_1BrAevm9i1dNsh2HpxuhIw_03y6Llct";

type SupabaseUser = { id?: string };

/**
 * Verifies the request's Supabase Auth bearer token and returns the user's
 * UUID, or null when the token is missing or invalid.
 */
async function authedUserId(request: Request, env: Env): Promise<string | null> {
  const header = request.headers.get("Authorization") ?? "";
  if (!header.startsWith("Bearer ")) return null;
  const token = header.slice("Bearer ".length).trim();
  if (!token) return null;

  const url = env.SUPABASE_URL ?? env.EXPO_PUBLIC_SUPABASE_URL ?? SUPABASE_URL;
  const anonKey = env.SUPABASE_ANON_KEY ?? env.EXPO_PUBLIC_SUPABASE_ANON_KEY ?? SUPABASE_ANON_KEY;
  try {
    const response = await fetch(`${url}/auth/v1/user`, {
      headers: { apikey: anonKey, Authorization: `Bearer ${token}` },
    });
    if (!response.ok) return null;
    const user = (await response.json()) as SupabaseUser;
    return user.id ?? null;
  } catch {
    return null;
  }
}

/** Per-account shelf snapshot storage. The state blob is opaque JSON. */
export class UserShelf extends DurableObject {
  override async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/shelf") {
      const state = await this.ctx.storage.get<string>("state");
      const updatedAt = (await this.ctx.storage.get<number>("updatedAt")) ?? 0;
      if (state == null) {
        return Response.json({ found: false, state: null, updatedAt: 0 });
      }
      return Response.json({ found: true, state, updatedAt });
    }

    if (request.method === "PUT" && url.pathname === "/shelf") {
      const body = (await request.json()) as { state?: string; updatedAt?: number };
      const incoming = typeof body.updatedAt === "number" ? body.updatedAt : Date.now();
      const current = (await this.ctx.storage.get<number>("updatedAt")) ?? 0;
      if (incoming < current) {
        // A straggler write — the newer snapshot stays.
        return Response.json({ ok: true, stale: true, updatedAt: current });
      }
      await this.ctx.storage.put({ state: body.state ?? "", updatedAt: incoming });
      return Response.json({ ok: true, stale: false, updatedAt: incoming });
    }

    return new Response("not found", { status: 404 });
  }
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === "/ping") {
      return Response.json({ ok: true, now: new Date().toISOString() });
    }

    if (url.pathname.startsWith("/leaderboard") || url.pathname === "/shelf") {
      const userId = await authedUserId(request, env);
      if (!userId) {
        return Response.json({ error: "unauthorized" }, { status: 401 });
      }
      const wrapped = new Request(request.url, request);
      wrapped.headers.set("X-Rork-User-Id", userId);
      if (url.pathname.startsWith("/leaderboard")) {
        wrapped.headers.set("X-Rork-DO-Class", "Leaderboard");
        wrapped.headers.set("X-Rork-DO-Id", "global:leaderboard");
      } else {
        wrapped.headers.set("X-Rork-DO-Class", "UserShelf");
        wrapped.headers.set("X-Rork-DO-Id", `user:${userId}`);
      }
      return env.DO.fetch(wrapped);
    }

    return new Response("not found", { status: 404 });
  },
} satisfies ExportedHandler<Env>;
