/**
 * KittyTune invite links with a personal preview.
 *
 * GitHub Pages serves the same HTML for every link and link previews never run scripts, so the static
 * invite page can only say "you're invited". This worker answers link-preview bots with a card that
 * names the host and the session code, and sends everyone else straight to the session on the site.
 *
 *   https://<worker>/j/XNW9-BMKZ?from=Alan
 *     Discord/WhatsApp/Telegram/... -> "🎧 Alan invited you to a listening party" · "Session XNW9-BMKZ is live"
 *     a browser                      -> 302 to https://alan7383.github.io/kittytune-website/together/?together=XNW9-BMKZ
 *
 * Nothing is stored or logged. The code is only a room name; joining still needs the app or the site.
 */
const SITE = 'https://alan7383.github.io/kittytune-website/';
const IMAGE = SITE + 'together/invite.png';
const BOTS = /discordbot|twitterbot|facebookexternalhit|slackbot|telegrambot|whatsapp|linkedinbot|embedly|skypeuripreview|vkshare|redditbot|applebot|iframely|bluesky|mastodon|signal/i;

function escapeHtml(value) {
  return value.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** "XNW9-BMKZ", or null when it is not an 8-character room code. */
function cleanCode(raw) {
  const code = String(raw || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
  return code.length === 8 ? code.slice(0, 4) + '-' + code.slice(4) : null;
}

/** A display name, trimmed and without control characters; empty when absent. */
function cleanName(raw) {
  return String(raw || '').replace(/[\u0000-\u001f\u007f]/g, '').trim().slice(0, 32);
}

function card({ code, from, target }) {
  const who = from ? escapeHtml(from) : 'A friend';
  const title = `🎧 ${who} invited you to a listening party`;
  const description = code
    ? `Session ${code} is live on KittyTune. Tap to join: same song, same second, lyrics in sync. No account, no ads.`
    : 'Join a live KittyTune session: same song, same second, lyrics in sync. No account, no ads.';
  return `<!DOCTYPE html>
<html lang="en"><head>
<meta charset="UTF-8">
<title>${title}</title>
<meta name="theme-color" content="#8fb2ff">
<meta property="og:type" content="website">
<meta property="og:site_name" content="KittyTune · Listen Together">
<meta property="og:title" content="${title}">
<meta property="og:description" content="${description}">
<meta property="og:url" content="${escapeHtml(target)}">
<meta property="og:image" content="${IMAGE}">
<meta property="og:image:type" content="image/png">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:image:alt" content="KittyTune invitation to a live listening party">
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="${title}">
<meta name="twitter:description" content="${description}">
<meta name="twitter:image" content="${IMAGE}">
<meta http-equiv="refresh" content="0; url=${escapeHtml(target)}">
</head><body><a href="${escapeHtml(target)}">Join the session</a></body></html>`;
}

export default {
  async fetch(request) {
    const url = new URL(request.url);
    const match = url.pathname.match(/^\/(?:j|join|together)\/([A-Za-z0-9-]{8,12})\/?$/);
    const code = cleanCode(match ? match[1] : url.searchParams.get('together'));
    const from = cleanName(url.searchParams.get('from'));

    const target = new URL('together/', SITE);
    if (code) target.searchParams.set('together', code);
    if (from) target.searchParams.set('from', from);

    const agent = request.headers.get('user-agent') || '';
    if (!BOTS.test(agent)) return Response.redirect(target.toString(), 302);

    return new Response(card({ code, from, target: target.toString() }), {
      headers: {
        'content-type': 'text/html; charset=utf-8',
        'cache-control': 'public, max-age=300',
        'x-robots-tag': 'noindex',
      },
    });
  },
};
