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
    gsap.set(dom.problemCards, {
      x: (index) => [-130, -72, -18, -86, 46][index] || 0,
      y: (index) => [-28, -10, 12, 34, 60][index] || 0,
      rotate: (index) => [-4, 3, -2, 4, -3][index] || 0,
      opacity: 0.96,
    });
    gsap.set(dom.problemLines, { opacity: 0 });
    gsap.set(dom.problemOrbit, { scale: 0.88, opacity: 0.74, rotate: -8 });
    gsap.set(dom.problemCase, { x: 96, scale: 0.96, opacity: 0.3 });
    gsap.set(dom.problemResult, { opacity: 0, y: 18 });

    gsap
      .timeline({
        defaults: { ease: "none" },
        scrollTrigger: {
          trigger: dom.problemSection,
          start: getPinnedStart,
          end: "+=170%",
          // Секция сохраняет ширину page-shell; внутренний grid больше не схлопывается при fixed.
          pin: dom.problemSection,
          pinSpacing: true,
          scrub: 1,
          anticipatePin: 1,
          invalidateOnRefresh: true,
        },
      })
      .to(dom.problemCards, {
        x: 0,
        y: 0,
        rotate: 0,
        stagger: 0.02,
        duration: 0.62,
      })
      .to(dom.problemLines, { opacity: 1, duration: 0.52 }, "<0.08")
      .to(dom.problemOrbit, { scale: 1.06, opacity: 1, rotate: 0, duration: 0.5 }, "<0.04")
      .to(dom.problemOrbit, { scale: 1, duration: 0.22 })
      .to(dom.problemCase, { x: 0, scale: 1, opacity: 1, duration: 0.42 }, "<0.02")
      .to(dom.problemResult, { opacity: 1, y: 0, duration: 0.24 }, "<");
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
