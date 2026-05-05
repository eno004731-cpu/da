import { API_BASE_URL } from "./endpoints.js";
import { getAccessToken } from "../state/auth-store.js";

function buildUrl(path) {
  return `${API_BASE_URL}${path}`;
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

  const response = await fetch(buildUrl(path), {
    method,
    headers: headers || createHeaders({ auth, json }),
    body,
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

