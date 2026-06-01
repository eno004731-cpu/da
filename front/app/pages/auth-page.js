import {
  GOOGLE_AUTH_ENABLED,
} from "../api/endpoints.js?v=20260528a";
import {
  isBackendUnavailableError,
  loginClient,
  loginWithGoogle,
  logoutClient,
  requestEmailVerification,
  registerClient,
} from "../api/auth-api.js?v=20260528a";
import {
  buildAuthUrl,
  clearPendingGoogleCompletion,
  clearSession,
  setPendingGoogleCompletion,
  resolvePostAuthUrl,
  setSession,
} from "../state/auth-store.js?v=20260525a";
import {
  getPasswordPolicyHint,
  validatePasswordPolicy,
} from "../utils/password-policy.js";

const loginTab = document.querySelector("#auth-tab-login");
const registerTab = document.querySelector("#auth-tab-register");
const loginPanel = document.querySelector("#auth-panel-login");
const registerPanel = document.querySelector("#auth-panel-register");
const loginForm = document.querySelector("#login-form");
const registerForm = document.querySelector("#register-form");
const feedbackNode = document.querySelector("#auth-feedback");
const backLink = document.querySelector("#auth-back-link");
const googleAuthSection = document.querySelector("#google-auth-section");
const googleAuthButton = document.querySelector("#google-auth-button");
const googleAuthHint = document.querySelector("#google-auth-hint");

const params = new URLSearchParams(window.location.search);
const nextUrl = params.get("next") || "./cabinet.html";
const shouldClearSession = params.get("switch") === "1";
const googleClientId =
  window.__LEGAL_GOOGLE_CLIENT_ID__ ||
  document.querySelector('meta[name="google-client-id"]')?.content?.trim() ||
  "";

function setFeedback(message = "", isError = false) {
  if (!feedbackNode) {
    return;
  }

  feedbackNode.textContent = message;
  feedbackNode.hidden = !message;
  feedbackNode.classList.toggle("is-error", isError);
  feedbackNode.classList.toggle("is-success", !isError && Boolean(message));
}

function activateMode(mode) {
  const loginActive = mode !== "register";
  loginTab?.classList.toggle("is-active", loginActive);
  registerTab?.classList.toggle("is-active", !loginActive);
  loginPanel.hidden = !loginActive;
  registerPanel.hidden = loginActive;
  window.history.replaceState({}, "", buildAuthUrl(loginActive ? "login" : "register", nextUrl));
}

function redirectAfterSuccessfulAuth(session) {
  const destination = resolvePostAuthUrl(session?.user, nextUrl);
  window.location.href = destination;
}

function setGoogleHint(message = "") {
  if (!googleAuthHint) {
    return;
  }

  googleAuthHint.textContent = message;
  googleAuthHint.hidden = !message;
}

function isOptionalVerificationRequestError(error) {
  const status = typeof error === "object" && error !== null ? error.status : undefined;
  return typeof status === "undefined" || [404, 405, 501, 502, 503, 504].includes(status);
}

function waitForGoogleIdentityServices(maxAttempts = 20, delayMs = 250) {
  return new Promise((resolve) => {
    let attempts = 0;

    const tryResolve = () => {
      if (window.google?.accounts?.id) {
        resolve(window.google.accounts.id);
        return;
      }

      attempts += 1;
      if (attempts >= maxAttempts) {
        resolve(null);
        return;
      }

      window.setTimeout(tryResolve, delayMs);
    };

    tryResolve();
  });
}

async function handleGoogleCredentialResponse(response) {
  const credential = typeof response?.credential === "string" ? response.credential : "";

  if (!credential) {
    setFeedback("Google не вернул credential для авторизации.", true);
    return;
  }

  setFeedback("Проверяем аккаунт Google…");

  try {
    const result = await loginWithGoogle(credential);

    if (result?.type === "authenticated" && result.session) {
      clearPendingGoogleCompletion();
      setSession(result.session);
      setFeedback("Вход через Google выполнен. Перенаправляем…");
      redirectAfterSuccessfulAuth(result.session);
      return;
    }

    if (result?.type === "google_completion_required" && result.flowToken) {
      clearSession();
      setPendingGoogleCompletion({
        flowToken: result.flowToken,
        profile: result.profile || {},
      });
      setFeedback("Нужно завершить регистрацию. Перенаправляем на заполнение профиля…");
      window.location.href = `./complete-profile.html?${new URLSearchParams({ next: nextUrl }).toString()}`;
      return;
    }

    throw new Error("Фронт получил неподдерживаемый результат Google auth.");
  } catch (error) {
    if (isBackendUnavailableError(error)) {
      setFeedback("Backend временно недоступен. Войти через Google сейчас нельзя.", true);
      return;
    }

    setFeedback(error.message || "Не удалось выполнить вход через Google.", true);
  }
}

async function initGoogleAuth() {
  if (!googleAuthSection || !googleAuthButton) {
    return;
  }

  if (!GOOGLE_AUTH_ENABLED) {
    googleAuthSection.hidden = true;
    return;
  }

  if (!googleClientId) {
    setGoogleHint("Google вход отключён: укажи client ID через window.__LEGAL_GOOGLE_CLIENT_ID__ или meta[name='google-client-id'].");
    return;
  }

  const googleIdentity = await waitForGoogleIdentityServices();
  if (!googleIdentity) {
    setGoogleHint("Не удалось загрузить Google Identity Services.");
    return;
  }

  // Инициализируем Google Sign-In в браузере:
  // Google возвращает credential, а backend потом меняет его на нашу app session.
  googleIdentity.initialize({
    client_id: googleClientId,
    callback: handleGoogleCredentialResponse,
  });

  googleIdentity.renderButton(googleAuthButton, {
    theme: "outline",
    size: "large",
    shape: "rectangular",
    text: "continue_with",
    width: 320,
  });

  setGoogleHint("");
}

async function handleLoginSubmit(event) {
  event.preventDefault();
  const formData = new FormData(loginForm);
  const email = String(formData.get("email") || "").trim();
  const password = String(formData.get("password") || "");

  setFeedback("Проверяем доступ…");

  try {
    const session = await loginClient({ email, password });
    setSession(session);
    setFeedback("Вход выполнен. Перенаправляем…");
    redirectAfterSuccessfulAuth(session);
  } catch (error) {
    if (isBackendUnavailableError(error)) {
      // Показываем понятное сообщение вместо сырого 502/Bad Gateway,
      // чтобы пользователь сразу понимал, что проблема не в логине/пароле.
      setFeedback("Backend временно недоступен. Войти сейчас нельзя, попробуйте позже.", true);
      return;
    }

    setFeedback(error.message || "Не удалось войти. Проверь backend endpoint /auth/login.", true);
  }
}

async function handleRegisterSubmit(event) {
  event.preventDefault();
  const formData = new FormData(registerForm);
  const fullName = String(formData.get("fullName") || "").trim();
  const email = String(formData.get("email") || "").trim();
  const phone = String(formData.get("phone") || "").trim();
  const companyName = String(formData.get("companyName") || "").trim();
  const password = String(formData.get("password") || "");
  const passwordRepeat = String(formData.get("passwordRepeat") || "");
  const passwordValidation = validatePasswordPolicy(password);

  if (!passwordValidation.valid) {
    setFeedback(passwordValidation.message, true);
    return;
  }

  if (password !== passwordRepeat) {
    setFeedback("Пароли не совпадают.", true);
    return;
  }

  setFeedback("Создаём аккаунт…");

  try {
    const session = await registerClient({
      fullName,
      email,
      phone,
      companyName,
      password,
    });
    setSession(session);

    try {
      await requestEmailVerification();
      setFeedback("Аккаунт создан. Код подтверждения email отправлен. Перенаправляем…");
    } catch (verificationError) {
      if (!isOptionalVerificationRequestError(verificationError)) {
        throw verificationError;
      }

      setFeedback("Аккаунт создан. Endpoint отправки кода подтверждения пока недоступен. Перенаправляем…");
    }

    redirectAfterSuccessfulAuth(session);
  } catch (error) {
    if (isBackendUnavailableError(error)) {
      // Аналогично для регистрации: если API не отвечает,
      // не вводим пользователя в заблуждение сообщением про форму.
      setFeedback(
        "Backend временно недоступен. Зарегистрироваться сейчас нельзя, попробуйте позже.",
        true
      );
      return;
    }

    setFeedback(error.message || "Не удалось зарегистрироваться. Проверь backend endpoint /auth/register.", true);
  }
}

async function init() {
  if (shouldClearSession) {
    await logoutClient().catch(() => null);
    clearSession();
    clearPendingGoogleCompletion();
  }

  backLink?.setAttribute("href", nextUrl.includes("da.html") ? nextUrl : "./da.html");
  document.querySelectorAll("[data-password-policy-hint]").forEach((node) => {
    node.textContent = getPasswordPolicyHint();
  });
  activateMode(params.get("mode") === "register" ? "register" : "login");

  loginTab?.addEventListener("click", () => activateMode("login"));
  registerTab?.addEventListener("click", () => activateMode("register"));
  loginForm?.addEventListener("submit", handleLoginSubmit);
  registerForm?.addEventListener("submit", handleRegisterSubmit);
  await initGoogleAuth();
}

init();
