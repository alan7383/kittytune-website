# Listen Together invite previews

The Cloudflare Worker sends browsers to the session on GitHub Pages. When a link preview service
requests the invite, the Worker uses the invite code to read the room's playback state from the
same MQTT brokers as the apps. The preview shows the host, track, artwork, progress, and
listener count. It displays up to four avatars.

Browser Rendering creates a PNG from a signed snapshot in the image URL. Cloudflare caches the
image for repeated requests. The banner at `together/invite.png` covers unavailable sessions
and rendering failures.

## Where it runs

Short links use `https://kittytune.pages.dev/XNW9-BMKZ`. The Pages function forwards invites
and image requests to the same Worker through a service binding.

The apps' `TogetherLinks.inviteUrl` uses
`https://kittytune-invite.kt-net.workers.dev/?together=XNW9-BMKZ&from=<name>`.
The `/j/<code>`, `/join/<code>`, and `/together/<code>` paths also accept invites.

## Redeploy after a change

```sh
cd invite-worker
npm ci
npx wrangler login
npm run deploy
```

For a new Worker, set `CARD_SECRET` to a random secret with `npx wrangler secret put CARD_SECRET`.
The `BROWSER` binding is defined in `wrangler.toml`.

## Deploy the short invite address

From `invite-worker/`, run `npm run deploy:pages`. This deploys `pages/` to the
existing Cloudflare Pages project named `kittytune`.

## Check a preview

Use the code of an active room to inspect the personalized Open Graph tags and image URL.

```sh
curl -A Discordbot "https://kittytune.pages.dev/XNW9-BMKZ"
```
