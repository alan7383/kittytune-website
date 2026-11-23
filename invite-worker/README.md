# Personal invite previews

Invite links normally point to `together/` on GitHub Pages, whose preview says
"You're invited to a listening party" for everyone. GitHub Pages cannot change that card per link
(previews never run scripts), so this Cloudflare Worker serves a card that names the host and the code:

> **🎧 Alan invited you to a listening party**
> Session XNW9-BMKZ is live on KittyTune. Tap to join…

Bots that build previews (Discord, WhatsApp, Telegram, Slack, X…) get that card; people are redirected
to the session on the site. Nothing is stored or logged.

## Where it runs

Deployed at **https://kittytune-invite.kt-net.workers.dev** (Cloudflare Workers, free plan). The apps'
`TogetherLinks.inviteUrl` builds `https://kittytune-invite.kt-net.workers.dev/?together=XNW9-BMKZ&from=<name>`,
the same `?together=` shape older versions already parse.

## Redeploy after a change

```sh
cd invite-worker
npx wrangler login   # once per machine
npx wrangler deploy
```

## Check a preview

```sh
curl -A Discordbot "https://kittytune-invite.kt-net.workers.dev/?together=XNW9-BMKZ&from=Alan"
```
