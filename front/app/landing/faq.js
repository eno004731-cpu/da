export function initFaq(dom) {
  dom.faqItems.forEach((item) => {
    const trigger = item.querySelector(".faq-question");

    if (!trigger) {
      return;
    }

    trigger.addEventListener("click", () => {
      const isOpen = item.classList.contains("open");

      dom.faqItems.forEach((faqItem) => {
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
}
