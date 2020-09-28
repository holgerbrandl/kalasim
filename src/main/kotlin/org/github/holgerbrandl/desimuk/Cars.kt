package org.github.holgerbrandl.desimuk


class Car(env: Environment) : Component(env = env) {

    override fun process(): Sequence<Component> {
        return super.process()
    }

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

