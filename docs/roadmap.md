# Development Roadmap


## Next steps


## v0.7

**{todo}** provide heatmap of component-list status

**{todo}** add `keepPositive` to `normal` 

**{todo}** build visualization for covid19 simulation

**{todo}** review simjulia <https://simjuliajl.readthedocs.io/en/stable/topical_guides/5_shared_resources.html#containers>

**{todo}** finish elevator example with visualization

**{todo}** consider if ComponentQueue should support all types (and not just Component)

**{todo}** wait lambda should use state.value as receiver

**{todo}** lambda builder for interrupt/resume

**{todo}** continue inline predicates

**{todo}** read: Flow itself is a very good model for even-based simulations.

## Later

**{todo}** review if simulation entities must have distinct names (optional policies) (see Hospital example)

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
