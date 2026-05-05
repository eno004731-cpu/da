const implementedTable = document.querySelector("#implemented-api-table");
const plannedTable = document.querySelector("#planned-api-table");
const implementedCount = document.querySelector("#implemented-api-count");
const plannedCount = document.querySelector("#planned-api-count");
const backendStyle = document.querySelector("#api-backend-style");
const frontendStyle = document.querySelector("#api-frontend-style");
const yearNode = document.querySelector("#year");

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

  if (yearNode) {
    yearNode.textContent = String(new Date().getFullYear());
  }
}

init();
