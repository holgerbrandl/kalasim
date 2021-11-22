package org.kalasim.misc

import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication

/**
 * Global context - current Koin Application available globally
 *
 * Support to help inject automatically instances once KoinApp has been started
 */
object DependencyContext {

    private var threadLocalValue = ThreadLocal<Koin?>()

//    private var koin: Koin? = null

         fun get(): Koin = threadLocalValue.get() ?: error("KoinApplication has not been started. See https://www.kalasim.org/faq/#how-to-fix-koinapplication-has-not-been-started")
//    fun get(): Koin = koin
        ?: error("KoinApplication has not been started. See https://www.kalasim.org/faq/#how-to-fix-koinapplication-has-not-been-started")


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

    internal fun setKoin(koin: Koin) {
        threadLocalValue.set(koin)
//        this.koin = koin
    }
}
