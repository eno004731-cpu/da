export const ORDER_STATUS_LABELS = {
  TODO: "To Do",
  NEW: "To Do",
  IN_PROGRESS: "In Progress",
  ON_REVIEW: "In Progress",
  REVIEW: "In Progress",
  REWORK: "На доработке",
  DONE: "Завершено",
};

export const ORDER_STATUS_TIMELINE = [
  { code: "TODO", label: "To Do" },
  { code: "IN_PROGRESS", label: "In Progress" },
  { code: "REWORK", label: "На доработке" },
  { code: "DONE", label: "Done" },
];

export const ORDER_STATUS_COLUMNS = [
  { code: "TODO", title: "To Do", accent: "#4e8cff" },
  { code: "IN_PROGRESS", title: "In Progress", accent: "#3fb884" },
  { code: "REWORK", title: "Rework", accent: "#b27cff" },
  { code: "DONE", title: "Done", accent: "#76879b" },
];

export function getOrderStatusLabel(status) {
  const normalizedStatus = normalizeOrderStatus(status);
  return ORDER_STATUS_LABELS[normalizedStatus] || normalizedStatus || status;
}

export function isCompletedStatus(status) {
  return normalizeOrderStatus(status) === "DONE";
}

export function isReworkStatus(status) {
  return normalizeOrderStatus(status) === "REWORK";
}

export function normalizeOrderStatus(status) {
  if (status === "NEW") {
    return "TODO";
  }

  if (status === "REVIEW" || status === "ON_REVIEW") {
    return "IN_PROGRESS";
  }

  return status;
}
