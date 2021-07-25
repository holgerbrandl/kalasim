# Development Roadmap


## Next steps

https://en.wikipedia.org/wiki/Erlang_(unit)


real-time metrics
```
     object : Component() {
                override fun process() = sequence {
                    while (true) {
                        hold(asTicks(Duration.ofDays(1)))
                        println("tick time is ${env.asWallTime(now)}")
                    }
                }
            }

```

distinguish between tick-duration and tick-time.

consider `logCoreInteractions=false` as default 

## v0.7



**{tbd}** can data class be a `Component`


**{todo}** mandatory jsonable on sim-entities feels cumbersome

---
**{todo}** use standard sl4j for simulation logging or at least provide adapter

> Structured logging is the practice of implementing a consistent, predetermined message format for application logs that allows them to be treated as data sets rather than text.

https://tersesystems.github.io/terse-logback/1.0.0/structured-logging/

https://www.innoq.com/en/blog/structured-logging/
---

**TODO** add planning example from classical scheduling theory
parallel maschines --> solve with or tools

> Other relevant applications arise in the context of health-care, where, for example, patients have to be assigned to surgery rooms that must be
equipped by considering the type (i.e., the family) of surgery to
be performed. In such cases, the weight usually models a level of
urgency for the patient.


**TODO** recent survy on scheduling with setups can be found in Allahverdi (2015)


---
**TODO** pathfinding example

https://github.com/citiususc/hipster

https://www.baeldung.com/java-a-star-pathfinding

---

**{todo}** get rid of `setup`


**{todo}** respect ticktrafo in built-invisualizations

**{todo}** TickTime comparator and more consistent use in API

**{todo}** provide heatmap of component-list status

**{todo}** add `keepPositive` to `normal` 

**{todo}** review simjulia <https://simjuliajl.readthedocs.io/en/stable/topical_guides/5_shared_resources.html#containers>

**{todo}** finish elevator example with visualization

**{todo}** consider if ComponentQueue should support all types (and not just Component)

**{todo}** wait lambda should use state.value as receiver

**{todo}** lambda builder for interrupt/resume

**{todo}** continue inline predicates

**{todo}** review if simulation entities must have distinct names (optional policies) (see Hospital example)

nothing better than a good picure--> rock the (shop)floor

**{todo}** consider to expose createTestSimulation via public API

**{todo}** storeRefs --> trackArrivals

**{todo}** detach from jcenter

**{todo}** add `description` to `request` 

## Later


**{todo}** find better way to draw random value without clutter `uniform(0,5)()` --> `uniform(0,5)`?!

**{todo}** should we support a resource queue limit ` queue_size = Inf)` (as in simmer ucar209,p19)

**{todo}** Benchmark desim, salabim, simmer vs kalasim
* See Ucar2019  5.1 `Comparison with similar frameworks`

---

Slicing of monitors <https://www.salabim.org/manual/Monitor.html#slicing-of-monitors>

---

consider modeling `Store` Using Stores you can model the production and consumption of concrete objects as in <https://simpy.readthedocs.io/en/latest/topical_guides/resources.html#stores>

---

**{todo}** restrict more methods in Component from being overridden


---

fix <https://github.com/holgerbrandl/kravis/issues/25>

---

also adjust random generator of kotlin to prevent non-determinism entering simulation when user is doing `listOf().random()`


## More Examples

**{todo}** port interesting examples from <https://simjuliajl.readthedocs.io/en/stable/examples/index.html>

**{todo}** port other interesting simmer examples

**{todo}** port machine repair example from  <https://github.com/matloff/des>

**{todo}** port interesting SimPy examples from <https://simpy.readthedocs.io/en/latest/examples/index.html>

**{todo}** port  machine maintenance example from ucar 2019
![](.roadmap_images/2bad897b.png)
