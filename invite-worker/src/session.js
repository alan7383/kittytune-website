/**
 * Reads a Listen Together session the way the website does: subscribe to the room on the public MQTT
 * brokers, ask the host for the state, and open what comes back with the key the code derives.
 * Mirrors TogetherWire (apps) and the relay in index.html (site).
 */

const BROKERS = ['https://broker.emqx.io:8084/mqtt', 'https://broker.hivemq.com:8884/mqtt'];
const SNAPSHOT_MAX_AGE_MS = 60_000; // Matches Together's stale-state cutoff.
const enc = new TextEncoder();
const dec = new TextDecoder();

async function sha256(text) {
  return new Uint8Array(await crypto.subtle.digest('SHA-256', enc.encode(text)));
}

function hex(bytes) {
  return [...bytes].map((b) => b.toString(16).padStart(2, '0')).join('');
}

function varInt(n) {
  const out = [];
  do {
    let b = n % 128;
    n = Math.floor(n / 128);
    if (n > 0) b |= 128;
    out.push(b);
  } while (n > 0);
  return out;
}

function packet(header, body) {
  return new Uint8Array([header, ...varInt(body.length), ...body]);
}

function str(s) {
  const b = enc.encode(s);
  return [(b.length >> 8) & 255, b.length & 255, ...b];
}

const connect = (id) => packet(0x10, [...str('MQTT'), 4, 2, 0, 30, ...str(id)]);
const subscribe = (id, topic) => packet(0x82, [(id >> 8) & 255, id & 255, ...str(topic), 0]);
const publish = (topic, payload) => packet(0x30, [...str(topic), ...payload]);

/**
 * The latest room state, or null if nobody responds and there is no recent snapshot.
 */
export async function fetchSession(code, timeoutMs = 4000) {
  const clean = String(code).toUpperCase().replace(/[^A-Z0-9]/g, '');
  if (clean.length !== 8) return null;
  const topic = 'kittytune/together/' + hex(await sha256('topic:' + clean)).slice(0, 32);
  const snap = 'kittytune/together/snap/' + hex(await sha256('snap:' + clean)).slice(0, 32) + '/state';
  const key = await crypto.subtle.importKey('raw', await sha256('key:kittytune-together:' + clean), { name: 'AES-GCM' }, false, ['encrypt', 'decrypt']);
  const me = 'preview-' + crypto.randomUUID().slice(0, 8);

  async function open(bytes) {
    if (bytes.length <= 12) return null;
    try {
      const plain = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: bytes.slice(0, 12), tagLength: 128 }, key, bytes.slice(12));
      return JSON.parse(dec.decode(plain));
    } catch {
      return null;
    }
  }

  async function seal(obj) {
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const ct = new Uint8Array(await crypto.subtle.encrypt({ name: 'AES-GCM', iv, tagLength: 128 }, key, enc.encode(JSON.stringify(obj))));
    const out = new Uint8Array(12 + ct.length);
    out.set(iv);
    out.set(ct, 12);
    return out;
  }

  const sockets = [];
  const controller = new AbortController();
  let snapshot = null;
  let liveState = null;
  let resolveLive;
  const live = new Promise((r) => (resolveLive = r));

  function consider(msg, retained) {
    if (controller.signal.aborted || liveState || msg?.type !== 'State' || msg.from === me) return;
    if (!retained) {
      liveState = msg;
      resolveLive();
    } else if (Math.abs(Date.now() - msg.sentAt) <= SNAPSHOT_MAX_AGE_MS &&
               (!snapshot || msg.sentAt > snapshot.sentAt)) {
      snapshot = msg;
    }
  }

  for (const [index, url] of BROKERS.entries()) {
    (async () => {
      const resp = await fetch(url, {
        headers: { Upgrade: 'websocket', 'Sec-WebSocket-Protocol': 'mqtt' },
        signal: controller.signal,
      });
      const ws = resp.webSocket;
      if (!ws) return;
      ws.binaryType = 'arraybuffer';
      ws.accept();
      if (controller.signal.aborted) {
        ws.close();
        return;
      }
      sockets.push(ws);
      let buf = new Uint8Array(0);
      ws.addEventListener('message', async (event) => {
        if (!(event.data instanceof ArrayBuffer)) return;
        const chunk = new Uint8Array(event.data);
        const merged = new Uint8Array(buf.length + chunk.length);
        merged.set(buf);
        merged.set(chunk, buf.length);
        buf = merged;
        while (buf.length >= 2) {
          let mult = 1, len = 0, pos = 1, byte;
          do {
            if (pos > 4) {
              ws.close(1002, 'Invalid MQTT packet');
              return;
            }
            if (pos >= buf.length) return;
            byte = buf[pos++];
            len += (byte & 127) * mult;
            mult *= 128;
          } while (byte & 128);
          if (buf.length < pos + len) return;
          const head = buf[0];
          const body = buf.slice(pos, pos + len);
          buf = buf.slice(pos + len);
          const type = head & 0xf0;
          if (type === 0x20 && body[1] === 0) {
            ws.send(subscribe(1, topic));
            ws.send(subscribe(2, snap));
          } else if (type === 0x90 && body[1] === 1 && body[2] === 0) {
            const query = await seal({ from: me, sentAt: Date.now(), type: 'SyncRequest' });
            if (ws.readyState === WebSocket.OPEN) ws.send(publish(topic, query));
          } else if (type === 0x30 && body.length > 2) {
            const qos = (head >> 1) & 3;
            const tLen = (body[0] << 8) | body[1];
            const name = dec.decode(body.slice(2, 2 + tLen));
            if (name !== topic && name !== snap) continue;
            const payload = body.slice(2 + tLen + (qos ? 2 : 0));
            const retained = (head & 1) === 1 || name === snap;
            consider(await open(payload), retained);
          }
        }
      });
      ws.send(connect(`${me}-${index}`));
    })().catch(() => {});
  }

  const timeout = setTimeout(resolveLive, timeoutMs);
  await live;
  clearTimeout(timeout);
  controller.abort();
  for (const ws of sockets) {
    try { ws.close(); } catch {}
  }
  return liveState || snapshot;
}
