export function attachKanbanDnD({ container, onDrop }) {
  const columns = Array.from(container.querySelectorAll(".kanban-column"));

  columns.forEach((column) => {
    column.addEventListener("dragover", (event) => {
      event.preventDefault();
      column.classList.add("is-drop-target");
    });

    column.addEventListener("dragleave", () => {
      column.classList.remove("is-drop-target");
    });

    column.addEventListener("drop", (event) => {
      event.preventDefault();
      column.classList.remove("is-drop-target");
      const statusCode = column.dataset.statusCode;
      if (statusCode) {
        onDrop(statusCode);
      }
    });
  });
}
