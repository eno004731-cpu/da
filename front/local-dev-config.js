(function configureLocalDevApiBase() {
  const host = window.location.hostname;
  const isLocalHost = host === "127.0.0.1" || host === "localhost";

  if (!isLocalHost) {
    return;
  }

  // На текущем локальном этапе поднимаем только auth-service на 8081.
  // API заявок оставляем отключённым, пока пользователь не реализует его отдельно.
  window.__LEGAL_LOCAL_AUTH_ONLY__ ??= true;
  window.__LEGAL_DISABLE_ORDERS_API__ ??= window.__LEGAL_LOCAL_AUTH_ONLY__;
  window.__LEGAL_API_BASE_URL__ ||= "http://127.0.0.1:8080/api";
  window.__LEGAL_AUTH_API_BASE_URL__ ||= "http://127.0.0.1:8081/api";

  // Google auth уже можно проверять локально даже в auth-only режиме,
  // потому что он живёт в отдельном auth-service на 8081.
  window.__LEGAL_AUTH_GOOGLE_ENABLED__ ??= true;
  window.__LEGAL_AUTH_ACCOUNT_DELETE_ENABLED__ ??= !window.__LEGAL_LOCAL_AUTH_ONLY__;
})();
