# F.A.Q.

## Why rebuilding `salabim`?

Great question! Initial development was driven by curiosity about the `salabim` internals. Also, it lacked (arguably) a modern API touch which made some of our use cases more tricky to implement.

`kalasim` implements all major features of `salabim` as documented under https://www.salabim.org/manual/.


## What (TF) is the meaning of `kalasim`?

We went through multiple iterations to come up with this great name:

1. `desimuk` - {d}iscrete {e}vent {simu}lation with {k}otlin seemed a very natural and great fit. Unfortunately, Google seemed more convinced  - for reasons that were outside the scope of this project - that this name related mostly with indian porn.
2. `desim` - seemed fine initially, until we discovered another simulation engine <https://github.com/aybabtme/desim> with the same name.
3. `kalasim`  honors its origin by being somewhat phonetically similar to `salabim` while stressing Kotlin with the `k`, and the simulation scope with the `sim` instead of the `bim`.

In case you also wonder why `salabim` was named `salabim`, see [here](https://www.salabim.org/manual/About.html#why-is-the-package-called-salabim).

## Can we use it with from Java?

[Kotlin-2-Java interop](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html) is a core design goal of Kotlin. Thus, kalasim should work without any issues from java. However, we have not tried yet, so in case you struggle please file a ticket.

## Why can we use resource.request(1)?

Admittedly, the provided [resource](resource.md) request syntax `request(resource)` feels a bit dated. It's designed in that way because we would need [multiple receiver](https://youtrack.jetbrains.com/issue/KT-10468) support for extensions functions to provide a more object-oriented API. However, extensions with multiple receivers are not (yet) supported by Kotlin.

## How to fix `Simulation environment context is missing` error?

You would need to create a simulation context before instantiating the resources, components or states. E.g. with

```kotlin
Environment().apply{
    val devices = Resource(name = "devices", capacity = 3)
}
```

For more details regarding koin and dependency injection see https://www.kalasim.org/basics/#dependency-injection