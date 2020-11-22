# Development Roadmap


## Pre-release

**{todo}**  replace scheduledTime = Double.MAX_VALUE with null which is semantically more meaningful here

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

Slicing of monitors https://www.salabim.org/manual/Monitor.html#slicing-of-monitors

## Later

Register project at awesome-kotlin


## Koin pointers

https://medium.com/mobile-app-development-publication/kotlin-koin-scope-illustrated-3bfa6c7ae98