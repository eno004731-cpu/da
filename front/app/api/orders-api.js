import { ENDPOINTS } from "./endpoints.js?v=20260512b";
import { formDataRequest, jsonRequest, request } from "./http-client.js?v=20260512b";
import { normalizeOrderStatus } from "../lib/status.js?v=20260512a";

function normalizeDocument(documentItem = {}) {
  return {
    id: String(documentItem.id ?? ""),
    fileName: documentItem.fileName || documentItem.name || "Документ без названия",
    mimeType: documentItem.mimeType || "application/octet-stream",
    size: Number(documentItem.size ?? 0),
    uploadedAt: documentItem.uploadedAt || documentItem.createdAt || "",
    downloadUrl: documentItem.downloadUrl || null,
    isDeleted: Boolean(documentItem.isDeleted ?? documentItem.deleted),
    deletedAt: documentItem.deletedAt || null,
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

export function fetchServices() {
  return request(ENDPOINTS.services.list);
}

/**
 * Sends a client application with attached files.
 * Backend contract can be implemented as multipart/form-data.
 */
export function createClientApplication(payload) {
  const formData = new FormData();
  formData.set("serviceCode", payload.serviceCode);
  formData.set("clientName", payload.clientName);
  formData.set("contact", payload.contact);
  formData.set("companyName", payload.companyName || "");
  formData.set("description", payload.description);

  payload.documents.forEach((file) => {
    formData.append("documents", file);
  });

  return formDataRequest(ENDPOINTS.client.applications, {
    method: "POST",
    body: formData,
  });
}

export function fetchClientOrders() {
  return request(ENDPOINTS.client.orders).then((payload) =>
    Array.isArray(payload) ? payload.map(normalizeOrderSummary) : []
  );
}

export function fetchClientOrderDetails(orderId) {
  return request(ENDPOINTS.client.orderDetails(orderId)).then(normalizeOrderDetails);
}

export function updateClientOrder(orderId, payload) {
  return jsonRequest(ENDPOINTS.client.orderUpdate(orderId), {
    method: "PATCH",
    body: payload,
  }).then((response) =>
    response && typeof response === "object"
      ? normalizeOrderDetails(response)
      : response
  );
}

export function deleteClientOrder(orderId) {
  return request(ENDPOINTS.client.orderDelete(orderId), {
    method: "DELETE",
  });
}

export function submitClientOrderRework(orderId, comment) {
  return jsonRequest(ENDPOINTS.client.orderRework(orderId), {
    method: "POST",
    body: { comment },
  }).then((response) =>
    response && typeof response === "object"
      ? normalizeOrderDetails(response)
      : response
  );
}
