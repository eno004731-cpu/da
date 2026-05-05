import { ENDPOINTS } from "./endpoints.js";
import { jsonRequest, request } from "./http-client.js";
import { normalizeOrderStatus } from "../lib/status.js";

function normalizeDocument(documentItem = {}) {
  return {
    id: String(documentItem.id ?? ""),
    fileName: documentItem.fileName || documentItem.name || "Документ без названия",
    mimeType: documentItem.mimeType || "application/octet-stream",
    size: Number(documentItem.size ?? 0),
    uploadedAt: documentItem.uploadedAt || documentItem.createdAt || "",
    downloadUrl: documentItem.downloadUrl || null,
    isDeleted: Boolean(documentItem.isDeleted),
    deletedAt: documentItem.deletedAt || null,
  };
}

function normalizeComment(comment = {}, index = 0) {
  return {
    id: String(comment.id ?? `comment-${index}`),
    authorName: comment.authorName || comment.author || "Команда",
    body: comment.body || "",
    createdAt: comment.createdAt || "",
  };
}

function normalizeHistoryEntry(entry = {}, index = 0) {
  return {
    id: String(entry.id ?? `history-${index}`),
    authorName: entry.authorName || entry.author || "Система",
    fieldName: entry.fieldName || entry.field || "status",
    oldValue: entry.oldValue ?? null,
    newValue: entry.newValue ?? null,
    createdAt: entry.createdAt || "",
  };
}

function normalizeTask(task = {}) {
  return {
    id: String(task.id ?? ""),
    orderId: task.orderId ? String(task.orderId) : null,
    trackingCode: task.trackingCode || task.publicCode || "—",
    title: task.title || "Задача без названия",
    clientName: task.clientName || "Клиент не указан",
    contact: task.contact || task.clientContact || "Контакт не указан",
    serviceType: task.serviceType || task.serviceName || "Услуга не указана",
    description: task.description || task.problemDescription || "",
    status: normalizeOrderStatus(task.status || task.statusCode || "TODO"),
    createdAt: task.createdAt || "",
    updatedAt: task.updatedAt || task.createdAt || "",
    assignedTo: task.assignedTo || task.assigneeName || null,
    priority: task.priority || "MEDIUM",
    clientRevisionComment: task.clientRevisionComment || null,
    clientRevisionRequestedAt: task.clientRevisionRequestedAt || null,
    revisionCount: Number(task.revisionCount ?? 0),
    documents: Array.isArray(task.documents) ? task.documents.map(normalizeDocument) : [],
    comments: Array.isArray(task.comments) ? task.comments.map(normalizeComment) : [],
    history: Array.isArray(task.history) ? task.history.map(normalizeHistoryEntry) : [],
  };
}

function extractTaskCollection(payload) {
  if (Array.isArray(payload)) {
    return payload;
  }

  if (Array.isArray(payload?.items)) {
    return payload.items;
  }

  if (Array.isArray(payload?.tasks)) {
    return payload.tasks;
  }

  return [];
}

/**
 * Staff board stays backend-agnostic: frontend only defines the contract
 * and normalizes the payload into a stable UI shape.
 */
export async function fetchStaffBoardTasks() {
  const payload = await request(ENDPOINTS.staff.boardTasks);
  return extractTaskCollection(payload).map(normalizeTask);
}

export async function fetchStaffTaskDetails(taskId) {
  const payload = await request(ENDPOINTS.staff.boardTaskDetails(taskId));
  return normalizeTask(payload);
}

export async function updateStaffTaskStatus(taskId, status) {
  const payload = await jsonRequest(ENDPOINTS.staff.boardTaskStatus(taskId), {
    method: "PATCH",
    body: { status },
  });

  return normalizeTask(payload);
}

export async function addStaffTaskComment(taskId, body) {
  const payload = await jsonRequest(ENDPOINTS.staff.boardTaskComments(taskId), {
    method: "POST",
    body: { body },
  });

  return normalizeTask(payload);
}
