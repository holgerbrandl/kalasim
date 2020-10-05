package org.github.holgerbrandl.desim


class Car : Component() {

    override suspend fun SequenceScope<Component>.process() {
        while (true) {
            yield(hold(1.0))
        }
    }
}

fun main() {
    Environment().apply {
        addComponent(Car())
        addComponent(Car())
        addComponent(Car())
    }.run(5.0)
}

