package org.kalasim.misc


/** Allows to set tracking defaults for internal metrics and logging.*/
@Suppress("PropertyName")
class SimEntityTrackingDefaults() {
    var DefaultComponentConfig = ComponentTrackingConfig()
    var DefaultStateConfig = StateTrackingConfig()
    var DefaultResourceConfig = ResourceTrackingConfig()
    var DefaultComponentCollectionConfig = ComponentCollectionTrackingConfig()

    fun disableAll() {
        DefaultComponentConfig = ComponentTrackingConfig.NONE
        DefaultStateConfig = StateTrackingConfig.NONE
        DefaultResourceConfig = ResourceTrackingConfig.NONE
        DefaultComponentCollectionConfig = ComponentCollectionTrackingConfig.NONE
    }
}


/**
 * Represents the configuration for component tracking.
 *
 * @property logCreation Whether to log component creation events.
 * @property logStateChangeEvents Whether to log component state change events.
 * @property logInteractionEvents Whether to log component interaction events.
 * @property trackComponentState Whether to track component state.
 */
data class ComponentTrackingConfig(
    val logCreation: Boolean = true,
    val logStateChangeEvents: Boolean = true,
    val logInteractionEvents: Boolean = true,
    val trackComponentState: Boolean = true
) {

    companion object {
        val NONE = ComponentTrackingConfig(
            logCreation = false,
            logStateChangeEvents = false,
            logInteractionEvents = false,
            trackComponentState = false
        )
    }
}

/**
 * Configuration class for resource tracking.
 *
 * This class provides options to enable or disable various tracking functionalities like logging,
 * queue statistics tracking, utilization tracking, activity tracking, requester tracking, and claimer tracking.
 *
 * @property logCreation Whether to log resource creation events.
 * @property logResourceChanges Whether to log resource change events.
 * @property trackQueueStatistics Whether to track queue statistics.
 * @property trackUtilization Whether to track resource utilization.
 * @property trackActivities Whether to track resource activities.
 * @property trackRequesters Whether to track resource requesters.
 * @property trackClaimers Whether to track resource claimers.
 */
data class ResourceTrackingConfig(
    val logCreation: Boolean = true,
    val logResourceChanges: Boolean = true,
    val trackQueueStatistics: Boolean = true,
    val trackUtilization: Boolean = true,
    val trackActivities: Boolean = true,
    val trackRequesters: Boolean = true,
    val trackClaimers: Boolean = true
) {

    companion object {
        val NONE = ResourceTrackingConfig(
            logCreation = false,
            logResourceChanges = false,
            trackQueueStatistics = false,
            trackUtilization = false,
            trackRequesters = false,
            trackClaimers = false
        )
    }
}

/**
 * Represents the configuration options for state tracking.
 *
 * @property logCreation Whether to log the creation of state instances.
 * @property trackQueueStatistics Whether to track statistics about state queues.
 * @property trackValue Whether to track the values of state instances.
 * @property logTriggers Whether to log state triggers.
 */
data class StateTrackingConfig(
    val logCreation: Boolean = true,
    val trackQueueStatistics: Boolean = true,
    val trackValue: Boolean = true,
    val logTriggers: Boolean = true
) {

    companion object {
        val NONE = StateTrackingConfig(
            logCreation = false,
            trackQueueStatistics = false,
            trackValue = false,
            logTriggers = false
        )
    }
}

/**
 * Represents a configuration for tracking component collection statistics.
 *
 * @property trackCollectionStatistics Indicates whether to track collection statistics or not.
 */
data class ComponentCollectionTrackingConfig(val trackCollectionStatistics: Boolean = true) {

    companion object {
        val NONE = ComponentCollectionTrackingConfig(false)
    }
}
