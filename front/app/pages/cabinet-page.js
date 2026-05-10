import { deleteClientAccount, fetchCurrentUser, isUnauthorizedError, logoutClient } from "../api/auth-api.js?v=20260510a";
import { fetchClientOrders } from "../api/orders-api.js?v=20260510a";
import { formatDateTime } from "../lib/date.js";
import { getOrderStatusLabel } from "../lib/status.js";
import { buildAuthUrl, clearSession, getCurrentUser, setSession } from "../state/auth-store.js?v=20260510a";

const userName = document.querySelector("#cabinet-user-name");
const userMeta = document.querySelector("#cabinet-user-meta");
const logoutButton = document.querySelector("#cabinet-logout");
const deleteAccountButton = document.querySelector("#cabinet-delete-account");
const ordersList = document.querySelector("#cabinet-orders-list");
const emptyState = document.querySelector("#cabinet-empty-state");
const feedbackNode = document.querySelector("#cabinet-feedback");
const totalOrders = document.querySelector("#cabinet-total-orders");

function setFeedback(message = "", isError = false) {
  if (!feedbackNode) {
    return;
  }

  feedbackNode.hidden = !message;
  feedbackNode.textContent = message;
  feedbackNode.classList.toggle("is-error", isError);
}

function renderUser() {
  const user = getCurrentUser();
  if (!user) {
    return;
  }

  userName.textContent = user.fullName;
  userMeta.textContent = [user.email, user.companyName].filter(Boolean).join(" • ");
}

function renderOrders(orders) {
  totalOrders.textContent = `${orders.length} заказов`;
  ordersList.innerHTML = "";
  emptyState.hidden = orders.length > 0;

  orders.forEach((order) => {
    const item = document.createElement("article");
    item.className = "portal-card order-list-card";
    item.innerHTML = `
      <div class="order-card-top">
        <span class="status-pill" data-status="${order.status}">${getOrderStatusLabel(order.status)}</span>
        <span class="order-date">${formatDateTime(order.createdAt)}</span>
      </div>
      <h3>${order.title}</h3>
      <p class="order-card-service">${order.serviceName}</p>
      <div class="order-card-bottom">
        <span>Возвратов: ${order.revisionCount ?? 0}</span>
        <a class="text-link" href="./order.html?orderId=${encodeURIComponent(order.id)}">Открыть</a>
      </div>
    `;
    ordersList.append(item);
  });
}

async function loadOrders() {
  setFeedback("Загружаем ваши заказы…");

  try {
    const orders = await fetchClientOrders();
    renderOrders(orders);
    setFeedback("");
  } catch (error) {
    if (isUnauthorizedError(error)) {
      clearSession();
      window.location.href = buildAuthUrl("login", "./cabinet.html");
      return;
    }

    renderOrders([]);
    setFeedback(
      error.message || "Не удалось загрузить список заказов. Проверь backend endpoint /client/orders.",
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

  deleteAccountButton?.addEventListener("click", async () => {
    const confirmed = window.confirm(
      "Удалить аккаунт? Это действие необратимо, и после него вы будете выведены из кабинета."
    );

    if (!confirmed) {
      return;
    }

    deleteAccountButton.disabled = true;
    setFeedback("Удаляем аккаунт…");

    try {
      await deleteClientAccount();
      clearSession();
      window.location.href = "./da.html?accountDeleted=1";
    } catch (error) {
      setFeedback(
        error.message || "Не удалось удалить аккаунт. Проверь backend endpoint POST /auth/account.",
        true
      );
      deleteAccountButton.disabled = false;
    }
  });
}

async function ensureActiveSession() {
  try {
    const user = await fetchCurrentUser();

    if (!user) {
      clearSession();
      window.location.href = buildAuthUrl("login", "./cabinet.html");
      return false;
    }

    setSession({ user });
    return true;
  } catch (error) {
    if (isUnauthorizedError(error)) {
      clearSession();
      window.location.href = buildAuthUrl("login", "./cabinet.html");
      return false;
    }

    setFeedback(error.message || "Не удалось проверить сессию пользователя.", true);
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
  loadOrders();
}

init();
