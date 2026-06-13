import {
  checkBackendAvailability,
  fetchCurrentUser,
  isBackendUnavailableError,
  isUnauthorizedError,
  logoutClient,
} from "../api/auth-api.js?v=20260524c";
import {
  buildAuthUrl,
  buildCompleteProfileUrl,
  clearSession,
  getCurrentUser,
  isAuthenticated,
  requiresProfileCompletion,
  shouldRequirePasswordCompletion,
  updateSessionUser,
} from "../state/auth-store.js?v=20260524a";
import { LOCAL_AUTH_ONLY_MODE, ORDERS_API_ENABLED } from "./runtime.js";
import { getReturnToContactUrl } from "./routes.js";

export function createAuthGateController({
  dom,
  state,
  showIdleStatus,
  syncContactRuntimeCopy,
  setBackendAvailabilityState,
  getAuthOnlyModeMessage,
}) {
  function syncClientAuthState() {
    const authenticated = isAuthenticated();
    const user = getCurrentUser();
    const needsCompletion = requiresProfileCompletion(user);
    const passwordSetupRequired = shouldRequirePasswordCompletion(user);
    const loginHref = buildAuthUrl("login", getReturnToContactUrl(), {
      switchAccount: authenticated,
    });
    const registerHref = buildAuthUrl("register", getReturnToContactUrl());
    const completionHref = buildCompleteProfileUrl(getReturnToContactUrl());

    if (dom.headerLoginLink) {
      dom.headerLoginLink.href = loginHref;
      dom.headerLoginLink.textContent = "Войти";
      dom.headerLoginLink.hidden = authenticated;
    }

    if (dom.headerRegisterLink) {
      dom.headerRegisterLink.href = registerHref;
      dom.headerRegisterLink.hidden = authenticated;
    }

    if (dom.headerCabinetLink) {
      dom.headerCabinetLink.href = needsCompletion ? completionHref : "./cabinet.html";
      dom.headerCabinetLink.textContent = needsCompletion
        ? "Завершить регистрацию"
        : "Личный кабинет";
      dom.headerCabinetLink.hidden = !authenticated;
    }

    if (dom.headerLogoutButton) {
      dom.headerLogoutButton.hidden = !authenticated;
    }

    if (dom.applicationLoginLink) {
      dom.applicationLoginLink.href = loginHref;
    }

    if (dom.applicationRegisterLink) {
      dom.applicationRegisterLink.href = registerHref;
    }

    if (dom.applicationAuthGate) {
      dom.applicationAuthGate.hidden = authenticated;
    }

    if (dom.applicationAuthSession) {
      dom.applicationAuthSession.hidden = !authenticated;
    }

    if (dom.contactForm) {
      dom.contactForm.hidden =
        !authenticated || !state.backendIsAvailable || !ORDERS_API_ENABLED;
    }

    syncContactRuntimeCopy(authenticated);

    if (!authenticated || !user) {
      return;
    }

    if (dom.applicationUserName) {
      dom.applicationUserName.textContent = user.fullName || "Регистрация ещё не завершена";
    }

    if (dom.applicationUserEmail) {
      dom.applicationUserEmail.textContent =
        [user.email, user.companyName].filter(Boolean).join(" • ") ||
        "Добавьте email и имя, чтобы завершить аккаунт.";
    }

    if (dom.applicationCabinetLink) {
      dom.applicationCabinetLink.href = needsCompletion ? completionHref : "./cabinet.html";
      dom.applicationCabinetLink.textContent = needsCompletion
        ? "Завершить регистрацию"
        : "Открыть кабинет";
    }

    if (dom.applicationAuthSessionCopy) {
      if (needsCompletion) {
        dom.applicationAuthSessionCopy.textContent = passwordSetupRequired
          ? "Вход уже выполнен, но аккаунт ещё не завершён: добавьте имя, email и задайте пароль, чтобы потом входить не только через Google."
          : "Вход уже выполнен, но аккаунт ещё не завершён: добавьте имя и email, после чего кабинет будет доступен полностью.";
      } else {
        dom.applicationAuthSessionCopy.textContent = LOCAL_AUTH_ONLY_MODE
          ? "Вы вошли в отдельный auth-service. Сейчас можно проверить кабинет, сохранение сессии после reload и явный logout. Заявки и документы появятся после подключения orders API."
          : "После входа можно открыть кабинет клиента и продолжить работу с заявками.";
      }
    }

    if (dom.contactNameInput && !dom.contactNameInput.value) {
      dom.contactNameInput.value = user.fullName || "";
    }

    if (dom.companyNameInput && !dom.companyNameInput.value) {
      dom.companyNameInput.value = user.companyName || "";
    }
  }

  async function syncClientAuthStateWithBackend() {
    if (LOCAL_AUTH_ONLY_MODE) {
      if (!getCurrentUser()) {
        setBackendAvailabilityState(false, getAuthOnlyModeMessage());
        syncClientAuthState();
        return false;
      }

      try {
        const user = await fetchCurrentUser();

        if (!user) {
          clearSession();
          setBackendAvailabilityState(false, getAuthOnlyModeMessage());
        } else {
          updateSessionUser(user);
          setBackendAvailabilityState(false, getAuthOnlyModeMessage());
        }
      } catch (error) {
        if (isUnauthorizedError(error)) {
          clearSession();
          setBackendAvailabilityState(false, getAuthOnlyModeMessage());
        } else if (isBackendUnavailableError(error)) {
          setBackendAvailabilityState(
            false,
            "Auth-service на 8081 временно недоступен. Войти сейчас нельзя, а API заявок локально ещё не подключён."
          );
        } else {
          setBackendAvailabilityState(false, getAuthOnlyModeMessage());
        }
      }

      syncClientAuthState();
      return false;
    }

    if (!getCurrentUser()) {
      try {
        await checkBackendAvailability();
        setBackendAvailabilityState(true);
      } catch (error) {
        if (isBackendUnavailableError(error)) {
          setBackendAvailabilityState(
            false,
            "Backend временно недоступен. Можно открыть форму входа, но кабинет и отправка заявок заработают только после запуска API."
          );
        } else {
          setBackendAvailabilityState(true);
        }
      }

      syncClientAuthState();
      return false;
    }

    try {
      const user = await fetchCurrentUser();

      if (!user) {
        clearSession();
        setBackendAvailabilityState(true);
      } else {
        updateSessionUser(user);
        setBackendAvailabilityState(true);
      }
    } catch (error) {
      if (isUnauthorizedError(error)) {
        clearSession();
        setBackendAvailabilityState(true);
      } else if (isBackendUnavailableError(error)) {
        setBackendAvailabilityState(
          false,
          "Backend временно недоступен. Сессия сохранена локально, но кабинет и отправка заявок будут ограничены, пока API снова не начнёт отвечать."
        );
      } else {
        setBackendAvailabilityState(true);
      }
    }

    syncClientAuthState();
    return state.backendIsAvailable && Boolean(getCurrentUser());
  }

  async function handleLogout(button) {
    if (!button) {
      return;
    }

    button.disabled = true;
    await logoutClient().catch(() => null);
    clearSession();
    syncClientAuthState();
    button.disabled = false;
  }

  function bindLogoutButtons() {
    dom.applicationLogoutButton?.addEventListener("click", async () => {
      await handleLogout(dom.applicationLogoutButton);
    });

    dom.headerLogoutButton?.addEventListener("click", async () => {
      await handleLogout(dom.headerLogoutButton);
    });
  }

  return {
    syncClientAuthState,
    syncClientAuthStateWithBackend,
    bindLogoutButtons,
  };
}
