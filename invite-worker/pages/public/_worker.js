const ROOM_PATH = /^\/(?:[A-Z0-9]{8}|[A-Z0-9]{4}-[A-Z0-9]{4})\/?$/i;

export default {
  fetch(request, env) {
    const url = new URL(request.url);
    if (ROOM_PATH.test(url.pathname)) {
      url.pathname = '/j' + url.pathname;
      return env.INVITES.fetch(new Request(url, request));
    }
    return env.INVITES.fetch(request);
  },
};
