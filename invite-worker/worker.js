/**
 * KittyTune invite links with a live preview.
 *
 *   https://<worker>/?together=XNW9-BMKZ&from=Alan
 *     link-preview bots -> a card with the host, the song playing, who is listening, the session code
 *     a browser         -> 302 to the session on the website
 *
 * GitHub Pages serves one HTML for every link and previews never run scripts, so the personal card is
 * made here. The session is read from the public MQTT brokers exactly as the website reads it (the code
 * is the key), and the picture is drawn by Cloudflare Browser Rendering from src/card.js, then cached.
 * When the session cannot be read the static banner is used.
 */
import puppeteer from '@cloudflare/puppeteer';
import { fetchSession } from './src/session.js';
import { cardHtml, esc } from './src/card.js';

const SITE = 'https://alan7383.github.io/kittytune-website/';
const STATIC_IMAGE = SITE + 'together/invite.png';
const BOTS = /discordbot|twitterbot|facebookexternalhit|slackbot|telegrambot|whatsapp|linkedinbot|embedly|skypeuripreview|vkshare|redditbot|applebot|iframely|bluesky|mastodon|signal|google-pagerenderer/i;

/** "XNW9-BMKZ", or null when it is not an 8-character room code. */
function cleanCode(raw) {
  const code = String(raw || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
  return code.length === 8 ? code.slice(0, 4) + '-' + code.slice(4) : null;
}

function cleanText(raw, max) {
  return [...String(raw || '').replace(/[\u0000-\u001f\u007f]/g, '').trim()].slice(0, max).join('');
}

function cleanUrl(raw) {
  const url = String(raw || '').trim();
  return /^https:\/\/[^\s"'<>]+$/.test(url) && url.length < 400 ? url : '';
}

function b64url(bytes) {
  let s = '';
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function unb64url(text) {
  const s = atob(text.replace(/-/g, '+').replace(/_/g, '/'));
  return Uint8Array.from(s, (c) => c.charCodeAt(0));
}

/** Image URLs carry the display data from the Worker, signed to prevent direct changes to the payload. */
async function sign(secret, data) {
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  return b64url(new Uint8Array(await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(data)))).slice(0, 22);
}

function cardData(state, code, fromName) {
  const track = state.track;
  let artwork = cleanUrl(track?.artworkUrl);
  if (/^https:\/\/[^/]+\.sndcdn\.com\//i.test(artwork)) artwork = artwork.replace('-large.', '-t500x500.');
  const listeners = Array.isArray(state.listeners) ? state.listeners : [];
  const duration = Number(track?.durationMs);
  const position = Number(state.positionMs);
  const durationMs = Number.isFinite(duration) ? Math.max(0, duration) : 0;
  return {
    host: { name: cleanText(state.hostName, 32) || fromName || 'A friend', avatarUrl: cleanUrl(state.hostAvatarUrl) },
    title: cleanText(track?.title, 90),
    artist: cleanText(track?.artist, 60) || cleanText(track?.username, 60),
    artwork,
    listeners: listeners.slice(0, 4).map((listener) => ({
      name: cleanText(listener?.name, 24),
      avatarUrl: cleanUrl(listener?.avatarUrl),
    })),
    listenerCount: Math.max(1, listeners.length),
    code,
    isPlaying: state.isPlaying === true,
    positionMs: Number.isFinite(position) ? Math.min(Math.max(0, position), durationMs) : 0,
    durationMs,
  };
}

function page({ title, description, image, target }) {
  return `<!DOCTYPE html>
<html lang="en"><head>
<meta charset="UTF-8">
<title>${esc(title)}</title>
<meta name="theme-color" content="#8dabff">
<meta property="og:type" content="website">
<meta property="og:site_name" content="KittyTune · Listen Together">
<meta property="og:title" content="${esc(title)}">
<meta property="og:description" content="${esc(description)}">
<meta property="og:url" content="${esc(target)}">
<meta property="og:image" content="${esc(image)}">
<meta property="og:image:type" content="image/png">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:image:alt" content="KittyTune Listen Together invitation">
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="${esc(title)}">
<meta name="twitter:description" content="${esc(description)}">
<meta name="twitter:image" content="${esc(image)}">
<meta http-equiv="refresh" content="0; url=${esc(target)}">
</head><body><a href="${esc(target)}">Join the session</a></body></html>`;
}

async function invite(request, env, url) {
  const match = url.pathname.match(/^\/(?:j|join|together)\/([A-Za-z0-9-]{8,12})\/?$/);
  const code = cleanCode(match ? match[1] : url.searchParams.get('together'));
  const from = cleanText(url.searchParams.get('from'), 32);
  const target = new URL('together/', SITE);
  if (code) target.searchParams.set('together', code);
  if (from) target.searchParams.set('from', from);

  if (!BOTS.test(request.headers.get('user-agent') || '')) {
    return new Response(null, { status: 302, headers: { location: target.toString(), vary: 'User-Agent' } });
  }

  const state = code ? await fetchSession(code).catch(() => null) : null;
  let image = STATIC_IMAGE;
  let title = `🎧 ${from || 'A friend'} invited you to a listening party`;
  let description = code ? `Join session ${code} on KittyTune.` : 'Join a listening party on KittyTune.';

  if (state) {
    const data = cardData(state, code, from);
    if (env.CARD_SECRET && env.BROWSER) {
      const encoded = b64url(new TextEncoder().encode(JSON.stringify(data)));
      image = `${url.origin}/card.png?d=${encoded}&s=${await sign(env.CARD_SECRET, encoded)}`;
    }
    const playback = data.title
      ? `${data.isPlaying ? 'Now playing' : 'Paused'}: ${data.title}${data.artist ? ` · ${data.artist}` : ''}`
      : 'Waiting for a track';
    title = `🎧 ${data.host.name} invited you to listen along`;
    description = `${playback}. ${data.listenerCount} listening · Session ${code}.`;
  }

  return new Response(page({ title, description, image, target: target.toString() }), {
    headers: {
      'content-type': 'text/html; charset=utf-8',
      'cache-control': 'public, max-age=60',
      'x-robots-tag': 'noindex',
      vary: 'User-Agent',
    },
  });
}

async function card(env, ctx, url) {
  const cache = caches.default;
  const cacheKey = url.toString();
  const cached = await cache.match(cacheKey);
  if (cached) return cached;

  const encoded = url.searchParams.get('d') || '';
  if (!env.CARD_SECRET || !encoded || url.searchParams.get('s') !== (await sign(env.CARD_SECRET, encoded))) {
    return Response.redirect(STATIC_IMAGE, 302);
  }
  let data;
  try {
    data = JSON.parse(new TextDecoder().decode(unb64url(encoded)));
  } catch {
    return Response.redirect(STATIC_IMAGE, 302);
  }

  let browser;
  try {
    browser = await puppeteer.launch(env.BROWSER);
    const tab = await browser.newPage();
    await tab.setViewport({ width: 1200, height: 630 });
    await tab.setContent(cardHtml(data), { waitUntil: 'networkidle0', timeout: 8000 }).catch(() => {});
    await tab.evaluate(() => document.fonts.ready).catch(() => {});
    const png = await tab.screenshot({ type: 'png' });
    const response = new Response(png, {
      headers: { 'content-type': 'image/png', 'cache-control': 'public, max-age=86400, immutable' },
    });
    ctx.waitUntil(cache.put(cacheKey, response.clone()));
    return response;
  } catch {
    // Out of the day's browser time, or the browser failed: the static banner still says it all.
    return Response.redirect(STATIC_IMAGE, 302);
  } finally {
    if (browser) ctx.waitUntil(browser.close().catch(() => {}));
  }
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (url.pathname === '/card.png') return card(env, ctx, url);
    return invite(request, env, url);
  },
};
