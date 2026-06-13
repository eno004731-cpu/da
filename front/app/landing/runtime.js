import { LOCAL_AUTH_ONLY_MODE, ORDERS_API_ENABLED } from "../api/endpoints.js?v=20260524c";

export { LOCAL_AUTH_ONLY_MODE, ORDERS_API_ENABLED };

export function createRuntimeController({ dom, state, showIdleStatus }) {
  function getAuthOnlyModeMessage() {
    return "Локально сейчас подключён только сервис авторизации на 8081. Вход и профиль клиента работают, а API заявок будет подключено позже.";
  }

  function syncContactRuntimeCopy(authenticated) {
    const authOnlyRuntime = LOCAL_AUTH_ONLY_MODE || !ORDERS_API_ENABLED;

    if (dom.contactTitle) {
      dom.contactTitle.textContent = authOnlyRuntime
        ? "Клиентский доступ и будущая форма заявки"
        : "Оставьте заявку на юридическую задачу";
    }

    if (dom.contactDescription) {
      dom.contactDescription.textContent = authOnlyRuntime
        ? "Локально сейчас можно полноценно проверить регистрацию, вход, reload, logout и профиль клиента. Отправка заявок, документы и статусы появятся после подключения отдельного API заказов."
        : "Клиент создаёт заявку только после входа или регистрации. Это нужно, чтобы документы, статусы и доработки были привязаны к личному кабинету, а не терялись между письмами и мессенджерами.";
    }

    if (dom.applicationAuthGateTitle) {
      dom.applicationAuthGateTitle.textContent = authOnlyRuntime
        ? "Войдите, чтобы открыть кабинет клиента"
        : "Войдите, чтобы отправить заявку и прикрепить документы";
    }

    if (dom.applicationAuthGateCopy) {
      dom.applicationAuthGateCopy.textContent = authOnlyRuntime
        ? "Локально в этом режиме доступна только авторизация клиента. После входа можно открыть кабинет и проверить сохранение сессии, а отправка заявок будет подключена позже."
        : "После входа заявка появится в вашем кабинете, а юрист увидит её на рабочей доске.";
    }

    if (dom.applicationAuthSessionCopy) {
      dom.applicationAuthSessionCopy.textContent = authOnlyRuntime
        ? "Вы вошли в отдельный auth-service. Сейчас можно проверить кабинет, сохранение сессии после reload и явный logout. Заявки и документы появятся после подключения orders API."
        : "После входа можно открыть кабинет клиента и продолжить работу с заявками.";
    }

    if (!authenticated && dom.formStatus && authOnlyRuntime) {
      showIdleStatus();
    }
  }

  function setBackendAvailabilityState(isAvailable, message = "") {
    state.backendIsAvailable = isAvailable;

    if (!dom.backendStatus) {
      return;
    }

    dom.backendStatus.hidden = isAvailable;
    dom.backendStatus.textContent =
      message ||
      "Backend временно недоступен. Вход и навигация остаются доступны, но кабинет и отправка заявок могут не работать.";
  }

  return {
    getAuthOnlyModeMessage,
    syncContactRuntimeCopy,
    setBackendAvailabilityState,
  };
}
