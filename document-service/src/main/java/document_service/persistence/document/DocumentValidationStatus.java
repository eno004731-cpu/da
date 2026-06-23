package document_service.persistence.document;

/**
 * Состояние проверки документа в order-service.
 */
public enum DocumentValidationStatus {
    DOCUMENT_VALIDATION_REQUESTED,
    DOCUMENT_VALIDATED,
    DOCUMENT_REJECTED
}
