package org.github.holgerbrandl.basamil


class Car(name:String, env:Environment) : Component(env = env, name = name, process = null) {
     override fun process(): Sequence<Any> {
        return sequence {
            while (true)
                yield(hold(1))
        }
    }

}

fun main() {
    Environment().build {
        addComponent(Car("c1", this))
        addComponent(Car("c2", this))
        addComponent(Car("c3", this))

        this
    }.run(5)
}

