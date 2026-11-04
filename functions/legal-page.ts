// functions/legal-page.ts — the shareable privacy policy and house rules.
//
// Two public document pages, GET /privacy and GET /terms, dressed like the
// deletion page: espresso dark, gold headings, comfortable on a phone. The
// wording is written to match what the app actually does with data — no
// ads, no analytics, no tracking — and links to the self-serve deletion
// page where the privacy promise is kept.

/** The slice of Env this page needs. */
type PageEnv = {
  SUPPORT_EMAIL?: string;
};

/** The house's public contact address; env keeps it overridable. */
const SUPPORT_EMAIL = "help@mentallydelicio.us";

const PATH_PRIVACY = "/privacy";
const PATH_TERMS = "/terms";
const PATH_DELETION = "/account-deletion";

const EFFECTIVE_DATE = "November 4, 2026";

/** Document chrome: same espresso room, wider table, long-form reading. */
function docShell(title: string, body: string, env: PageEnv): string {
  const support = `<p class="support">Questions about any of this? Write to <a href="mailto:${env.SUPPORT_EMAIL ?? SUPPORT_EMAIL}">${env.SUPPORT_EMAIL ?? SUPPORT_EMAIL}</a>.</p>`;
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title>${title} · Soundbar</title>
<style>
  * { box-sizing: border-box; margin: 0; }
  body {
    font-family: Georgia, 'Times New Roman', serif;
    background: #1b1210;
    color: #f3e9dc;
    min-height: 100vh;
    padding: 32px 20px 48px;
  }
  .sheet {
    max-width: 640px;
    margin: 0 auto;
    background: #2a1d18;
    border: 1px solid #4a3226;
    border-radius: 20px;
    padding: 36px 28px;
  }
  h1 { font-size: 26px; font-weight: 600; color: #f3e9dc; }
  .gold { color: #d4a853; }
  .stamp { margin-top: 6px; font-size: 13px; color: #8f7a66; }
  h2 {
    margin-top: 28px;
    font-size: 17px;
    font-weight: 600;
    color: #d4a853;
  }
  p { margin-top: 10px; font-size: 15px; line-height: 1.6; color: #cbb59d; }
  ul { margin: 10px 0 0; padding-left: 22px; }
  li { margin-top: 6px; font-size: 15px; line-height: 1.6; color: #cbb59d; }
  li::marker { color: #d4a853; }
  a { color: #d4a853; }
  .support { margin-top: 30px; padding-top: 18px; border-top: 1px solid #4a3226; }
  .nav { margin-top: 18px; font-size: 14px; }
</style>
</head>
<body>
<div class="sheet">
  <h1>Soundbar <span class="gold">·</span> ${title}</h1>
  <p class="stamp">Effective ${EFFECTIVE_DATE}</p>
  ${body}
  ${support}
  <p class="nav"><a href="${PATH_DELETION}">Delete your account</a> &nbsp;·&nbsp; <a href="${PATH_PRIVACY}">Privacy</a> &nbsp;·&nbsp; <a href="${PATH_TERMS}">House rules</a></p>
</div>
</body>
</html>`;
}

function privacyPage(env: PageEnv): string {
  return docShell(
    "Privacy policy",
    `
    <p>Soundbar is a listening journal with a friends leaderboard, run by a small
    independent house. This policy describes everything the app collects and
    everything it deliberately does not. The short version: we hold only what
    the app needs to work, we sell nothing, we track no one, and you can
    delete everything yourself, without asking us.</p>

    <h2>What we collect</h2>
    <ul>
      <li><strong>Your account.</strong> If you create one, an email address and a
      password — kept by our sign-in provider, Supabase. We also note whether
      your email has been confirmed, and, if you have asked to delete your
      account, the date that request takes effect.</li>
      <li><strong>Your play.</strong> On the leaderboard, a display name you choose, a
      short friend code, your points, an avatar you pick from a fixed
      catalogue, and your friend connections — who has added whom.</li>
      <li><strong>Your shelf.</strong> The drinks, dishes, recipes, and notes you save
      are synced to our servers so they follow you between devices. They
      belong to your account alone.</li>
      <li><strong>Spotify, if you connect it.</strong> Your Spotify permission tokens
      and display name are stored only on your own phone, in the phone's
      encrypted storage. They never touch our servers. Disconnecting Spotify
      erases them.</li>
    </ul>

    <h2>What we never do</h2>
    <ul>
      <li>No advertising, no ad networks, no trackers.</li>
      <li>No analytics — we do not count your screens or your taps.</li>
      <li>We do not sell, rent, or trade personal information. There is
      nothing here to sell it to.</li>
      <li>We do not read your shelf, your notes, or your listening history.</li>
    </ul>

    <h2>Who handles what</h2>
    <p>Account sign-in runs on Supabase (your email and password). The app's
    own services — the shelf sync, the leaderboard, and this website — run on
    Cloudflare. Spotify receives requests only when you connect your account
    and pour something into its player. Nobody else gets anything.</p>

    <h2>Deleting your account</h2>
    <p>You never have to email anyone. Delete your account from the app's
    account page, or from our
    <a href="${PATH_DELETION}">self-serve deletion page</a> — you confirm with a
    link sent to your own inbox. Deletion takes effect 48 hours later, in case
    you change your mind; until then you can undo it from the app or the same
    web page. When it takes effect, your email and password, your leaderboard
    presence, your friend connections, and everything on your shelf are
    removed for good.</p>

    <h2>Children</h2>
    <p>Soundbar is not directed at children under 13, and we do not knowingly
    collect their personal information. If you believe a child has created an
    account, write to us and we will remove it.</p>

    <h2>Changes to this policy</h2>
    <p>If the house changes what it collects, we will update this page and the
    effective date above. If a change is substantial, the app will point to it
    as well.</p>`,
    env
  );
}

function termsPage(env: PageEnv): string {
  return docShell(
    "House rules",
    `
    <p>Welcome to the house. These plain-language terms are the agreement
    between you and Soundbar when you use the app or its website.</p>

    <h2>Using the house</h2>
    <p>You may browse, mix, and pour without an account. If you create one,
    keep your password to yourself and use an email you actually check — the
    house sends sign-in and account links there. You are responsible for what
    happens under your account.</p>

    <h2>Grounds for the room</h2>
    <p>Be a good guest. Don't attempt to break, overload, or sneak into the
    service, don't use it to break the law or anyone else's rights, and don't
    lift the house's name, artwork, or content to pass off as your own. We may
    suspend an account that abuses the house or its guests.</p>

    <h2>Spotify</h2>
    <p>Connecting Spotify is up to you, is governed by Spotify's own terms, and
    queues music through your own Premium account — the house never plays
    anything on your behalf without your say-so. Soundbar is not affiliated
    with Spotify.</p>

    <h2>Points and badges</h2>
    <p>Points, badges, and multipliers are for bragging rights around the
    leaderboard. They have no monetary value, cannot be bought or sold, and
    the house may adjust how they are earned.</p>

    <h2>The service, as poured</h2>
    <p>Soundbar is provided as-is. We work hard to keep the lights on and the
    shelf intact, but we do not promise the service will be uninterrupted or
    error-free, and your data is yours to back up. To the fullest extent the
    law allows, the house is not liable for indirect or consequential damages
    from your use of the app.</p>

    <h2>Closing your tab</h2>
    <p>You can stop using Soundbar whenever you like, and you can delete your
    account yourself — from the app or our
    <a href="${PATH_DELETION}">deletion page</a> — with a 48-hour grace period
    to change your mind. After deletion, the account and its data are gone for
    good. We may also end these terms for an account that breaks the house
    rules.</p>

    <h2>Changes to the rules</h2>
    <p>If the house rules change, the new version goes up on this page with a
    fresh effective date. Keeping the app after that means you're on board
    with the new rules.</p>`,
    env
  );
}

function htmlResponse(body: string): Response {
  return new Response(body, {
    headers: { "Content-Type": "text/html; charset=utf-8" },
  });
}

/**
 * Handles the legal pages' routes; returns null when the request is
 * somebody else's and the normal routing should carry on.
 */
export function handleLegalPage(request: Request, env: PageEnv): Response | null {
  const url = new URL(request.url);

  if (request.method === "GET" && url.pathname === PATH_PRIVACY) {
    return htmlResponse(privacyPage(env));
  }

  if (request.method === "GET" && url.pathname === PATH_TERMS) {
    return htmlResponse(termsPage(env));
  }

  return null;
}
