/**
 * Shared frontend data contracts for the legal services site.
 * These typedefs document the payloads the frontend expects from your backend.
 */

/**
 * @typedef {"CLIENT" | "STAFF"} UserRole
 */

/**
 * @typedef {"TODO" | "IN_PROGRESS" | "ON_REVIEW" | "REWORK" | "DONE"} OrderStatusCode
 */

/**
 * @typedef {"LOW" | "MEDIUM" | "HIGH" | "URGENT"} PriorityCode
 */

/**
 * @typedef {Object} AuthUser
 * @property {string} id
 * @property {string} fullName
 * @property {string} email
 * @property {string | null} phone
 * @property {string | null} companyName
 * @property {UserRole} role
 */

/**
 * @typedef {Object} AuthSession
 * @property {AuthUser} user
 */

/**
 * @typedef {Object} ServiceItem
 * @property {string} id
 * @property {string} code
 * @property {string} name
 * @property {string | null} shortDescription
 */

/**
 * @typedef {Object} UploadedDocument
 * @property {string} id
 * @property {string} fileName
 * @property {string} mimeType
 * @property {number} size
 * @property {string} uploadedAt
 * @property {string | null} downloadUrl
 * @property {boolean} isDeleted
 * @property {string | null} deletedAt
 */

/**
 * @typedef {Object} ClientOrderSummary
 * @property {string} id
 * @property {string} title
 * @property {string} serviceCode
 * @property {string} serviceName
 * @property {OrderStatusCode} status
 * @property {string} createdAt
 * @property {string} updatedAt
 * @property {number} revisionCount
 */

/**
 * @typedef {Object} ClientOrderDetails
 * @property {string} id
 * @property {string} title
 * @property {string} serviceCode
 * @property {string} serviceName
 * @property {string} problemDescription
 * @property {OrderStatusCode} status
 * @property {string} createdAt
 * @property {string} updatedAt
 * @property {string | null} clientRevisionComment
 * @property {string | null} clientRevisionRequestedAt
 * @property {number} revisionCount
 * @property {UploadedDocument[]} documents
 */

/**
 * @typedef {Object} CreateApplicationPayload
 * @property {string} serviceCode
 * @property {string} clientName
 * @property {string} contact
 * @property {string} companyName
 * @property {string} description
 * @property {File[]} documents
 */

/**
 * @typedef {Object} CreateApplicationResponse
 * @property {string} orderId
 * @property {string} taskId
 * @property {string | null} trackingCode
 * @property {OrderStatusCode} status
 * @property {string} createdAt
 */

/**
 * @typedef {Object} PublicTaskStatus
 * @property {string} taskId
 * @property {string} trackingCode
 * @property {string} title
 * @property {OrderStatusCode} status
 * @property {string} statusLabel
 * @property {string} clientName
 * @property {string} serviceType
 * @property {string} createdAt
 * @property {string} updatedAt
 * @property {string | null} clientRevisionComment
 * @property {string | null} clientRevisionRequestedAt
 * @property {number} revisionCount
 */

/**
 * @typedef {Object} TeamComment
 * @property {string} id
 * @property {string} authorName
 * @property {string} body
 * @property {string} createdAt
 */

/**
 * @typedef {Object} TaskHistoryEntry
 * @property {string} id
 * @property {string} authorName
 * @property {string} fieldName
 * @property {string | null} oldValue
 * @property {string | null} newValue
 * @property {string} createdAt
 */

/**
 * @typedef {Object} StaffBoardTask
 * @property {string} id
 * @property {string | null} orderId
 * @property {string} trackingCode
 * @property {string} title
 * @property {string} clientName
 * @property {string} contact
 * @property {string} serviceType
 * @property {string} description
 * @property {OrderStatusCode} status
 * @property {string} createdAt
 * @property {string} updatedAt
 * @property {string | null} assignedTo
 * @property {PriorityCode} priority
 * @property {string | null} clientRevisionComment
 * @property {string | null} clientRevisionRequestedAt
 * @property {number} revisionCount
 * @property {UploadedDocument[]} documents
 * @property {TeamComment[]} comments
 * @property {TaskHistoryEntry[]} history
 */
