// com/protomil/core/shared/constants/ErrorCodes.java
package com.protomil.core.shared.constants;

public final class ErrorCodes {

    // Authentication & Authorization
    public static final String AUTHENTICATION_FAILED = "AUTH_001";
    public static final String ACCESS_DENIED = "AUTH_002";
    public static final String TOKEN_EXPIRED = "AUTH_003";
    public static final String TOKEN_INVALID = "AUTH_004";
    public static final String INSUFFICIENT_PRIVILEGES = "AUTH_005";

    // Validation
    public static final String VALIDATION_ERROR = "VAL_001";
    public static final String CONSTRAINT_VIOLATION = "VAL_002";
    public static final String MALFORMED_REQUEST = "VAL_003";
    public static final String MISSING_PARAMETER = "VAL_004";
    public static final String TYPE_MISMATCH = "VAL_005";

    // Resource Management
    public static final String RESOURCE_NOT_FOUND = "RES_001";
    public static final String RESOURCE_ALREADY_EXISTS = "RES_002";
    public static final String RESOURCE_IN_USE = "RES_003";
    public static final String RESOURCE_LOCKED = "RES_004";

    // Business Logic
    public static final String BUSINESS_RULE_VIOLATION = "BIZ_001";
    public static final String INVALID_STATE_TRANSITION = "BIZ_002";
    public static final String OPERATION_NOT_ALLOWED = "BIZ_003";
    public static final String QUOTA_EXCEEDED = "BIZ_004";

    // Data Integrity
    public static final String DATA_INTEGRITY_VIOLATION = "DATA_001";
    public static final String CONCURRENT_MODIFICATION = "DATA_002";
    public static final String FOREIGN_KEY_VIOLATION = "DATA_003";
    public static final String UNIQUE_CONSTRAINT_VIOLATION = "DATA_004";

    // External Services
    public static final String EXTERNAL_SERVICE_ERROR = "EXT_001";
    public static final String EXTERNAL_SERVICE_TIMEOUT = "EXT_002";
    public static final String EXTERNAL_SERVICE_UNAVAILABLE = "EXT_003";
    public static final String AWS_SERVICE_ERROR = "AWS_001";
    public static final String COGNITO_SERVICE_ERROR = "AWS_002";

    // System Errors
    public static final String INTERNAL_SERVER_ERROR = "SYS_001";
    public static final String SERVICE_UNAVAILABLE = "SYS_002";
    public static final String CONFIGURATION_ERROR = "SYS_003";

    // HTTP Errors
    public static final String METHOD_NOT_SUPPORTED = "HTTP_001";
    public static final String MEDIA_TYPE_NOT_SUPPORTED = "HTTP_002";
    public static final String ENDPOINT_NOT_FOUND = "HTTP_003";

    // User Management Specific
    public static final String USER_NOT_FOUND = "USER_001";
    public static final String USER_ALREADY_EXISTS = "USER_002";
    public static final String USER_INACTIVE = "USER_003";
    public static final String USER_SUSPENDED = "USER_004";
    public static final String INVALID_USER_STATUS = "USER_005";

    // Job Card Specific
    public static final String JOB_CARD_NOT_FOUND = "JOB_001";
    public static final String JOB_CARD_ALREADY_ASSIGNED = "JOB_002";
    public static final String JOB_CARD_INVALID_STATUS = "JOB_003";
    public static final String JOB_CARD_PERMISSION_DENIED = "JOB_004";

    // Personnel Specific
    public static final String PERSONNEL_NOT_FOUND = "PER_001";
    public static final String PERSONNEL_NOT_AVAILABLE = "PER_002";
    public static final String SKILL_NOT_FOUND = "PER_003";
    public static final String INSUFFICIENT_SKILLS = "PER_004";

    // Equipment Specific
    public static final String EQUIPMENT_NOT_FOUND = "EQP_001";
    public static final String EQUIPMENT_NOT_AVAILABLE = "EQP_002";
    public static final String EQUIPMENT_MAINTENANCE_REQUIRED = "EQP_003";

    private ErrorCodes() {
        // Utility class - prevent instantiation
    }
}