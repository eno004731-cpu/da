/**
 * Centralized API endpoint catalog for the frontend.
 * Backend implementation is intentionally left to the user.
 */
// By default, call the backend on the same host that serves the frontend.
// This avoids sending browser requests to a user's own localhost in production.
export const API_BASE_URL = window.__LEGAL_API_BASE_URL__ || `${window.location.origin}/api`;

export const ENDPOINTS = {
  auth: {
    login: "/auth/login",
    register: "/auth/register",
    logout: "/auth/logout",
    me: "/auth/me",
    deleteAccount: "/auth/account",
  },
  services: {
    list: "/services",
  },
  client: {
    applications: "/client/applications",
    orders: "/client/orders",
    orderDetails: (orderId) => `/client/orders/${encodeURIComponent(orderId)}`,
    orderRework: (orderId) => `/client/orders/${encodeURIComponent(orderId)}/rework`,
    orderDocuments: (orderId) => `/client/orders/${encodeURIComponent(orderId)}/documents`,
  },
  staff: {
    boardTasks: "/staff/board/tasks",
    boardTaskDetails: (taskId) => `/staff/board/tasks/${encodeURIComponent(taskId)}`,
    boardTaskStatus: (taskId) => `/staff/board/tasks/${encodeURIComponent(taskId)}/status`,
    boardTaskComments: (taskId) => `/staff/board/tasks/${encodeURIComponent(taskId)}/comments`,
  },
};
