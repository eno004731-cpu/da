(function configureLocalDevApiBase() {
  const host = window.location.hostname;
  const isLocalHost = host === "127.0.0.1" || host === "localhost";

  if (!isLocalHost) {
    return;
  }

  // Локально auth и orders живут отдельно, поэтому явно разводим их по разным base URL.
  // Здесь намеренно не используем ??= для feature flags: runtime-config может
  // остаться от старой auth-only сборки, а в localhost нам нужно всегда пробовать
  // настоящий orders API, чтобы ошибки backend были видны в Network/Console.
  window.__LEGAL_LOCAL_AUTH_ONLY__ = false;
  window.__LEGAL_DISABLE_ORDERS_API__ = false;
  window.__LEGAL_API_BASE_URL__ ||= "http://127.0.0.1:8083/api";
  window.__LEGAL_AUTH_API_BASE_URL__ ||= "http://127.0.0.1:8081/api";
  // Документы загружаются напрямую в document-service, минуя старый REST proxy order-service.
  window.__LEGAL_DOCUMENT_API_BASE_URL__ ||= "http://127.0.0.1:8085/api";

  // Google auth уже можно проверять локально даже в auth-only режиме,
  // потому что он живёт в отдельном auth-service на 8081.
  window.__LEGAL_AUTH_GOOGLE_ENABLED__ ??= true;
  window.__LEGAL_AUTH_ACCOUNT_DELETE_ENABLED__ ??= true;
})();
