export function createBoardStore() {
  return {
    tasks: [],
    selectedTaskId: null,
    selectedTask: null,
    usingFallbackData: false,
    filters: {
      search: "",
      assignee: "ALL",
    },
    activeTaskId: null,
    draggingTaskId: null,
    syncState: "idle",
    errorMessage: "",
  };
}
