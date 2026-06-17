export const SERVICE_CODE_BY_LABEL = {
  Договоры: "CONTRACT_DRAFTING",
  Претензии: "CLAIM_ANALYSIS",
  Консультации: "CORPORATE_PROCEDURES",
  "Судебное сопровождение": "LITIGATION",
  "Юридическое сопровождение бизнеса": "OUTSOURCE_BASIC",
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

export function createServicesController({ dom, state, showIdleStatus }) {
  function syncActiveService(serviceName) {
    state.activeService = serviceName;

    dom.serviceCards.forEach((card) => {
      card.classList.toggle("active", card.dataset.service === serviceName);
    });

    if (dom.serviceSelect) {
      dom.serviceSelect.value = serviceName;
    }

    if (dom.selectedService) {
      dom.selectedService.textContent = serviceName;
    }
  }

  function getActiveService() {
    return state.activeService;
  }

  function getServiceCodeByLabel(label) {
    return SERVICE_CODE_BY_LABEL[label] || "REGISTRATION";
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

  function filterServices(query) {
    const normalizedQuery = query.trim().toLowerCase();
    let visibleCount = 0;

    dom.serviceCards.forEach((card) => {
      const matches = !normalizedQuery || getServiceSearchText(card).includes(normalizedQuery);
      card.hidden = !matches;

      if (matches) {
        visibleCount += 1;
        revealServiceCardIfVisible(card);
      }
    });

    if (dom.servicesEmpty) {
      dom.servicesEmpty.hidden = visibleCount > 0;
    }

    if (dom.serviceSearchNote) {
      dom.serviceSearchNote.textContent = normalizedQuery
        ? `Найдено услуг: ${visibleCount}`
        : "Начните вводить название услуги или ключевое слово.";
    }
  }

  function resetServiceSearchOnSmallScreens() {
    if (!dom.serviceSearch || !window.matchMedia("(max-width: 960px)").matches) {
      return;
    }

    dom.serviceSearch.value = "";
    filterServices("");
  }

  function getContactScrollOffset() {
    if (!dom.siteHeader) {
      return 24;
    }

    const headerHeight = dom.siteHeader.getBoundingClientRect().height;
    const extraGap = window.matchMedia("(max-width: 680px)").matches ? 18 : 28;
    return headerHeight + extraGap;
  }

  function scrollToContactSection(behavior = "smooth") {
    if (!dom.contactSection) {
      return;
    }

    const top =
      window.scrollY + dom.contactSection.getBoundingClientRect().top - getContactScrollOffset();

    window.scrollTo({
      top: Math.max(top, 0),
      behavior,
    });
  }

  function setServiceDetailTriggerState(activeButton = null) {
    dom.serviceCards.forEach((card) => {
      const moreButton = card.querySelector(".service-more");
      const isActive = Boolean(activeButton && moreButton === activeButton);
      card.classList.toggle("details-open", isActive);

      if (moreButton) {
        moreButton.setAttribute("aria-expanded", String(isActive));
      }
    });
  }

  function closeServiceModal({ restoreFocus = true } = {}) {
    if (!dom.serviceModal || dom.serviceModal.hidden) {
      setServiceDetailTriggerState();
      state.activeMoreButton = null;
      return;
    }

    const focusTarget = state.activeMoreButton;

    dom.serviceModal.hidden = true;
    dom.serviceModal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("modal-open");
    setServiceDetailTriggerState();
    state.activeMoreButton = null;

    if (dom.serviceModalTitle) {
      dom.serviceModalTitle.textContent = "";
    }

    if (dom.serviceModalBody) {
      dom.serviceModalBody.innerHTML = "";
    }

    if (restoreFocus && focusTarget) {
      focusTarget.focus({ preventScroll: true });
    }
  }

  function openServiceModal(card, moreButton) {
    const detail = card.querySelector(".service-detail");

    if (!detail || !dom.serviceModal || !dom.serviceModalTitle || !dom.serviceModalBody) {
      return;
    }

    const serviceName =
      card.dataset.service || card.querySelector(".service-title span")?.textContent || state.activeService;

    syncActiveService(serviceName);
    showIdleStatus();
    detail.hidden = true;
    detail.setAttribute("aria-hidden", "true");
    state.activeMoreButton = moreButton || null;
    setServiceDetailTriggerState(state.activeMoreButton);
    dom.serviceModalTitle.textContent = serviceName;
    dom.serviceModalBody.innerHTML = detail.innerHTML;
    dom.serviceModal.hidden = false;
    dom.serviceModal.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");

    window.requestAnimationFrame(() => {
      dom.serviceModalClose?.focus({ preventScroll: true });
    });
  }

  function goToContactWithService(serviceName) {
    syncActiveService(serviceName);
    showIdleStatus();
    closeServiceModal({ restoreFocus: false });
    scrollToContactSection();

    window.setTimeout(() => {
      const targetGap = Math.abs(
        dom.contactSection?.getBoundingClientRect().top - getContactScrollOffset()
      );

      if (Number.isFinite(targetGap) && targetGap > 24) {
        scrollToContactSection("auto");
      }
    }, 420);

    window.setTimeout(() => {
      if (!window.matchMedia("(max-width: 960px)").matches) {
        dom.contactNameInput?.focus({ preventScroll: true });
      }
    }, 360);
  }

  function initCards() {
    dom.serviceCards.forEach((card) => {
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
  }

  function initGlobalListeners() {
    if (dom.serviceSelect) {
      dom.serviceSelect.addEventListener("change", (event) => {
        syncActiveService(event.target.value);
        showIdleStatus();
      });
    }

    if (dom.serviceSearch) {
      dom.serviceSearch.addEventListener("input", (event) => {
        filterServices(event.target.value);
      });
    }

    window.addEventListener("pageshow", resetServiceSearchOnSmallScreens);

    dom.contactLinks.forEach((link) => {
      link.addEventListener("click", (event) => {
        event.preventDefault();
        closeServiceModal({ restoreFocus: false });
        scrollToContactSection();
      });
    });

    if (dom.serviceModal) {
      dom.serviceModal.addEventListener("click", (event) => {
        const target = event.target;

        if (!(target instanceof Element)) {
          return;
        }

        if (target.hasAttribute("data-close-modal") || target === dom.serviceModal) {
          closeServiceModal();
        }
      });
    }

    dom.serviceModalClose?.addEventListener("click", () => {
      closeServiceModal();
    });

    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape") {
        closeServiceModal();
      }
    });
  }

  function init() {
    initCards();
    initGlobalListeners();
  }

  return {
    init,
    syncActiveService,
    getActiveService,
    getServiceCodeByLabel,
    filterServices,
    resetServiceSearchOnSmallScreens,
  };
}
