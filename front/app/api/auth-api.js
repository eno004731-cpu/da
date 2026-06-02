import {
  ACCOUNT_DELETE_ENABLED,
  API_BASE_URL,
  AUTH_API_BASE_URL,
  AUTH_USES_DEDICATED_SERVICE,
  AUTH_USES_SAME_BACKEND,
  ENDPOINTS,
  GOOGLE_AUTH_ENABLED,
  LOCAL_AUTH_ONLY_MODE,
} from "./endpoints.js?v=20260528a";
import { request } from "./http-client.js?v=20260525a";
import { getPendingGoogleCompletion, getSession } from "../state/auth-store.js?v=20260525a";

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
  return {
    user,
    accessToken: null,
    refreshToken: null,
    tokenType: "Bearer",
    expiresIn: null,
  };
}

function readOptionalBoolean(...candidates) {
  for (const candidate of candidates) {
    if (typeof candidate === "boolean") {
      return candidate;
    }

    if (typeof candidate === "string") {
      const normalized = candidate.trim().toLowerCase();
      if (["true", "1", "yes", "on"].includes(normalized)) {
        return true;
      }

      if (["false", "0", "no", "off"].includes(normalized)) {
        return false;
      }
    }
  }

  return null;
}

function normalizeStringArray(value) {
  if (!Array.isArray(value)) {
    return null;
  }

  const items = value
    .map((item) => String(item || "").trim())
    .filter(Boolean);

  return items.length ? items : null;
}

function normalizeUser(user = {}) {
  const authProviders = normalizeStringArray(
    user.authProviders ?? user.providers ?? user.linkedAuthProviders
  );
  const authProvider = String(
    user.authProvider ?? user.oauthProvider ?? authProviders?.[0] ?? ""
  ).trim();

  return {
    id: String(user.id ?? ""),
    fullName: user.fullName || user.name || "",
    email: user.email || "",
    phone: user.phone || null,
    companyName: user.companyName || null,
    role: user.role || "CLIENT",
    emailVerified: readOptionalBoolean(
      user.emailVerified,
      user.verifiedEmail,
      user.isEmailVerified
    ),
    authProvider: authProvider || null,
    authProviders,
    isOAuthUser: readOptionalBoolean(
      user.isOAuthUser,
      user.oauthUser,
      authProvider ? true : null
    ),
    hasPassword: readOptionalBoolean(
      user.hasPassword,
      user.passwordConfigured,
      user.localPasswordConfigured
    ),
    needsPasswordSetup: readOptionalBoolean(
      user.needsPasswordSetup,
      user.requiresPasswordSetup,
      user.passwordSetupRequired
    ),
    requiresProfileCompletion: readOptionalBoolean(
      user.requiresProfileCompletion,
      user.profileCompletionRequired
    ),
  };
}

function createSessionFromAuthResponse(payload = {}) {
  const user = normalizeUser(payload.user || {});
  return {
    user,
    accessToken: payload.accessToken || null,
    refreshToken: payload.refreshToken || null,
    tokenType: payload.tokenType || "Bearer",
    expiresIn: Number(payload.expiresIn ?? 0) || null,
  };
}

function createGoogleCompletionResult(payload = {}) {
  return {
    type: "google_completion_required",
    flowToken: payload.flowToken || "",
    profile: normalizeUser({
      ...(payload.profile || {}),
      isOAuthUser: true,
      authProvider: "google",
      authProviders: ["google"],
      hasPassword: false,
      needsPasswordSetup: true,
      requiresProfileCompletion: true,
    }),
  };
}

function isAuthResponsePayload(payload = {}) {
  return Boolean(payload?.accessToken && payload?.user);
}

function isGoogleCompletionPayload(payload = {}) {
  return typeof payload?.flowToken === "string" && payload.flowToken.trim().length > 0;
}

function getNestedAuthResponsePayload(payload = {}) {
  const candidates = [payload?.auth, payload?.authResponse, payload?.authResponce];

  for (const candidate of candidates) {
    if (isAuthResponsePayload(candidate)) {
      return candidate;
    }
  }

  return null;
}

function getNestedGoogleCompletionPayload(payload = {}) {
  const candidates = [payload?.googleResponse, payload?.googleResponce];

  for (const candidate of candidates) {
    if (isGoogleCompletionPayload(candidate)) {
      return candidate;
    }
  }

  return null;
}

function parseGoogleLoginResponse(payload = {}) {
  // Вариант 1: backend сразу вернул обычный AuthResponce в корне JSON.
  if (isAuthResponsePayload(payload)) {
    return {
      type: "authenticated",
      session: createSessionFromAuthResponse(payload),
    };
  }

  // Вариант 2: backend вернул обёртку с nested auth/authResponce.
  const nestedAuthPayload = getNestedAuthResponsePayload(payload);
  if (nestedAuthPayload) {
    return {
      type: "authenticated",
      session: createSessionFromAuthResponse(nestedAuthPayload),
    };
  }

  // Вариант 3: backend вернул только completion DTO в корне JSON.
  if (isGoogleCompletionPayload(payload)) {
    return createGoogleCompletionResult(payload);
  }

  // Вариант 4: backend завернул completion DTO в googleResponse/googleResponce.
  const nestedGooglePayload = getNestedGoogleCompletionPayload(payload);
  if (nestedGooglePayload) {
    return createGoogleCompletionResult(nestedGooglePayload);
  }

  return null;
}

function parseGoogleCompletionResponse(payload = {}) {
  // После completion backend может вернуть либо обычный AuthResponce,
  // либо обёртку со status/auth.
  if (isAuthResponsePayload(payload)) {
    return createSessionFromAuthResponse(payload);
  }

  const nestedAuthPayload = getNestedAuthResponsePayload(payload);
  if (nestedAuthPayload) {
    return createSessionFromAuthResponse(nestedAuthPayload);
  }

  return null;
}

function shouldUseDevFallback(error) {
  // Dev fallback нужен только для локальной разработки,
  // когда endpoint ещё не реализован или backend не запущен.
  // Продовые 5xx/502 не прячем, чтобы не маскировать реальные сбои.
  const status = typeof error === "object" && error !== null ? error.status : undefined;
  return typeof status === "undefined" || [404, 405].includes(status);
}

function usesSessionAuthBackend() {
  return AUTH_USES_SAME_BACKEND;
}

function usesDedicatedAuthService() {
  return AUTH_USES_DEDICATED_SERVICE;
}

function allowsLocalMockAuthFallback() {
  return !LOCAL_AUTH_ONLY_MODE;
}

function buildAuthReadOptions() {
  return {
    baseUrl: AUTH_API_BASE_URL,
    includeCredentials: usesSessionAuthBackend(),
    disableCsrf: true,
    // Для session-auth backend не нужен Bearer header из localStorage.
    // Для JWT auth-service он как раз нужен, чтобы /auth/me работал после reload.
    useAuth: usesDedicatedAuthService(),
  };
}

function buildAuthWriteOptions() {
  return {
    baseUrl: AUTH_API_BASE_URL,
    includeCredentials: usesSessionAuthBackend(),
    // Старый monolith на Spring Session требует CSRF header.
    // Отдельный JWT auth-service работает stateless и без CSRF.
    disableCsrf: usesDedicatedAuthService(),
    useAuth: usesDedicatedAuthService(),
  };
}

export function isUnauthorizedError(error) {
  const status = typeof error === "object" && error !== null ? error.status : undefined;
  return status === 401 || status === 403;
}

export function isBackendUnavailableError(error) {
  const status = typeof error === "object" && error !== null ? error.status : undefined;
  return typeof status === "undefined" || [502, 503, 504].includes(status);
}

function createFeatureDisabledError(message, code = "AUTH_FEATURE_DISABLED") {
  const error = new Error(message);
  error.code = code;
  return error;
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
    isOAuthUser: false,
    hasPassword: true,
    needsPasswordSetup: false,
    requiresProfileCompletion: false,
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
    isOAuthUser: Boolean(user.isOAuthUser),
    hasPassword: user.hasPassword ?? true,
    needsPasswordSetup: user.needsPasswordSetup ?? false,
    requiresProfileCompletion: user.requiresProfileCompletion ?? false,
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

function updateCurrentUserProfileLocally(payload) {
  const session = getSession();
  const currentUser = session?.user;

  if (!currentUser) {
    throw new Error("Нельзя обновить профиль без активной локальной сессии.");
  }

  const fullName = String(payload.fullName || "").trim();
  const email = String(payload.email || "").trim().toLowerCase();
  const password = String(payload.password || "");

  if (!fullName || !email) {
    throw new Error("Для завершения регистрации нужны имя и email.");
  }

  const users = readMockUsers();
  const emailTakenByAnotherUser = users.some(
    (user) => user.id !== currentUser.id && user.email.toLowerCase() === email
  );

  if (emailTakenByAnotherUser) {
    throw new Error("Пользователь с таким email уже существует в локальной dev-заглушке.");
  }

  const nextUsers = users.map((user) => {
    if (user.id !== currentUser.id) {
      return user;
    }

    return {
      ...user,
      fullName,
      email,
      password: password || user.password,
      hasPassword: password ? true : user.hasPassword ?? true,
      needsPasswordSetup: false,
      requiresProfileCompletion: false,
    };
  });
  writeMockUsers(nextUsers);

  return normalizeUser({
    ...currentUser,
    fullName,
    email,
    hasPassword: password ? true : currentUser.hasPassword ?? true,
    needsPasswordSetup: false,
    requiresProfileCompletion: false,
  });
}

async function withDevFallback(requestFn, fallbackFn, { enabled = true } = {}) {
  try {
    return await requestFn();
  } catch (error) {
    if (!enabled) {
      throw error;
    }

    if (!shouldUseDevFallback(error)) {
      throw error;
    }

    return fallbackFn();
  }
}

async function fetchCurrentUserFromBackend() {
  const user = await request(ENDPOINTS.auth.me, {
    method: "GET",
    ...buildAuthReadOptions(),
  });

  return user ? normalizeUser(user) : null;
}

export function checkBackendAvailability() {
  if (usesSessionAuthBackend()) {
    return request(ENDPOINTS.auth.csrf, {
      method: "GET",
      ...buildAuthReadOptions(),
      useAuth: false,
    });
  }

  return request(ENDPOINTS.auth.me, {
    method: "GET",
    ...buildAuthReadOptions(),
  });
}

export function loginClient(payload) {
  return withDevFallback(
    async () => {
      const response = await request(ENDPOINTS.auth.login, {
        method: "POST",
        json: true,
        body: JSON.stringify(payload),
        ...buildAuthWriteOptions(),
        useAuth: false,
      });

      if (response === true) {
        const user = await fetchCurrentUserFromBackend();
        return createSessionFromUser(normalizeUser(user));
      }

      if (response?.accessToken && response?.user) {
        return createSessionFromAuthResponse(response);
      }

      throw new Error("Backend вернул неподдерживаемый ответ логина.");
    },
    () => loginClientLocally(payload),
    { enabled: allowsLocalMockAuthFallback() }
  );
}

export function registerClient(payload) {
  return withDevFallback(
    async () => {
      const response = await request(ENDPOINTS.auth.register, {
        method: "POST",
        json: true,
        body: JSON.stringify(payload),
        ...buildAuthWriteOptions(),
        useAuth: false,
      });

      if (response === true) {
        const user = await fetchCurrentUserFromBackend();
        return createSessionFromUser(normalizeUser(user));
      }

      if (response?.accessToken && response?.user) {
        return createSessionFromAuthResponse(response);
      }

      throw new Error("Backend вернул неподдерживаемый ответ регистрации.");
    },
    () => registerClientLocally(payload),
    { enabled: allowsLocalMockAuthFallback() }
  );
}

export async function loginWithGoogle(credential) {
  if (!GOOGLE_AUTH_ENABLED) {
    throw createFeatureDisabledError(
      "Вход через Google отключён текущими runtime-флагами фронтенда.",
      "AUTH_GOOGLE_DISABLED"
    );
  }

  try {
    const response = await request(ENDPOINTS.auth.googleLogin, {
      method: "POST",
      json: true,
      body: JSON.stringify({ credential }),
      ...buildAuthWriteOptions(),
      useAuth: false,
    });

    const parsedResponse = parseGoogleLoginResponse(response);
    if (parsedResponse) {
      return parsedResponse;
    }

    throw new Error("Backend вернул неподдерживаемый ответ Google auth.");
  } catch (error) {
    const status = typeof error === "object" && error !== null ? error.status : undefined;
    if ([404, 405].includes(status)) {
      const endpointError = new Error("Backend ещё не реализовал Google auth flow.");
      endpointError.status = status;
      throw endpointError;
    }

    throw error;
  }
}

export function completeGoogleRegistration(payload) {
  return request(ENDPOINTS.auth.googleComplete, {
    method: "POST",
    json: true,
    body: JSON.stringify(payload),
    ...buildAuthWriteOptions(),
    useAuth: false,
  }).then((response) => {
    const session = parseGoogleCompletionResponse(response);
    if (session) {
      return session;
    }

    throw new Error("Backend вернул неподдерживаемый ответ завершения Google регистрации.");
  });
}

export function requestEmailVerification() {
  return request(ENDPOINTS.auth.emailVerificationRequest, {
    method: "POST",
    json: true,
    body: JSON.stringify({}),
    ...buildAuthWriteOptions(),
  });
}

export function logoutClient() {
  const session = getSession();
  const body = session?.refreshToken ? { refreshToken: session.refreshToken } : undefined;

  return withDevFallback(
    () => request(ENDPOINTS.auth.logout, {
      method: "POST",
      json: true,
      body: body ? JSON.stringify(body) : undefined,
      ...buildAuthWriteOptions(),
    }),
    () => ({ success: true })
  );
}

export function fetchCurrentUser() {
  return withDevFallback(
    () => fetchCurrentUserFromBackend(),
    () => normalizeUser(getSession()?.user || {}),
    { enabled: allowsLocalMockAuthFallback() }
  );
}

export function updateCurrentUserProfile(payload) {
  return withDevFallback(
    async () => {
      const response = await request(ENDPOINTS.auth.updateMe, {
        method: "PATCH",
        json: true,
        body: JSON.stringify(payload),
        ...buildAuthWriteOptions(),
      });

      if (!response) {
        throw new Error("Backend вернул пустой ответ на обновление профиля.");
      }

      return normalizeUser(response);
    },
    () => updateCurrentUserProfileLocally(payload),
    { enabled: allowsLocalMockAuthFallback() }
  );
}

export function getPendingGoogleProfileDraft() {
  const pendingCompletion = getPendingGoogleCompletion();
  return pendingCompletion?.profile ? normalizeUser(pendingCompletion.profile) : null;
}

export function deleteClientAccount() {
  if (!ACCOUNT_DELETE_ENABLED) {
    throw createFeatureDisabledError(
      "Удаление аккаунта пока не реализовано в локальном auth-service.",
      "AUTH_DELETE_ACCOUNT_DISABLED"
    );
  }

  return withDevFallback(
    () => request(ENDPOINTS.auth.deleteAccount, {
      method: "DELETE",
      ...buildAuthWriteOptions(),
    }),
    () => deleteClientLocally(),
    { enabled: allowsLocalMockAuthFallback() }
  );
}

export function shouldPreserveClientSessionOnOrdersUnauthorized() {
  // Когда auth живёт отдельно от backend заявок, 401 на /client/orders
  // ещё не означает, что пользователь разлогинен в auth-service.
  return usesDedicatedAuthService();
}
