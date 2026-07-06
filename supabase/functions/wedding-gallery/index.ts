import { createClient } from "npm:@supabase/supabase-js@2";

const bucket = "wedding-media";
const corsHeaders = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, content-type, x-upload-token",
  "access-control-allow-methods": "GET, POST, OPTIONS",
};

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? Deno.env.get("WEDDING_SERVICE_ROLE_KEY") ?? "";
const uploadToken = Deno.env.get("WEDDING_UPLOAD_TOKEN") ?? "";
const sessionSecret = Deno.env.get("WEDDING_SESSION_SECRET") ?? "";
const sitePublicUrl = Deno.env.get("WEDDING_SITE_URL") ?? "";

const supabase = createClient(supabaseUrl, serviceRoleKey, {
  auth: { persistSession: false },
});

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const url = new URL(req.url);
    const path = normalizePath(url.pathname);

    if (req.method === "POST" && path === "/api/upload") return upload(req);
    if (req.method === "POST" && path === "/api/login") return login(req);
    if (req.method === "GET" && path === "/api/photos") return photos(req);
    if (req.method === "GET" && path.startsWith("/api/photo/")) return photo(req, path);
    if (req.method === "GET" && sitePublicUrl) return redirectToSite(req, path);
    if (req.method === "GET") return html();

    return json({ error: "Not found" }, 404);
  } catch (error) {
    return json({ error: error instanceof Error ? error.message : "Unexpected error" }, 500);
  }
});

function normalizePath(pathname: string): string {
  const stripped = pathname.replace(/^\/wedding-gallery/, "") || "/";
  return stripped.endsWith("/") && stripped !== "/" ? stripped.slice(0, -1) : stripped;
}

async function upload(req: Request): Promise<Response> {
  if (!uploadToken || req.headers.get("x-upload-token") !== uploadToken) {
    return json({ error: "Unauthorized" }, 401);
  }

  const form = await req.formData();
  const file = form.get("file");
  if (!(file instanceof File)) return json({ error: "Missing file" }, 400);

  const id = crypto.randomUUID();
  const safeName = sanitizeFilename(file.name || "photo.jpg");
  const ext = safeName.includes(".") ? safeName.split(".").pop() : "jpg";
  const storagePath = `${new Date().toISOString().slice(0, 10)}/${id}.${ext}`;
  const bytes = await file.arrayBuffer();
  const mimeType = file.type || mimeTypeFor(safeName);

  const uploaded = await supabase.storage
    .from(bucket)
    .upload(storagePath, bytes, { contentType: mimeType, upsert: false });
  if (uploaded.error) throw uploaded.error;

  const inserted = await supabase
    .from("wedding_media")
    .insert({
      id,
      storage_path: storagePath,
      filename: safeName,
      mime_type: mimeType,
      size_bytes: bytes.byteLength,
      event_name: String(form.get("event") || "Charles & Jessica Hartmann Wedding"),
    })
    .select("id")
    .single();
  if (inserted.error) throw inserted.error;

  const origin = new URL(req.url).origin;
  const basePath = new URL(req.url).pathname.startsWith("/wedding-gallery") ? "/wedding-gallery" : "";
  const pageUrl = sitePublicUrl
    ? `${sitePublicUrl}?photo=${encodeURIComponent(id)}`
    : `${origin}${basePath}/photo/${id}`;
  return json({ id, url: pageUrl });
}

function redirectToSite(req: Request, path: string): Response {
  const target = new URL(sitePublicUrl);
  if (path.startsWith("/photo/")) {
    target.searchParams.set("photo", decodeURIComponent(path.replace("/photo/", "")));
  }
  return Response.redirect(target.toString(), 302);
}

async function login(req: Request): Promise<Response> {
  const body = await req.json().catch(() => ({}));
  const password = String(body.password ?? "");
  const config = await readConfig(["password_salt", "password_hash"]);
  const salt = config.password_salt ?? Deno.env.get("WEDDING_PASSWORD_SALT") ?? "";
  const hash = config.password_hash ?? Deno.env.get("WEDDING_PASSWORD_HASH") ?? "";
  if (!salt || !hash || !sessionSecret) return json({ error: "Gallery auth is not configured" }, 500);
  if ((await sha256(`${salt}:${password}`)) !== hash) return json({ error: "Incorrect password" }, 401);
  return json({ token: await signSession() });
}

async function photos(req: Request): Promise<Response> {
  const token = bearerToken(req);
  if (!(await verifySession(token))) return json({ error: "Unauthorized" }, 401);

  const result = await supabase
    .from("wedding_media")
    .select("id, filename, mime_type, size_bytes, created_at, storage_path")
    .order("created_at", { ascending: false });
  if (result.error) throw result.error;

  const items = await Promise.all((result.data ?? []).map(async (item) => ({
    id: item.id,
    filename: item.filename,
    mimeType: item.mime_type,
    sizeBytes: item.size_bytes,
    createdAt: item.created_at,
    url: await signedUrl(item.storage_path),
  })));
  return json({ photos: items });
}

async function photo(req: Request, path: string): Promise<Response> {
  const token = bearerToken(req);
  if (!(await verifySession(token))) return json({ error: "Unauthorized" }, 401);

  const id = decodeURIComponent(path.replace("/api/photo/", ""));
  const result = await supabase
    .from("wedding_media")
    .select("id, filename, mime_type, size_bytes, created_at, storage_path")
    .eq("id", id)
    .single();
  if (result.error) return json({ error: "Photo not found" }, 404);

  return json({
    photo: {
      id: result.data.id,
      filename: result.data.filename,
      mimeType: result.data.mime_type,
      sizeBytes: result.data.size_bytes,
      createdAt: result.data.created_at,
      url: await signedUrl(result.data.storage_path),
    },
  });
}

async function readConfig(keys: string[]): Promise<Record<string, string>> {
  const result = await supabase.from("wedding_gallery_config").select("key,value").in("key", keys);
  if (result.error) throw result.error;
  return Object.fromEntries((result.data ?? []).map((row) => [row.key, row.value]));
}

async function signedUrl(path: string): Promise<string> {
  const result = await supabase.storage.from(bucket).createSignedUrl(path, 60 * 30);
  if (result.error) throw result.error;
  return result.data.signedUrl;
}

function bearerToken(req: Request): string {
  const auth = req.headers.get("authorization") ?? "";
  return auth.toLowerCase().startsWith("bearer ") ? auth.slice(7) : "";
}

async function signSession(): Promise<string> {
  const expires = Math.floor(Date.now() / 1000) + 60 * 60 * 12;
  const payload = btoa(JSON.stringify({ expires }));
  const sig = await hmac(payload, sessionSecret);
  return `${payload}.${sig}`;
}

async function verifySession(token: string): Promise<boolean> {
  const [payload, sig] = token.split(".");
  if (!payload || !sig || (await hmac(payload, sessionSecret)) !== sig) return false;
  const parsed = JSON.parse(atob(payload));
  return Number(parsed.expires) > Math.floor(Date.now() / 1000);
}

async function hmac(value: string, secret: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(value));
  return hex(sig);
}

async function sha256(value: string): Promise<string> {
  return hex(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)));
}

function hex(buffer: ArrayBuffer): string {
  return [...new Uint8Array(buffer)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

function sanitizeFilename(name: string): string {
  return name.replace(/[^a-zA-Z0-9._-]/g, "_").slice(0, 100) || "photo.jpg";
}

function mimeTypeFor(name: string): string {
  const lower = name.toLowerCase();
  if (lower.endsWith(".png")) return "image/png";
  if (lower.endsWith(".gif")) return "image/gif";
  if (lower.endsWith(".mp4")) return "video/mp4";
  if (lower.endsWith(".webm")) return "video/webm";
  return "image/jpeg";
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: responseHeaders("application/json; charset=utf-8"),
  });
}

function html(): Response {
  return new Response(svgPage, {
    status: 200,
    headers: responseHeaders("image/svg+xml"),
  });
}

function responseHeaders(contentType: string): Headers {
  const headers = new Headers();
  Object.entries(corsHeaders).forEach(([key, value]) => headers.set(key, value));
  headers.set("content-type", contentType);
  headers.set("content-disposition", "inline");
  headers.set("cache-control", "no-store");
  return headers;
}

const xhtmlPage = `<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
<head>
  <title>Charles &amp; Jessica Hartmann Wedding Gallery</title>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <style type="text/css"><![CDATA[
    :root { --ink:#241719; --rose:#a84b5b; --gold:#b9894a; --paper:#fff9f4; --line:#eadfd6; }
    * { box-sizing: border-box; }
    body { margin:0; font-family: Inter, ui-sans-serif, system-ui, -apple-system, Segoe UI, sans-serif; color:var(--ink); background:var(--paper); }
    .hero { min-height:52vh; display:grid; align-items:end; padding:40px clamp(20px, 6vw, 80px); color:white; background:linear-gradient(rgba(36,23,25,.20), rgba(36,23,25,.62)), url('https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=1800&q=80') center/cover; }
    .date { color:#ffd9a5; font-weight:800; text-transform:uppercase; letter-spacing:.13em; font-size:13px; }
    h1 { margin:8px 0 0; font-family: Georgia, serif; font-size:clamp(44px, 8vw, 104px); line-height:.92; letter-spacing:0; }
    .hero p { max-width:760px; font-size:clamp(18px, 2.4vw, 28px); margin:16px 0 0; }
    main { padding:28px clamp(16px, 5vw, 64px) 64px; }
    .lock, .single { max-width:560px; margin:44px auto; background:white; border:1px solid var(--line); border-radius:8px; padding:24px; box-shadow:0 18px 60px rgba(36,23,25,.08); }
    .single { max-width:1000px; }
    input, button, .download { font:inherit; border-radius:8px; }
    input { width:100%; border:1px solid #d8c9bd; padding:14px 16px; margin:12px 0; }
    button, .download { border:0; background:var(--rose); color:white; padding:12px 16px; font-weight:800; cursor:pointer; text-decoration:none; display:inline-flex; justify-content:center; }
    .bar { display:flex; gap:16px; align-items:center; justify-content:space-between; margin-bottom:24px; flex-wrap:wrap; }
    .bar h2 { margin:4px 0 0; font-family: Georgia, serif; font-size:38px; }
    .grid { display:grid; grid-template-columns:repeat(auto-fill, minmax(220px, 1fr)); gap:14px; }
    .tile { background:white; border:1px solid var(--line); border-radius:8px; overflow:hidden; box-shadow:0 10px 30px rgba(36,23,25,.06); }
    .tile img, .tile video, .single img, .single video { width:100%; display:block; background:#eee; }
    .tile img, .tile video { aspect-ratio:4/3; object-fit:cover; }
    .single img, .single video { max-height:72vh; object-fit:contain; border-radius:6px; background:#111; }
    .tile footer { padding:12px; display:flex; justify-content:space-between; align-items:center; gap:12px; }
    .error { color:#8f1d2c; font-weight:700; }
    .hidden { display:none; }
  ]]></style>
</head>
<body>
  <section class="hero">
    <div>
      <div class="date">August 22, 2026</div>
      <h1>Charles &amp; Jessica Hartmann</h1>
      <p>Wedding photobooth gallery</p>
    </div>
  </section>
  <main>
    <div id="lock" class="lock">
      <h2>Enter the gallery password</h2>
      <input id="password" type="password" autocomplete="current-password" placeholder="Password" />
      <button id="unlock">Unlock gallery</button>
      <p id="error" class="error"></p>
    </div>
    <section id="gallery" class="hidden">
      <div class="bar"><div><div class="date">Wedding Gallery</div><h2>Photo Downloads</h2></div><button id="refresh">Refresh</button></div>
      <div id="items" class="grid"></div>
    </section>
    <section id="single" class="single hidden"></section>
  </main>
  <script type="text/javascript"><![CDATA[
    const tokenKey = 'hartmannGalleryToken';
    const apiBase = location.pathname.startsWith('/wedding-gallery') ? '/wedding-gallery' : '';
    const token = () => sessionStorage.getItem(tokenKey) || '';
    const photoId = new URLSearchParams(location.search).get('photo') || location.pathname.match(/\\/photo\\/([^/]+)/)?.[1];
    const lock = document.getElementById('lock');
    const gallery = document.getElementById('gallery');
    const single = document.getElementById('single');
    document.getElementById('unlock').onclick = unlock;
    document.getElementById('password').onkeydown = (e) => { if (e.key === 'Enter') unlock(); };
    document.getElementById('refresh').onclick = load;
    if (token()) load();
    async function unlock() {
      const password = document.getElementById('password').value;
      const res = await fetch(apiBase + '/api/login', { method:'POST', headers:{'content-type':'application/json'}, body:JSON.stringify({password}) });
      const data = await res.json();
      if (!res.ok) { document.getElementById('error').textContent = data.error || 'Try again'; return; }
      sessionStorage.setItem(tokenKey, data.token);
      await load();
    }
    async function load() {
      lock.classList.add('hidden');
      if (photoId) return loadSingle(photoId);
      gallery.classList.remove('hidden');
      const res = await fetch(apiBase + '/api/photos', { headers:{ authorization:'Bearer ' + token() } });
      if (res.status === 401) { lock.classList.remove('hidden'); gallery.classList.add('hidden'); return; }
      const data = await res.json();
      document.getElementById('items').innerHTML = (data.photos || []).map(card).join('') || '<p>No photos yet.</p>';
    }
    async function loadSingle(id) {
      const res = await fetch(apiBase + '/api/photo/' + encodeURIComponent(id), { headers:{ authorization:'Bearer ' + token() } });
      if (res.status === 401) { lock.classList.remove('hidden'); return; }
      const { photo } = await res.json();
      single.classList.remove('hidden');
      single.innerHTML = media(photo) + '<p><a class="download" download="download" href="' + photo.url + '">Download photo</a></p>';
    }
    function card(p) { return '<article class="tile">' + media(p) + '<footer><span>' + new Date(p.createdAt).toLocaleDateString() + '</span><a class="download" download="download" href="' + p.url + '">Download</a></footer></article>'; }
    function media(p) { return p.mimeType.startsWith('video/') ? '<video controls="controls" src="' + p.url + '"></video>' : '<img alt="Wedding photobooth photo" src="' + p.url + '" />'; }
  ]]></script>
</body>
</html>`;

const svgPage = `<svg xmlns="http://www.w3.org/2000/svg" width="1440" height="1600" viewBox="0 0 1440 1600">
  <foreignObject width="1440" height="1600">
    <div xmlns="http://www.w3.org/1999/xhtml">
      <style>
        :root { --ink:#241719; --rose:#a84b5b; --gold:#b9894a; --paper:#fff9f4; --line:#eadfd6; }
        * { box-sizing: border-box; }
        body, .page { margin:0; min-height:1600px; font-family: Inter, ui-sans-serif, system-ui, -apple-system, Segoe UI, sans-serif; color:var(--ink); background:var(--paper); }
        .hero { min-height:520px; display:grid; align-items:end; padding:56px 80px; color:white; background:linear-gradient(rgba(36,23,25,.20), rgba(36,23,25,.62)), url('https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&amp;fit=crop&amp;w=1800&amp;q=80') center/cover; }
        .date { color:#ffd9a5; font-weight:800; text-transform:uppercase; letter-spacing:.13em; font-size:18px; }
        h1 { margin:12px 0 0; font-family: Georgia, serif; font-size:96px; line-height:.92; letter-spacing:0; }
        .hero p { max-width:760px; font-size:30px; margin:20px 0 0; }
        main { padding:36px 72px 80px; }
        .lock, .single { max-width:620px; margin:44px auto; background:white; border:1px solid var(--line); border-radius:8px; padding:28px; box-shadow:0 18px 60px rgba(36,23,25,.08); }
        .single { max-width:1100px; }
        input, button, .download { font:inherit; border-radius:8px; }
        input { width:100%; border:1px solid #d8c9bd; padding:16px 18px; margin:14px 0; font-size:20px; }
        button, .download { border:0; background:var(--rose); color:white; padding:14px 18px; font-weight:800; cursor:pointer; text-decoration:none; display:inline-flex; justify-content:center; font-size:18px; }
        .bar { display:flex; gap:16px; align-items:center; justify-content:space-between; margin-bottom:24px; flex-wrap:wrap; }
        .bar h2 { margin:4px 0 0; font-family: Georgia, serif; font-size:42px; }
        .grid { display:grid; grid-template-columns:repeat(auto-fill, minmax(250px, 1fr)); gap:16px; }
        .tile { background:white; border:1px solid var(--line); border-radius:8px; overflow:hidden; box-shadow:0 10px 30px rgba(36,23,25,.06); }
        .tile img, .tile video, .single img, .single video { width:100%; display:block; background:#eee; }
        .tile img, .tile video { aspect-ratio:4/3; object-fit:cover; }
        .single img, .single video { max-height:800px; object-fit:contain; border-radius:6px; background:#111; }
        .tile footer { padding:12px; display:flex; justify-content:space-between; align-items:center; gap:12px; }
        .error { color:#8f1d2c; font-weight:700; }
        .hidden { display:none; }
      </style>
      <div class="page">
        <section class="hero">
          <div>
            <div class="date">August 22, 2026</div>
            <h1>Charles &amp; Jessica Hartmann</h1>
            <p>Wedding photobooth gallery</p>
          </div>
        </section>
        <main>
          <div id="lock" class="lock">
            <h2>Enter the gallery password</h2>
            <input id="password" type="password" autocomplete="current-password" placeholder="Password" />
            <button id="unlock">Unlock gallery</button>
            <p id="error" class="error"></p>
          </div>
          <section id="gallery" class="hidden">
            <div class="bar"><div><div class="date">Wedding Gallery</div><h2>Photo Downloads</h2></div><button id="refresh">Refresh</button></div>
            <div id="items" class="grid"></div>
          </section>
          <section id="single" class="single hidden"></section>
        </main>
      </div>
      <script>
        const tokenKey = 'hartmannGalleryToken';
        const apiBase = '/functions/v1/wedding-gallery';
        const token = () =&gt; sessionStorage.getItem(tokenKey) || '';
        const photoId = new URLSearchParams(location.search).get('photo') || location.pathname.match(/\\/photo\\/([^/]+)/)?.[1];
        const lock = document.getElementById('lock');
        const gallery = document.getElementById('gallery');
        const single = document.getElementById('single');
        document.getElementById('unlock').onclick = unlock;
        document.getElementById('password').onkeydown = (e) =&gt; { if (e.key === 'Enter') unlock(); };
        document.getElementById('refresh').onclick = load;
        if (token()) load();
        async function unlock() {
          const password = document.getElementById('password').value;
          const res = await fetch(apiBase + '/api/login', { method:'POST', headers:{'content-type':'application/json'}, body:JSON.stringify({password}) });
          const data = await res.json();
          if (!res.ok) { document.getElementById('error').textContent = data.error || 'Try again'; return; }
          sessionStorage.setItem(tokenKey, data.token);
          await load();
        }
        async function load() {
          lock.classList.add('hidden');
          if (photoId) return loadSingle(photoId);
          gallery.classList.remove('hidden');
          const res = await fetch(apiBase + '/api/photos', { headers:{ authorization:'Bearer ' + token() } });
          if (res.status === 401) { lock.classList.remove('hidden'); gallery.classList.add('hidden'); return; }
          const data = await res.json();
          document.getElementById('items').innerHTML = (data.photos || []).map(card).join('') || '&lt;p&gt;No photos yet.&lt;/p&gt;';
        }
        async function loadSingle(id) {
          const res = await fetch(apiBase + '/api/photo/' + encodeURIComponent(id), { headers:{ authorization:'Bearer ' + token() } });
          if (res.status === 401) { lock.classList.remove('hidden'); return; }
          const { photo } = await res.json();
          single.classList.remove('hidden');
          single.innerHTML = media(photo) + '&lt;p&gt;&lt;a class="download" download="download" href="' + photo.url + '"&gt;Download photo&lt;/a&gt;&lt;/p&gt;';
        }
        function card(p) { return '&lt;article class="tile"&gt;' + media(p) + '&lt;footer&gt;&lt;span&gt;' + new Date(p.createdAt).toLocaleDateString() + '&lt;/span&gt;&lt;a class="download" download="download" href="' + p.url + '"&gt;Download&lt;/a&gt;&lt;/footer&gt;&lt;/article&gt;'; }
        function media(p) { return p.mimeType.startsWith('video/') ? '&lt;video controls="controls" src="' + p.url + '"&gt;&lt;/video&gt;' : '&lt;img alt="Wedding photobooth photo" src="' + p.url + '" /&gt;'; }
      </script>
    </div>
  </foreignObject>
</svg>`;

const page = `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Charles & Jessica Hartmann Wedding Gallery</title>
  <style>
    :root { color-scheme: light; --ink:#201719; --rose:#a84b5b; --gold:#ba8a4a; --mist:#f7f3ef; --paper:#fffaf6; }
    * { box-sizing: border-box; }
    body { margin:0; font-family: Inter, ui-sans-serif, system-ui, -apple-system, Segoe UI, sans-serif; color:var(--ink); background:var(--paper); }
    .hero { min-height:54vh; display:grid; align-items:end; padding:40px clamp(20px, 6vw, 80px); background:linear-gradient(rgba(32,23,25,.18), rgba(32,23,25,.55)), url('https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=1800&q=80') center/cover; color:white; }
    .hero h1 { margin:0; font-family: Georgia, serif; font-size:clamp(48px, 8vw, 112px); line-height:.9; letter-spacing:0; }
    .hero p { max-width:760px; font-size:clamp(18px, 2.4vw, 28px); margin:18px 0 0; }
    main { padding:28px clamp(16px, 5vw, 64px) 64px; }
    .bar { display:flex; gap:16px; align-items:center; justify-content:space-between; margin-bottom:24px; flex-wrap:wrap; }
    .date { color:var(--rose); font-weight:800; text-transform:uppercase; letter-spacing:.12em; font-size:13px; }
    .lock { max-width:440px; margin:48px auto; background:white; border:1px solid #eadfd6; border-radius:8px; padding:24px; box-shadow:0 18px 60px rgba(32,23,25,.08); }
    input, button { font:inherit; border-radius:8px; }
    input { width:100%; border:1px solid #d8c9bd; padding:14px 16px; margin:12px 0; }
    button, .download { border:0; background:var(--rose); color:white; padding:12px 16px; font-weight:800; cursor:pointer; text-decoration:none; display:inline-flex; justify-content:center; border-radius:8px; }
    .grid { display:grid; grid-template-columns:repeat(auto-fill, minmax(220px, 1fr)); gap:14px; }
    .tile { background:white; border:1px solid #eadfd6; border-radius:8px; overflow:hidden; box-shadow:0 10px 30px rgba(32,23,25,.06); }
    .tile img, .tile video { width:100%; aspect-ratio:4/3; object-fit:cover; display:block; background:#eee; }
    .tile footer { padding:12px; display:flex; justify-content:space-between; align-items:center; gap:12px; }
    .single { max-width:1000px; margin:0 auto; background:white; border:1px solid #eadfd6; border-radius:8px; padding:16px; }
    .single img, .single video { width:100%; max-height:72vh; object-fit:contain; background:#111; border-radius:6px; }
    .error { color:#8f1d2c; font-weight:700; }
    .hidden { display:none; }
  </style>
</head>
<body>
  <section class="hero">
    <div>
      <div class="date">August 22, 2026</div>
      <h1>Charles & Jessica Hartmann</h1>
      <p>Wedding photobooth gallery</p>
    </div>
  </section>
  <main>
    <div id="lock" class="lock">
      <h2>Enter the gallery password</h2>
      <input id="password" type="password" autocomplete="current-password" placeholder="Password" />
      <button id="unlock">Unlock gallery</button>
      <p id="error" class="error"></p>
    </div>
    <section id="gallery" class="hidden">
      <div class="bar"><div><div class="date">Wedding Gallery</div><h2>Photo Downloads</h2></div><button id="refresh">Refresh</button></div>
      <div id="items" class="grid"></div>
    </section>
    <section id="single" class="single hidden"></section>
  </main>
  <script>
    const tokenKey = 'hartmannGalleryToken';
    const apiBase = location.pathname.startsWith('/wedding-gallery') ? '/wedding-gallery' : '';
    const token = () => sessionStorage.getItem(tokenKey) || '';
    const photoId = location.pathname.match(/\\/photo\\/([^/]+)/)?.[1];
    const lock = document.getElementById('lock');
    const gallery = document.getElementById('gallery');
    const single = document.getElementById('single');
    document.getElementById('unlock').onclick = unlock;
    document.getElementById('password').onkeydown = (e) => { if (e.key === 'Enter') unlock(); };
    document.getElementById('refresh').onclick = load;
    if (token()) load();
    async function unlock() {
      const password = document.getElementById('password').value;
      const res = await fetch(apiBase + '/api/login', { method:'POST', headers:{'content-type':'application/json'}, body:JSON.stringify({password}) });
      const data = await res.json();
      if (!res.ok) { document.getElementById('error').textContent = data.error || 'Try again'; return; }
      sessionStorage.setItem(tokenKey, data.token);
      await load();
    }
    async function load() {
      lock.classList.add('hidden');
      if (photoId) return loadSingle(photoId);
      gallery.classList.remove('hidden');
      const res = await fetch(apiBase + '/api/photos', { headers:{ authorization:'Bearer ' + token() } });
      if (res.status === 401) { lock.classList.remove('hidden'); gallery.classList.add('hidden'); return; }
      const data = await res.json();
      document.getElementById('items').innerHTML = (data.photos || []).map(card).join('') || '<p>No photos yet.</p>';
    }
    async function loadSingle(id) {
      const res = await fetch(apiBase + '/api/photo/' + encodeURIComponent(id), { headers:{ authorization:'Bearer ' + token() } });
      if (res.status === 401) { lock.classList.remove('hidden'); return; }
      const { photo } = await res.json();
      single.classList.remove('hidden');
      single.innerHTML = media(photo) + '<p><a class="download" download href="' + photo.url + '">Download photo</a></p>';
    }
    function card(p) { return '<article class="tile">' + media(p) + '<footer><span>' + new Date(p.createdAt).toLocaleDateString() + '</span><a class="download" download href="' + p.url + '">Download</a></footer></article>'; }
    function media(p) { return p.mimeType.startsWith('video/') ? '<video controls src="' + p.url + '"></video>' : '<img alt="Wedding photobooth photo" src="' + p.url + '" />'; }
  </script>
</body>
</html>`;
