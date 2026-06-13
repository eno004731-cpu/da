import { getSession } from "../state/auth-store.js?v=20260525a";
import { API_BASE_URL } from "./endpoints.js?v=20260525a";

let csrfTokenFromBackend = null;
const CSRF_ENDPOINT_PATH = "/auth/csrf";

function buildUrl(path, baseUrl = API_BASE_URL) {
  return `${baseUrl}${path}`;
}

function readCookie(name) {
  const cookie = document.cookie
    .split("; ")
    .find((item) => item.startsWith(`${name}=`));

  return cookie ? decodeURIComponent(cookie.split("=").slice(1).join("=")) : null;
}

function needsCsrf(method) {
  const safeMethods = ["GET", "HEAD", "OPTIONS", "TRACE"];
  return !safeMethods.includes(String(method).toUpperCase());
}

export function setCsrfToken(token) {
  csrfTokenFromBackend = token || null;
}

async function refreshCsrfToken(baseUrl = API_BASE_URL) {
  const response = await fetch(buildUrl(CSRF_ENDPOINT_PATH, baseUrl), {
    method: "GET",
    credentials: "include",
  });

  const payload = await parseResponse(response);
  const csrfToken =
    payload?.token ||
    readCookie("XSRF-TOKEN") ||
    readCookie("CSRF-TOKEN");

  setCsrfToken(csrfToken);
  return csrfToken;
}

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  const isJson = contentType.includes("application/json");
  const payload = response.status === 204
    ? null
    : isJson
      ? await response.json().catch(() => null)
      : await response.text();

  if (!response.ok) {
    const details =
      isJson && Array.isArray(payload?.details)
        ? payload.details.filter(Boolean)
        : isJson && Array.isArray(payload?.errors)
          ? payload.errors.filter(Boolean)
          : [];
    const detailsMessage = details.join("\n");
    const baseMessage =
      (isJson && payload?.message) ||
      (isJson && payload?.error) ||
      (typeof payload === "string" && payload) ||
      `Request failed with status ${response.status}`;
    const message =
      detailsMessage && baseMessage && baseMessage !== detailsMessage
        ? `${baseMessage}\n${detailsMessage}`
        : detailsMessage || baseMessage;

    const error = new Error(message);
    error.status = response.status;
    error.payload = payload;
    throw error;
  }

  return payload;
}

function createHeaders({ json = false } = {}) {
  const headers = new Headers();

  if (json) {
    headers.set("Content-Type", "application/json");
  }

  return headers;
}

/**
 * Shared HTTP wrapper.
 * Browser cookies are always sent, so protected session endpoints
 * work через JSESSIONID, а не через Bearer token в заголовке.
 */
export async function request(path, options = {}) {
  const {
    method = "GET",
    json = false,
    body,
    headers,
    baseUrl = API_BASE_URL,
    includeCredentials = true,
    disableCsrf = false,
    useAuth = true,
  } = options;

  const finalHeaders = new Headers(headers || createHeaders({ json }));
  const session = useAuth ? getSession() : null;
  const accessToken = session?.accessToken || null;

  // JWT-based requests use Authorization header instead of JSESSIONID.
  if (accessToken && !finalHeaders.has("Authorization")) {
    finalHeaders.set("Authorization", `Bearer ${accessToken}`);
  }

  // For session-based auth + CSRF protection, browser cookies must be sent
  // and the CSRF token must be echoed back in the request header.
  // Перед mutating-запросами заново забираем актуальный token,
  // потому что после login/register Spring может выдать новый CSRF token.
  if (needsCsrf(method) && !disableCsrf) {
    await refreshCsrfToken(baseUrl);

    const csrfToken =
      csrfTokenFromBackend ||
      readCookie("XSRF-TOKEN") ||
      readCookie("CSRF-TOKEN");
    if (csrfToken) {
      finalHeaders.set("X-XSRF-TOKEN", csrfToken);
    }
  }

  const response = await fetch(buildUrl(path, baseUrl), {
    method,
    headers: finalHeaders,
    body,
    credentials: includeCredentials ? "include" : "omit",
  });

  return parseResponse(response);
}

export function jsonRequest(path, { method = "GET", body, ...options } = {}) {
  return request(path, {
    method,
    json: true,
    body: body ? JSON.stringify(body) : undefined,
    ...options,
  });
}

export function formDataRequest(path, { method = "POST", body, ...options } = {}) {
  return request(path, {
    method,
    body,
    ...options,
  });
}
