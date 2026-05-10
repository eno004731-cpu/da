import { createClientApplication } from "./app/api/orders-api.js";
import {
  fetchCurrentUser,
  isUnauthorizedError,
  logoutClient,
} from "./app/api/auth-api.js";
import {
  buildAuthUrl,
  clearSession,
  getCurrentUser,
  isAuthenticated,
  setSession,
} from "./app/state/auth-store.js";

const serviceCards = Array.from(document.querySelectorAll(".service-card"));
const serviceSelect = document.querySelector("#service-select");
const selectedService = document.querySelector("#selected-service");
const statusPrefix = document.querySelector("#status-prefix");
const contactForm = document.querySelector("#contact-form");
const formStatus = document.querySelector("#form-status");
const yearNode = document.querySelector("#year");
const faqItems = Array.from(document.querySelectorAll(".faq-item"));
const serviceSearch = document.querySelector("#service-search");
const serviceSearchNote = document.querySelector("#service-search-note");
const servicesEmpty = document.querySelector("#services-empty");
const servicesSection = document.querySelector("#services");
const serviceIntroTargets = Array.from(
  document.querySelectorAll("#services .section-head, #services .service-search-block")
);
const contactSection = document.querySelector("#contact");
const contactNameInput = document.querySelector('input[name="name"]');
const companyNameInput = document.querySelector('input[name="companyName"]');
const siteHeader = document.querySelector(".site-header");
const contactLinks = Array.from(document.querySelectorAll('a[href="#contact"]'));
const headerLoginLink = document.querySelector("#header-login-link");
const headerRegisterLink = document.querySelector("#header-register-link");
const headerCabinetLink = document.querySelector("#header-cabinet-link");
const headerLogoutButton = document.querySelector("#header-logout-button");
const serviceModal = document.querySelector("#service-modal");
const serviceModalTitle = document.querySelector("#service-modal-title");
const serviceModalBody = document.querySelector("#service-modal-body");
const serviceModalClose = document.querySelector("#service-modal-close");
const applicationAuthGate = document.querySelector("#application-auth-gate");
const applicationAuthSession = document.querySelector("#application-auth-session");
const applicationUserName = document.querySelector("#application-user-name");
const applicationUserEmail = document.querySelector("#application-user-email");
const applicationLoginLink = document.querySelector("#application-login-link");
const applicationRegisterLink = document.querySelector("#application-register-link");
const applicationLogoutButton = document.querySelector("#application-logout-button");
const documentsInput = document.querySelector("#application-documents");
const documentsSummary = document.querySelector("#documents-summary");
const revealTargets = Array.from(
  document.querySelectorAll(
    ".section:not(#services), .trust-card, .benefit-card, .timeline-step, .faq-item"
  )
);
let activeService = serviceSelect?.value || "Регистрация ООО / ИП";
let activeMoreButton = null;

const SERVICE_CODE_BY_LABEL = {
  "Регистрация ООО / ИП": "REGISTRATION",
  "Изменения в учредительные документы / ЕГРЮЛ": "CORPORATE_CHANGES",
  "Смена генерального директора / состава учредителей": "DIRECTOR_CHANGE",
  "Разработка и правовой аудит устава": "CHARTER_AUDIT",
  "Корпоративные процедуры и сделки": "CORPORATE_PROCEDURES",
  "Составление договора под ключ": "CONTRACT_DRAFTING",
  "Правовая экспертиза договора контрагента": "CONTRACT_REVIEW",
  "Типовые формы договоров под бизнес": "CONTRACT_TEMPLATES",
  "Претензии и анализ перспектив спора": "CLAIM_ANALYSIS",
  "Исковое заявление и взыскание задолженности": "DEBT_RECOVERY",
  "Суд первой инстанции и арбитраж": "LITIGATION",
  "Абонентское обслуживание «Базовый»": "OUTSOURCE_BASIC",
  "Абонентское обслуживание «Оптимальный»": "OUTSOURCE_OPTIMAL",
};

function syncActiveService(serviceName) {
  activeService = serviceName;

  serviceCards.forEach((card) => {
    card.classList.toggle("active", card.dataset.service === serviceName);
  });

  if (serviceSelect) {
    serviceSelect.value = serviceName;
  }

  if (selectedService) {
    selectedService.textContent = serviceName;
  }
}

function getServiceSearchText(card) {
  return [
    card.dataset.service || "",
    card.querySelector("h3")?.textContent || "",
    card.querySelector("p")?.textContent || "",
    ...Array.from(card.querySelectorAll("li")).map((item) => item.textContent || ""),
  ]
    .join(" ")
    .toLowerCase();
}

function filterServices(query) {
  const normalizedQuery = query.trim().toLowerCase();
  let visibleCount = 0;

  serviceCards.forEach((card) => {
    const matches = !normalizedQuery || getServiceSearchText(card).includes(normalizedQuery);
    card.hidden = !matches;

    if (matches) {
      visibleCount += 1;
      revealServiceCardIfVisible(card);
    }
  });

  if (servicesEmpty) {
    servicesEmpty.hidden = visibleCount > 0;
  }

  if (serviceSearchNote) {
    serviceSearchNote.textContent = normalizedQuery
      ? `Найдено услуг: ${visibleCount}`
      : "Начните вводить название услуги или ключевое слово.";
  }
}

function resetServiceSearchOnSmallScreens() {
  if (!serviceSearch || !window.matchMedia("(max-width: 960px)").matches) {
    return;
  }

  serviceSearch.value = "";
  filterServices("");
}

function setupRevealAnimations() {
  const disableRevealAnimations =
    window.matchMedia("(prefers-reduced-motion: reduce)").matches ||
    window.matchMedia("(max-width: 960px)").matches ||
    typeof IntersectionObserver === "undefined";

  if (disableRevealAnimations) {
    revealTargets.forEach((item) => item.classList.add("is-visible"));
    return;
  }

  revealTargets.forEach((item, index) => {
    item.classList.add("reveal");
    item.style.setProperty("--reveal-delay", `${Math.min(index % 4, 3) * 90}ms`);
  });

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) {
          return;
        }

        entry.target.classList.add("is-visible");
        observer.unobserve(entry.target);
      });
    },
    {
      threshold: 0.14,
      rootMargin: "0px 0px -6% 0px",
    }
  );

  revealTargets.forEach((item) => observer.observe(item));
}

function revealServiceCardIfVisible(card) {
  if (!card || card.hidden || card.classList.contains("service-card-visible")) {
    return;
  }

  const rect = card.getBoundingClientRect();
  const viewportHeight = window.innerHeight || document.documentElement.clientHeight;

  if (rect.top <= viewportHeight * 0.92 && rect.bottom >= 0) {
    card.classList.add("service-card-visible");
  }
}

function setupServiceAnimations() {
  if (!servicesSection) {
    return;
  }

  const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const isMobile = window.matchMedia("(max-width: 960px)").matches;
  const hasObserverSupport = typeof IntersectionObserver !== "undefined";

  serviceIntroTargets.forEach((target, index) => {
    target.classList.add("services-intro-reveal");
    target.style.setProperty("--services-intro-delay", `${index * 90}ms`);
  });

  serviceCards.forEach((card, index) => {
    card.classList.add("service-card-reveal");
    card.style.setProperty(
      "--service-card-delay",
      `${(index % (isMobile ? 2 : 4)) * (isMobile ? 70 : 90)}ms`
    );
  });

  if (prefersReducedMotion || !hasObserverSupport) {
    serviceIntroTargets.forEach((target) => target.classList.add("services-intro-visible"));
    serviceCards.forEach((card) => card.classList.add("service-card-visible"));
    return;
  }

  const introObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) {
          return;
        }

        entry.target.classList.add("services-intro-visible");
        introObserver.unobserve(entry.target);
      });
    },
    {
      threshold: isMobile ? 0.06 : 0.16,
      rootMargin: isMobile ? "0px 0px -2% 0px" : "0px 0px -8% 0px",
    }
  );

  const cardObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) {
          return;
        }

        entry.target.classList.add("service-card-visible");
        cardObserver.unobserve(entry.target);
      });
    },
    {
      threshold: isMobile ? 0.04 : 0.14,
      rootMargin: isMobile ? "0px 0px -2% 0px" : "0px 0px -6% 0px",
    }
  );

  serviceIntroTargets.forEach((target) => introObserver.observe(target));
  serviceCards.forEach((card) => cardObserver.observe(card));
}

function showIdleStatus() {
  if (formStatus) {
    formStatus.classList.remove("success", "error");
  }

  if (statusPrefix) {
    statusPrefix.textContent = "Сейчас выбрано:";
  }

  if (selectedService) {
    selectedService.textContent = activeService;
  }
}

function getServiceCodeByLabel(label) {
  return SERVICE_CODE_BY_LABEL[label] || "REGISTRATION";
}

function showFormError(message) {
  if (!formStatus || !statusPrefix || !selectedService) {
    return;
  }

  formStatus.classList.remove("success");
  formStatus.classList.add("error");
  statusPrefix.textContent = "Ошибка отправки:";
  selectedService.textContent = message;
}

function getReturnToContactUrl() {
  return "./da.html#contact";
}

function updateDocumentsSummary() {
  if (!documentsSummary || !documentsInput) {
    return;
  }

  const files = Array.from(documentsInput.files || []);

  if (!files.length) {
    documentsSummary.textContent =
      "Можно приложить договоры, переписку, расчёты, учредительные документы или сканы.";
    return;
  }

  const firstFiles = files.slice(0, 3).map((file) => file.name);
  const overflow = files.length > 3 ? ` и ещё ${files.length - 3}` : "";
  documentsSummary.textContent = `Прикреплено ${files.length}: ${firstFiles.join(", ")}${overflow}.`;
}

function syncClientAuthState() {
  const authenticated = isAuthenticated();
  const user = getCurrentUser();
  const loginHref = buildAuthUrl("login", getReturnToContactUrl(), {
    switchAccount: authenticated,
  });
  const registerHref = buildAuthUrl("register", getReturnToContactUrl());

  if (headerLoginLink) {
    headerLoginLink.href = loginHref;
    headerLoginLink.textContent = "Войти";
    headerLoginLink.hidden = authenticated;
  }

  if (headerRegisterLink) {
    headerRegisterLink.href = registerHref;
    headerRegisterLink.hidden = authenticated;
  }

  if (headerCabinetLink) {
    headerCabinetLink.href = "./cabinet.html";
    headerCabinetLink.hidden = !authenticated;
  }

  if (headerLogoutButton) {
    headerLogoutButton.hidden = !authenticated;
  }

  if (applicationLoginLink) {
    applicationLoginLink.href = loginHref;
  }

  if (applicationRegisterLink) {
    applicationRegisterLink.href = registerHref;
  }

  if (applicationAuthGate) {
    applicationAuthGate.hidden = authenticated;
  }

  if (applicationAuthSession) {
    applicationAuthSession.hidden = !authenticated;
  }

  if (contactForm) {
    contactForm.hidden = !authenticated;
  }

  if (!authenticated || !user) {
    return;
  }

  if (applicationUserName) {
    applicationUserName.textContent = user.fullName || "Клиентский кабинет";
  }

  if (applicationUserEmail) {
    applicationUserEmail.textContent = [user.email, user.companyName].filter(Boolean).join(" • ");
  }

  if (contactNameInput && !contactNameInput.value) {
    contactNameInput.value = user.fullName || "";
  }

  if (companyNameInput && !companyNameInput.value) {
    companyNameInput.value = user.companyName || "";
  }
}

async function syncClientAuthStateWithBackend() {
  try {
    const user = await fetchCurrentUser();

    if (!user) {
      clearSession();
    } else {
      setSession({ user });
    }
  } catch (error) {
    if (isUnauthorizedError(error)) {
      clearSession();
    }
  }

  syncClientAuthState();
  return Boolean(getCurrentUser());
}

function getContactScrollOffset() {
  if (!siteHeader) {
    return 24;
  }

  const headerHeight = siteHeader.getBoundingClientRect().height;
  const extraGap = window.matchMedia("(max-width: 680px)").matches ? 18 : 28;
  return headerHeight + extraGap;
}

function scrollToContactSection(behavior = "smooth") {
  if (!contactSection) {
    return;
  }

  const top =
    window.scrollY + contactSection.getBoundingClientRect().top - getContactScrollOffset();

  window.scrollTo({
    top: Math.max(top, 0),
    behavior,
  });
}

function setServiceDetailTriggerState(activeButton = null) {
  serviceCards.forEach((card) => {
    const moreButton = card.querySelector(".service-more");
    const isActive = Boolean(activeButton && moreButton === activeButton);
    card.classList.toggle("details-open", isActive);

    if (moreButton) {
      moreButton.setAttribute("aria-expanded", String(isActive));
    }
  });
}

function closeServiceModal({ restoreFocus = true } = {}) {
  if (!serviceModal || serviceModal.hidden) {
    setServiceDetailTriggerState();
    activeMoreButton = null;
    return;
  }

  const focusTarget = activeMoreButton;

  serviceModal.hidden = true;
  serviceModal.setAttribute("aria-hidden", "true");
  document.body.classList.remove("modal-open");
  setServiceDetailTriggerState();
  activeMoreButton = null;

  if (serviceModalTitle) {
    serviceModalTitle.textContent = "";
  }

  if (serviceModalBody) {
    serviceModalBody.innerHTML = "";
  }

  if (restoreFocus && focusTarget) {
    focusTarget.focus({ preventScroll: true });
  }
}

function openServiceModal(card, moreButton) {
  const detail = card.querySelector(".service-detail");

  if (!detail || !serviceModal || !serviceModalTitle || !serviceModalBody) {
    return;
  }

  const serviceName =
    card.dataset.service || card.querySelector(".service-title span")?.textContent || activeService;

  syncActiveService(serviceName);
  showIdleStatus();
  detail.hidden = true;
  detail.setAttribute("aria-hidden", "true");
  activeMoreButton = moreButton || null;
  setServiceDetailTriggerState(activeMoreButton);
  serviceModalTitle.textContent = serviceName;
  serviceModalBody.innerHTML = detail.innerHTML;
  serviceModal.hidden = false;
  serviceModal.setAttribute("aria-hidden", "false");
  document.body.classList.add("modal-open");

  window.requestAnimationFrame(() => {
    serviceModalClose?.focus({ preventScroll: true });
  });
}

function goToContactWithService(serviceName) {
  syncActiveService(serviceName);
  showIdleStatus();
  closeServiceModal({ restoreFocus: false });
  scrollToContactSection();

  window.setTimeout(() => {
    const targetGap = Math.abs(contactSection?.getBoundingClientRect().top - getContactScrollOffset());

    if (Number.isFinite(targetGap) && targetGap > 24) {
      scrollToContactSection("auto");
    }
  }, 420);

  window.setTimeout(() => {
    if (!window.matchMedia("(max-width: 960px)").matches) {
      contactNameInput?.focus({ preventScroll: true });
    }
  }, 360);
}

serviceCards.forEach((card) => {
  card.addEventListener("click", () => {
    syncActiveService(card.dataset.service);
    showIdleStatus();
  });

  const titleButton = card.querySelector(".service-title");
  const detail = card.querySelector(".service-detail");
  const actionButton = card.querySelector(".card-action");

  if (detail) {
    detail.hidden = true;
    detail.setAttribute("aria-hidden", "true");

    const moreButton = document.createElement("button");
    moreButton.type = "button";
    moreButton.className = "service-more";
    moreButton.setAttribute("aria-expanded", "false");
    moreButton.setAttribute("aria-haspopup", "dialog");
    moreButton.setAttribute("aria-controls", "service-modal");
    moreButton.textContent = "Подробнее";
    detail.before(moreButton);

    moreButton.addEventListener("click", (event) => {
      event.preventDefault();
      event.stopPropagation();
      openServiceModal(card, moreButton);
    });
  }

  if (titleButton) {
    titleButton.setAttribute("tabindex", "-1");
    titleButton.setAttribute("aria-hidden", "true");
  }

  if (!actionButton) {
    return;
  }

  actionButton.addEventListener("click", (event) => {
    event.preventDefault();
    event.stopPropagation();
    goToContactWithService(card.dataset.service);
  });
});

if (serviceSelect) {
  serviceSelect.addEventListener("change", (event) => {
    syncActiveService(event.target.value);
    showIdleStatus();
  });
}

if (serviceSearch) {
  serviceSearch.addEventListener("input", (event) => {
    filterServices(event.target.value);
  });
}

window.addEventListener("pageshow", resetServiceSearchOnSmallScreens);

contactLinks.forEach((link) => {
  link.addEventListener("click", (event) => {
    event.preventDefault();
    closeServiceModal({ restoreFocus: false });
    scrollToContactSection();
  });
});

if (serviceModal) {
  serviceModal.addEventListener("click", (event) => {
    const target = event.target;

    if (!(target instanceof Element)) {
      return;
    }

    if (target.hasAttribute("data-close-modal") || target === serviceModal) {
      closeServiceModal();
    }
  });
}

serviceModalClose?.addEventListener("click", () => {
  closeServiceModal();
});

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    closeServiceModal();
  }
});

faqItems.forEach((item) => {
  const trigger = item.querySelector(".faq-question");

  if (!trigger) {
    return;
  }

  trigger.addEventListener("click", () => {
    const isOpen = item.classList.contains("open");

    faqItems.forEach((faqItem) => {
      faqItem.classList.remove("open");

      const button = faqItem.querySelector(".faq-question");

      if (button) {
        button.setAttribute("aria-expanded", "false");
      }
    });

    if (!isOpen) {
      item.classList.add("open");
      trigger.setAttribute("aria-expanded", "true");
    }
  });
});

if (contactForm && formStatus) {
  contactForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!isAuthenticated()) {
      const hasActiveSession = await syncClientAuthStateWithBackend();
      if (hasActiveSession) {
        contactForm.requestSubmit();
        return;
      }

      showFormError("Для отправки заявки сначала войдите в кабинет клиента.");
      window.location.href = buildAuthUrl("login", getReturnToContactUrl());
      return;
    }

    const service = serviceSelect?.value || activeService;
    const serviceCode = getServiceCodeByLabel(service);
    const formData = new FormData(contactForm);
    const name = String(formData.get("name") || "").trim();
    const contact = String(formData.get("contact") || "").trim();
    const companyName = String(formData.get("companyName") || "").trim();
    const message = String(formData.get("message") || "").trim();
    const files = Array.from(documentsInput?.files || []);

    syncActiveService(service);
    formStatus.classList.remove("error");
    formStatus.classList.add("success");

    if (statusPrefix) {
      statusPrefix.textContent = "Отправка:";
    }

    if (selectedService) {
      selectedService.textContent =
        `${service}. Заявка отправляется в кабинет клиента и рабочую систему.`;
    }

    try {
      const result = await createClientApplication({
        serviceCode,
        clientName: name,
        contact,
        companyName,
        description: message || `Клиент отправил заявку по услуге «${service}».`,
        documents: files,
      });
      formStatus.classList.remove("error");
      formStatus.classList.add("success");
      statusPrefix.textContent = "Создан заказ:";

      if (selectedService) {
        selectedService.textContent = `${service}. Заказ сохранён в личном кабинете клиента.`;
      }

      contactForm.reset();
      syncActiveService(service);
      updateDocumentsSummary();

      if (result.orderId) {
        window.setTimeout(() => {
          window.location.href = `./order.html?orderId=${encodeURIComponent(result.orderId)}`;
        }, 900);
      }
    } catch (_error) {
      showFormError(
        "Не удалось создать заявку. Проверь backend endpoint /client/applications и multipart-контракт."
      );
    }
  });
}

applicationLogoutButton?.addEventListener("click", async () => {
  applicationLogoutButton.disabled = true;
  await logoutClient().catch(() => null);
  clearSession();
  syncClientAuthState();
  applicationLogoutButton.disabled = false;
});

headerLogoutButton?.addEventListener("click", async () => {
  headerLogoutButton.disabled = true;
  await logoutClient().catch(() => null);
  clearSession();
  syncClientAuthState();
  headerLogoutButton.disabled = false;
});

documentsInput?.addEventListener("change", updateDocumentsSummary);

if (yearNode) {
  yearNode.textContent = new Date().getFullYear();
}

showIdleStatus();
syncClientAuthStateWithBackend();
updateDocumentsSummary();
resetServiceSearchOnSmallScreens();
setupServiceAnimations();
filterServices(serviceSearch?.value || "");
setupRevealAnimations();
