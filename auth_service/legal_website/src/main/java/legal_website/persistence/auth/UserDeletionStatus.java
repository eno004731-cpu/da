package legal_website.persistence.auth;

/**
 * Состояние жизненного цикла удаления аккаунта.
 *
 * ACTIVE используется только пока аккаунт работает нормально.
 * Остальные значения описывают асинхронный процесс удаления.
 */
public enum UserDeletionStatus {
    ACTIVE,
    DELETION_REQUESTED,
    DELETION_IN_PROGRESS,
    DELETION_FAILED,
    DELETED
}
