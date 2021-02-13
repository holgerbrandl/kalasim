package org.kalasim.scratch

import org.kalasim.Component
import org.kalasim.Environment
import org.kalasim.Resource

val env = Environment()

val c = Component("foo")

//c.hold(1)

c.status

env.run(2)

c.status

c.activate(process = Component::process)

val r = Resource(capacity = 0)
sequence<Component> {
    with(c) {
        request(r)
    }
}.toList()

env.run(1)

r.capacity = 2.0

env.run(1)
c.status

env.run(1)

c.status

c.statusMonitor.printHistogram()


c.statusMonitor[1.0]

