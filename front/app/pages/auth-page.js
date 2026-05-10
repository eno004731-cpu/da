import {
  isBackendUnavailableError,
  loginClient,
  logoutClient,
  registerClient,
} from "../api/auth-api.js?v=20260510b";
import { buildAuthUrl, clearSession, setSession } from "../state/auth-store.js?v=20260510b";

const loginTab = document.querySelector("#auth-tab-login");
const registerTab = document.querySelector("#auth-tab-register");
const loginPanel = document.querySelector("#auth-panel-login");
const registerPanel = document.querySelector("#auth-panel-register");
const loginForm = document.querySelector("#login-form");
const registerForm = document.querySelector("#register-form");
const feedbackNode = document.querySelector("#auth-feedback");
const backLink = document.querySelector("#auth-back-link");

const params = new URLSearchParams(window.location.search);
const nextUrl = params.get("next") || "./cabinet.html";
const shouldClearSession = params.get("switch") === "1";

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
    window.location.href = nextUrl;
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
    setFeedback("Аккаунт создан. Перенаправляем…");
    window.location.href = nextUrl;
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
  }

  backLink?.setAttribute("href", nextUrl.includes("da.html") ? nextUrl : "./da.html");
  activateMode(params.get("mode") === "register" ? "register" : "login");

  loginTab?.addEventListener("click", () => activateMode("login"));
  registerTab?.addEventListener("click", () => activateMode("register"));
  loginForm?.addEventListener("submit", handleLoginSubmit);
  registerForm?.addEventListener("submit", handleRegisterSubmit);
}

init();
