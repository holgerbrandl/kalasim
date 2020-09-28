package org.github.holgerbrandl.basamil


class Car(env: Environment) : Component(env = env) {
    override suspend fun SequenceScope<Component>.process() {
        while (true) {
            yield(hold(1.0))
        }
    }
}

fun main() {
    Environment().build {
        addComponent(Car(this))
        addComponent(Car( this))
        addComponent(Car( this))

        this
    }.run(5.0)
}

