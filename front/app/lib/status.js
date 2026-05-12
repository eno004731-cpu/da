export const ORDER_STATUS_LABELS = {
  TODO: "Новая",
  NEW: "Новая",
  IN_PROGRESS: "В работе",
  ON_REVIEW: "В проверке",
  REVIEW: "В проверке",
  REWORK: "На доработке",
  DONE: "Завершено",
  REJECTED: "Отклонено",
};

export const ORDER_STATUS_TIMELINE = [
  { code: "TODO", label: "Новая" },
  { code: "IN_PROGRESS", label: "В работе" },
  { code: "ON_REVIEW", label: "В проверке" },
  { code: "REWORK", label: "На доработке" },
  { code: "DONE", label: "Завершено" },
];

export const ORDER_STATUS_COLUMNS = [
  { code: "TODO", title: "Новые", accent: "#4e8cff" },
  { code: "IN_PROGRESS", title: "В работе", accent: "#3fb884" },
  { code: "ON_REVIEW", title: "В проверке", accent: "#f2b24a" },
  { code: "REWORK", title: "На доработке", accent: "#b27cff" },
  { code: "DONE", title: "Завершено", accent: "#76879b" },
];

export function getOrderStatusLabel(status) {
  const normalizedStatus = normalizeOrderStatus(status);
  return ORDER_STATUS_LABELS[normalizedStatus] || normalizedStatus || status;
}

export function isCompletedStatus(status) {
  return normalizeOrderStatus(status) === "DONE";
}

export function isRejectedStatus(status) {
  return normalizeOrderStatus(status) === "REJECTED";
}

export function isUnavailableDocumentStatus(status) {
  const normalizedStatus = normalizeOrderStatus(status);
  return normalizedStatus === "DONE" || normalizedStatus === "REJECTED";
}

export function isReworkStatus(status) {
  return normalizeOrderStatus(status) === "REWORK";
}

export function normalizeOrderStatus(status) {
  if (status === "NEW") {
    return "TODO";
  }

  if (status === "REVIEW") {
    return "ON_REVIEW";
  }

  return status;
}
