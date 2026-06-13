export function createLandingState(dom) {
  return {
    activeService: dom.serviceSelect?.value || "Регистрация ООО / ИП",
    activeMoreButton: null,
    backendIsAvailable: true,
  };
}
