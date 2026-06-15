/**
 * Centralized API endpoint catalog for the frontend.
 * Backend implementation is intentionally left to the user.
 */
function readBooleanFlag(value, fallback = false) {
  if (typeof value === "boolean") {
    return value;
  }

  if (typeof value === "string") {
    const normalized = value.trim().toLowerCase();
    if (["1", "true", "yes", "on"].includes(normalized)) {
      return true;
    }

    if (["0", "false", "no", "off"].includes(normalized)) {
      return false;
    }
  }

  return fallback;
}

// By default, call the backend on the same host that serves the frontend.
// This avoids sending browser requests to a user's own localhost in production.
export const API_BASE_URL = window.__LEGAL_API_BASE_URL__ || `${window.location.origin}/api`;
// Auth can be moved to a separate service. If no dedicated URL is provided,
// fallback to the same API host to preserve the old monolith contract.
export const AUTH_API_BASE_URL = window.__LEGAL_AUTH_API_BASE_URL__ || API_BASE_URL;
export const AUTH_USES_SAME_BACKEND = AUTH_API_BASE_URL === API_BASE_URL;
export const AUTH_USES_DEDICATED_SERVICE = AUTH_API_BASE_URL !== API_BASE_URL;
export const LOCAL_AUTH_ONLY_MODE = readBooleanFlag(window.__LEGAL_LOCAL_AUTH_ONLY__, false);
export const ORDERS_API_ENABLED = !readBooleanFlag(
  window.__LEGAL_DISABLE_ORDERS_API__,
  LOCAL_AUTH_ONLY_MODE
);
export const GOOGLE_AUTH_ENABLED = readBooleanFlag(
  window.__LEGAL_AUTH_GOOGLE_ENABLED__,
  !LOCAL_AUTH_ONLY_MODE
);
export const ACCOUNT_DELETE_ENABLED = readBooleanFlag(
  window.__LEGAL_AUTH_ACCOUNT_DELETE_ENABLED__,
  !LOCAL_AUTH_ONLY_MODE
);

export const ENDPOINTS = {
  auth: {
    csrf: "/auth/csrf",
    login: "/auth/login",
    register: "/auth/register",
    emailVerificationRequest: "/auth/email-verification/request",
    emailVerificationConfirm: "/auth/email-verification/confirm",
    googleLogin: "/auth/google/login",
    googleComplete: "/auth/google/complete",
    refresh: "/auth/refresh",
    logout: "/auth/logout",
    me: "/auth/me",
    updateMe: "/auth/me",
    deleteAccount: "/auth/account",
  },
  services: {
    list: "/services",
  },
  client: {
    applications: "/client/applications",
    orders: "/client/orders",
    orderDetails: (orderId) => `/client/orders/${encodeURIComponent(orderId)}`,
    orderUpdate: (orderId) => `/client/orders/${encodeURIComponent(orderId)}`,
    orderDelete: (orderId) => `/client/orders/${encodeURIComponent(orderId)}`,
    orderRework: (orderId) => `/client/orders/${encodeURIComponent(orderId)}/rework`,
    orderDocuments: (orderId) => `/client/orders/${encodeURIComponent(orderId)}/documents`,
    orderDocumentDelete: (orderId, documentId) =>
      `/client/orders/${encodeURIComponent(orderId)}/documents/${encodeURIComponent(documentId)}`,
  },
  staff: {
    boardTasks: "/staff/board/tasks",
    boardTaskDetails: (taskId) => `/staff/board/tasks/${encodeURIComponent(taskId)}`,
    boardTaskUpdate: (taskId) => `/staff/board/tasks/${encodeURIComponent(taskId)}`,
    boardTaskStatus: (taskId) => `/staff/board/tasks/${encodeURIComponent(taskId)}/status`,
    boardTaskReject: (taskId) => `/staff/board/tasks/${encodeURIComponent(taskId)}/reject`,
    boardTaskDelete: (taskId) => `/staff/board/tasks/${encodeURIComponent(taskId)}`,
    boardTaskComments: (taskId) => `/staff/board/tasks/${encodeURIComponent(taskId)}/comments`,
  },
};
