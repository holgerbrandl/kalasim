package org.kalasim.scratch

import org.koin.core.context.startKoin
import org.koin.dsl.module


class Foo(val bar: Bar) { init {
    println(javaClass)
}
}

class Bar { init {
    println(javaClass)
}
}

class Something(val foo: Foo) { init {
    println(javaClass)
}
}

fun main() {
    val app = startKoin {
        modules(
            module {
                single(createdAtStart = true) { Something(get()) }
                single(createdAtStart = true) { Bar() }
                single(createdAtStart = true) { Foo(get()) }
            }
        )
    }

    // we find: the components are resolved correctly irrespective of the used order

    val defA = app.koin.get<Foo>()
}

