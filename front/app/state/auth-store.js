const SESSION_STORAGE_KEY = "philosophy-business-client-session";

function safeParseSession(raw) {
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch (_error) {
    return null;
  }
}

export function getSession() {
  return safeParseSession(window.localStorage.getItem(SESSION_STORAGE_KEY));
}

export function setSession(session) {
  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
  window.dispatchEvent(new CustomEvent("auth:changed", { detail: session }));
}

export function clearSession() {
  window.localStorage.removeItem(SESSION_STORAGE_KEY);
  window.dispatchEvent(new CustomEvent("auth:changed", { detail: null }));
}

export function getAccessToken() {
  return getSession()?.accessToken || null;
}

export function getCurrentUser() {
  return getSession()?.user || null;
}

export function isAuthenticated() {
  return Boolean(getAccessToken());
}

export function buildAuthUrl(mode = "login", next = "./cabinet.html", options = {}) {
  const params = new URLSearchParams({
    mode,
    next,
  });

  if (options.switchAccount) {
    params.set("switch", "1");
  }

  return `./auth.html?${params.toString()}`;
}

export function requireAuth(next = "./cabinet.html") {
  if (isAuthenticated()) {
    return true;
  }

  window.location.href = buildAuthUrl("login", next);
  return false;
}
