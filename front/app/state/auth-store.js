const SESSION_STORAGE_KEY = "philosophy-business-client-session";
const COMPLETE_PROFILE_PATH = "./complete-profile.html";
const GOOGLE_COMPLETION_STORAGE_KEY = "philosophy-business-google-completion";

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

export function updateSessionUser(user) {
  const currentSession = getSession();

  // Обновляем только профиль пользователя после /auth/me,
  // но не теряем access/refresh token, нужные для следующих запросов.
  setSession({
    ...(currentSession || {}),
    user,
    accessToken: currentSession?.accessToken || null,
    refreshToken: currentSession?.refreshToken || null,
    tokenType: currentSession?.tokenType || "Bearer",
    expiresIn: currentSession?.expiresIn ?? null,
  });
}

export function clearSession() {
  window.localStorage.removeItem(SESSION_STORAGE_KEY);
  window.dispatchEvent(new CustomEvent("auth:changed", { detail: null }));
}

export function getPendingGoogleCompletion() {
  return safeParseSession(window.sessionStorage.getItem(GOOGLE_COMPLETION_STORAGE_KEY));
}

export function setPendingGoogleCompletion(state) {
  window.sessionStorage.setItem(GOOGLE_COMPLETION_STORAGE_KEY, JSON.stringify(state));
}

export function clearPendingGoogleCompletion() {
  window.sessionStorage.removeItem(GOOGLE_COMPLETION_STORAGE_KEY);
}

export function getCurrentUser() {
  return getSession()?.user || null;
}

export function isAuthenticated() {
  return Boolean(getCurrentUser());
}

function hasTextValue(value) {
  return typeof value === "string" && value.trim().length > 0;
}

export function isProfileIncomplete(user = getCurrentUser()) {
  if (!user) {
    return false;
  }

  return !hasTextValue(user.fullName) || !hasTextValue(user.email);
}

export function shouldRequirePasswordCompletion(user = getCurrentUser()) {
  if (!user) {
    return false;
  }

  if (user.needsPasswordSetup === true) {
    return true;
  }

  const hasOAuthIdentity =
    user.isOAuthUser === true ||
    Boolean(user.authProvider) ||
    (Array.isArray(user.authProviders) && user.authProviders.length > 0);

  return hasOAuthIdentity && user.hasPassword === false;
}

export function requiresProfileCompletion(user = getCurrentUser()) {
  if (!user) {
    return false;
  }

  return user.requiresProfileCompletion === true || isProfileIncomplete(user) || shouldRequirePasswordCompletion(user);
}

export function buildCompleteProfileUrl(next = "./cabinet.html") {
  const params = new URLSearchParams({ next });
  return `${COMPLETE_PROFILE_PATH}?${params.toString()}`;
}

export function resolvePostAuthUrl(user, next = "./cabinet.html") {
  return requiresProfileCompletion(user) ? buildCompleteProfileUrl(next) : next;
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
