import { ENDPOINTS } from "./endpoints.js";
import { formDataRequest, jsonRequest, request } from "./http-client.js";

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
    auth: true,
    body: formData,
  });
}

export function fetchClientOrders() {
  return request(ENDPOINTS.client.orders, {
    auth: true,
  });
}

export function fetchClientOrderDetails(orderId) {
  return request(ENDPOINTS.client.orderDetails(orderId), {
    auth: true,
  });
}

export function submitClientOrderRework(orderId, comment) {
  return jsonRequest(ENDPOINTS.client.orderRework(orderId), {
    method: "POST",
    auth: true,
    body: { comment },
  });
}
