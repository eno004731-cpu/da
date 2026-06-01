import {
  completeGoogleRegistration,
  fetchCurrentUser,
  getPendingGoogleProfileDraft,
  isBackendUnavailableError,
  isUnauthorizedError,
  logoutClient,
  updateCurrentUserProfile,
} from "../api/auth-api.js?v=20260525c";
import {
  buildAuthUrl,
  clearPendingGoogleCompletion,
  clearSession,
  getCurrentUser,
  getPendingGoogleCompletion,
  getSession,
  requiresProfileCompletion,
  resolvePostAuthUrl,
  setSession,
  shouldRequirePasswordCompletion,
  updateSessionUser,
} from "../state/auth-store.js?v=20260525a";
import {
  getPasswordPolicyHint,
  validatePasswordPolicy,
} from "../utils/password-policy.js";

const DEFAULT_NEXT_URL = "./cabinet.html";
const params = new URLSearchParams(window.location.search);
const nextUrl = normalizeNextUrl(params.get("next"));

const form = document.querySelector("#complete-profile-form");
const feedbackNode = document.querySelector("#complete-profile-feedback");
const fullNameField = document.querySelector("#complete-profile-full-name");
const passwordCard = document.querySelector("#completion-password-card");
const passwordPoint = document.querySelector("#completion-password-point");
const passwordField = document.querySelector("#complete-profile-password");
const passwordRepeatField = document.querySelector("#complete-profile-password-repeat");
const submitButton = document.querySelector("#complete-profile-submit");
const logoutButton = document.querySelector("#complete-profile-logout");
const nextLink = document.querySelector("#complete-profile-next-link");
const backLink = document.querySelector("#complete-profile-back-link");
const pendingGoogleCompletion = getPendingGoogleCompletion();

function normalizeNextUrl(candidate) {
  const value = String(candidate || "").trim();

  if (!value || value.includes("complete-profile.html")) {
    return DEFAULT_NEXT_URL;
  }

  return value;
}

function setFeedback(message = "", isError = false) {
  if (!feedbackNode) {
    return;
  }

  feedbackNode.hidden = !message;
  feedbackNode.textContent = message;
  feedbackNode.classList.toggle("is-error", isError);
  feedbackNode.classList.toggle("is-success", !isError && Boolean(message));
}

function setPasswordMode(required) {
  if (passwordCard) {
    passwordCard.hidden = !required;
  }

  if (passwordPoint) {
    passwordPoint.hidden = !required;
  }

  if (passwordField) {
    passwordField.required = required;
  }

  if (passwordRepeatField) {
    passwordRepeatField.required = required;
  }
}

function fillForm(user) {
  if (!user) {
    return;
  }

  if (fullNameField) {
    fullNameField.value = user.fullName || "";
  }
}

function syncLinks() {
  nextLink?.setAttribute("href", nextUrl);
  backLink?.setAttribute("href", nextUrl);
}

function redirectToLogin() {
  clearPendingGoogleCompletion();
  clearSession();
  window.location.href = buildAuthUrl("login", nextUrl);
}

function redirectIfProfileCompleted(user) {
  if (!requiresProfileCompletion(user)) {
    window.location.href = resolvePostAuthUrl(user, nextUrl);
    return true;
  }

  return false;
}

function isGoogleCompletionFlow() {
  return Boolean(pendingGoogleCompletion?.flowToken);
}

function buildGoogleDraftUser() {
  const profile = getPendingGoogleProfileDraft();
  if (!profile) {
    return null;
  }

  return {
    ...profile,
    isOAuthUser: true,
    authProvider: "google",
    authProviders: ["google"],
    hasPassword: false,
    needsPasswordSetup: true,
    requiresProfileCompletion: true,
  };
}

async function resolveActiveUser() {
  if (isGoogleCompletionFlow()) {
    const draftUser = buildGoogleDraftUser();
    if (!draftUser) {
      redirectToLogin();
      return null;
    }

    return draftUser;
  }

  const session = getSession();
  if (!session?.user) {
    redirectToLogin();
    return null;
  }

  try {
    const user = await fetchCurrentUser();
    if (!user) {
      redirectToLogin();
      return null;
    }

    updateSessionUser(user);
    return user;
  } catch (error) {
    if (isUnauthorizedError(error)) {
      redirectToLogin();
      return null;
    }

    if (isBackendUnavailableError(error)) {
      // Если auth-service временно недоступен, сохраняем локальную сессию
      // и даём пользователю хотя бы увидеть completion form без forced logout.
      setFeedback(
        "Auth-service временно недоступен. Можно проверить форму, но сохранить профиль получится после восстановления backend.",
        true
      );
      return getCurrentUser();
    }

    setFeedback(error.message || "Не удалось загрузить текущий профиль.", true);
    return getCurrentUser();
  }
}

function validatePayload(user) {
  const fullName = String(fullNameField?.value || "").trim();
  const passwordRequired = shouldRequirePasswordCompletion(user);
  const password = String(passwordField?.value || "");
  const passwordRepeat = String(passwordRepeatField?.value || "");

  if (!fullName) {
    throw new Error("Для завершения регистрации заполните имя.");
  }

  if (passwordRequired) {
    if (!password || !passwordRepeat) {
      throw new Error("Для аккаунта из Google нужно задать пароль и повторить его.");
    }

    const passwordValidation = validatePasswordPolicy(password);
    if (!passwordValidation.valid) {
      throw new Error(passwordValidation.message);
    }

    if (password !== passwordRepeat) {
      throw new Error("Пароли не совпадают.");
    }
  }

  return {
    fullName,
    ...(passwordRequired ? { password } : {}),
  };
}

async function handleSubmit(event) {
  event.preventDefault();
  const currentUser = isGoogleCompletionFlow() ? buildGoogleDraftUser() : getCurrentUser();

  if (!currentUser) {
    redirectToLogin();
    return;
  }

  let payload;
  try {
    payload = validatePayload(currentUser);
  } catch (error) {
    setFeedback(error.message || "Проверьте форму и попробуйте снова.", true);
    return;
  }

  submitButton.disabled = true;
  setFeedback("Сохраняем данные профиля…");

  try {
    if (isGoogleCompletionFlow()) {
      const completedSession = await completeGoogleRegistration({
        flowToken: pendingGoogleCompletion.flowToken,
        ...payload,
      });
      clearPendingGoogleCompletion();
      setSession(completedSession);
      setFeedback("Регистрация завершена. Перенаправляем…");
      window.location.href = resolvePostAuthUrl(completedSession.user, nextUrl);
      return;
    }

    const updatedUser = await updateCurrentUserProfile(payload);
    updateSessionUser(updatedUser);

    if (requiresProfileCompletion(updatedUser)) {
      setFeedback(
        "Backend сохранил профиль не полностью: фронт всё ещё видит незавершённый аккаунт. Проверь флаги completion в ответе `/api/auth/me` и `PATCH /api/auth/me`.",
        true
      );
      return;
    }

    setFeedback("Регистрация завершена. Перенаправляем…");
    window.location.href = resolvePostAuthUrl(updatedUser, nextUrl);
  } catch (error) {
    if (isUnauthorizedError(error)) {
      redirectToLogin();
      return;
    }

    setFeedback(error.message || "Не удалось сохранить профиль.", true);
  } finally {
    submitButton.disabled = false;
  }
}

async function handleLogout() {
  logoutButton.disabled = true;
  if (!isGoogleCompletionFlow()) {
    await logoutClient().catch(() => null);
  }
  clearPendingGoogleCompletion();
  clearSession();
  window.location.href = buildAuthUrl("login", nextUrl);
}

async function init() {
  syncLinks();
  document.querySelectorAll("[data-password-policy-hint]").forEach((node) => {
    node.textContent = getPasswordPolicyHint();
  });

  const activeUser = await resolveActiveUser();
  if (!activeUser) {
    return;
  }

  if (redirectIfProfileCompleted(activeUser)) {
    return;
  }

  fillForm(activeUser);
  setPasswordMode(shouldRequirePasswordCompletion(activeUser));

  form?.addEventListener("submit", handleSubmit);
  logoutButton?.addEventListener("click", handleLogout);
}

init();
