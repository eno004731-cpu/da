import { fetchClientOrderDetails, submitClientOrderRework } from "../api/orders-api.js";
import { fetchCurrentUser, isUnauthorizedError, logoutClient } from "../api/auth-api.js";
import { formatDate, formatDateTime } from "../lib/date.js";
import { formatFileSize } from "../lib/files.js";
import {
  getOrderStatusLabel,
  isCompletedStatus,
  isReworkStatus,
  normalizeOrderStatus,
  ORDER_STATUS_TIMELINE,
} from "../lib/status.js";
import { buildAuthUrl, clearSession, getCurrentUser, setSession } from "../state/auth-store.js";

const params = new URLSearchParams(window.location.search);
const orderId = params.get("orderId");

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
const reworkForm = document.querySelector("#client-rework-form");
const reworkTextarea = document.querySelector("#client-rework-text");
const timelineNode = document.querySelector("#order-timeline");

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
}

function renderDocuments(documents, status) {
  orderDocuments.innerHTML = "";
  deletedNotice.hidden = !isCompletedStatus(status);

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
    const availability = documentItem.isDeleted || isCompletedStatus(status);
    const actionHtml =
      documentItem.downloadUrl && !availability
        ? `<a class="text-link" href="${documentItem.downloadUrl}" target="_blank" rel="noreferrer">Открыть</a>`
        : `<span class="document-deleted-tag">Удалено после завершения</span>`;

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

  const hasRework = Boolean(order.clientRevisionComment);
  lastReworkCard.hidden = !hasRework;
  if (hasRework) {
    lastReworkComment.textContent = order.clientRevisionComment;
    lastReworkDate.textContent = order.clientRevisionRequestedAt
      ? `Отправлено: ${formatDateTime(order.clientRevisionRequestedAt)}`
      : "";
  }
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

    setFeedback(
      error.message || "Не удалось загрузить заказ. Проверь backend endpoint /client/orders/:id.",
      true
    );
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

    setFeedback(
      error.message || "Не удалось отправить заказ на доработку. Проверь backend endpoint /rework.",
      true
    );
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
