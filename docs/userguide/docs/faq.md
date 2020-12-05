# F.A.Q.

## Why rebuilding `salabim`?

Great question! Initial development was driven by curiosity about the `salabim` internals. Also, it lacked (arguably) a modern touch which made some of our usecases more tricky to implement.


`kalasim` implements the great majority of `salabim` features as documented under https://www.salabim.org/manual/ including

* [Components](https://www.salabim.org/manual/Component.html)
* [ComponentGenerator](https://www.salabim.org/manual/ComponentGenerator.html)
* [Queue](https://www.salabim.org/manual/Queue.html)
* [Distributions](https://www.salabim.org/manual/Distributions.html) (via apache-commons-math)
* [Monitor](https://www.salabim.org/manual/Monitor.html) (via apache-commons-math)
* [Resource](https://www.salabim.org/manual/Resource.html)
* [State](https://www.salabim.org/manual/State.html)

Not planned

* [Animation](https://www.salabim.org/manual/Animation.html) - which we believe should live in a separate codebase. Visualization in `kalasim` is detailed out in the [visualization chapter](analysis.md).


## What (TF) is the meaning of `kalasim`?

We went through multiple iterations to come up with this great name:

1. `desimuk` - {d}iscrete {e}vent {simu}lation with {k}otlin seemed a very natural and great fit. Unfortunately, Google seemed more convinced  - for reasons that were outside the scope of this project - that this name related mostly with indian porn.
2. `desim` - seemed fine initially, until we discovered another simulation engine <https://github.com/aybabtme/desim> with the same name.
3. `kalasim`  honors its origin by being somewhat phonetically similar to `salabim` while stressing Kotlin with the `k`, and the simulation scope with the `sim` instead of the `bim`.

In case you also wonder why `salabim` was named `salabim`, see [here](https://www.salabim.org/manual/About.html#why-is-the-package-called-salabim).

## Can we use it with from Java?

[Kotlin-2-Java interop](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html) is a core design goal of Kotlin. Thus, kalasim should work without any issues from java. However, we have not tried yet, so in case you struggle please file a ticket.