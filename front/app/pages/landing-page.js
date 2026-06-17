import {
  setupGsapAnimations,
  setupRevealAnimations,
  setCurrentYear,
  setupServiceAnimations,
} from "../landing/animations.js";
import { createAuthGateController } from "../landing/auth-gate.js";
import { initContactForm } from "../landing/contact-form.js?v=20260605b";
import { getLandingDom } from "../landing/dom.js";
import { initFaq } from "../landing/faq.js";
import { createRuntimeController } from "../landing/runtime.js";
import { createServicesController } from "../landing/services.js";
import { createLandingState } from "../landing/state.js";
import { createLandingUi } from "../landing/ui.js";

const dom = getLandingDom();
const state = createLandingState(dom);
const ui = createLandingUi({ dom, state });
const runtime = createRuntimeController({
  dom,
  state,
  showIdleStatus: ui.showIdleStatus,
});
const services = createServicesController({
  dom,
  state,
  showIdleStatus: ui.showIdleStatus,
});
const auth = createAuthGateController({
  dom,
  state,
  showIdleStatus: ui.showIdleStatus,
  syncContactRuntimeCopy: runtime.syncContactRuntimeCopy,
  setBackendAvailabilityState: runtime.setBackendAvailabilityState,
  getAuthOnlyModeMessage: runtime.getAuthOnlyModeMessage,
});

services.init();
auth.bindLogoutButtons();
initFaq(dom);
initContactForm({
  dom,
  ui,
  services,
  auth: {
    syncClientAuthState: auth.syncClientAuthState,
    syncClientAuthStateWithBackend: auth.syncClientAuthStateWithBackend,
    isBackendAvailable: () => state.backendIsAvailable,
  },
  setBackendAvailabilityState: runtime.setBackendAvailabilityState,
});

dom.documentsInput?.addEventListener("change", ui.updateDocumentsSummary);

setCurrentYear(dom);
ui.showIdleStatus();
auth.syncClientAuthStateWithBackend();
ui.updateDocumentsSummary();
services.resetServiceSearchOnSmallScreens();
setupServiceAnimations(dom);
services.filterServices(dom.serviceSearch?.value || "");
setupRevealAnimations(dom);
setupGsapAnimations(dom);
