# Development Roadmap


## Pre-release

**{todo}** use kontext isolation if possibel https://github.com/InsertKoinIO/koin/blob/master/koin-projects/docs/reference/koin-core/start-koin.md#koin-context-isolation

**{todo}** implement `enabled` disabled for monitors

**{todo}**  make `scheduledTime` nullable: replace scheduledTime = Double.MAX_VALUE with `null` which is semantically more meaningful here

**{todo}** update logs example in docs intro

**{todo}** review docs for https://holgerbrandl.github.io/kalasim/state/

**{todo}** complete concepts docs

**{todo}** prepare basic examples

**{todo}** make component registration explict using builder context
```
//now
env.apply {
    CustomerGenerator()
}

//better

env.apply {
    add{ CustomerGenerator() } // which would configure env behind the scenes
    // or even more explict
    CustomerGenerator(env)
}

```

## 0.3

**{todo}** ensure that just yieldable methods are used in yield by checking stacktrace

Slicing of monitors https://www.salabim.org/manual/Monitor.html#slicing-of-monitors

revise data prep and rendering of histogram get more similar to kalasim https://www.salabim.org/manual/Monitor.html

## Later

Review if request and wait should really need
```
failAt: RealDistribution? = null,
failDelay: RealDistribution? = null,
```
and why the distribution can not be used at the call site?

---

Register project at awesome-kotlin
