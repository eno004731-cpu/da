import { API_BASE_URL } from "./endpoints.js?v=20260510a";

let csrfTokenFromBackend = null;
const CSRF_ENDPOINT_PATH = "/auth/csrf";

function buildUrl(path) {
  return `${API_BASE_URL}${path}`;
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

async function refreshCsrfToken() {
  const response = await fetch(buildUrl(CSRF_ENDPOINT_PATH), {
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
  const payload = isJson ? await response.json().catch(() => null) : await response.text();

  if (!response.ok) {
    const message =
      (isJson && payload?.message) ||
      (isJson && payload?.error) ||
      (typeof payload === "string" && payload) ||
      `Request failed with status ${response.status}`;

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
  } = options;

  const finalHeaders = new Headers(headers || createHeaders({ json }));

  // For session-based auth + CSRF protection, browser cookies must be sent
  // and the CSRF token must be echoed back in the request header.
  // Перед mutating-запросами заново забираем актуальный token,
  // потому что после login/register Spring может выдать новый CSRF token.
  if (needsCsrf(method)) {
    await refreshCsrfToken();

    const csrfToken =
      csrfTokenFromBackend ||
      readCookie("XSRF-TOKEN") ||
      readCookie("CSRF-TOKEN");
    if (csrfToken) {
      finalHeaders.set("X-XSRF-TOKEN", csrfToken);
    }
  }

  const response = await fetch(buildUrl(path), {
    method,
    headers: finalHeaders,
    body,
    credentials: "include",
  });

  return parseResponse(response);
}

export function jsonRequest(path, { method = "GET", body } = {}) {
  return request(path, {
    method,
    json: true,
    body: body ? JSON.stringify(body) : undefined,
  });
}

export function formDataRequest(path, { method = "POST", body } = {}) {
  return request(path, {
    method,
    body,
  });
}
