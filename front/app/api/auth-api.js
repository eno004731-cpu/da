import { ENDPOINTS } from "./endpoints.js?v=20260510b";
import { jsonRequest } from "./http-client.js?v=20260510b";
import { getSession } from "../state/auth-store.js?v=20260510b";

const MOCK_USERS_STORAGE_KEY = "philosophy-business-mock-users";

function readMockUsers() {
  try {
    return JSON.parse(window.localStorage.getItem(MOCK_USERS_STORAGE_KEY) || "[]");
  } catch (_error) {
    return [];
  }
}

function writeMockUsers(users) {
  window.localStorage.setItem(MOCK_USERS_STORAGE_KEY, JSON.stringify(users));
}

function createMockSession(user) {
  return createSessionFromUser(user);
}

function createSessionFromUser(user) {
  return { user };
}

function shouldUseDevFallback(error) {
  // Dev fallback нужен только для локальной разработки,
  // когда endpoint ещё не реализован или backend не запущен.
  // Продовые 5xx/502 не прячем, чтобы не маскировать реальные сбои.
  const status = typeof error === "object" && error !== null ? error.status : undefined;
  return typeof status === "undefined" || [404, 405].includes(status);
}

export function isUnauthorizedError(error) {
  const status = typeof error === "object" && error !== null ? error.status : undefined;
  return status === 401 || status === 403;
}

export function isBackendUnavailableError(error) {
  const status = typeof error === "object" && error !== null ? error.status : undefined;
  return typeof status === "undefined" || [502, 503, 504].includes(status);
}

function registerClientLocally(payload) {
  const users = readMockUsers();
  const email = String(payload.email || "").trim().toLowerCase();

  const alreadyExists = users.some((user) => user.email.toLowerCase() === email);
  if (alreadyExists) {
    throw new Error("Пользователь с таким email уже зарегистрирован в локальной dev-заглушке.");
  }

  const user = {
    id: String(Date.now()),
    fullName: payload.fullName,
    email,
    phone: payload.phone || "",
    companyName: payload.companyName || "",
    role: "CLIENT",
  };

  users.push({
    ...user,
    password: payload.password,
  });
  writeMockUsers(users);

  return createMockSession(user);
}

function loginClientLocally(payload) {
  const users = readMockUsers();
  const email = String(payload.email || "").trim().toLowerCase();
  const password = String(payload.password || "");

  const user = users.find((item) => item.email.toLowerCase() === email);
  if (!user || user.password !== password) {
    throw new Error("Не удалось войти. Проверь email и пароль в локальной dev-заглушке.");
  }

  return createMockSession({
    id: user.id,
    fullName: user.fullName,
    email: user.email,
    phone: user.phone,
    companyName: user.companyName,
    role: user.role,
  });
}

function deleteClientLocally() {
  const session = getSession();
  const currentUser = session?.user;

  if (!currentUser) {
    return { success: true };
  }

  const users = readMockUsers();
  const nextUsers = users.filter((user) => user.id !== currentUser.id);
  writeMockUsers(nextUsers);

  return { success: true };
}

async function withDevFallback(requestFn, fallbackFn) {
  try {
    return await requestFn();
  } catch (error) {
    if (!shouldUseDevFallback(error)) {
      throw error;
    }

    return fallbackFn();
  }
}

async function fetchCurrentUserFromBackend() {
  return jsonRequest(ENDPOINTS.auth.me, {
    method: "GET",
  });
}

export function checkBackendAvailability() {
  return jsonRequest(ENDPOINTS.auth.csrf, {
    method: "GET",
  });
}

/**
 * Session-based login contract:
 * backend returns boolean success, then frontend loads current user via GET /auth/me.
 */
export function loginClient(payload) {
  return withDevFallback(
    async () => {
      const success = await jsonRequest(ENDPOINTS.auth.login, {
        method: "POST",
        body: payload,
      });

      if (success !== true) {
        throw new Error("Backend не подтвердил вход.");
      }

      const user = await fetchCurrentUserFromBackend();
      return createSessionFromUser(user);
    },
    () => loginClientLocally(payload)
  );
}

export function registerClient(payload) {
  return withDevFallback(
    async () => {
      const success = await jsonRequest(ENDPOINTS.auth.register, {
        method: "POST",
        body: payload,
      });

      if (success !== true) {
        throw new Error("Backend не подтвердил регистрацию.");
      }

      const user = await fetchCurrentUserFromBackend();
      return createSessionFromUser(user);
    },
    () => registerClientLocally(payload)
  );
}

export function logoutClient() {
  return withDevFallback(
    () => jsonRequest(ENDPOINTS.auth.logout, {
      method: "POST",
    }),
    () => ({ success: true })
  );
}

export function fetchCurrentUser() {
  return withDevFallback(
    () => fetchCurrentUserFromBackend(),
    () => getSession()?.user || null
  );
}

/**
 * Account deletion is session-based:
 * backend resolves the current user from the active server session.
 * Current Spring controller accepts POST /api/auth/account.
 */
export function deleteClientAccount() {
  return withDevFallback(
    () => jsonRequest(ENDPOINTS.auth.deleteAccount, {
      method: "POST",
    }),
    () => deleteClientLocally()
  );
}
