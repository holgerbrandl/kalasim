package org.kalasim.misc

import org.kalasim.*


interface TrackingConfig

data class ComponentTrackingConfig(
    val logCreation: Boolean = true,
    val logStateChangeEvents: Boolean = true,
    val logInteractionEvents: Boolean = true,
    val trackComponentState: Boolean = true
) : TrackingConfig

data class ResourceTrackingConfig(
    val logCreation: Boolean = true,
    val logClaimRelease: Boolean = true,
    val trackQueueStatistics: Boolean = true,
    val trackUtilization: Boolean = true,
    val trackActivities: Boolean = true
) : TrackingConfig

data class StateTrackingConfig(
    val logCreation: Boolean = true,
    val trackQueueStatistics: Boolean = true,
    val trackValue: Boolean = true,
    val logTriggers: Boolean = true
) : TrackingConfig

data class ComponentCollectionTrackingConfig(val trackQueueStatistics: Boolean = true) : TrackingConfig

typealias EntityFilter = (SimulationEntity) -> Boolean

class TrackingPolicyFactory {

    val policies: List<Pair<EntityFilter, TrackingConfig>> = mutableListOf()

    var defaultComponentConfig = ComponentTrackingConfig()
    var defaultResourceConfig = ResourceTrackingConfig()
    var defaultStateConfig = StateTrackingConfig()
    var defaultCollectionConfig = ComponentCollectionTrackingConfig()

    inline fun <reified T : TrackingConfig> getPolicy(entity: SimulationEntity): T {
        val config = policies.firstOrNull { it.first(entity)  && it.second is T }?.second

        if (config != null) return config as T

        return when (entity) {
            is ComponentQueue<*> -> defaultCollectionConfig
            is Component -> defaultComponentConfig
            is Resource -> defaultResourceConfig
            is State<*> -> defaultStateConfig

            else -> TODO("no tracking configuration for entity ${entity}. Use env.addTrackingPolicy() to define it")
        } as T
    }

    fun disableAll() {
         defaultComponentConfig = ComponentTrackingConfig(false, false,false, false)
         defaultResourceConfig = ResourceTrackingConfig(false, false, false, false, false, )
         defaultStateConfig = StateTrackingConfig(false, false, false,false)
         defaultCollectionConfig = ComponentCollectionTrackingConfig(false)
    }

    fun register(customPolicy: TrackingConfig, filter: EntityFilter) {
        (policies as MutableList).add(filter to customPolicy)
    }
}