export function setupRevealAnimations(dom) {
  const disableRevealAnimations =
    window.matchMedia("(prefers-reduced-motion: reduce)").matches ||
    window.matchMedia("(max-width: 960px)").matches ||
    typeof IntersectionObserver === "undefined";

  if (disableRevealAnimations) {
    dom.revealTargets.forEach((item) => item.classList.add("is-visible"));
    return;
  }

  dom.revealTargets.forEach((item, index) => {
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

  dom.revealTargets.forEach((item) => observer.observe(item));
}

export function setupServiceAnimations(dom) {
  if (!dom.servicesSection) {
    return;
  }

  const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const isMobile = window.matchMedia("(max-width: 960px)").matches;
  const hasObserverSupport = typeof IntersectionObserver !== "undefined";

  dom.serviceIntroTargets.forEach((target, index) => {
    target.classList.add("services-intro-reveal");
    target.style.setProperty("--services-intro-delay", `${index * 90}ms`);
  });

  dom.serviceCards.forEach((card, index) => {
    card.classList.add("service-card-reveal");
    card.style.setProperty(
      "--service-card-delay",
      `${(index % (isMobile ? 2 : 4)) * (isMobile ? 70 : 90)}ms`
    );
  });

  if (prefersReducedMotion || !hasObserverSupport) {
    dom.serviceIntroTargets.forEach((target) => target.classList.add("services-intro-visible"));
    dom.serviceCards.forEach((card) => card.classList.add("service-card-visible"));
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

  dom.serviceIntroTargets.forEach((target) => introObserver.observe(target));
  dom.serviceCards.forEach((card) => cardObserver.observe(card));
}

export function setCurrentYear(dom) {
  if (dom.yearNode) {
    dom.yearNode.textContent = new Date().getFullYear();
  }
}
