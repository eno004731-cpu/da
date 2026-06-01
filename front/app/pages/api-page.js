const implementedTable = document.querySelector("#implemented-api-table");
const plannedTable = document.querySelector("#planned-api-table");
const implementedCount = document.querySelector("#implemented-api-count");
const plannedCount = document.querySelector("#planned-api-count");
const backendStyle = document.querySelector("#api-backend-style");
const frontendStyle = document.querySelector("#api-frontend-style");
const authBaseUrlNode = document.querySelector("#api-auth-base-url");
const ordersBaseUrlNode = document.querySelector("#api-orders-base-url");
const runtimeModeNode = document.querySelector("#api-runtime-mode");
const ordersModeNoteNode = document.querySelector("#api-orders-mode-note");
const yearNode = document.querySelector("#year");

function readBooleanFlag(value, fallback = false) {
  if (typeof value === "boolean") {
    return value;
  }

  if (typeof value === "string") {
    const normalized = value.trim().toLowerCase();
    if (["1", "true", "yes", "on"].includes(normalized)) {
      return true;
    }

    if (["0", "false", "no", "off"].includes(normalized)) {
      return false;
    }
  }

  return fallback;
}

function renderMethod(method = "") {
  return `<span class="api-method api-method--${method.toLowerCase()}">${method}</span>`;
}

function renderStatus(status = "") {
  const normalized = status.toLowerCase().replace(/\s+/g, "-");
  return `<span class="api-status api-status--${normalized}">${status}</span>`;
}

function renderRows(items) {
  return items
    .map(
      (item) => `
        <tr>
          <td>${renderMethod(item.method)}</td>
          <td><code>${item.path}</code></td>
          <td>${item.access}</td>
          <td>${renderStatus(item.status)}</td>
          <td>${item.purpose}</td>
          <td><code>${item.requestShape}</code></td>
          <td><code>${item.responseShape}</code></td>
          <td>${item.notes}</td>
        </tr>
      `
    )
    .join("");
}

function init() {
  const catalog = window.API_CATALOG;

  if (!catalog) {
    return;
  }

  if (implementedTable) {
    implementedTable.innerHTML = renderRows(catalog.IMPLEMENTED_API);
  }

  if (plannedTable) {
    plannedTable.innerHTML = renderRows(catalog.PLANNED_API);
  }

  if (implementedCount) {
    implementedCount.textContent = String(catalog.API_SUMMARY.implementedCount);
  }

  if (plannedCount) {
    plannedCount.textContent = String(catalog.API_SUMMARY.plannedCount);
  }

  if (backendStyle) {
    backendStyle.textContent = catalog.API_SUMMARY.backendStyle;
  }

  if (frontendStyle) {
    frontendStyle.textContent = catalog.API_SUMMARY.frontendStyle;
  }

  const localAuthOnlyMode = readBooleanFlag(window.__LEGAL_LOCAL_AUTH_ONLY__, false);
  const ordersApiEnabled = !readBooleanFlag(
    window.__LEGAL_DISABLE_ORDERS_API__,
    localAuthOnlyMode
  );
  const authBaseUrl = window.__LEGAL_AUTH_API_BASE_URL__ || `${window.location.origin}/api`;
  const ordersBaseUrl = window.__LEGAL_API_BASE_URL__ || `${window.location.origin}/api`;

  if (authBaseUrlNode) {
    authBaseUrlNode.textContent = authBaseUrl;
  }

  if (ordersBaseUrlNode) {
    ordersBaseUrlNode.textContent = ordersApiEnabled ? ordersBaseUrl : "disabled by flag";
  }

  if (runtimeModeNode) {
    runtimeModeNode.textContent = localAuthOnlyMode
      ? "Сейчас локально включён auth-only режим: фронт ждёт auth-service на 8081 и не требует backend заявок."
      : "Runtime-флаги подставили обычный backend-режим без auth-only ограничений.";
  }

  if (ordersModeNoteNode) {
    ordersModeNoteNode.textContent = ordersApiEnabled
      ? "Orders API включён текущим конфигом и может обрабатываться как активный backend-контракт."
      : "Orders API намеренно отключён флагом. Страницы кабинета и заказа должны показывать понятный unavailable state.";
  }

  if (yearNode) {
    yearNode.textContent = String(new Date().getFullYear());
  }
}

init();
