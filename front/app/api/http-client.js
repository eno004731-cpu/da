import { API_BASE_URL } from "./endpoints.js";
import { getAccessToken } from "../state/auth-store.js";

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

function createHeaders({ auth = false, json = false } = {}) {
  const headers = new Headers();

  if (json) {
    headers.set("Content-Type", "application/json");
  }

  if (auth) {
    const token = getAccessToken();
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
  }

  return headers;
}

/**
 * Shared HTTP wrapper. Keeps auth handling in one place.
 */
export async function request(path, options = {}) {
  const {
    method = "GET",
    auth = false,
    json = false,
    body,
    headers,
  } = options;

  const finalHeaders = new Headers(headers || createHeaders({ auth, json }));

  // For session-based auth + CSRF protection, browser cookies must be sent
  // and the CSRF token must be echoed back in the request header.
  if (needsCsrf(method)) {
    const csrfToken = readCookie("XSRF-TOKEN") || readCookie("CSRF-TOKEN");
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

export function jsonRequest(path, { method = "GET", auth = false, body } = {}) {
  return request(path, {
    method,
    auth,
    json: true,
    body: body ? JSON.stringify(body) : undefined,
  });
}

export function formDataRequest(path, { method = "POST", auth = false, body } = {}) {
  return request(path, {
    method,
    auth,
    body,
  });
}
