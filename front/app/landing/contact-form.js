import {
  createClientApplication,
  uploadClientOrderDocuments,
} from "../api/orders-api.js?v=20260512b";
import {
  isBackendUnavailableError,
  isUnauthorizedError,
  shouldPreserveClientSessionOnOrdersUnauthorized,
} from "../api/auth-api.js?v=20260524c";
import { cacheOrderSnapshot } from "../lib/order-snapshot.js";
import {
  buildAuthUrl,
  clearSession,
  isAuthenticated,
} from "../state/auth-store.js?v=20260524a";
import { ORDERS_API_ENABLED } from "./runtime.js";
import { getReturnToContactUrl } from "./routes.js";
import { ALLOWED_DOCUMENT_FORMATS_LABEL } from "./ui.js";

export function initContactForm({
  dom,
  ui,
  services,
  auth,
  setBackendAvailabilityState,
}) {
  if (!dom.contactForm || !dom.formStatus) {
    return;
  }

  const formFields = Array.from(
    dom.contactForm.querySelectorAll("input, textarea, select, button")
  );

  function getFilesUploadCopy(filesCount) {
    if (!filesCount) {
      return "Загружаем заявку.";
    }

    const noun =
      filesCount % 10 === 1 && filesCount % 100 !== 11
        ? "файл"
        : [2, 3, 4].includes(filesCount % 10) && ![12, 13, 14].includes(filesCount % 100)
          ? "файла"
          : "файлов";

    return `Загружаем заявку и ${filesCount} ${noun}.`;
  }

  function setSubmittingState(isSubmitting) {
    formFields.forEach((field) => {
      field.disabled = isSubmitting;
    });
  }

  dom.contactForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!ORDERS_API_ENABLED) {
      ui.showFormError(
        "Локально сейчас работает только авторизация. Отправка заявок появится после подключения отдельного API заказов."
      );
      return;
    }

    if (!auth.isBackendAvailable()) {
      ui.showFormError("Backend временно недоступен. Отправка заявки сейчас невозможна.");
      return;
    }

    if (!isAuthenticated()) {
      const hasActiveSession = await auth.syncClientAuthStateWithBackend();
      if (hasActiveSession) {
        dom.contactForm.requestSubmit();
        return;
      }

      ui.showFormError("Для отправки заявки сначала войдите в кабинет клиента.");
      window.location.href = buildAuthUrl("login", getReturnToContactUrl());
      return;
    }

    const service = dom.serviceSelect?.value || services.getActiveService();
    const serviceCode = services.getServiceCodeByLabel(service);
    const formData = new FormData(dom.contactForm);
    const name = String(formData.get("name") || "").trim();
    const contact = String(formData.get("contact") || "").trim();
    const companyName = String(formData.get("companyName") || "").trim();
    const message = String(formData.get("message") || "").trim();
    const files = Array.from(dom.documentsInput?.files || []);
    const unsupportedFiles = ui.getUnsupportedDocuments(files);
    let shouldKeepLocked = false;

    services.syncActiveService(service);
    dom.formStatus.classList.remove("error");
    dom.formStatus.classList.add("success");

    if (dom.statusPrefix) {
      dom.statusPrefix.textContent = "Отправка:";
    }

    if (dom.selectedService) {
      dom.selectedService.textContent =
        `${service}. Заявка отправляется в кабинет клиента и рабочую систему.`;
    }

    if (unsupportedFiles.length) {
      ui.showFormError(
        `Формат файлов не поддерживается: ${unsupportedFiles.join(", ")}. Разрешены: ${ALLOWED_DOCUMENT_FORMATS_LABEL}.`
      );
      return;
    }

    setSubmittingState(true);
    dom.formStatus.classList.remove("error");
    dom.formStatus.classList.add("success");

    if (dom.statusPrefix) {
      dom.statusPrefix.textContent = "Загрузка:";
    }

    if (dom.selectedService) {
      dom.selectedService.textContent = `${service}. ${getFilesUploadCopy(files.length)}`;
    }

    if (dom.documentsSummary && files.length) {
      dom.documentsSummary.textContent = getFilesUploadCopy(files.length);
    }

    try {
      const result = await createClientApplication({
        serviceCode,
        clientName: name,
        contact,
        companyName,
        description: message || `Клиент отправил заявку по услуге «${service}».`,
      });

      const redirectOrderId = String(result.orderId || result.id || "").trim();
      let uploadedDocuments = [];

      if (redirectOrderId && files.length) {
        dom.statusPrefix.textContent = "Документы:";
        if (dom.selectedService) {
          dom.selectedService.textContent = `${service}. Загружаем документы в созданный заказ.`;
        }

        try {
          uploadedDocuments = await uploadClientOrderDocuments(redirectOrderId, files);
        } catch (uploadError) {
          cacheOrderSnapshot(buildOrderSnapshot({
            orderId: redirectOrderId,
            serviceCode,
            serviceName: service,
            clientName: name,
            contact,
            companyName,
            description: message || `Клиент отправил заявку по услуге «${service}».`,
            documents: [],
          }));
          shouldKeepLocked = true;
          window.setTimeout(() => {
            window.location.href = `./order.html?id=${encodeURIComponent(redirectOrderId)}`;
          }, 900);
          ui.showFormError(
            uploadError.message ||
            "Заказ создан, но документы не удалось загрузить. Откроем карточку заказа, чтобы не потерять данные."
          );
          return;
        }
      }

      dom.formStatus.classList.remove("error");
      dom.formStatus.classList.add("success");
      dom.statusPrefix.textContent = "Создан заказ:";

      if (dom.selectedService) {
        dom.selectedService.textContent = `${service}. Заказ сохранён в личном кабинете клиента.`;
      }

      if (redirectOrderId) {
        cacheOrderSnapshot(buildOrderSnapshot({
          orderId: redirectOrderId,
          serviceCode,
          serviceName: service,
          clientName: name,
          contact,
          companyName,
          description: message || `Клиент отправил заявку по услуге «${service}».`,
          documents: uploadedDocuments,
        }));
        shouldKeepLocked = true;
      }

      dom.contactForm.reset();
      services.syncActiveService(service);
      ui.updateDocumentsSummary();

      if (redirectOrderId) {
        window.setTimeout(() => {
          window.location.href = `./order.html?id=${encodeURIComponent(redirectOrderId)}`;
        }, 900);
        return;
      }

      ui.showFormError(
        "Заявка создана, но backend не вернул id заказа. Открой кабинет клиента, чтобы найти её вручную."
      );
    } catch (error) {
      if (isUnauthorizedError(error)) {
        if (shouldPreserveClientSessionOnOrdersUnauthorized()) {
          ui.showFormError(
            "Вы вошли в отдельный auth-service, но backend заявок пока не принимает эту сессию. Отправка заявки временно недоступна."
          );
        } else {
          clearSession();
          ui.showFormError("Сессия истекла. Войдите снова, чтобы отправить заявку.");
          window.location.href = buildAuthUrl("login", getReturnToContactUrl());
        }
        return;
      }

      if (isBackendUnavailableError(error)) {
        setBackendAvailabilityState(
          false,
          "Backend перестал отвечать во время отправки заявки. Попробуй ещё раз после восстановления API."
        );
        ui.showFormError("Backend временно недоступен. Не удалось отправить заявку.");
        auth.syncClientAuthState();
        return;
      }

      ui.showFormError(
        error.message ||
        "Не удалось создать заявку. Проверь backend endpoint /client/applications и JSON-контракт создания заказа."
      );
    } finally {
      if (!shouldKeepLocked) {
        setSubmittingState(false);
      }
    }
  });
}

function buildOrderSnapshot({
  orderId,
  serviceCode,
  serviceName,
  clientName,
  contact,
  companyName,
  description,
  documents,
}) {
  const now = new Date().toISOString();
  return {
    id: orderId,
    orderId,
    title: description,
    serviceCode,
    serviceName,
    clientName,
    contact,
    companyName,
    problemDescription: description,
    status: "ON_REVIEW",
    createdAt: now,
    updatedAt: now,
    revisionCount: 0,
    documents,
  };
}
