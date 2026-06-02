import {
  fetchCurrentUser,
  isBackendUnavailableError,
  isUnauthorizedError,
  logoutClient,
  requestEmailVerification,
  updateCurrentUserProfile,
} from "../api/auth-api.js?v=20260525c";
import {
  buildAuthUrl,
  buildCompleteProfileUrl,
  clearSession,
  getCurrentUser,
  requiresProfileCompletion,
  updateSessionUser,
} from "../state/auth-store.js?v=20260525a";

const DEFAULT_NEXT_URL = "./settings.html";

const feedbackNode = document.querySelector("#settings-feedback");
const logoutButton = document.querySelector("#settings-logout");
const backLink = document.querySelector("#settings-back-link");

const profileForm = document.querySelector("#settings-profile-form");
const fullNameField = document.querySelector("#settings-full-name");
const emailField = document.querySelector("#settings-email");
const phoneField = document.querySelector("#settings-phone");
const companyNameField = document.querySelector("#settings-company-name");
const profileSubmitButton = document.querySelector("#settings-profile-submit");

const passwordForm = document.querySelector("#settings-password-form");
const passwordSubmitButton = document.querySelector("#settings-password-submit");

const verificationEmailNode = document.querySelector("#settings-verification-email");
const verificationStatusNode = document.querySelector("#settings-verification-status");
const resendVerificationButton = document.querySelector("#settings-resend-verification");

function setFeedback(message = "", isError = false) {
  if (!feedbackNode) {
    return;
  }

  feedbackNode.hidden = !message;
  feedbackNode.textContent = message;
  feedbackNode.classList.toggle("is-error", isError);
  feedbackNode.classList.toggle("is-success", !isError && Boolean(message));
}

function renderUser(user) {
  if (!user) {
    return;
  }

  if (fullNameField) {
    fullNameField.value = user.fullName || "";
  }

  if (emailField) {
    emailField.value = user.email || "";
  }

  if (phoneField) {
    phoneField.value = user.phone || "";
  }

  if (companyNameField) {
    companyNameField.value = user.companyName || "";
  }

  if (verificationEmailNode) {
    verificationEmailNode.textContent = user.email || "Email не указан.";
  }

  if (verificationStatusNode) {
    if (typeof user.emailVerified === "boolean") {
      verificationStatusNode.textContent = user.emailVerified
        ? "Email подтверждён."
        : "Email ещё не подтверждён.";
    } else {
      verificationStatusNode.textContent = "Статус подтверждения пока не передаётся backend.";
    }
  }
}

function normalizeNextUrl(candidate) {
  const value = String(candidate || "").trim();
  return value || DEFAULT_NEXT_URL;
}

function redirectToLogin() {
  clearSession();
  window.location.href = buildAuthUrl("login", DEFAULT_NEXT_URL);
}

async function ensureActiveSession() {
  const cachedUser = getCurrentUser();

  if (!cachedUser) {
    redirectToLogin();
    return false;
  }

  try {
    const user = await fetchCurrentUser();

    if (!user) {
      redirectToLogin();
      return false;
    }

    updateSessionUser(user);

    if (requiresProfileCompletion(user)) {
      window.location.href = buildCompleteProfileUrl(DEFAULT_NEXT_URL);
      return false;
    }

    return true;
  } catch (error) {
    if (isUnauthorizedError(error)) {
      redirectToLogin();
      return false;
    }

    if (isBackendUnavailableError(error)) {
      renderUser(cachedUser);
      setFeedback(
        "Backend временно недоступен. Настройки открыты по сохранённой сессии, но часть действий может не сработать.",
        true
      );
      return true;
    }

    setFeedback(error.message || "Не удалось проверить активную сессию.", true);
    return false;
  }
}

function buildProfilePayload() {
  return {
    fullName: String(fullNameField?.value || "").trim(),
    phone: String(phoneField?.value || "").trim(),
    companyName: String(companyNameField?.value || "").trim(),
  };
}

async function handleProfileSubmit(event) {
  event.preventDefault();

  const payload = buildProfilePayload();
  if (!payload.fullName) {
    setFeedback("Имя и фамилия не могут быть пустыми.", true);
    return;
  }

  profileSubmitButton.disabled = true;
  setFeedback("Сохраняем профиль…");

  try {
    const updatedUser = await updateCurrentUserProfile(payload);
    updateSessionUser(updatedUser);
    renderUser(updatedUser);
    setFeedback("Профиль обновлён.");
  } catch (error) {
    if (isUnauthorizedError(error)) {
      redirectToLogin();
      return;
    }

    if (isBackendUnavailableError(error)) {
      setFeedback("Backend временно недоступен. Профиль не удалось сохранить.", true);
      return;
    }

    setFeedback(error.message || "Не удалось сохранить профиль.", true);
  } finally {
    profileSubmitButton.disabled = false;
  }
}

function handlePasswordSubmit(event) {
  event.preventDefault();
  passwordSubmitButton.disabled = true;
  setFeedback("Смена пароля пока не реализована в backend.", true);
  window.setTimeout(() => {
    passwordSubmitButton.disabled = false;
  }, 200);
}

async function handleResendVerification() {
  resendVerificationButton.disabled = true;
  setFeedback("Отправляем письмо подтверждения…");

  try {
    await requestEmailVerification();
    setFeedback("Письмо подтверждения отправлено.");
  } catch (error) {
    if (isUnauthorizedError(error)) {
      redirectToLogin();
      return;
    }

    if (isBackendUnavailableError(error)) {
      setFeedback("Backend временно недоступен. Письмо не удалось отправить.", true);
      return;
    }

    setFeedback(error.message || "Не удалось отправить письмо подтверждения.", true);
  } finally {
    resendVerificationButton.disabled = false;
  }
}

async function handleLogout() {
  logoutButton.disabled = true;
  await logoutClient().catch(() => null);
  clearSession();
  window.location.href = buildAuthUrl("login", DEFAULT_NEXT_URL);
}

async function init() {
  backLink?.setAttribute("href", normalizeNextUrl("./cabinet.html"));

  const sessionIsActive = await ensureActiveSession();
  if (!sessionIsActive) {
    return;
  }

  renderUser(getCurrentUser());

  profileForm?.addEventListener("submit", handleProfileSubmit);
  passwordForm?.addEventListener("submit", handlePasswordSubmit);
  resendVerificationButton?.addEventListener("click", handleResendVerification);
  logoutButton?.addEventListener("click", handleLogout);
}

init();
