import {
  ACCOUNT_DELETE_ENABLED,
  LOCAL_AUTH_ONLY_MODE,
  ORDERS_API_ENABLED,
} from "../api/endpoints.js?v=20260525a";
import {
  deleteClientOrder,
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
const brandCompany = document.querySelector("#cabinet-brand-company");
const logoutButton = document.querySelector("#cabinet-logout");
const deleteAccountButton = document.querySelector("#cabinet-delete-account");
const ordersBody = document.querySelector("#cabinet-orders-body");
const emptyState = document.querySelector("#cabinet-empty-state");
const feedbackNode = document.querySelector("#cabinet-feedback");
const totalOrders = document.querySelector("#cabinet-total-orders");
const reworkOrders = document.querySelector("#cabinet-rework-orders");
const deleteAccountNote = document.querySelector("#cabinet-delete-account-note");
const statusTodo = document.querySelector("#cabinet-status-todo");
const statusInProgress = document.querySelector("#cabinet-status-in-progress");
const statusOnReview = document.querySelector("#cabinet-status-on-review");
const statusRework = document.querySelector("#cabinet-status-rework");
const statusDone = document.querySelector("#cabinet-status-done");
const statusRejected = document.querySelector("#cabinet-status-rejected");
const pageParams = new URLSearchParams(window.location.search);
const orderDeletedFlash = pageParams.get("orderDeleted") === "1";

const STATUS_COUNTERS = {
  TODO: statusTodo,
  IN_PROGRESS: statusInProgress,
  ON_REVIEW: statusOnReview,
  REWORK: statusRework,
  DONE: statusDone,
  REJECTED: statusRejected,
};

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
  if (brandCompany) {
    brandCompany.textContent = user.companyName || "Личный кабинет клиента";
  }
}

function renderOrderCounters(orders) {
  const counts = {
    TODO: 0,
    IN_PROGRESS: 0,
    ON_REVIEW: 0,
    REWORK: 0,
    DONE: 0,
    REJECTED: 0,
  };

  orders.forEach((order) => {
    if (counts[order.status] !== undefined) {
      counts[order.status] += 1;
    }
  });

  Object.entries(STATUS_COUNTERS).forEach(([status, node]) => {
    if (node) {
      node.textContent = String(counts[status] || 0);
    }
  });

  totalOrders.textContent = String(orders.length);
  reworkOrders.textContent = String(counts.REWORK || 0);
}

function createActionButton(label, title, handler) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = "orders-table-action";
  button.setAttribute("aria-label", title);
  button.setAttribute("title", title);
  button.textContent = label;
  button.addEventListener("click", handler);
  return button;
}

async function handleDeleteOrder(order) {
  const confirmed = window.confirm(
    `Удалить заявку «${order.title}»? Это действие необратимо.`
  );

  if (!confirmed) {
    return;
  }

  setFeedback("Удаляем заявку…");

  try {
    await deleteClientOrder(order.id);
    await loadOrders();
    setFeedback("Заявка удалена.");
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
      setFeedback("Backend временно недоступен. Не удалось удалить заявку.", true);
      return;
    }

    setFeedback(error.message || "Не удалось удалить заявку.", true);
  }
}

function renderOrders(orders) {
  renderOrderCounters(orders);
  ordersBody.innerHTML = "";
  emptyState.hidden = orders.length > 0;
  emptyState.textContent = orders.length
    ? ""
    : "У вас пока нет активных заявок. Новые обращения появятся здесь после отправки.";

  orders.forEach((order) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td data-label="Услуга">
        <div class="orders-table-primary">
          <strong>${order.serviceName}</strong>
          <span>Создан: ${formatDateTime(order.createdAt)}</span>
        </div>
      </td>
      <td data-label="Кратко вопрос">
        <div class="orders-table-primary">
          <strong>${order.title}</strong>
          <span>Обновлён: ${formatDateTime(order.updatedAt)}</span>
        </div>
      </td>
      <td data-label="Документы">
        <div class="orders-table-primary">
          <strong>н/д</strong>
          <span>Summary API не отдаёт счётчик документов</span>
        </div>
      </td>
      <td data-label="Статус">
        <div class="orders-table-status">
          <span class="status-pill" data-status="${order.status}">${getOrderStatusLabel(order.status)}</span>
          <span>Доработок: ${order.revisionCount ?? 0}</span>
        </div>
      </td>
      <td data-label="Действия">
        <div class="orders-table-actions-row"></div>
      </td>
    `;

    const actionsNode = row.querySelector(".orders-table-actions-row");
    const openLink = document.createElement("a");
    openLink.className = "orders-table-action orders-table-action--link";
    openLink.href = `./order.html?id=${encodeURIComponent(order.id)}`;
    openLink.textContent = "↗";
    openLink.setAttribute("aria-label", "Открыть заявку");
    openLink.setAttribute("title", "Открыть заявку");

    const editLink = document.createElement("a");
    editLink.className = "orders-table-action orders-table-action--link";
    editLink.href = `./order.html?id=${encodeURIComponent(order.id)}&edit=1`;
    editLink.textContent = "✎";
    editLink.setAttribute("aria-label", "Редактировать заявку");
    editLink.setAttribute("title", "Редактировать заявку");

    const deleteButton = createActionButton("✕", "Удалить заявку", () => handleDeleteOrder(order));

    actionsNode?.append(openLink, editLink, deleteButton);
    ordersBody.append(row);
  });
}

function renderOrdersUnavailableState() {
  totalOrders.textContent = "—";
  reworkOrders.textContent = "—";
  ordersBody.innerHTML = "";
  Object.values(STATUS_COUNTERS).forEach((node) => {
    if (node) {
      node.textContent = "—";
    }
  });
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
      renderOrdersUnavailableState();
      setFeedback("Кабинет временно недоступен. Backend не отвечает, попробуйте позже.", true);
      return;
    }

    renderOrdersUnavailableState();
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
