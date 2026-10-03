// functions/leaderboard.ts — the friends leaderboard.
//
// One global Durable Object ("global:leaderboard") keeps a SQLite row per
// player and per friendship. Players join by signing in (Rork Auth stamps
// X-Rork-User-Id), share a short friend code, and invite each other by code —
// a friendship only exists once BOTH sides have added each other (a clink).
// Scores ratchet: a player's stored points only ever move up, so a stale
// client can never drag a score backwards.

import { DurableObject } from "cloudflare:workers";

/** Friend-code alphabet — digits and letters without 0/O/1/I look-alikes. */
const CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

/** The avatar glyphs the client's catalogue knows; anything else is rejected. */
const AVATAR_GLYPHS = new Set([
  "vinyl",
  "note",
  "headphones",
  "cocktail",
  "chef",
  "radio",
  "mic",
  "star",
]);

type PlayerRow = {
  id: string;
  name: string;
  code: string;
  points: number;
  avatar: string | null;
};

function cleanName(raw: string | null | undefined): string | null {
  const name = (raw ?? "").trim().replace(/\s+/g, " ").slice(0, 24);
  return name.length > 0 ? name : null;
}

/** Accepts "glyph:paletteIndex" codes from the client's fixed avatar catalogue. */
function cleanAvatar(raw: unknown): string | null {
  if (typeof raw !== "string") return null;
  const match = /^([a-z]+):([0-5])$/.exec(raw);
  if (!match || !AVATAR_GLYPHS.has(match[1])) return null;
  return raw;
}

function sanitizePoints(raw: unknown): number {
  const value = typeof raw === "number" ? raw : Number(raw);
  if (!Number.isFinite(value)) return 0;
  return Math.max(0, Math.floor(value));
}

export class Leaderboard extends DurableObject {
  constructor(ctx: DurableObjectState, env: unknown) {
    super(ctx, env);
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS players (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        code TEXT NOT NULL UNIQUE,
        points INTEGER NOT NULL DEFAULT 0,
        updated_at INTEGER NOT NULL DEFAULT 0
      )
    `);
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS friends (
        user_id TEXT NOT NULL,
        friend_id TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        PRIMARY KEY (user_id, friend_id)
      )
    `);
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS requests (
        from_id TEXT NOT NULL,
        to_id TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        PRIMARY KEY (from_id, to_id)
      )
    `);
    // Avatars arrived after the first tables; older databases grow the column.
    try {
      this.ctx.storage.sql.exec("ALTER TABLE players ADD COLUMN avatar TEXT");
    } catch {
      // Column already exists.
    }
    // Mutual confirmation arrived after the first friendships: one-way rows
    // from the old add-by-code flow are dropped — both sides must clink.
    this.ctx.storage.sql.exec(`
      DELETE FROM friends WHERE NOT EXISTS (
        SELECT 1 FROM friends reciprocal
        WHERE reciprocal.user_id = friends.friend_id
          AND reciprocal.friend_id = friends.user_id
      )
    `);
  }

  override async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    const userId = request.headers.get("X-Rork-User-Id");
    if (!userId) {
      return Response.json({ error: "unauthorized" }, { status: 401 });
    }
    const fallbackName = request.headers.get("X-Rork-User-Name") ?? "";

    // Register or rename: every sign-in lands here first, so the player row
    // and its friend code exist before anything else reads the board.
    if (request.method === "POST" && url.pathname === "/leaderboard/profile") {
      const body = (await request.json().catch(() => ({}))) as {
        name?: string;
        avatar?: string | null;
      };
      const name = cleanName(body.name) ?? cleanName(fallbackName) ?? "Guest";
      const existing = this.getPlayer(userId);
      const avatar = "avatar" in body ? cleanAvatar(body.avatar) : existing?.avatar ?? null;
      return Response.json(this.ensurePlayer(userId, name, avatar));
    }

    if (request.method === "GET" && url.pathname === "/leaderboard") {
      const me = this.getPlayer(userId);
      if (me == null) {
        return Response.json({ me: null, friends: [], incoming: [], outgoing: [] });
      }
      return Response.json({
        me,
        friends: this.getFriends(userId),
        incoming: this.getRequests(userId, "in"),
        outgoing: this.getRequests(userId, "out"),
      });
    }

    // Score updates ratchet upward; the newest name and mark always stick.
    if (request.method === "PUT" && url.pathname === "/leaderboard/score") {
      const body = (await request.json().catch(() => ({}))) as {
        points?: number;
        name?: string;
        avatar?: string | null;
      };
      const existing = this.getPlayer(userId);
      const name = cleanName(body.name) ?? existing?.name ?? cleanName(fallbackName) ?? "Guest";
      const points = Math.max(sanitizePoints(body.points), existing?.points ?? 0);
      const avatar = "avatar" in body ? cleanAvatar(body.avatar) : existing?.avatar ?? null;
      this.savePlayer(userId, name, points, avatar);
      return Response.json({ ok: true, points });
    }

    // Offering a code sends an invite. If the other side already offered
    // theirs, the glasses clink and the friendship is confirmed at once.
    if (request.method === "POST" && url.pathname === "/leaderboard/friends") {
      const body = (await request.json().catch(() => ({}))) as { code?: string };
      const code = (body.code ?? "").trim().toUpperCase();
      const friend = this.findByCode(code);
      if (friend == null) {
        return Response.json({ error: "unknown-code" }, { status: 404 });
      }
      if (friend.id === userId) {
        return Response.json({ error: "self" }, { status: 400 });
      }
      if (this.areFriends(userId, friend.id)) {
        return Response.json({ ok: true, status: "confirmed", friend });
      }
      if (this.hasRequest(friend.id, userId)) {
        this.confirmFriendship(userId, friend.id);
        return Response.json({ ok: true, status: "confirmed", friend });
      }
      this.ctx.storage.sql.exec(
        "INSERT OR IGNORE INTO requests (from_id, to_id, created_at) VALUES (?, ?, ?)",
        userId,
        friend.id,
        Date.now(),
      );
      return Response.json({ ok: true, status: "pending", friend });
    }

    // Accepting an invite clinks the glasses: both sides land on both boards.
    if (request.method === "POST" && url.pathname === "/leaderboard/friends/accept") {
      const body = (await request.json().catch(() => ({}))) as { id?: string };
      const fromId = body.id ?? "";
      if (!this.hasRequest(fromId, userId)) {
        return Response.json({ error: "no-request" }, { status: 404 });
      }
      this.confirmFriendship(userId, fromId);
      const friend = this.getPlayer(fromId);
      return Response.json({
        ok: true,
        friend: friend && {
          id: friend.id,
          name: friend.name,
          points: friend.points,
          avatar: friend.avatar ?? null,
        },
      });
    }

    // Passing on an invite, or taking one back — either direction dissolves it.
    if (request.method === "DELETE" && url.pathname === "/leaderboard/friends/requests") {
      const otherId = url.searchParams.get("id") ?? "";
      this.ctx.storage.sql.exec(
        "DELETE FROM requests WHERE (from_id = ? AND to_id = ?) OR (from_id = ? AND to_id = ?)",
        userId,
        otherId,
        otherId,
        userId,
      );
      return Response.json({ ok: true });
    }

    if (request.method === "DELETE" && url.pathname === "/leaderboard/friends") {
      const friendId = url.searchParams.get("id") ?? "";
      // Removing a friend removes the clink for both sides.
      this.ctx.storage.sql.exec(
        "DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)",
        userId,
        friendId,
        friendId,
        userId,
      );
      return Response.json({ ok: true });
    }

    return new Response("not found", { status: 404 });
  }

  private getPlayer(userId: string): PlayerRow | null {
    const rows = this.ctx.storage.sql
      .exec<PlayerRow>("SELECT id, name, code, points, avatar FROM players WHERE id = ?", userId)
      .toArray();
    return rows[0] ?? null;
  }

  private findByCode(code: string): PlayerRow | null {
    if (code.length === 0) return null;
    const rows = this.ctx.storage.sql
      .exec<PlayerRow>("SELECT id, name, code, points, avatar FROM players WHERE code = ?", code)
      .toArray();
    return rows[0] ?? null;
  }

  private getFriends(userId: string): PlayerRow[] {
    return this.ctx.storage.sql
      .exec<PlayerRow>(
        `SELECT p.id, p.name, p.code, p.points, p.avatar
         FROM friends f JOIN players p ON p.id = f.friend_id
         WHERE f.user_id = ?
         ORDER BY p.points DESC, p.name ASC`,
        userId,
      )
      .toArray();
  }

  /** True once both sides hold a friends row — a confirmed clink. */
  private areFriends(userId: string, friendId: string): boolean {
    return (
      this.ctx.storage.sql
        .exec(
          "SELECT 1 FROM friends WHERE (user_id = ? AND friend_id = ?) AND EXISTS (SELECT 1 FROM friends WHERE user_id = ? AND friend_id = ?)",
          userId,
          friendId,
          friendId,
          userId,
        )
        .toArray()
        .length > 0
    );
  }

  private hasRequest(fromId: string, toId: string): boolean {
    return (
      this.ctx.storage.sql
        .exec("SELECT 1 FROM requests WHERE from_id = ? AND to_id = ?", fromId, toId)
        .toArray().length > 0
    );
  }

  /** Seals a friendship in both directions and sweeps away any invites. */
  private confirmFriendship(aId: string, bId: string): void {
    const now = Date.now();
    this.ctx.storage.sql.exec(
      "INSERT OR IGNORE INTO friends (user_id, friend_id, created_at) VALUES (?, ?, ?), (?, ?, ?)",
      aId,
      bId,
      now,
      bId,
      aId,
      now,
    );
    this.ctx.storage.sql.exec(
      "DELETE FROM requests WHERE (from_id = ? AND to_id = ?) OR (from_id = ? AND to_id = ?)",
      aId,
      bId,
      bId,
      aId,
    );
  }

  /** Pending invites toward ("in") or away from ("out") this player. */
  private getRequests(userId: string, direction: "in" | "out"): PlayerRow[] {
    const joinColumn = direction === "in" ? "r.from_id" : "r.to_id";
    const filterColumn = direction === "in" ? "r.to_id" : "r.from_id";
    return this.ctx.storage.sql
      .exec<PlayerRow>(
        `SELECT p.id, p.name, p.code, p.points, p.avatar
         FROM requests r JOIN players p ON p.id = ${joinColumn}
         WHERE ${filterColumn} = ?
         ORDER BY r.created_at ASC`,
        userId,
      )
      .toArray();
  }

  /**
   * Returns the player's row, creating one with a fresh friend code if needed.
   * A null avatar means the reader has no mark; undefined keeps the stored one.
   */
  private ensurePlayer(userId: string, name: string, avatar?: string | null): PlayerRow {
    const existing = this.getPlayer(userId);
    if (existing != null) {
      const nextAvatar = avatar === undefined ? existing.avatar : avatar;
      if (existing.name !== name || existing.avatar !== nextAvatar) {
        this.savePlayer(userId, name, existing.points, nextAvatar);
        return { ...existing, name, avatar: nextAvatar };
      }
      return existing;
    }
    for (let attempt = 0; attempt < 5; attempt++) {
      const code = this.newCode();
      try {
        this.ctx.storage.sql.exec(
          "INSERT INTO players (id, name, code, points, avatar, updated_at) VALUES (?, ?, ?, 0, ?, ?)",
          userId,
          name,
          code,
          avatar ?? null,
          Date.now(),
        );
        return { id: userId, name, code, points: 0, avatar: avatar ?? null };
      } catch {
        // A code collision is vanishingly rare; draw another and retry.
      }
    }
    throw new Error("could not allocate a friend code");
  }

  private savePlayer(
    userId: string,
    name: string,
    points: number,
    avatar: string | null,
  ): void {
    this.ctx.storage.sql.exec(
      `INSERT INTO players (id, name, code, points, avatar, updated_at)
       VALUES (?, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         name = excluded.name,
         points = MAX(players.points, excluded.points),
         avatar = excluded.avatar,
         updated_at = excluded.updated_at`,
      userId,
      name,
      this.getPlayer(userId)?.code ?? this.newCode(),
      sanitizePoints(points),
      avatar ?? null,
      Date.now(),
    );
  }

  private newCode(): string {
    const bytes = new Uint8Array(6);
    crypto.getRandomValues(bytes);
    return Array.from(bytes, (b) => CODE_ALPHABET[b % CODE_ALPHABET.length]).join("");
  }
}
