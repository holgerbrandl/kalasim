package org.kalasim.misc

import org.kalasim.*


interface TrackingConfig

data class ComponentTrackingConfig(
    val logCreation: Boolean = true,
    val logStateChangeEvents: Boolean = true,
    val logInteractionEvents: Boolean = true,
    val trackComponentState: Boolean = true
) : TrackingConfig {

    companion object {
        @Suppress("unused")
        val NONE = ComponentTrackingConfig(false, false, false, false)
    }
}

data class ResourceTrackingConfig(
    val logCreation: Boolean = true,
    val logResourceChanges: Boolean = true,
    val trackQueueStatistics: Boolean = true,
    val trackUtilization: Boolean = true,
    val trackActivities: Boolean = true
) : TrackingConfig {

    companion object {
        @Suppress("unused")
        val NONE = ResourceTrackingConfig(
            logCreation = false,
            logResourceChanges = false,
            trackQueueStatistics = false,
            trackUtilization = false
        )
    }
}

data class StateTrackingConfig(
    val logCreation: Boolean = true,
    val trackQueueStatistics: Boolean = true,
    val trackValue: Boolean = true,
    val logTriggers: Boolean = true
) : TrackingConfig {

    companion object {
        @Suppress("unused")
        val NONE = StateTrackingConfig(
            logCreation = false,
            trackQueueStatistics = false,
            trackValue = false,
            logTriggers = false
        )
    }
}

data class ComponentCollectionTrackingConfig(val trackCollectionStatistics: Boolean = true) : TrackingConfig {

    companion object {
        @Suppress("unused")
        val NONE = ComponentCollectionTrackingConfig(false)
    }
}


typealias EntityFilter = (SimulationEntity) -> Boolean

class TrackingPolicyFactory {

    val policies: List<Pair<EntityFilter, TrackingConfig>> = mutableListOf()

    var defaultComponentConfig = ComponentTrackingConfig()
    var defaultResourceConfig = ResourceTrackingConfig()
    var defaultStateConfig = StateTrackingConfig()
    var defaultCollectionConfig = ComponentCollectionTrackingConfig()

    inline fun <reified T : TrackingConfig> getPolicy(entity: SimulationEntity): T {
        val config = policies.firstOrNull { it.first(entity) && it.second is T }?.second

        if (config != null) return config as T

        return when (entity) {
            is ComponentQueue<*> -> defaultCollectionConfig
            is ComponentList<*> -> defaultCollectionConfig
            is Component -> defaultComponentConfig
            is Resource -> defaultResourceConfig
            is State<*> -> defaultStateConfig

            else -> TODO("no tracking configuration for entity ${entity}. Use env.addTrackingPolicy() to define it")
        } as T
    }

    @Suppress("BooleanLiteralArgument")
    fun disableAll() {
        defaultComponentConfig = ComponentTrackingConfig.NONE
        defaultResourceConfig = ResourceTrackingConfig.NONE
        defaultStateConfig = StateTrackingConfig.NONE
        defaultCollectionConfig = ComponentCollectionTrackingConfig.NONE
    }

    fun enableAll(){
        defaultComponentConfig = ComponentTrackingConfig()
        defaultResourceConfig = ResourceTrackingConfig()
        defaultStateConfig = StateTrackingConfig()
        defaultCollectionConfig = ComponentCollectionTrackingConfig()
    }

    fun register(customPolicy: TrackingConfig, filter: EntityFilter) {
        (policies as MutableList).add(filter to customPolicy)
    }
}