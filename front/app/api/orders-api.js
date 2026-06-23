import {
  DOCUMENT_API_BASE_URL,
  ENDPOINTS,
  LOCAL_AUTH_ONLY_MODE,
  ORDERS_API_ENABLED,
} from "./endpoints.js?v=20260621a";
import { isUnauthorizedError, refreshClientSession } from "./auth-api.js?v=20260615a";
import { blobRequest, formDataRequest, jsonRequest, request } from "./http-client.js?v=20260622a";
import { normalizeOrderStatus } from "../lib/status.js?v=20260512a";

function normalizeDocument(documentItem = {}) {
  return {
    id: String(documentItem.id ?? documentItem.documentId ?? ""),
    fileName: documentItem.fileName || documentItem.name || "Документ без названия",
    mimeType: documentItem.mimeType || "application/octet-stream",
    size: Number(documentItem.size ?? documentItem.sizeBytes ?? 0),
    uploadedAt: documentItem.uploadedAt || documentItem.createdAt || "",
    downloadUrl: documentItem.downloadUrl || null,
    isDeleted: Boolean(documentItem.isDeleted ?? documentItem.deleted),
    deletedAt: documentItem.deletedAt || null,
    validationStatus: String(
      documentItem.validationStatus ||
      documentItem.validation_status ||
      "DOCUMENT_VALIDATION_REQUESTED"
    ).toUpperCase(),
  };
}

function normalizeOrderSummary(order = {}) {
  return {
    id: String(order.id ?? ""),
    title: order.title || "Заявка без названия",
    serviceCode: order.serviceCode || "",
    serviceName: order.serviceName || order.serviceType || "Услуга не указана",
    status: normalizeOrderStatus(order.status || "TODO"),
    createdAt: order.createdAt || "",
    updatedAt: order.updatedAt || order.createdAt || "",
    revisionCount: Number(order.revisionCount ?? 0),
  };
}

function normalizeOrderDetails(order = {}) {
  return {
    id: String(order.id ?? ""),
    title: order.title || "Заявка без названия",
    serviceCode: order.serviceCode || "",
    serviceName: order.serviceName || order.serviceType || "Услуга не указана",
    clientName: order.clientName || null,
    contact: order.contact || order.clientContact || null,
    companyName: order.companyName || null,
    problemDescription: order.problemDescription || order.description || "",
    status: normalizeOrderStatus(order.status || "TODO"),
    createdAt: order.createdAt || "",
    updatedAt: order.updatedAt || order.createdAt || "",
    clientRevisionComment: order.clientRevisionComment || null,
    clientRevisionRequestedAt: order.clientRevisionRequestedAt || null,
    rejectionReason:
      order.rejectionReason ||
      order.rejectedReason ||
      order.rejectionComment ||
      order.rejectedComment ||
      null,
    rejectedAt: order.rejectedAt || null,
    revisionCount: Number(order.revisionCount ?? 0),
    documents: Array.isArray(order.documents) ? order.documents.map(normalizeDocument) : [],
  };
}

function normalizeCreatedApplicationResponse(payload) {
  if (!payload || typeof payload !== "object") {
    return payload;
  }

  const orderId = String(payload.orderId ?? payload.id ?? "").trim();
  const normalizedOrder = normalizeOrderDetails({
    ...payload,
    id: payload.id ?? orderId,
  });

  return {
    ...normalizedOrder,
    orderId: orderId || normalizedOrder.id,
    taskId: payload.taskId ? String(payload.taskId) : null,
    trackingCode: payload.trackingCode || null,
  };
}

function createOrdersApiDisabledError() {
  const error = new Error(
    LOCAL_AUTH_ONLY_MODE
      ? "Локально сейчас подключён только сервис авторизации. API заявок будет добавлен позже."
      : "API заявок временно отключён."
  );
  error.code = "ORDERS_API_DISABLED";
  return error;
}

function ensureOrdersApiEnabled() {
  if (!ORDERS_API_ENABLED) {
    throw createOrdersApiDisabledError();
  }
}

async function withAuthRefreshRetry(requestFn) {
  try {
    return await requestFn();
  } catch (error) {
    if (!isUnauthorizedError(error)) {
      throw error;
    }

    await refreshClientSession();
    return requestFn();
  }
}

export function fetchServices() {
  if (!ORDERS_API_ENABLED) {
    return Promise.resolve([]);
  }

  return request(ENDPOINTS.services.list);
}

/**
 * Sends a client application with attached files.
 * Backend contract can be implemented as multipart/form-data.
 */
export function createClientApplication(payload) {
  ensureOrdersApiEnabled();
  return withAuthRefreshRetry(() => jsonRequest(ENDPOINTS.client.applications, {
    method: "POST",
    body: {
      serviceCode: payload.serviceCode,
      clientName: payload.clientName,
      contact: payload.contact,
      companyName: payload.companyName || "",
      description: payload.description,
    },
    disableCsrf: true,
  })).then(normalizeCreatedApplicationResponse);
}

export function uploadClientOrderDocuments(orderId, documents = []) {
  ensureOrdersApiEnabled();

  const normalizedOrderId = String(orderId || "").trim();
  if (!normalizedOrderId) {
    throw new Error("Не удалось загрузить документы: orderId ещё не получен.");
  }

  const formData = new FormData();
  documents.forEach((file) => {
    formData.append("documents", file);
  });

  // Document-service сначала сохраняет файл со статусом ожидания проверки.
  // Затем order-service проверяет заявку через Kafka и при отказе отправляет
  // обратную Kafka-команду на удаление документа.
  return withAuthRefreshRetry(() => formDataRequest(ENDPOINTS.client.orderDocuments(normalizedOrderId), {
    method: "POST",
    body: formData,
    baseUrl: DOCUMENT_API_BASE_URL,
    disableCsrf: true,
  })).then((payload) =>
    Array.isArray(payload) ? payload.map(normalizeDocument) : []
  );
}

export function fetchClientOrders() {
  ensureOrdersApiEnabled();
  return withAuthRefreshRetry(() => request(ENDPOINTS.client.orders)).then((payload) =>
    Array.isArray(payload) ? payload.map(normalizeOrderSummary) : []
  );
}

export function fetchClientOrderDetails(orderId) {
  ensureOrdersApiEnabled();
  return withAuthRefreshRetry(async () => {
    // Заказ и документы принадлежат разным сервисам, поэтому читаем их параллельно.
    const [order, documents] = await Promise.all([
      request(ENDPOINTS.client.orderDetails(orderId)),
      request(ENDPOINTS.client.orderDocuments(orderId), {
        baseUrl: DOCUMENT_API_BASE_URL,
      }),
    ]);

    return normalizeOrderDetails({
      ...order,
      documents: Array.isArray(documents) ? documents : [],
    });
  });
}

export function updateClientOrder(orderId, payload) {
  ensureOrdersApiEnabled();
  return withAuthRefreshRetry(() => jsonRequest(ENDPOINTS.client.orderUpdate(orderId), {
    method: "PATCH",
    body: payload,
    disableCsrf: true,
  })).then((response) =>
    response && typeof response === "object"
      ? normalizeOrderDetails(response)
      : response
  );
}

export function deleteClientOrder(orderId) {
  ensureOrdersApiEnabled();
  return withAuthRefreshRetry(() => request(ENDPOINTS.client.orderDelete(orderId), {
    method: "DELETE",
    disableCsrf: true,
  }));
}

export function deleteClientOrderDocument(orderId, documentId) {
  ensureOrdersApiEnabled();
  return withAuthRefreshRetry(() => request(ENDPOINTS.client.orderDocumentDelete(orderId, documentId), {
    method: "DELETE",
    // Физическим владельцем документа является document-service.
    baseUrl: DOCUMENT_API_BASE_URL,
    disableCsrf: true,
  }));
}

export function downloadClientOrderDocument(downloadUrl) {
  ensureOrdersApiEnabled();

  if (!downloadUrl) {
    throw new Error("Документ пока недоступен для скачивания.");
  }

  return withAuthRefreshRetry(() => blobRequest(downloadUrl, {
    baseUrl: DOCUMENT_API_BASE_URL,
  }));
}

export function submitClientOrderRework(orderId, comment) {
  ensureOrdersApiEnabled();
  return withAuthRefreshRetry(() => jsonRequest(ENDPOINTS.client.orderRework(orderId), {
    method: "POST",
    body: { comment },
    disableCsrf: true,
  })).then((response) =>
    response && typeof response === "object"
      ? normalizeOrderDetails(response)
      : response
  );
}
