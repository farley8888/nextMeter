package com.vismo.nxgnfirebasemodule.model

/**
 * Extension properties for easy access to configuration flags
 */

/**
 * Returns true if key logging is enabled in the configuration
 * Default: false
 */
val MeterSdkConfiguration?.isKeyLogEnabled: Boolean
    get() = this?.common?.isEnabledKeyLog ?: false

/**
 * Returns true if page navigation logging is enabled in the configuration
 * Default: false
 */
val MeterSdkConfiguration?.isPageLogEnabled: Boolean
    get() = this?.common?.isEnabledPageLog ?: false

/**
 * Returns true if system shutdown is enabled in the configuration
 * Default: true (shutdown is enabled by default for safety)
 */
val MeterSdkConfiguration?.isShutdownEnabled: Boolean
    get() = this?.common?.isEnabledShutdown ?: true

/**
 * Returns true if detailed ACC logging is enabled in the configuration
 * Default: false
 */
val MeterSdkConfiguration?.isDetailAccLogEnabled: Boolean
    get() = this?.common?.isEnabledDetailAccLog ?: false

/**
 * Returns true if Firestore corruption auto-fix is enabled in the configuration
 * When enabled, app will automatically delete corrupted Firestore database and restart
 * When disabled, app will only log the corruption and crash normally
 * Default: false (disabled for safety - only enable for specific devices/debugging)
 */
val MeterSdkConfiguration?.isFirestoreCorruptionAutoFixEnabled: Boolean
    get() = this?.common?.isEnabledFirestoreCorruptionAutoFix ?: false

/**
 * Returns the ACC debounce confirmation count
 * Number of consecutive readings needed to confirm ACC status change
 * Higher value = more stable but slower detection
 * Lower value = faster detection but less stable
 * Default: 500 (500 readings × 2ms = 1 second debounce time)
 */
val MeterSdkConfiguration?.accDebounceCount: Int
    get() = this?.common?.accDebounceCount ?: 500
