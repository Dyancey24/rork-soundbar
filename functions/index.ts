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
// Account deletion v1: a user can request deletion; the account lingers for a
// 48-hour grace (stored in Supabase user_metadata), then the worker removes
// the auth user plus its leaderboard rows and shelf snapshot — lazily on the
// user's own requests, and for absent users on a cron schedule.
// The public request pages live in deletion-page.ts: /account-deletion.
// The public legal pages live in legal-page.ts: /privacy and /terms.
// (The service key for the deletion sweep arrives through the project env.)

import { DurableObject } from "cloudflare:workers";

export { Leaderboard } from "./leaderboard";
import { handleDeletionPage } from "./deletion-page";
import { handleLegalPage } from "./legal-page";

type Env = {
  DO: Fetcher;
  SUPABASE_URL?: string;
  EXPO_PUBLIC_SUPABASE_URL?: string;
  SUPABASE_ANON_KEY?: string;
  EXPO_PUBLIC_SUPABASE_ANON_KEY?: string;
  SUPABASE_SERVICE_ROLE_KEY?: string;
};

/** Public Supabase credentials — safe to embed; env overrides keep it flexible. */
const SUPABASE_URL =
  "https://lqttmgwoslgksvenpjjl.supabase.co";
const SUPABASE_ANON_KEY = "sb_publishable_1BrAevm9i1dNsh2HpxuhIw_03y6Llct";

type SupabaseUser = { id?: string; user_metadata?: { deletion_scheduled_at?: number | string } };

type AuthedUser = { id: string; deletionAt: number | null };

/**
 * Verifies the request's Supabase Auth bearer token and returns the user's
 * UUID (plus any pending deletion deadline), or null when the token is
 * missing or invalid.
 */
async function authedUser(request: Request, env: Env): Promise<AuthedUser | null> {
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
    if (!user.id) return null;
    const scheduledAt = Number(user.user_metadata?.deletion_scheduled_at ?? 0);
    return { id: user.id, deletionAt: scheduledAt > 0 ? scheduledAt : null };
  } catch {
    return null;
  }
}

/** Builds a request the DO routing layer understands. */
function doRequest(
  path: string,
  userId: string,
  doClass: string,
  doId: string,
  method = "DELETE"
): Request {
  const wrapped = new Request(`https://do${path}`, { method });
  wrapped.headers.set("X-Rork-User-Id", userId);
  wrapped.headers.set("X-Rork-DO-Class", doClass);
  wrapped.headers.set("X-Rork-DO-Id", doId);
  return wrapped;
}

/**
 * Removes an account for good: leaderboard rows, the shelf snapshot, and the
 * Supabase auth user itself. Needs the service role key; without it the
 * deletion request simply stays pending (the grace holds).
 */
async function deleteAccount(env: Env, userId: string): Promise<void> {
  const serviceKey = env.SUPABASE_SERVICE_ROLE_KEY;
  if (!serviceKey) return;
  const url = env.SUPABASE_URL ?? env.EXPO_PUBLIC_SUPABASE_URL ?? SUPABASE_URL;
  const headers = { apikey: serviceKey, Authorization: `Bearer ${serviceKey}` };
  try {
    await env.DO.fetch(
      doRequest("/leaderboard/account", userId, "Leaderboard", "global:leaderboard")
    );
    await env.DO.fetch(doRequest("/shelf", userId, "UserShelf", `user:${userId}`));
    await fetch(`${url}/auth/v1/admin/users/${userId}`, { method: "DELETE", headers });
  } catch {
    // Best effort — the next sweep retries whatever is left.
  }
}

/** Deletes every user whose 48-hour grace ran out. */
async function sweepDeletions(env: Env): Promise<void> {
  const serviceKey = env.SUPABASE_SERVICE_ROLE_KEY;
  if (!serviceKey) return;
  const url = env.SUPABASE_URL ?? env.EXPO_PUBLIC_SUPABASE_URL ?? SUPABASE_URL;
  const headers = { apikey: serviceKey, Authorization: `Bearer ${serviceKey}` };
  for (let page = 1; page <= 100; page++) {
    const response = await fetch(
      `${url}/auth/v1/admin/users?per_page=50&page=${page}`,
      { headers }
    );
    if (!response.ok) return;
    const listing = (await response.json()) as { users?: SupabaseUser[] };
    const users = listing.users ?? [];
    for (const user of users) {
      const scheduledAt = Number(user.user_metadata?.deletion_scheduled_at ?? 0);
      if (user.id && scheduledAt > 0 && Date.now() >= scheduledAt) {
        await deleteAccount(env, user.id);
      }
    }
    if (users.length < 50) return;
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

    // Account deletion: the whole snapshot goes with the account.
    if (request.method === "DELETE" && url.pathname === "/shelf") {
      await this.ctx.storage.deleteAll();
      return Response.json({ ok: true });
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

    // The public legal pages — privacy policy and house rules.
    const legalResponse = handleLegalPage(request, env);
    if (legalResponse) return legalResponse;

    // The public account-deletion pages live outside the authed DO routing.
    const deletionResponse = await handleDeletionPage(request, env);
    if (deletionResponse) return deletionResponse;

    if (url.pathname.startsWith("/leaderboard") || url.pathname === "/shelf") {
      const auth = await authedUser(request, env);
      if (!auth) {
        return Response.json({ error: "unauthorized" }, { status: 401 });
      }
      // The grace is over: the account goes, and its data with it.
      if (auth.deletionAt != null && Date.now() >= auth.deletionAt) {
        await deleteAccount(env, auth.id);
        return Response.json({ error: "account_deleted" }, { status: 403 });
      }
      const userId = auth.id;
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

  // The nightly janitor: accounts whose grace ran out while nobody knocked.
  async scheduled(_event: unknown, env: Env): Promise<void> {
    await sweepDeletions(env);
  },
} satisfies ExportedHandler<Env>;
