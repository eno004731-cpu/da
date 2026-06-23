import {
  deleteClientOrder,
  deleteClientOrderDocument,
  downloadClientOrderDocument,
  fetchClientOrderDetails,
  fetchServices,
  submitClientOrderRework,
  updateClientOrder,
  uploadClientOrderDocuments,
} from "../api/orders-api.js?v=20260622b";
import {
  LOCAL_AUTH_ONLY_MODE,
  ORDERS_API_ENABLED,
} from "../api/endpoints.js?v=20260525a";
import {
  fetchCurrentUser,
  isBackendUnavailableError,
  isUnauthorizedError,
  logoutClient,
  shouldPreserveClientSessionOnOrdersUnauthorized,
} from "../api/auth-api.js?v=20260525c";
import { formatDate, formatDateTime } from "../lib/date.js";
import { formatFileSize } from "../lib/files.js";
import {
  cacheOrderSnapshot,
  clearOrderSnapshot,
  readOrderSnapshot,
} from "../lib/order-snapshot.js";
import {
  getOrderStatusLabel,
  isCompletedStatus,
  isRejectedStatus,
  isReworkStatus,
  isUnavailableDocumentStatus,
  normalizeOrderStatus,
  ORDER_STATUS_TIMELINE,
} from "../lib/status.js";
import {
  buildAuthUrl,
  buildCompleteProfileUrl,
  clearSession,
  getCurrentUser,
  requiresProfileCompletion,
  updateSessionUser,
} from "../state/auth-store.js?v=20260525a";

const params = new URLSearchParams(window.location.search);
const orderId = params.get("id") || params.get("orderId");
const shouldOpenEditOnLoad = params.get("edit") === "1";
let currentOrder = null;
let servicesCatalog = null;
let editModalAutoOpened = false;

const userName = document.querySelector("#order-user-name");
const userMeta = document.querySelector("#order-user-meta");
const logoutButton = document.querySelector("#order-logout");
const backLink = document.querySelector("#order-back-link");
const feedbackNode = document.querySelector("#order-feedback");

const orderTitle = document.querySelector("#order-title");
const orderStatus = document.querySelector("#order-status");
const orderService = document.querySelector("#order-service");
const orderIdMeta = document.querySelector("#order-id-meta");
const orderDateMeta = document.querySelector("#order-date-meta");
const orderRevisionCount = document.querySelector("#order-revision-count");
const orderDescription = document.querySelector("#order-description");
const orderDocuments = document.querySelector("#order-documents");
const documentUploadZone = document.querySelector("#document-upload-zone");
const documentUploadInput = document.querySelector("#order-documents-input");
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

function hasRenderableOrderSnapshot(order) {
  return Boolean(
    order &&
    typeof order === "object" &&
    (order.title || order.serviceName || order.status || order.problemDescription || order.createdAt)
  );
}

function setFeedback(message = "", isError = false) {
  if (!feedbackNode) {
    return;
  }

  feedbackNode.hidden = !message;
  feedbackNode.textContent = message;
  feedbackNode.classList.toggle("is-error", isError);
  feedbackNode.classList.toggle("is-success", !isError && Boolean(message));
}

function isDocumentUploadAllowed(status) {
  return !isUnavailableDocumentStatus(status);
}

function getDocumentId(documentItem = {}) {
  return String(documentItem.id || documentItem.documentId || "").trim();
}

function getDocumentSize(documentItem = {}) {
  return Number(documentItem.size ?? documentItem.sizeBytes ?? 0);
}

function getDocumentValidationStatus(documentItem = {}) {
  return String(
    documentItem.validationStatus || "DOCUMENT_VALIDATION_REQUESTED"
  ).toUpperCase();
}

function getDocumentValidationLabel(validationStatus) {
  if (validationStatus === "DOCUMENT_VALIDATED") {
    return "Проверен";
  }

  if (validationStatus === "DOCUMENT_REJECTED") {
    return "Не прошёл проверку";
  }

  return "Проверяется";
}

function mergeDocuments(existingDocuments = [], uploadedDocuments = []) {
  const documentsByKey = new Map();

  [...existingDocuments, ...uploadedDocuments].forEach((documentItem) => {
    const documentId = getDocumentId(documentItem);
    const fallbackKey = [
      documentItem.fileName || documentItem.name || "document",
      getDocumentSize(documentItem),
      documentItem.uploadedAt || documentItem.createdAt || "",
    ].join(":");

    documentsByKey.set(documentId || fallbackKey, documentItem);
  });

  return Array.from(documentsByKey.values());
}

function setDocumentUploadState({ disabled = false, dragging = false } = {}) {
  if (documentUploadInput) {
    documentUploadInput.disabled = disabled;
  }

  if (documentUploadZone) {
    documentUploadZone.classList.toggle("is-disabled", disabled);
    documentUploadZone.classList.toggle("is-dragging", dragging);
  }
}

function renderUser() {
  const user = getCurrentUser();
  if (!user) {
    return;
  }

  userName.textContent = user.fullName || "Завершите регистрацию";
  userMeta.textContent =
    [user.email, user.companyName].filter(Boolean).join(" • ") ||
    "Профиль ещё не завершён.";
}

function renderTimeline(status) {
  timelineNode.innerHTML = "";
  const normalizedStatus = normalizeOrderStatus(status);

  ORDER_STATUS_TIMELINE.filter((step) => step.code !== "TODO").forEach((step) => {
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
  setDocumentUploadState({ disabled: unavailableDocuments });
  deletedNotice.hidden = !unavailableDocuments;
  deletedNotice.textContent = isRejectedStatus(status)
    ? "Заявка отклонена. Связанные документы считаются недоступными для дальнейшей работы."
    : "Заказ завершён. Связанные документы должны считаться удаляемыми и могут быть уже удалены backend.";

  if (!documents?.length) {
    const emptyState = document.createElement("div");
    emptyState.className = "empty-inline-state";
    emptyState.textContent = unavailableDocuments
      ? "Документы по этой заявке недоступны."
      : "Документы пока не прикреплены.";
    orderDocuments.append(emptyState);
    return;
  }

  documents.forEach((documentItem) => {
    const row = document.createElement("article");
    row.className = "document-row";
    const availability = documentItem.isDeleted || unavailableDocuments;
    const validationStatus = getDocumentValidationStatus(documentItem);
    const isValidated = validationStatus === "DOCUMENT_VALIDATED";

    const icon = document.createElement("span");
    icon.className = "document-file-icon";
    icon.textContent = "DOC";

    const body = document.createElement("div");
    body.className = "document-row-body";

    const name = document.createElement("strong");
    name.textContent = documentItem.fileName || "Документ без названия";

    const meta = document.createElement("span");
    meta.textContent = [
      documentItem.mimeType || "Файл",
      formatFileSize(getDocumentSize(documentItem)),
      formatDate(documentItem.uploadedAt),
    ]
      .filter(Boolean)
      .join(" • ");

    body.append(name, meta);

    const action = document.createElement("div");
    action.className = "document-row-actions";
    if (documentItem.downloadUrl && isValidated && !availability) {
      const downloadButton = document.createElement("button");
      downloadButton.type = "button";
      downloadButton.className = "text-link document-download-button";
      downloadButton.textContent = "Скачать";
      downloadButton.addEventListener("click", () => handleDownloadDocument(documentItem));
      action.append(downloadButton);
    } else {
      const statusTag = document.createElement("span");
      statusTag.className = availability ? "document-deleted-tag" : "document-status-tag";
      statusTag.textContent = availability
        ? isRejectedStatus(status)
          ? "Недоступно"
          : "Удалено"
        : getDocumentValidationLabel(validationStatus);
      action.append(statusTag);
    }

    if (!availability && documentItem.id) {
      const deleteDocumentButton = document.createElement("button");
      deleteDocumentButton.type = "button";
      deleteDocumentButton.className = "document-delete-button";
      deleteDocumentButton.textContent = "Удалить";
      deleteDocumentButton.addEventListener("click", () => handleDeleteDocument(documentItem));
      action.append(deleteDocumentButton);
    }

    row.append(icon, body, action);
    orderDocuments.append(row);
  });
}

async function handleDownloadDocument(documentItem) {
  if (getDocumentValidationStatus(documentItem) !== "DOCUMENT_VALIDATED") {
    setFeedback("Документ можно скачать только после успешной проверки.", true);
    return;
  }

  setFeedback("Подготавливаем документ…");

  try {
    const fileBlob = await downloadClientOrderDocument(documentItem.downloadUrl);
    const objectUrl = URL.createObjectURL(fileBlob);
    const downloadLink = document.createElement("a");

    // Временная browser-ссылка живёт только до начала скачивания.
    downloadLink.href = objectUrl;
    downloadLink.download = documentItem.fileName || "document";
    document.body.append(downloadLink);
    downloadLink.click();
    downloadLink.remove();
    URL.revokeObjectURL(objectUrl);

    setFeedback("Скачивание документа началось.");
  } catch (error) {
    if (isUnauthorizedError(error)) {
      handleOrdersUnauthorized(window.location.pathname + window.location.search);
      return;
    }

    setFeedback(error.message || "Не удалось скачать документ.", true);
  }
}

async function handleDeleteDocument(documentItem) {
  if (!currentOrder || !documentItem?.id) {
    return;
  }

  const confirmed = window.confirm(`Удалить документ «${documentItem.fileName || "Документ"}»?`);
  if (!confirmed) {
    return;
  }

  setDocumentUploadState({ disabled: true });
  setFeedback("Удаляем документ…");

  try {
    await deleteClientOrderDocument(orderId, documentItem.id);
    const deletedAt = new Date().toISOString();
    const nextOrder = {
      ...currentOrder,
      documents: currentOrder.documents.map((document) =>
        document.id === documentItem.id
          ? { ...document, isDeleted: true, deletedAt }
          : document
      ),
    };
    renderOrder(nextOrder);
    setFeedback("Документ удалён.");
  } catch (error) {
    if (isUnauthorizedError(error)) {
      handleOrdersUnauthorized(window.location.pathname + window.location.search);
      return;
    }

    setFeedback(error.message || "Не удалось удалить документ.", true);
  } finally {
    setDocumentUploadState({ disabled: !isDocumentUploadAllowed(currentOrder?.status) });
  }
}

function renderOrder(order) {
  currentOrder = order;
  cacheOrderSnapshot({
    ...order,
    orderId: order.orderId || order.id,
  });
  const normalizedStatus = normalizeOrderStatus(order.status);
  const createdAt = formatDateTime(order.createdAt);
  const updatedAt = formatDateTime(order.updatedAt);
  const revisionCount = Number(order.revisionCount ?? 0);

  orderTitle.textContent = order.title || "Заявка без названия";
  orderStatus.textContent = getOrderStatusLabel(order.status);
  orderStatus.dataset.status = normalizedStatus;
  orderService.textContent = order.serviceName || "Услуга не указана";
  orderIdMeta.textContent = `Order ID: ${order.orderId || order.id || orderId}`;
  orderDateMeta.textContent = `Создано ${createdAt} · Обновлено ${updatedAt}`;
  orderRevisionCount.textContent = revisionCount
    ? `Доработок по заявке: ${revisionCount}`
    : "Доработок по заявке пока нет.";
  orderDescription.textContent =
    order.problemDescription || "Клиент пока не добавил описание к заявке.";

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

  if (shouldOpenEditOnLoad && !editModalAutoOpened) {
    editModalAutoOpened = true;
    window.requestAnimationFrame(() => {
      openEditModal().catch(() => {
        setFeedback("Не удалось открыть форму редактирования автоматически.", true);
      });
    });
  }
}

function showOrdersAuthBridgeMessage(message = "") {
  setFeedback(
    message ||
      "Вы остались авторизованы, но backend заказов пока не принимает сессию из auth-service. Поэтому операции в кабинете сейчас недоступны.",
    true
  );
}

function handleOrdersUnauthorized(redirectUrl) {
  if (shouldPreserveClientSessionOnOrdersUnauthorized()) {
    showOrdersAuthBridgeMessage(
      "Вы вошли в отдельный auth-service, но backend заказов пока не принимает эту сессию."
    );
    return;
  }

  clearSession();
  window.location.href = buildAuthUrl("login", redirectUrl);
}

function renderOrdersUnavailableState() {
  orderTitle.textContent = "Карточка заказа пока недоступна";
  orderStatus.textContent = "API заявок не подключён";
  orderStatus.removeAttribute("data-status");
  orderService.textContent = "Сейчас локально работает только auth-service";
  orderIdMeta.textContent = orderId ? `Order ID: ${orderId}` : "";
  orderDateMeta.textContent = "Даты появятся после загрузки карточки заказа.";
  orderRevisionCount.textContent = "Доработок по заявке пока нет.";
  orderDescription.textContent = LOCAL_AUTH_ONLY_MODE
    ? "Авторизация и профиль клиента уже можно проверять локально. Карточки заявок появятся после подключения отдельного backend API заказов."
    : "Раздел заказов временно отключён.";
  orderDocuments.innerHTML = `
    <div class="empty-inline-state">
      Документы появятся здесь после загрузки карточки заказа.
    </div>
  `;
  timelineNode.innerHTML = `
    <div class="timeline-pill" data-state="inactive">API заявок пока не подключён</div>
  `;
  deletedNotice.hidden = true;
  lastReworkCard.hidden = true;
  rejectedCard.hidden = true;
  reworkSection.hidden = true;
  editButton.hidden = true;
  deleteButton.hidden = true;
  setDocumentUploadState({ disabled: true });
  setFeedback(
    LOCAL_AUTH_ONLY_MODE
      ? "Локально сейчас поднят только auth-service. Открыть реальную карточку заказа пока нельзя."
      : "Карточка заказа временно недоступна."
  );
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

  const cachedOrder = readOrderSnapshot(orderId);
  const hasCachedOrder = hasRenderableOrderSnapshot(cachedOrder);

  if (hasCachedOrder) {
    renderOrder(cachedOrder);
    setFeedback("Показываем только что созданный заказ. Обновляем детали…");
  } else {
    setFeedback("Загружаем детали заказа…");
  }

  try {
    const order = await fetchClientOrderDetails(orderId);
    renderOrder(order);
    clearOrderSnapshot(orderId);
    setFeedback("");
  } catch (error) {
    if (isUnauthorizedError(error)) {
      handleOrdersUnauthorized(window.location.pathname + window.location.search);
      return;
    }

    if (isBackendUnavailableError(error)) {
      setFeedback(
        hasCachedOrder
          ? "Backend временно недоступен. Показаны недавно сохранённые данные заказа."
          : "Backend временно недоступен. Не удалось загрузить заказ.",
        true
      );
      return;
    }

    if (hasCachedOrder) {
      setFeedback(
        error.message || "Не удалось обновить детали заказа. Показаны недавно сохранённые данные.",
        true
      );
      return;
    }

    setFeedback(error.message || "Не удалось загрузить заказ. Проверь backend endpoint /client/orders/:id.", true);
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
        ? {
            // Order-service больше не возвращает документы, поэтому сохраняем
            // уже загруженный список из состояния страницы.
            ...response,
            documents: currentOrder.documents,
          }
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
      handleOrdersUnauthorized(window.location.pathname + window.location.search);
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
    renderOrder({
      // Ответ изменения статуса приходит только из order-service.
      ...order,
      documents: currentOrder?.documents || [],
    });
    reworkForm.reset();
    setFeedback("Замечание отправлено. Заказ переведён в статус «На доработке».");
  } catch (error) {
    if (isUnauthorizedError(error)) {
      handleOrdersUnauthorized(window.location.pathname + window.location.search);
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
    clearOrderSnapshot(orderId);
    window.location.href = "./cabinet.html?orderDeleted=1";
  } catch (error) {
    if (isUnauthorizedError(error)) {
      handleOrdersUnauthorized("./cabinet.html");
      return;
    }

    setFeedback(error.message || "Не удалось удалить заявку.", true);
    deleteButton.disabled = false;
  }
}

async function handleDocumentFiles(files) {
  const selectedFiles = Array.from(files || []).filter(Boolean);
  if (!selectedFiles.length) {
    return;
  }

  if (!currentOrder) {
    setFeedback("Сначала нужно загрузить карточку заказа.", true);
    return;
  }

  if (!isDocumentUploadAllowed(currentOrder.status)) {
    setFeedback("Для завершённой или отклонённой заявки загрузка документов недоступна.", true);
    return;
  }

  setDocumentUploadState({ disabled: true });
  setFeedback(
    selectedFiles.length === 1
      ? "Загружаем документ…"
      : `Загружаем документы: ${selectedFiles.length} файла…`
  );

  try {
    const uploadedDocuments = await uploadClientOrderDocuments(orderId, selectedFiles);
    currentOrder = {
      ...currentOrder,
      documents: mergeDocuments(currentOrder.documents, uploadedDocuments),
      updatedAt: new Date().toISOString(),
    };
    renderOrder(currentOrder);
    setFeedback("Документы загружены и добавлены в карточку заказа.");
  } catch (error) {
    if (isUnauthorizedError(error)) {
      handleOrdersUnauthorized(window.location.pathname + window.location.search);
      return;
    }

    if (isBackendUnavailableError(error)) {
      setFeedback("Backend временно недоступен. Документы не удалось загрузить.", true);
      return;
    }

    setFeedback(error.message || "Не удалось загрузить документы.", true);
  } finally {
    if (documentUploadInput) {
      documentUploadInput.value = "";
    }
    setDocumentUploadState({
      disabled: currentOrder ? !isDocumentUploadAllowed(currentOrder.status) : true,
    });
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
  documentUploadInput?.addEventListener("change", (event) => {
    handleDocumentFiles(event.target.files);
  });
  documentUploadZone?.addEventListener("dragover", (event) => {
    event.preventDefault();
    if (!documentUploadInput?.disabled) {
      setDocumentUploadState({ dragging: true });
    }
  });
  documentUploadZone?.addEventListener("dragleave", () => {
    setDocumentUploadState({ dragging: false, disabled: Boolean(documentUploadInput?.disabled) });
  });
  documentUploadZone?.addEventListener("drop", (event) => {
    event.preventDefault();
    setDocumentUploadState({ dragging: false, disabled: Boolean(documentUploadInput?.disabled) });
    if (!documentUploadInput?.disabled) {
      handleDocumentFiles(event.dataTransfer?.files);
    }
  });
  backLink?.setAttribute("href", "./cabinet.html");
}

async function ensureActiveSession() {
  const nextUrl = window.location.pathname + window.location.search;
  const cachedUser = getCurrentUser();

  if (!cachedUser) {
    clearSession();
    window.location.href = buildAuthUrl("login", nextUrl);
    return false;
  }

  try {
    const user = await fetchCurrentUser();

    if (!user) {
      clearSession();
      window.location.href = buildAuthUrl("login", nextUrl);
      return false;
    }

    updateSessionUser(user);

    if (requiresProfileCompletion(user)) {
      window.location.href = buildCompleteProfileUrl(nextUrl);
      return false;
    }

    return true;
  } catch (error) {
    if (isUnauthorizedError(error)) {
      clearSession();
      window.location.href = buildAuthUrl("login", nextUrl);
      return false;
    }

    if (isBackendUnavailableError(error)) {
      if (cachedUser && requiresProfileCompletion(cachedUser)) {
        window.location.href = buildCompleteProfileUrl(nextUrl);
        return false;
      }

      setFeedback("Backend временно недоступен. Не удалось проверить активную сессию.", true);
      return false;
    }

    setFeedback(error.message || "Не удалось проверить активную сессию.", true);
    return false;
  }
}

async function init() {
  setDocumentUploadState({ disabled: true });

  const sessionIsActive = await ensureActiveSession();
  if (!sessionIsActive) {
    return;
  }

  renderUser();
  attachEvents();

  if (!ORDERS_API_ENABLED) {
    renderOrdersUnavailableState();
    return;
  }

  loadOrder();
}

init();
