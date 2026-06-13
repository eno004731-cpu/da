const ORDER_SNAPSHOT_STORAGE_KEY = "LEGAL_ORDER_SNAPSHOT_V1";
const ORDER_SNAPSHOT_TTL_MS = 10 * 60 * 1000;

function getStorage() {
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

function getOrderIdentity(order = {}) {
  return String(order.orderId ?? order.id ?? "").trim();
}

function safeParseSnapshot(value) {
  if (!value) {
    return null;
  }

  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
}

function isExpired(snapshot) {
  const storedAt = Number(snapshot?.storedAt ?? 0);
  return !storedAt || Date.now() - storedAt > ORDER_SNAPSHOT_TTL_MS;
}

export function cacheOrderSnapshot(order) {
  if (!order || typeof order !== "object") {
    return "";
  }

  const orderId = getOrderIdentity(order);
  const storage = getStorage();

  if (!storage || !orderId) {
    return orderId;
  }

  storage.setItem(
    ORDER_SNAPSHOT_STORAGE_KEY,
    JSON.stringify({
      orderId,
      storedAt: Date.now(),
      order,
    })
  );

  return orderId;
}

export function readOrderSnapshot(expectedOrderId = "") {
  const storage = getStorage();
  if (!storage) {
    return null;
  }

  const snapshot = safeParseSnapshot(storage.getItem(ORDER_SNAPSHOT_STORAGE_KEY));
  if (!snapshot || isExpired(snapshot)) {
    storage.removeItem(ORDER_SNAPSHOT_STORAGE_KEY);
    return null;
  }

  const snapshotOrderId = String(snapshot.orderId || "").trim();
  const normalizedExpectedId = String(expectedOrderId || "").trim();

  if (normalizedExpectedId && snapshotOrderId && snapshotOrderId !== normalizedExpectedId) {
    return null;
  }

  return snapshot.order && typeof snapshot.order === "object" ? snapshot.order : null;
}

export function clearOrderSnapshot(expectedOrderId = "") {
  const storage = getStorage();
  if (!storage) {
    return;
  }

  if (!expectedOrderId) {
    storage.removeItem(ORDER_SNAPSHOT_STORAGE_KEY);
    return;
  }

  const snapshot = safeParseSnapshot(storage.getItem(ORDER_SNAPSHOT_STORAGE_KEY));
  const snapshotOrderId = String(snapshot?.orderId || "").trim();

  if (snapshotOrderId === String(expectedOrderId).trim()) {
    storage.removeItem(ORDER_SNAPSHOT_STORAGE_KEY);
  }
}
