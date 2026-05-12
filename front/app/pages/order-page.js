import {
  deleteClientOrder,
  fetchClientOrderDetails,
  fetchServices,
  submitClientOrderRework,
  updateClientOrder,
} from "../api/orders-api.js?v=20260512a";
import {
  fetchCurrentUser,
  isBackendUnavailableError,
  isUnauthorizedError,
  logoutClient,
} from "../api/auth-api.js?v=20260510b";
import { formatDate, formatDateTime } from "../lib/date.js";
import { formatFileSize } from "../lib/files.js";
import {
  getOrderStatusLabel,
  isCompletedStatus,
  isRejectedStatus,
  isReworkStatus,
  isUnavailableDocumentStatus,
  normalizeOrderStatus,
  ORDER_STATUS_TIMELINE,
} from "../lib/status.js";
import { buildAuthUrl, clearSession, getCurrentUser, setSession } from "../state/auth-store.js?v=20260510b";

const params = new URLSearchParams(window.location.search);
const orderId = params.get("orderId");
let currentOrder = null;
let servicesCatalog = null;

const userName = document.querySelector("#order-user-name");
const userMeta = document.querySelector("#order-user-meta");
const logoutButton = document.querySelector("#order-logout");
const backLink = document.querySelector("#order-back-link");
const feedbackNode = document.querySelector("#order-feedback");

const orderTitle = document.querySelector("#order-title");
const orderStatus = document.querySelector("#order-status");
const orderService = document.querySelector("#order-service");
const orderCreated = document.querySelector("#order-created-at");
const orderUpdated = document.querySelector("#order-updated-at");
const orderRevisionCount = document.querySelector("#order-revision-count");
const orderDescription = document.querySelector("#order-description");
const orderDocuments = document.querySelector("#order-documents");
const deletedNotice = document.querySelector("#deleted-documents-notice");
const lastReworkCard = document.querySelector("#last-rework-card");
const lastReworkComment = document.querySelector("#last-rework-comment");
const lastReworkDate = document.querySelector("#last-rework-date");
const rejectedCard = document.querySelector("#order-rejected-card");
const rejectedReason = document.querySelector("#order-rejected-reason");
const rejectedDate = document.querySelector("#order-rejected-date");
const reworkSection = document.querySelector("#client-rework-section");
const reworkForm = document.querySelector("#client-rework-form");
const reworkTextarea = document.querySelector("#client-rework-text");
const timelineNode = document.querySelector("#order-timeline");
const editButton = document.querySelector("#order-edit-button");
const deleteButton = document.querySelector("#order-delete-button");
const editModal = document.querySelector("#order-edit-modal");
const editForm = document.querySelector("#order-edit-form");
const editCloseButton = document.querySelector("#order-edit-close");
const editCancelButton = document.querySelector("#order-edit-cancel");
const editSubmitButton = document.querySelector("#order-edit-submit");
const editServiceField = document.querySelector("#order-edit-service");
const editClientNameField = document.querySelector("#order-edit-client-name");
const editContactField = document.querySelector("#order-edit-contact");
const editCompanyNameField = document.querySelector("#order-edit-company-name");
const editDescriptionField = document.querySelector("#order-edit-description");

function setFeedback(message = "", isError = false) {
  if (!feedbackNode) {
    return;
  }

  feedbackNode.hidden = !message;
  feedbackNode.textContent = message;
  feedbackNode.classList.toggle("is-error", isError);
  feedbackNode.classList.toggle("is-success", !isError && Boolean(message));
}

function renderUser() {
  const user = getCurrentUser();
  if (!user) {
    return;
  }

  userName.textContent = user.fullName;
  userMeta.textContent = [user.email, user.companyName].filter(Boolean).join(" • ");
}

function renderTimeline(status) {
  timelineNode.innerHTML = "";
  const normalizedStatus = normalizeOrderStatus(status);

  ORDER_STATUS_TIMELINE.forEach((step) => {
    const node = document.createElement("div");
    node.className = "timeline-pill";
    node.textContent = step.label;
    node.dataset.state = step.code === normalizedStatus ? "active" : "inactive";
    if (isReworkStatus(normalizedStatus) && step.code === "REWORK") {
      node.dataset.state = "rework";
    }
    timelineNode.append(node);
  });

  if (isRejectedStatus(normalizedStatus)) {
    const rejectedNode = document.createElement("div");
    rejectedNode.className = "timeline-pill";
    rejectedNode.textContent = getOrderStatusLabel(normalizedStatus);
    rejectedNode.dataset.state = "rejected";
    timelineNode.append(rejectedNode);
  }
}

function renderDocuments(documents, status) {
  orderDocuments.innerHTML = "";
  const unavailableDocuments = isUnavailableDocumentStatus(status);
  deletedNotice.hidden = !unavailableDocuments;
  deletedNotice.textContent = isRejectedStatus(status)
    ? "Заявка отклонена. Связанные документы считаются недоступными для дальнейшей работы."
    : "Заказ завершён. Связанные документы должны считаться удаляемыми и могут быть уже удалены backend.";

  if (!documents?.length) {
    orderDocuments.innerHTML = `
      <div class="empty-inline-state">
        Документы пока не прикреплены.
      </div>
    `;
    return;
  }

  documents.forEach((documentItem) => {
    const row = document.createElement("article");
    row.className = "document-row";
    const availability = documentItem.isDeleted || unavailableDocuments;
    const actionHtml =
      documentItem.downloadUrl && !availability
        ? `<a class="text-link" href="${documentItem.downloadUrl}" target="_blank" rel="noreferrer">Открыть</a>`
        : `<span class="document-deleted-tag">${
            isRejectedStatus(status) ? "Недоступно после отклонения" : "Удалено после завершения"
          }</span>`;

    row.innerHTML = `
      <div>
        <strong>${documentItem.fileName}</strong>
        <span>${formatFileSize(documentItem.size)} • ${formatDate(documentItem.uploadedAt)}</span>
      </div>
      ${actionHtml}
    `;
    orderDocuments.append(row);
  });
}

function renderOrder(order) {
  currentOrder = order;
  orderTitle.textContent = order.title;
  orderStatus.textContent = getOrderStatusLabel(order.status);
  orderStatus.dataset.status = order.status;
  orderService.textContent = order.serviceName;
  orderCreated.textContent = formatDateTime(order.createdAt);
  orderUpdated.textContent = formatDateTime(order.updatedAt);
  orderRevisionCount.textContent = String(order.revisionCount ?? 0);
  orderDescription.textContent = order.problemDescription;

  renderTimeline(order.status);
  renderDocuments(order.documents, order.status);

  const hasRejectReason = Boolean(order.rejectionReason) || isRejectedStatus(order.status);
  rejectedCard.hidden = !hasRejectReason;
  if (hasRejectReason) {
    rejectedReason.textContent = order.rejectionReason || "Юрист отклонил заявку без текстового комментария.";
    rejectedDate.textContent = order.rejectedAt
      ? `Отклонено: ${formatDateTime(order.rejectedAt)}`
      : "";
  }

  const hasRework = Boolean(order.clientRevisionComment);
  lastReworkCard.hidden = !hasRework;
  if (hasRework) {
    lastReworkComment.textContent = order.clientRevisionComment;
    lastReworkDate.textContent = order.clientRevisionRequestedAt
      ? `Отправлено: ${formatDateTime(order.clientRevisionRequestedAt)}`
      : "";
  }

  reworkSection.hidden = isRejectedStatus(order.status) || isCompletedStatus(order.status);
}

function setModalState(isOpen) {
  editModal.hidden = !isOpen;
  document.body.classList.toggle("portal-modal-open", isOpen);
}

function closeEditModal() {
  setModalState(false);
}

async function ensureServicesLoaded() {
  if (servicesCatalog) {
    return servicesCatalog;
  }

  servicesCatalog = await fetchServices().catch(() => []);
  return servicesCatalog;
}

function renderServiceOptions(services = [], selectedCode = "", selectedName = "") {
  editServiceField.innerHTML = "";

  if (!services.length) {
    const option = document.createElement("option");
    option.value = selectedCode || selectedName || "";
    option.textContent = selectedName || selectedCode || "Текущая услуга";
    editServiceField.append(option);
    return;
  }

  const hasSelectedService = services.some((service) => service.code === selectedCode);
  if (!hasSelectedService && (selectedCode || selectedName)) {
    const fallbackOption = document.createElement("option");
    fallbackOption.value = selectedCode || "";
    fallbackOption.textContent = selectedName || selectedCode;
    fallbackOption.selected = true;
    editServiceField.append(fallbackOption);
  }

  services.forEach((service) => {
    const option = document.createElement("option");
    option.value = service.code;
    option.textContent = service.name;
    option.selected = service.code === selectedCode;
    editServiceField.append(option);
  });
}

async function openEditModal() {
  if (!currentOrder) {
    return;
  }

  setFeedback("");
  const services = await ensureServicesLoaded();
  renderServiceOptions(services, currentOrder.serviceCode, currentOrder.serviceName);
  editClientNameField.value = currentOrder.clientName || "";
  editContactField.value = currentOrder.contact || "";
  editCompanyNameField.value = currentOrder.companyName || "";
  editDescriptionField.value = currentOrder.problemDescription || "";
  setModalState(true);
}

function buildEditedOrderPayload() {
  return {
    serviceCode: editServiceField.value,
    clientName: editClientNameField.value.trim(),
    contact: editContactField.value.trim(),
    companyName: editCompanyNameField.value.trim(),
    description: editDescriptionField.value.trim(),
  };
}

async function loadOrder() {
  if (!orderId) {
    setFeedback("Не передан идентификатор заказа.", true);
    return;
  }

  setFeedback("Загружаем детали заказа…");

  try {
    const order = await fetchClientOrderDetails(orderId);
    renderOrder(order);
    setFeedback("");
  } catch (error) {
    if (isUnauthorizedError(error)) {
      clearSession();
      window.location.href = buildAuthUrl("login", window.location.pathname + window.location.search);
      return;
    }

    if (isBackendUnavailableError(error)) {
      setFeedback("Backend временно недоступен. Не удалось загрузить заказ.", true);
      return;
    }

    setFeedback(
      error.message || "Не удалось загрузить заказ. Проверь backend endpoint /client/orders/:id.",
      true
    );
  }
}

async function handleEditSubmit(event) {
  event.preventDefault();

  if (!currentOrder) {
    return;
  }

  const payload = buildEditedOrderPayload();
  if (!payload.description) {
    setFeedback("Описание заявки не может быть пустым.", true);
    return;
  }

  editSubmitButton.disabled = true;
  setFeedback("Сохраняем изменения заявки…");

  try {
    const response = await updateClientOrder(orderId, payload);
    const nextOrder =
      response && typeof response === "object" && response.id
        ? response
        : {
            ...currentOrder,
            ...payload,
            serviceName:
              servicesCatalog?.find((service) => service.code === payload.serviceCode)?.name ||
              currentOrder.serviceName,
            problemDescription: payload.description,
            updatedAt: new Date().toISOString(),
          };

    renderOrder(nextOrder);
    closeEditModal();
    setFeedback("Изменения по заявке сохранены.");
  } catch (error) {
    if (isUnauthorizedError(error)) {
      clearSession();
      window.location.href = buildAuthUrl("login", window.location.pathname + window.location.search);
      return;
    }

    setFeedback(error.message || "Не удалось сохранить изменения заявки.", true);
  } finally {
    editSubmitButton.disabled = false;
  }
}

async function handleReworkSubmit(event) {
  event.preventDefault();
  const comment = reworkTextarea.value.trim();

  if (!comment) {
    setFeedback("Для отправки на доработку нужно описать замечание.", true);
    return;
  }

  setFeedback("Отправляем замечание юристу…");

  try {
    const order = await submitClientOrderRework(orderId, comment);
    renderOrder(order);
    reworkForm.reset();
    setFeedback("Замечание отправлено. Заказ переведён в статус «На доработке».");
  } catch (error) {
    if (isUnauthorizedError(error)) {
      clearSession();
      window.location.href = buildAuthUrl("login", window.location.pathname + window.location.search);
      return;
    }

    if (isBackendUnavailableError(error)) {
      setFeedback("Backend временно недоступен. Замечание не удалось отправить.", true);
      return;
    }

    setFeedback(
      error.message || "Не удалось отправить заказ на доработку. Проверь backend endpoint /rework.",
      true
    );
  }
}

async function handleDeleteOrder() {
  if (!currentOrder) {
    return;
  }

  const confirmed = window.confirm(
    "Удалить заявку? Это действие необратимо, и после него карточка заказа будет недоступна."
  );

  if (!confirmed) {
    return;
  }

  deleteButton.disabled = true;
  setFeedback("Удаляем заявку…");

  try {
    await deleteClientOrder(orderId);
    window.location.href = "./cabinet.html?orderDeleted=1";
  } catch (error) {
    if (isUnauthorizedError(error)) {
      clearSession();
      window.location.href = buildAuthUrl("login", "./cabinet.html");
      return;
    }

    setFeedback(error.message || "Не удалось удалить заявку.", true);
    deleteButton.disabled = false;
  }
}

function attachEvents() {
  logoutButton?.addEventListener("click", async () => {
    logoutButton.disabled = true;
    await logoutClient().catch(() => null);
    clearSession();
    window.location.href = buildAuthUrl("login", "./cabinet.html");
  });

  reworkForm?.addEventListener("submit", handleReworkSubmit);
  editButton?.addEventListener("click", openEditModal);
  deleteButton?.addEventListener("click", handleDeleteOrder);
  editForm?.addEventListener("submit", handleEditSubmit);
  editCloseButton?.addEventListener("click", closeEditModal);
  editCancelButton?.addEventListener("click", closeEditModal);
  editModal?.addEventListener("click", (event) => {
    if (event.target === editModal) {
      closeEditModal();
    }
  });
  backLink?.setAttribute("href", "./cabinet.html");
}

async function ensureActiveSession() {
  try {
    const user = await fetchCurrentUser();

    if (!user) {
      clearSession();
      window.location.href = buildAuthUrl("login", window.location.pathname + window.location.search);
      return false;
    }

    setSession({ user });
    return true;
  } catch (error) {
    if (isUnauthorizedError(error)) {
      clearSession();
      window.location.href = buildAuthUrl("login", window.location.pathname + window.location.search);
      return false;
    }

    if (isBackendUnavailableError(error)) {
      setFeedback("Backend временно недоступен. Не удалось проверить активную сессию.", true);
      return false;
    }

    setFeedback(error.message || "Не удалось проверить активную сессию.", true);
    return false;
  }
}

async function init() {
  const sessionIsActive = await ensureActiveSession();
  if (!sessionIsActive) {
    return;
  }

  renderUser();
  attachEvents();
  loadOrder();
}

init();
