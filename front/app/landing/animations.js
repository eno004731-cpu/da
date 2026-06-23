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

export function setupGsapAnimations(dom) {
  const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  // На компактной ширине секции идут обычным потоком: pin не обрезает второй экран.
  const isCompactLayout = window.matchMedia("(max-width: 1100px)").matches;
  const gsap = window.gsap;
  const ScrollTrigger = window.ScrollTrigger;

  if (prefersReducedMotion || isCompactLayout || !gsap || !ScrollTrigger) {
    return;
  }

  gsap.registerPlugin(ScrollTrigger);
  document.body.classList.add("motion-ready");

  const getHeaderOffset = () => {
    const headerHeight = dom.siteHeader?.getBoundingClientRect().height || 100;
    return Math.ceil(headerHeight);
  };

  const getPinnedStart = () => `top top+=${getHeaderOffset()}`;

  if (dom.hero) {
    const heroTimeline = gsap.timeline({
      defaults: {
        ease: "power3.out",
        duration: 1.1,
      },
    });

    heroTimeline
      .from(dom.siteHeader, { y: -24, opacity: 0 })
      .from(dom.heroCopy, { y: 42, opacity: 0 }, "-=0.72")
      .from(dom.heroVisual, { x: 70, y: 18, rotate: 2.5, opacity: 0 }, "-=0.82");

    if (dom.heroStats.length) {
      heroTimeline.from(dom.heroStats, { y: 18, opacity: 0, stagger: 0.1 }, "-=0.52");
    }
  }

  if (dom.problemSection && dom.problemCards.length && dom.problemOrbit) {
    const cardStates = [
      { x: -172, y: -96, rotate: -13, scale: 0.92, opacity: 0.98 },
      { x: -116, y: -34, rotate: 9, scale: 0.94, opacity: 0.96 },
      { x: -74, y: 30, rotate: -8, scale: 0.95, opacity: 0.97 },
      { x: -138, y: 94, rotate: 11, scale: 0.93, opacity: 0.95 },
      { x: -18, y: 152, rotate: -10, scale: 0.91, opacity: 0.94 },
    ];

    gsap.set(dom.problemCards, {
      x: (index) => cardStates[index]?.x || 0,
      y: (index) => cardStates[index]?.y || 0,
      rotate: (index) => cardStates[index]?.rotate || 0,
      scale: (index) => cardStates[index]?.scale || 1,
      opacity: (index) => cardStates[index]?.opacity || 1,
      transformOrigin: "50% 50%",
    });
    gsap.set(dom.problemLines, {
      opacity: 0.08,
      strokeDashoffset: (index) => 34 + index * 10,
    });
    gsap.set(dom.problemOrbit, {
      scale: 0.58,
      opacity: 0.34,
      rotate: -16,
      filter: "drop-shadow(0 18px 28px rgba(181, 138, 82, 0.12))",
    });
    gsap.set(dom.problemCase, {
      x: 118,
      y: 18,
      scale: 0.92,
      opacity: 0.22,
      filter: "blur(1.6px) drop-shadow(0 22px 36px rgba(53, 45, 34, 0.08))",
    });
    gsap.set(dom.problemResult, { opacity: 0, y: 24 });

    gsap
      .timeline({
        defaults: { ease: "none" },
        scrollTrigger: {
          trigger: dom.problemSection,
          start: getPinnedStart,
          end: "+=180%",
          // Секция сохраняет ширину page-shell; внутренний grid больше не схлопывается при fixed.
          pin: dom.problemSection,
          pinSpacing: true,
          scrub: 1,
          anticipatePin: 1,
          invalidateOnRefresh: true,
        },
      })
      .to(dom.problemCards[0], { x: -74, y: -22, rotate: -4, scale: 0.98, duration: 0.18 })
      .to(dom.problemCards[1], { x: -38, y: -10, rotate: 3, scale: 0.985, duration: 0.18 }, "<")
      .to(dom.problemCards[2], { x: -10, y: 0, rotate: -2, scale: 0.99, duration: 0.18 }, "<")
      .to(dom.problemCards[3], { x: -34, y: 10, rotate: 2, scale: 0.985, duration: 0.18 }, "<")
      .to(dom.problemCards[4], { x: 20, y: 18, rotate: -3, scale: 0.98, duration: 0.18 }, "<")
      .to(dom.problemLines, { opacity: 0.42, strokeDashoffset: 14, stagger: 0.02, duration: 0.16 }, "<0.02")
      .to(dom.problemOrbit, { scale: 0.86, opacity: 0.72, rotate: -4, duration: 0.16 }, "<")
      .to(dom.problemCase, { x: 72, y: 10, scale: 0.96, opacity: 0.46, filter: "blur(0.8px) drop-shadow(0 26px 42px rgba(53, 45, 34, 0.12))", duration: 0.18 }, "<0.02")
      .to(dom.problemCards[0], { x: 0, y: 0, rotate: 0, scale: 1, duration: 0.22 })
      .to(dom.problemCards[1], { x: 0, y: 0, rotate: 0, scale: 1, duration: 0.22 }, "<0.03")
      .to(dom.problemCards[2], { x: 0, y: 0, rotate: 0, scale: 1, duration: 0.22 }, "<0.03")
      .to(dom.problemCards[3], { x: 0, y: 0, rotate: 0, scale: 1, duration: 0.22 }, "<0.03")
      .to(dom.problemCards[4], { x: 0, y: 0, rotate: 0, scale: 1, duration: 0.22 }, "<0.03")
      .to(dom.problemLines, { opacity: 1, strokeDashoffset: 0, stagger: 0.02, duration: 0.2 }, "<0.04")
      .to(dom.problemOrbit, {
        scale: 1.08,
        opacity: 1,
        rotate: 0,
        filter: "drop-shadow(0 30px 48px rgba(181, 138, 82, 0.24))",
        duration: 0.2,
      }, "<0.04")
      .to(dom.problemOrbit, { scale: 1, duration: 0.08 })
      .to(dom.problemCase, {
        x: 0,
        y: 0,
        scale: 1,
        opacity: 1,
        filter: "blur(0px) drop-shadow(0 30px 58px rgba(53, 45, 34, 0.17))",
        duration: 0.22,
      }, "<0.02")
      .to(dom.problemResult, { opacity: 1, y: 0, duration: 0.16 }, "<0.02");
  }

  if (dom.workflowSection && dom.workflowCards.length) {
    if (dom.workflowLineFill) {
      gsap.fromTo(
        dom.workflowLineFill,
        { scaleX: 0, transformOrigin: "left center" },
        {
          scaleX: 1,
          ease: "none",
          scrollTrigger: {
            trigger: dom.workflowSection,
            start: "top 68%",
            end: "bottom 46%",
            scrub: 0.8,
          },
        }
      );
    }

    gsap.from(dom.workflowCards, {
      y: 46,
      opacity: 0,
      stagger: 0.12,
      duration: 0.9,
      ease: "power3.out",
      scrollTrigger: {
        trigger: dom.workflowSection,
        start: "top 58%",
      },
    });
  }

  if (dom.productSection && dom.productUi) {
    // Сначала проявляется общий список дел, затем поверх него раскрывается выбранное дело.
    gsap.set(dom.productKanban, {
      x: 90,
      y: -34,
      rotate: 3,
      scale: 0.94,
      opacity: 0.56,
    });
    gsap.set(dom.clientCasePanel, {
      x: -42,
      y: 118,
      rotate: -1.2,
      scale: 0.9,
      opacity: 0.22,
      filter: "blur(1px)",
    });

    gsap
      .timeline({
        defaults: { ease: "none" },
        scrollTrigger: {
          trigger: dom.productSection,
          start: getPinnedStart,
          end: "+=180%",
          // Фиксируем полноширинную секцию, а не внутренний визуальный контейнер.
          pin: dom.productSection,
          pinSpacing: true,
          scrub: 1,
          anticipatePin: 1,
          invalidateOnRefresh: true,
        },
      })
      .to(dom.productKanban, { x: 0, y: 0, rotate: 1.2, scale: 1, opacity: 1, duration: 0.48 })
      .to(dom.clientCasePanel, { x: 0, y: 34, scale: 0.98, opacity: 0.92, filter: "blur(0px)", duration: 0.5 }, "<0.12")
      .to(dom.clientCasePanel, { y: 0, rotate: -0.45, scale: 1, opacity: 1, duration: 0.28 });
  }

  dom.gsapSections.forEach((section) => {
    if (!section || section === dom.problemSection || section === dom.workflowSection || section === dom.productSection) {
      return;
    }

    const sectionIntroTargets = section.querySelectorAll(".eyebrow, h2, .section-lead, .lead");
    if (!sectionIntroTargets.length) {
      return;
    }

    gsap.from(sectionIntroTargets, {
      y: 26,
      opacity: 0,
      stagger: 0.08,
      duration: 0.75,
      ease: "power2.out",
      scrollTrigger: {
        trigger: section,
        start: "top 72%",
      },
    });
  });

  window.addEventListener(
    "load",
    () => {
      ScrollTrigger.refresh();

      if (!window.location.hash) {
        return;
      }

      const hashTarget = document.querySelector(window.location.hash);
      if (!hashTarget) {
        return;
      }

      window.setTimeout(() => {
        hashTarget.scrollIntoView({ block: "start", behavior: "auto" });
      }, 80);
    },
    { once: true }
  );
}

export function setCurrentYear(dom) {
  if (dom.yearNode) {
    dom.yearNode.textContent = new Date().getFullYear();
  }
}
