// functions/index.ts — Soundbar cloud sync.
//
// One Durable Object instance per signed-in account ("user:<id>") holds that
// account's shelf snapshot. The platform verifies the client's Rork Auth
// bearer token and stamps X-Rork-User-Id; without it, requests are guests and
// get a 401. Writes are last-write-wins on a millisecond stamp.
// Cloud sync v1: GET/PUT /shelf, one snapshot per account.

import { DurableObject } from "cloudflare:workers";

type Env = { DO: Fetcher };

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

    if (url.pathname !== "/shelf") {
      return new Response("not found", { status: 404 });
    }

    const userId = request.headers.get("X-Rork-User-Id");
    if (!userId) {
      return Response.json({ error: "unauthorized" }, { status: 401 });
    }

    const wrapped = new Request(request.url, request);
    wrapped.headers.set("X-Rork-DO-Class", "UserShelf");
    wrapped.headers.set("X-Rork-DO-Id", `user:${userId}`);
    return env.DO.fetch(wrapped);
  },
} satisfies ExportedHandler<Env>;
