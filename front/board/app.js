import { fetchBoardTasks, updateTaskStatus } from "./api.js";
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
}

export async function initBoardApp() {
  attachEvents();
  await loadBoard();
}
