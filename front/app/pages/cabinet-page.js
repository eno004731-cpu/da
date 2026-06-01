import {
  ACCOUNT_DELETE_ENABLED,
  LOCAL_AUTH_ONLY_MODE,
  ORDERS_API_ENABLED,
} from "../api/endpoints.js?v=20260525a";
import {
  deleteClientAccount,
  fetchCurrentUser,
  isBackendUnavailableError,
  isUnauthorizedError,
  logoutClient,
  shouldPreserveClientSessionOnOrdersUnauthorized,
} from "../api/auth-api.js?v=20260525c";
import { fetchClientOrders } from "../api/orders-api.js?v=20260512a";
import { formatDateTime } from "../lib/date.js";
import { getOrderStatusLabel } from "../lib/status.js";
import {
  buildAuthUrl,
  buildCompleteProfileUrl,
  clearSession,
  getCurrentUser,
  requiresProfileCompletion,
  updateSessionUser,
} from "../state/auth-store.js?v=20260525a";

const userName = document.querySelector("#cabinet-user-name");
const userMeta = document.querySelector("#cabinet-user-meta");
const logoutButton = document.querySelector("#cabinet-logout");
const deleteAccountButton = document.querySelector("#cabinet-delete-account");
const ordersList = document.querySelector("#cabinet-orders-list");
const emptyState = document.querySelector("#cabinet-empty-state");
const feedbackNode = document.querySelector("#cabinet-feedback");
const totalOrders = document.querySelector("#cabinet-total-orders");
const deleteAccountNote = document.querySelector("#cabinet-delete-account-note");
const pageParams = new URLSearchParams(window.location.search);
const orderDeletedFlash = pageParams.get("orderDeleted") === "1";

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

  userName.textContent = user.fullName || "Завершите регистрацию";
  userMeta.textContent =
    [user.email, user.companyName].filter(Boolean).join(" • ") ||
    "Добавьте email и имя, чтобы кабинет отображался полностью.";
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

function renderOrdersUnavailableState() {
  totalOrders.textContent = "API позже";
  ordersList.innerHTML = "";
  emptyState.hidden = false;
  emptyState.textContent = LOCAL_AUTH_ONLY_MODE
    ? "Локально сейчас подключён только auth-service. Список заявок появится после подключения отдельного API заказов."
    : "Список заказов временно недоступен.";
  setFeedback(
    LOCAL_AUTH_ONLY_MODE
      ? "Профиль загружен из auth-service. Раздел заказов локально отключён по плану разработки."
      : "Раздел заказов временно отключён."
  );
}

function showOrdersAuthBridgeMessage() {
  renderOrders([]);
  setFeedback(
    "Вы остались авторизованы, но backend заявок пока не принимает сессию из auth-service. Поэтому кабинет заказов временно недоступен.",
    true
  );
}

async function loadOrders() {
  setFeedback("Загружаем ваши заказы…");

  try {
    const orders = await fetchClientOrders();
    renderOrders(orders);
    setFeedback(orderDeletedFlash ? "Заявка удалена. Список заказов обновлён." : "");
  } catch (error) {
    if (isUnauthorizedError(error)) {
      if (shouldPreserveClientSessionOnOrdersUnauthorized()) {
        showOrdersAuthBridgeMessage();
      } else {
        clearSession();
        window.location.href = buildAuthUrl("login", "./cabinet.html");
      }
      return;
    }

    if (isBackendUnavailableError(error)) {
      renderOrders([]);
      setFeedback("Кабинет временно недоступен. Backend не отвечает, попробуйте позже.", true);
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
        error.message || "Не удалось удалить аккаунт. Проверь backend endpoint DELETE /auth/account.",
        true
      );
      deleteAccountButton.disabled = false;
    }
  });
}

function syncFeatureAvailability() {
  if (deleteAccountButton) {
    deleteAccountButton.hidden = !ACCOUNT_DELETE_ENABLED;
  }

  if (deleteAccountNote) {
    deleteAccountNote.textContent = ACCOUNT_DELETE_ENABLED
      ? "Действие необратимо. Кнопка активна только когда backend действительно реализовал удаление аккаунта."
      : "В локальном auth-service удаление аккаунта пока не реализовано, поэтому кнопка скрыта.";
  }
}

async function ensureActiveSession() {
  const nextUrl = "./cabinet.html";
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

    setFeedback(error.message || "Не удалось проверить сессию пользователя.", true);
    return false;
  }
}

async function init() {
  const sessionIsActive = await ensureActiveSession();
  if (!sessionIsActive) {
    return;
  }

  if (orderDeletedFlash) {
    setFeedback("Заявка удалена. Список заказов обновлён.");
  }

  renderUser();
  syncFeatureAvailability();
  attachEvents();

  if (!ORDERS_API_ENABLED) {
    renderOrdersUnavailableState();
    return;
  }

  loadOrders();
}

init();
