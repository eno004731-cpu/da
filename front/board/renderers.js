import { formatDateTime } from "../app/lib/date.js";
import { formatFileSize } from "../app/lib/files.js";
import { getOrderStatusLabel, isCompletedStatus, normalizeOrderStatus } from "../app/lib/status.js";
import { BOARD_COLUMNS, PRIORITY_LABELS } from "./constants.js";

function formatDate(value) {
  if (!value) {
    return "Без даты";
  }

  return formatDateTime(value);
}

function truncate(text, limit = 92) {
  if (!text) {
    return "";
  }

  return text.length > limit ? `${text.slice(0, limit).trim()}…` : text;
}

export function renderStatusLegend(node, tasks) {
  const counts = new Map(BOARD_COLUMNS.map((column) => [column.code, 0]));

  tasks.forEach((task) => {
    const statusCode = normalizeOrderStatus(task.status);
    counts.set(statusCode, (counts.get(statusCode) || 0) + 1);
  });

  node.innerHTML = BOARD_COLUMNS.map(
    (column) => `
      <li class="status-legend-item">
        <span><span class="status-dot" style="background:${column.accent}"></span> ${column.title}</span>
        <strong>${counts.get(column.code) || 0}</strong>
      </li>
    `
  ).join("");
}

export function renderColumns({ container, tasks, selectedTaskId, onTaskSelect, onTaskDragStart, onTaskDragEnd }) {
  container.innerHTML = "";

  BOARD_COLUMNS.forEach((column) => {
    const lane = document.createElement("section");
    lane.className = "kanban-column";
    lane.dataset.statusCode = column.code;

    const laneTasks = tasks.filter((task) => normalizeOrderStatus(task.status) === column.code);
    lane.innerHTML = `
      <div class="kanban-column-head">
        <h2>${column.title}</h2>
        <span class="column-count">${laneTasks.length}</span>
      </div>
      <div class="kanban-card-list"></div>
    `;

    const listNode = lane.querySelector(".kanban-card-list");

    if (!laneTasks.length) {
      listNode.innerHTML = `
        <div class="empty-column">
          <p class="empty-column-copy">Пока нет задач в этой колонке.</p>
        </div>
      `;
    }

    laneTasks.forEach((task) => {
      const card = document.getElementById("task-card-template").content.firstElementChild.cloneNode(true);
      card.dataset.taskId = String(task.id);
      card.classList.toggle("is-active", task.id === selectedTaskId);

      card.querySelector(".priority-pill").textContent = PRIORITY_LABELS[task.priority] || task.priority;
      card.querySelector(".priority-pill").dataset.priority = task.priority;
      card.querySelector(".task-date").textContent = formatDate(task.updatedAt || task.createdAt);
      card.querySelector(".task-title").textContent = task.title;
      card.querySelector(".task-client").textContent = task.clientName;
      card.querySelector(".task-service").textContent = task.serviceType;
      card.querySelector(".task-badge").textContent = getOrderStatusLabel(task.status);

      const reworkNote = card.querySelector(".task-rework-note");
      if (normalizeOrderStatus(task.status) === "REWORK" && task.clientRevisionComment) {
        reworkNote.hidden = false;
        reworkNote.textContent = `Клиент вернул на доработку: ${truncate(task.clientRevisionComment, 84)}`;
      }

      card.addEventListener("click", () => onTaskSelect(task.id));
      card.addEventListener("dragstart", () => onTaskDragStart(task.id));
      card.addEventListener("dragend", onTaskDragEnd);
      listNode.append(card);
    });

    container.append(lane);
  });
}

export function renderTaskDetails({ task, nodeMap }) {
  const {
    emptyNode,
    cardNode,
    trackingCode,
    title,
    status,
    clientName,
    contact,
    serviceType,
    priority,
    assignedTo,
    createdAt,
    updatedAt,
    revisionCount,
    description,
    reworkBlock,
    reworkComment,
    reworkDate,
    documentsCount,
    documentsState,
    documentsNode,
    commentsCount,
    commentsNode,
    historyCount,
    historyNode,
  } = nodeMap;

  if (!task) {
    emptyNode.hidden = false;
    cardNode.hidden = true;
    return;
  }

  emptyNode.hidden = true;
  cardNode.hidden = false;

  trackingCode.textContent = task.trackingCode;
  title.textContent = task.title;
  const normalizedStatus = normalizeOrderStatus(task.status);
  status.textContent = getOrderStatusLabel(normalizedStatus);
  status.dataset.status = normalizedStatus;
  clientName.textContent = task.clientName;
  contact.textContent = task.contact;
  serviceType.textContent = task.serviceType;
  priority.textContent = `Приоритет: ${PRIORITY_LABELS[task.priority] || task.priority}`;
  assignedTo.textContent = task.assignedTo || "Не назначено";
  createdAt.textContent = formatDate(task.createdAt);
  updatedAt.textContent = `Обновлено: ${formatDate(task.updatedAt)}`;
  revisionCount.textContent = `Возвратов: ${task.revisionCount ?? 0}`;
  description.textContent = task.description || "Описание пока не заполнено.";

  const hasRework = normalizedStatus === "REWORK" && task.clientRevisionComment;
  reworkBlock.hidden = !hasRework;
  if (hasRework) {
    reworkComment.textContent = task.clientRevisionComment;
    reworkDate.textContent = task.clientRevisionRequestedAt
      ? `Запрошено: ${formatDate(task.clientRevisionRequestedAt)}`
      : "";
  }

  const documents = task.documents || [];
  documentsCount.textContent = String(documents.length);
  documentsNode.innerHTML = "";
  documentsState.hidden = true;

  if (!documents.length) {
    documentsNode.innerHTML = `<p class="detail-copy">Документы по задаче пока не прикреплены.</p>`;
  } else {
    documents.forEach((documentItem) => {
      const item = document.createElement("article");
      item.className = "document-item";

      const isUnavailable = documentItem.isDeleted || isCompletedStatus(normalizedStatus);
      const actionMarkup =
        documentItem.downloadUrl && !isUnavailable
          ? `<a class="document-link" href="${documentItem.downloadUrl}" target="_blank" rel="noreferrer">Открыть</a>`
          : `<span class="document-tag">Недоступно</span>`;

      item.innerHTML = `
        <div class="document-copy">
          <strong>${documentItem.fileName}</strong>
          <span>${formatFileSize(documentItem.size)} • ${formatDate(documentItem.uploadedAt)}</span>
        </div>
        ${actionMarkup}
      `;
      documentsNode.append(item);
    });
  }

  if (isCompletedStatus(normalizedStatus)) {
    documentsState.hidden = false;
    documentsState.textContent =
      "После завершения заказа документы должны считаться удалёнными или недоступными по backend-контракту.";
  }

  commentsCount.textContent = String(task.comments?.length || 0);
  commentsNode.innerHTML = "";
  (task.comments || []).forEach((comment) => {
    const item = document.createElement("article");
    item.className = "comment-item";
    item.innerHTML = `
      <div class="comment-meta">
        <span class="comment-author">${comment.authorName}</span>
        <span>${formatDate(comment.createdAt)}</span>
      </div>
      <p class="comment-copy">${comment.body}</p>
    `;
    commentsNode.append(item);
  });

  if (!(task.comments || []).length) {
    commentsNode.innerHTML = `<p class="detail-copy">Комментариев команды пока нет.</p>`;
  }

  historyCount.textContent = String(task.history?.length || 0);
  historyNode.innerHTML = "";
  (task.history || []).forEach((entry) => {
    const item = document.createElement("article");
    item.className = "history-item";
    item.innerHTML = `
      <div class="history-meta">
        <span class="history-author">${entry.authorName}</span>
        <span>${formatDate(entry.createdAt)}</span>
      </div>
      <p class="history-copy">${entry.fieldName}: ${entry.oldValue || "—"} → ${entry.newValue || "—"}</p>
    `;
    historyNode.append(item);
  });

  if (!(task.history || []).length) {
    historyNode.innerHTML = `<p class="detail-copy">История изменений пока пустая.</p>`;
  }
}
