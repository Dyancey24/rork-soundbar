// functions/deletion-page.ts — the shareable account-deletion request page.
//
// A public page (GET /account-deletion) where a user types their account
// email and receives a Supabase magic link. The link lands on the confirm
// page (GET /account-deletion/confirm), which uses the session in its URL
// fragment to offer the same 48-hour-grace deletion — and undo — as the app.
// Unknown addresses get the same "check your inbox" screen as known ones,
// so nobody can learn which emails are registered here.

/** The slice of Env this page needs. */
type PageEnv = {
  SUPABASE_URL?: string;
  EXPO_PUBLIC_SUPABASE_URL?: string;
  SUPABASE_ANON_KEY?: string;
  EXPO_PUBLIC_SUPABASE_ANON_KEY?: string;
  SUPPORT_EMAIL?: string;
};

/** Server-side fallbacks; the publishable key is safe to embed. */
const SUPABASE_URL = "https://lqttmgwoslgksvenpjjl.supabase.co";
const SUPABASE_ANON_KEY = "sb_publishable_1BrAevm9i1dNsh2HpxuhIw_03y6Llct";

const PATH_REQUEST = "/account-deletion";
const PATH_CONFIRM = "/account-deletion/confirm";

function supabaseCreds(env: PageEnv): { url: string; key: string } {
  return {
    url: env.SUPABASE_URL ?? env.EXPO_PUBLIC_SUPABASE_URL ?? SUPABASE_URL,
    key: env.SUPABASE_ANON_KEY ?? env.EXPO_PUBLIC_SUPABASE_ANON_KEY ?? SUPABASE_ANON_KEY,
  };
}

/** Shared chrome: espresso dark, gold accents, warm house voice. */
function pageShell(title: string, body: string, env: PageEnv): string {
  const support = env.SUPPORT_EMAIL
    ? `<p class="support">Trouble signing in? Write to <a href="mailto:${env.SUPPORT_EMAIL}">${env.SUPPORT_EMAIL}</a>.</p>`
    : "";
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
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
  }
  .card {
    max-width: 420px;
    width: 100%;
    background: #2a1d18;
    border: 1px solid #4a3226;
    border-radius: 20px;
    padding: 32px 28px;
  }
  h1 { font-size: 26px; font-weight: 600; color: #f3e9dc; }
  .gold { color: #d4a853; }
  p { font-size: 15px; line-height: 1.55; color: #cbb59d; margin-top: 12px; }
  .warn { color: #e8907e; }
  input {
    width: 100%;
    margin-top: 20px;
    padding: 14px 16px;
    border-radius: 12px;
    border: 1px solid #4a3226;
    background: #1b1210;
    color: #f3e9dc;
    font-size: 16px;
  }
  button {
    width: 100%;
    margin-top: 14px;
    padding: 14px 16px;
    border: none;
    border-radius: 999px;
    background: #d4a853;
    color: #1b1210;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
  }
  button.danger { background: #c4573f; color: #f9ece4; }
  button.ghost { background: none; border: 1px solid #4a3226; color: #cbb59d; margin-top: 10px; }
  .hidden { display: none; }
  .support { margin-top: 24px; font-size: 13px; }
  .support a { color: #d4a853; }
</style>
</head>
<body>
<div class="card">
  <h1>Soundbar <span class="gold">·</span> ${title}</h1>
  ${body}
  ${support}
</div>
</body>
</html>`;
}

/** Step one: the email form. */
function requestPage(env: PageEnv): string {
  return pageShell(
    "Close your account",
    `
    <p>Tell us the account's email and we'll send a link to prove it's yours. Deleting is permanent — your points, badges, and shelf go with it — and it happens 48 hours after you confirm, in case you change your mind.</p>
    <form id="form">
      <input id="email" type="email" autocomplete="email" placeholder="you@example.com" required>
      <button type="submit">Send the link</button>
    </form>
    <div id="sent" class="hidden">
      <p class="gold">The link is on its way.</p>
      <p>Check your inbox — and the spam drawer — and open the newest email from Soundbar.</p>
    </div>
    <p id="error" class="warn hidden">We couldn't send the link just now — give it a minute and try again.</p>
    <script>
      document.getElementById('form').addEventListener('submit', async function (event) {
        event.preventDefault();
        var email = document.getElementById('email').value.trim();
        var button = event.target.querySelector('button');
        button.disabled = true;
        try {
          var response = await fetch('/account-deletion/request', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email })
          });
          // The same outcome whatever the inbox holds behind the address.
          document.getElementById('form').classList.add('hidden');
          document.getElementById('sent').classList.remove('hidden');
        } catch (err) {
          document.getElementById('error').classList.remove('hidden');
          button.disabled = false;
        }
      });
    </script>`,
    env
  );
}

/**
 * Step two: the confirm page. The magic link lands here with the session in
 * the fragment, so the work happens in the reader's browser — the worker
 * itself never holds a token.
 */
function confirmPage(env: PageEnv): string {
  const creds = supabaseCreds(env);
  const apiUrl = JSON.stringify(creds.url + "/auth/v1");
  const apiKey = JSON.stringify(creds.key);
  return pageShell(
    "Close your account",
    `
    <div id="loading"><p class="gold">Checking the link…</p></div>

    <div id="expired" class="hidden">
      <p>This link has already been used or has expired.</p>
      <button class="ghost" onclick="location.href = '/account-deletion'">Request a new link</button>
    </div>

    <div id="ready" class="hidden">
      <p>Signed in as <span class="gold" id="who"></span>.</p>
      <div id="pending" class="hidden">
        <p class="warn" id="pendingLine"></p>
        <p>Changed your mind? Keep the account with one tap — the countdown stops and everything stays.</p>
        <button id="keep">Keep my account</button>
      </div>
      <div id="idle" class="hidden">
        <p class="warn">This permanently deletes your account — your points, badges, and everything on your shelf — and it cannot be undone or recovered.</p>
        <p>Your tab actually closes 48 hours from now. Until then you can change your mind, here or in the app, and keep everything.</p>
        <button class="danger" id="delete">Delete my account</button>
        <button class="ghost" onclick="location.href = '/'">Never mind</button>
      </div>
      <div id="done" class="hidden">
        <p class="gold" id="doneLine"></p>
        <p>You can undo any time before the countdown ends — from the app's account page, or by opening this link's email again.</p>
      </div>
      <p id="trouble" class="warn hidden">That didn't go through — try again in a moment.</p>
    </div>

    <script>
    (async function () {
      var API = ${apiUrl};
      var KEY = ${apiKey};
      var params = new URLSearchParams(location.hash.slice(1));
      var token = params.get('access_token');
      if (!token || params.get('error')) {
        document.getElementById('loading').classList.add('hidden');
        document.getElementById('expired').classList.remove('hidden');
        return;
      }
      var headers = { 'apikey': KEY, 'Authorization': 'Bearer ' + token };
      var user;
      try {
        var response = await fetch(API + '/user', { headers: headers });
        if (!response.ok) throw new Error('expired');
        user = await response.json();
      } catch (err) {
        document.getElementById('loading').classList.add('hidden');
        document.getElementById('expired').classList.remove('hidden');
        return;
      }
      var scheduledAt = Number(user.user_metadata && user.user_metadata.deletion_scheduled_at || 0) || null;
      document.getElementById('who').textContent = user.email;
      document.getElementById('loading').classList.add('hidden');
      document.getElementById('ready').classList.remove('hidden');
      var pending = document.getElementById('pending');
      var idle = document.getElementById('idle');
      function timeLeft() {
        var ms = scheduledAt - Date.now();
        if (ms <= 0) return 'moments from now';
        var minutes = Math.ceil(ms / 60000);
        var hours = Math.floor(minutes / 60);
        if (hours > 0) return hours + ' hr ' + (minutes % 60) + ' min';
        return minutes + ' min';
      }
      async function setScheduled(at) {
        var body = JSON.stringify({ data: { deletion_scheduled_at: at } });
        var response = await fetch(API + '/user', { method: 'PUT', headers: Object.assign({ 'Content-Type': 'application/json' }, headers), body: body });
        if (!response.ok) throw new Error('failed');
        scheduledAt = at;
      }
      function show(section, line) {
        pending.classList.add('hidden');
        idle.classList.add('hidden');
        if (line) document.getElementById('doneLine').textContent = line;
        document.getElementById('done').classList.remove('hidden');
      }
      if (scheduledAt) {
        document.getElementById('pendingLine').textContent =
          'This account is scheduled to be deleted in ' + timeLeft() + ' — permanently.';
        pending.classList.remove('hidden');
      } else {
        idle.classList.remove('hidden');
      }
      document.getElementById('delete').addEventListener('click', async function () {
        this.disabled = true;
        try {
          await setScheduled(Date.now() + 48 * 60 * 60 * 1000);
          show(null, 'Your account will be deleted in 48 hours.');
        } catch (err) {
          this.disabled = false;
          document.getElementById('trouble').classList.remove('hidden');
        }
      });
      document.getElementById('keep').addEventListener('click', async function () {
        this.disabled = true;
        try {
          await setScheduled(null);
          show(null, 'Your account is safe — the deletion was cancelled.');
        } catch (err) {
          this.disabled = false;
          document.getElementById('trouble').classList.remove('hidden');
        }
      });
    })();
    </script>`,
    env
  );
}

function htmlResponse(body: string): Response {
  return new Response(body, {
    headers: { "Content-Type": "text/html; charset=utf-8" },
  });
}

/**
 * Handles the page's own routes; returns null when the request is somebody
 * else's and the normal routing should carry on.
 */
export async function handleDeletionPage(
  request: Request,
  env: PageEnv
): Promise<Response | null> {
  const url = new URL(request.url);

  if (request.method === "GET" && url.pathname === PATH_REQUEST) {
    return htmlResponse(requestPage(env));
  }

  if (request.method === "GET" && url.pathname === PATH_CONFIRM) {
    return htmlResponse(confirmPage(env));
  }

  if (request.method === "POST" && url.pathname === `${PATH_REQUEST}/request`) {
    const body = (await request.json().catch(() => ({}))) as { email?: string };
    const email = (body.email ?? "").trim();
    if (!email || !email.includes("@")) {
      // Same silence as a real send — no hint either way.
      return Response.json({ ok: true });
    }
    const creds = supabaseCreds(env);
    try {
      await fetch(`${creds.url}/auth/v1/otp`, {
        method: "POST",
        headers: { apikey: creds.key, "Content-Type": "application/json" },
        body: JSON.stringify({
          email: email,
          create_user: false,
          options: {
            email_redirect_to: `${url.origin}${PATH_CONFIRM}`,
          },
        }),
      });
    } catch {
      // Fall through to the same response — no enumeration, no error leaks.
    }
    return Response.json({ ok: true });
  }

  return null;
}
