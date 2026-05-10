import { ENDPOINTS } from "./endpoints.js?v=20260510b";
import { formDataRequest, jsonRequest, request } from "./http-client.js?v=20260510b";

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
  return request(ENDPOINTS.client.orders);
}

export function fetchClientOrderDetails(orderId) {
  return request(ENDPOINTS.client.orderDetails(orderId));
}

export function submitClientOrderRework(orderId, comment) {
  return jsonRequest(ENDPOINTS.client.orderRework(orderId), {
    method: "POST",
    body: { comment },
  });
}
