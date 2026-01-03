package org.kalasim

import org.kalasim.misc.DependencyContext
import org.koin.core.Koin


/**
 * Functional interface for providing access to a simulation environment.
 */
fun interface EnvProvider {
    /**
     * Returns the simulation environment instance.
     *
     * @return The [Environment] instance
     */
    fun getEnv(): Environment
}

/**
 * Default implementation of [EnvProvider] that retrieves the environment from the dependency context.
 *
 * The environment is lazily cached on first access using thread-unsafe lazy initialization.
 */
class DefaultProvider : EnvProvider {
    private val cachedEnv by lazy(LazyThreadSafetyMode.NONE) { DependencyContext.get().get<Environment>() }
    override fun getEnv(): Environment = cachedEnv
}

/**
 * Implementation of [EnvProvider] that retrieves the environment from a Koin dependency injection container.
 *
 * @property koin The Koin instance used to resolve the environment
 */
class KoinEnvProvider(val koin: Koin) : EnvProvider {
    private val cachedEnv by lazy(LazyThreadSafetyMode.NONE) { koin.get<Environment>() }
    override fun getEnv(): Environment = cachedEnv
}

/**
 * Implementation of [EnvProvider] that wraps a pre-existing environment instance.
 *
 * @property environment The environment instance to be provided
 */
class WrappedProvider(val environment: Environment) : EnvProvider {
    override fun getEnv() = environment
}
