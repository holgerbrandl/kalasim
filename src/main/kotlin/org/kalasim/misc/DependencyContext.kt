package org.kalasim.misc

import org.koin.core.Koin
import org.koin.dsl.koinApplication

class MissingDependencyContextException : Exception(
    "Simulation environment context is missing. Such a context is required to properly instantiate simulation entities. " +
            "See https://www.kalasim.org/basics/#simulation-environment"
)
/**
 * Global context - current Koin Application available globally
 *
 * Support to help inject automatically instances once KoinApp has been started
 */
object DependencyContext {

    private var threadLocalValue = ThreadLocal<Koin?>()

//    private var koin: Koin? = null

    fun get(): Koin = threadLocalValue.get() ?: throw MissingDependencyContextException()
    // previous version without thread-local (to enable fast rollback in case this turns out to be a bad idea
//    fun get(): Koin = koin
//        ?: error("KoinApplication has not been started. See https://www.kalasim.org/faq/#how-to-fix-koinapplication-has-not-been-started")


    fun invoke(): Koin = get()

    fun stopKoin() = synchronized(this) {
        get().close()
        threadLocalValue.set(null)
//        koin = null
    }

    internal fun startKoin(): Koin {
        setKoin(koinApplication {}.koin)
        return get()
    }

    // public to enable restoring
    fun setKoin(koin: Koin) {
        threadLocalValue.set(koin)
//        this.koin = koin
    }
}
