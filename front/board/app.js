import { deleteStaffTask, fetchBoardTasks, rejectStaffTask, updateStaffTask, updateTaskStatus } from "./api.js";
import { normalizeOrderStatus } from "../app/lib/status.js";
import { attachKanbanDnD } from "./dnd.js";
import { renderColumns, renderStatusLegend } from "./renderers.js";
import { createBoardStore } from "./store.js";

const store = createBoardStore();

function createFallbackTask() {
  return [
    {
      id: "frontend-demo-task",
      orderId: "frontend-demo-order",
      trackingCode: "KAN-1",
      title: "Подготовить правки к договору поставки",
      clientName: "ООО Вектор",
      contact: "+7 999 123-45-67",
      companyName: "ООО Вектор",
      serviceCode: "CONTRACT_REVIEW",
      serviceType: "Проверка договора",
      description:
        "Временная frontend-карточка для разработки доски. Её можно удалить, когда backend начнёт отдавать реальные задачи.",
      status: "IN_PROGRESS",
      createdAt: "2026-05-06T10:00:00+03:00",
      updatedAt: "2026-05-06T10:00:00+03:00",
      assignedTo: "Юрист команды",
      priority: "HIGH",
      clientRevisionComment: null,
      clientRevisionRequestedAt: null,
      revisionCount: 0,
      documents: [],
      comments: [],
      history: [],
    },
  ];
}

const nodes = {
  alert: document.querySelector("#board-alert"),
  search: document.querySelector("#board-search"),
  assignee: document.querySelector("#board-assignee"),
  refresh: document.querySelector("#refresh-board"),
  syncState: document.querySelector("#sync-state"),
  taskTotal: document.querySelector("#task-total"),
  legend: document.querySelector("#status-legend"),
  columns: document.querySelector("#kanban-columns"),
  editModal: document.querySelector("#board-edit-modal"),
  editForm: document.querySelector("#board-edit-form"),
  editClose: document.querySelector("#board-edit-close"),
  editCancel: document.querySelector("#board-edit-cancel"),
  editSubmit: document.querySelector("#board-edit-submit"),
  editServiceCode: document.querySelector("#board-edit-service-code"),
  editClientName: document.querySelector("#board-edit-client-name"),
  editContact: document.querySelector("#board-edit-contact"),
  editCompanyName: document.querySelector("#board-edit-company-name"),
  editDescription: document.querySelector("#board-edit-description"),
  rejectModal: document.querySelector("#board-reject-modal"),
  rejectForm: document.querySelector("#board-reject-form"),
  rejectClose: document.querySelector("#board-reject-close"),
  rejectCancel: document.querySelector("#board-reject-cancel"),
  rejectSubmit: document.querySelector("#board-reject-submit"),
  rejectReason: document.querySelector("#board-reject-reason"),
};

function setAlert(message = "") {
  nodes.alert.hidden = !message;
  nodes.alert.textContent = message;
}

function setSyncState(text) {
  nodes.syncState.textContent = text;
}

function getFilteredTasks() {
  const query = store.filters.search.trim().toLowerCase();

  return store.tasks.filter((task) => {
    if (normalizeOrderStatus(task.status) === "REJECTED") {
      return false;
    }

    const matchesSearch =
      !query ||
      [task.title, task.clientName, task.contact, task.serviceType, task.trackingCode]
        .join(" ")
        .toLowerCase()
        .includes(query);

    const matchesAssignee =
      store.filters.assignee === "ALL" ||
      (task.assignedTo || "Не назначено") === store.filters.assignee;

    return matchesSearch && matchesAssignee;
  });
}

function getTaskById(taskId) {
  return store.tasks.find((task) => task.id === taskId) || null;
}

function setModalState(node, isOpen) {
  if (!node) {
    return;
  }

  node.hidden = !isOpen;
  document.body.classList.toggle("board-modal-open", isOpen);
}

function closeEditModal() {
  setModalState(nodes.editModal, false);
}

function closeRejectModal() {
  setModalState(nodes.rejectModal, false);
}

function openEditModal(taskId) {
  const task = getTaskById(taskId);
  if (!task) {
    return;
  }

  store.activeTaskId = taskId;
  nodes.editServiceCode.value = task.serviceCode || task.serviceType || "";
  nodes.editClientName.value = task.clientName || "";
  nodes.editContact.value = task.contact || "";
  nodes.editCompanyName.value = task.companyName || "";
  nodes.editDescription.value = task.description || "";
  setModalState(nodes.editModal, true);
}

function openRejectModal(taskId) {
  const task = getTaskById(taskId);
  if (!task) {
    return;
  }

  store.activeTaskId = taskId;
  nodes.rejectReason.value = task.rejectionReason || "";
  setModalState(nodes.rejectModal, true);
}

function patchTask(taskId, updater) {
  store.tasks = store.tasks.map((task) => (task.id === taskId ? updater(task) : task));
}

function removeTask(taskId) {
  store.tasks = store.tasks.filter((task) => task.id !== taskId);
}

function renderAssigneeFilter(tasks) {
  const assignees = Array.from(
    new Set(tasks.map((task) => task.assignedTo).filter(Boolean))
  ).sort((left, right) => left.localeCompare(right, "ru"));

  const currentValue = store.filters.assignee;
  nodes.assignee.innerHTML = '<option value="ALL">Все</option>';

  assignees.forEach((name) => {
    const option = document.createElement("option");
    option.value = name;
    option.textContent = name;
    nodes.assignee.append(option);
  });

  nodes.assignee.value = assignees.includes(currentValue) ? currentValue : "ALL";
  store.filters.assignee = nodes.assignee.value;
}

async function selectTask(taskId) {
  store.selectedTaskId = taskId;
  render();
}

async function loadBoard() {
  setSyncState("Загрузка…");

  try {
    store.tasks = await fetchBoardTasks();
    store.usingFallbackData = false;
    store.errorMessage = "";

    if (!store.tasks.length) {
      store.tasks = createFallbackTask();
      store.usingFallbackData = true;
      setAlert("Backend пока не вернул задачи. Показываю одну временную карточку для разработки доски.");
      setSyncState("Временный пример");
    } else {
      setAlert("");
      setSyncState("Синхронизировано");
    }

    renderAssigneeFilter(store.tasks);
  } catch (error) {
    store.tasks = createFallbackTask();
    store.usingFallbackData = true;
    store.selectedTaskId = null;
    store.selectedTask = null;
    store.errorMessage =
      "Не удалось получить задачи из backend. Показываю одну временную карточку, чтобы можно было собрать и проверить доску.";
    setAlert(store.errorMessage);
    renderAssigneeFilter(store.tasks);
    setSyncState("Временный пример");
  }

  render();
}

async function handleDrop(nextStatusCode) {
  if (!store.draggingTaskId) {
    return;
  }

  const task = store.tasks.find((item) => item.id === store.draggingTaskId);
  if (!task || task.status === nextStatusCode) {
    store.draggingTaskId = null;
    return;
  }

  if (store.usingFallbackData) {
    store.tasks = store.tasks.map((item) =>
      item.id === task.id
        ? {
            ...item,
            status: nextStatusCode,
            updatedAt: new Date().toISOString(),
          }
        : item
    );
    setSyncState("Временный пример");
    setAlert("Сейчас двигается временная карточка frontend-заглушки. Потом её можно удалить.");
    store.draggingTaskId = null;
    render();
    return;
  }

  setSyncState("Сохраняю статус…");

  try {
    const updatedTask = await updateTaskStatus(task.id, nextStatusCode);

    store.tasks = store.tasks.map((item) => (item.id === updatedTask.id ? updatedTask : item));
    setSyncState("Синхронизировано");
    setAlert("");
  } catch (error) {
    setSyncState("Ошибка API");
    setAlert("Не удалось обновить статус задачи. Попробуй ещё раз после проверки backend.");
  } finally {
    store.draggingTaskId = null;
    render();
  }
}

async function handleTaskEdit(taskId) {
  openEditModal(taskId);
}

async function handleTaskReject(taskId) {
  openRejectModal(taskId);
}

async function handleTaskDelete(taskId) {
  const task = getTaskById(taskId);
  if (!task) {
    return;
  }

  const confirmed = window.confirm(
    `Удалить заявку «${task.title}»? После этого карточка исчезнет с доски.`
  );

  if (!confirmed) {
    return;
  }

  setSyncState("Удаляю заявку…");

  if (store.usingFallbackData) {
    removeTask(taskId);
    setAlert("Удалена временная карточка frontend-заглушки.");
    setSyncState("Временный пример");
    render();
    return;
  }

  try {
    await deleteStaffTask(taskId);
    removeTask(taskId);
    setAlert("");
    setSyncState("Синхронизировано");
  } catch (error) {
    setSyncState("Ошибка API");
    setAlert("Не удалось удалить заявку с доски. Проверь backend-контракт staff delete endpoint.");
  }

  render();
}

async function handleEditSubmit(event) {
  event.preventDefault();

  const task = getTaskById(store.activeTaskId);
  if (!task) {
    return;
  }

  const payload = {
    serviceCode: nodes.editServiceCode.value.trim(),
    clientName: nodes.editClientName.value.trim(),
    contact: nodes.editContact.value.trim(),
    companyName: nodes.editCompanyName.value.trim(),
    description: nodes.editDescription.value.trim(),
  };

  nodes.editSubmit.disabled = true;
  setSyncState("Сохраняю заявку…");

  if (store.usingFallbackData) {
    patchTask(task.id, (item) => ({
      ...item,
      serviceCode: payload.serviceCode,
      serviceType: payload.serviceCode || item.serviceType,
      clientName: payload.clientName,
      contact: payload.contact,
      companyName: payload.companyName,
      description: payload.description,
      updatedAt: new Date().toISOString(),
    }));
    setAlert("Обновлена временная карточка frontend-заглушки.");
    setSyncState("Временный пример");
    closeEditModal();
    nodes.editSubmit.disabled = false;
    render();
    return;
  }

  try {
    const updatedTask = await updateStaffTask(task.id, payload);
    patchTask(task.id, () => updatedTask);
    setAlert("");
    setSyncState("Синхронизировано");
    closeEditModal();
  } catch (error) {
    setSyncState("Ошибка API");
    setAlert("Не удалось сохранить изменения заявки. Проверь staff update endpoint.");
  } finally {
    nodes.editSubmit.disabled = false;
    render();
  }
}

async function handleRejectSubmit(event) {
  event.preventDefault();

  const task = getTaskById(store.activeTaskId);
  if (!task) {
    return;
  }

  const reason = nodes.rejectReason.value.trim();
  if (!reason) {
    setAlert("Для отклонения заявки нужно указать причину.");
    return;
  }

  nodes.rejectSubmit.disabled = true;
  setSyncState("Отклоняю заявку…");

  if (store.usingFallbackData) {
    removeTask(task.id);
    setAlert(`Временная карточка отклонена: ${reason}`);
    setSyncState("Временный пример");
    closeRejectModal();
    nodes.rejectSubmit.disabled = false;
    render();
    return;
  }

  try {
    await rejectStaffTask(task.id, reason);
    removeTask(task.id);
    setAlert("");
    setSyncState("Синхронизировано");
    closeRejectModal();
  } catch (error) {
    setSyncState("Ошибка API");
    setAlert("Не удалось отклонить заявку. Проверь staff reject endpoint.");
  } finally {
    nodes.rejectSubmit.disabled = false;
    render();
  }
}

function render() {
  const filteredTasks = getFilteredTasks();
  nodes.taskTotal.textContent = `${filteredTasks.length} задач`;

  renderStatusLegend(nodes.legend, filteredTasks);
  renderColumns({
    container: nodes.columns,
    tasks: filteredTasks,
    selectedTaskId: store.selectedTaskId,
    onTaskSelect: selectTask,
    onTaskDragStart: (taskId) => {
      store.draggingTaskId = taskId;
    },
    onTaskDragEnd: () => {
      store.draggingTaskId = null;
    },
    onTaskEdit: handleTaskEdit,
    onTaskReject: handleTaskReject,
    onTaskDelete: handleTaskDelete,
  });

  attachKanbanDnD({
    container: nodes.columns,
    onDrop: handleDrop,
  });
}

function attachEvents() {
  nodes.search.addEventListener("input", (event) => {
    store.filters.search = event.target.value;
    render();
  });

  nodes.assignee.addEventListener("change", (event) => {
    store.filters.assignee = event.target.value;
    render();
  });

  nodes.refresh.addEventListener("click", loadBoard);
  nodes.editForm?.addEventListener("submit", handleEditSubmit);
  nodes.rejectForm?.addEventListener("submit", handleRejectSubmit);
  nodes.editClose?.addEventListener("click", closeEditModal);
  nodes.editCancel?.addEventListener("click", closeEditModal);
  nodes.rejectClose?.addEventListener("click", closeRejectModal);
  nodes.rejectCancel?.addEventListener("click", closeRejectModal);
  nodes.editModal?.addEventListener("click", (event) => {
    if (event.target === nodes.editModal) {
      closeEditModal();
    }
  });
  nodes.rejectModal?.addEventListener("click", (event) => {
    if (event.target === nodes.rejectModal) {
      closeRejectModal();
    }
  });
}

export async function initBoardApp() {
  attachEvents();
  await loadBoard();
}
