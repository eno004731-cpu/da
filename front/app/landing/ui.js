const ALLOWED_DOCUMENT_EXTENSIONS = new Set([
  ".pdf",
  ".doc",
  ".docx",
  ".dotx",
  ".dotm",
  ".jpg",
  ".jpeg",
  ".png",
  ".heic",
  ".heif",
  ".mp4",
  ".mov",
]);

export const ALLOWED_DOCUMENT_FORMATS_LABEL =
  "PNG, JPG/JPEG, HEIC/HEIF, PDF, DOC, DOCX, DOTX, DOTM, MP4, MOV";

export function createLandingUi({ dom, state }) {
  function showIdleStatus() {
    if (dom.formStatus) {
      dom.formStatus.classList.remove("success", "error");
    }

    if (dom.statusPrefix) {
      dom.statusPrefix.textContent = "Сейчас выбрано:";
    }

    if (dom.selectedService) {
      dom.selectedService.textContent = state.activeService;
    }
  }

  function showFormError(message) {
    if (!dom.formStatus || !dom.statusPrefix || !dom.selectedService) {
      return;
    }

    dom.formStatus.classList.remove("success");
    dom.formStatus.classList.add("error");
    dom.statusPrefix.textContent = "Ошибка отправки:";
    dom.selectedService.textContent = message;
  }

  function getUnsupportedDocuments(files) {
    return files
      .filter((file) => {
        const fileName = String(file?.name || "").trim().toLowerCase();
        const extensionStart = fileName.lastIndexOf(".");
        if (extensionStart < 0) {
          return true;
        }

        const extension = fileName.slice(extensionStart);
        return !ALLOWED_DOCUMENT_EXTENSIONS.has(extension);
      })
      .map((file) => file.name);
  }

  function updateDocumentsSummary() {
    if (!dom.documentsSummary || !dom.documentsInput) {
      return;
    }

    const files = Array.from(dom.documentsInput.files || []);

    if (!files.length) {
      dom.documentsSummary.textContent =
        "Можно приложить договоры, переписку, расчёты, учредительные документы, сканы или короткое видео.";
      return;
    }

    const unsupportedFiles = getUnsupportedDocuments(files);
    if (unsupportedFiles.length) {
      dom.documentsSummary.textContent =
        `Неподдерживаемые файлы: ${unsupportedFiles.join(", ")}. ` +
        `Разрешены: ${ALLOWED_DOCUMENT_FORMATS_LABEL}.`;
      return;
    }

    const firstFiles = files.slice(0, 3).map((file) => file.name);
    const overflow = files.length > 3 ? ` и ещё ${files.length - 3}` : "";
    dom.documentsSummary.textContent =
      `Прикреплено ${files.length}: ${firstFiles.join(", ")}${overflow}.`;
  }

  return {
    showIdleStatus,
    showFormError,
    getUnsupportedDocuments,
    updateDocumentsSummary,
  };
}
