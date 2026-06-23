package legal_website.services.delete;

/**
 * Единый источник типов outbox-событий удаления.
 *
 * Эти значения используются и при создании события, и при выборке,
 * и при определении Kafka topic, поэтому строки не смогут разъехаться.
 */
public enum DeleteOutboxEventType {
    DELETE_ALL_ORDERS,
    DELETE_ALL_DOCUMENTS
}
