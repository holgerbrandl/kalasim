# Kalasim Release History

## v0.6 (In Progress)

* Added `selectResource()` to [select from resources with policy](https://www.kalasim.org/resource/#resource-selection)
```kotlin
val doctors = List(3) { Resource() }
val selected = selectResource( doctors, policy = SHORTEST_QUEUE )
```

* Allow generating not just `Component`s with `ComponentGenerator`
```kotlin
ComponentGenerator(uniform(0,1)){ counter -> "smthg no$counter"}
```

* New suspending [`batch`](https://www.kalasim.org/component/#batching) interaction to group an entity stream into blocks
```kotlin
val queue = ComponentQueue<Customer>()
val batchLR: List<Customer> = batch(queue, 4, timeout = 10)
```

## v0.5

Released 2021-01-12

Major Enhancements

* Added first [jupyter notebook](https://github.com/holgerbrandl/kalasim/blob/master/simulations/notebooks/dining.ipynb) example
* New [depletable resource](https://www.kalasim.org/resource/#depletable-resources) type
* New [statistical distributions](https://www.kalasim.org/basics/#randomness-distributions) API
* New more structured event logging. See [user manual](https://www.kalasim.org/analysis/#event-log)
* Implemented support for [real-time simulations](https://www.kalasim.org/advanced/#clock-synchronization)
* New example [Dining Philosophers](https://www.kalasim.org/examples/dining_philosophers/)
* New example [Movie Theater](https://www.kalasim.org/examples/movie_theater/)
* New API to add dependencies in simulation context using `dependency {}`

Notable Fixes
* Fixed `failAt` in `request`


## v0.4

Released 2021-01-03

Major Enhancements

* Implemented [`interrupt`](https://www.kalasim.org/component/#interrupt) interaction
* Reworked documentation and examples
* Implemented [`standby`](https://www.kalasim.org/component/#standby)
* Implement disable/enable for [monitors](https://www.kalasim.org/monitors/)
* Yield internally, to simplify process definitions
```kotlin
// before
object : Component() {
    override fun process() = sequence { yield(hold(1.0)) }
}

// now
object : Component() {
    override fun process() = sequence { hold(1.0) }
}
```

* Made `scheduledTime` nullable: Replaced `scheduledTime = Double.MAX_VALUE` with `null` where possible to provide better mental execution model
* Provide lambda parameter to enable auto-releasing of resources
```kotlin
// before
object : Component() {
    override fun process() = sequence { 
        request(r)
        hold(1)
        release(r)
    }
}

// now
object : Component() {
    override fun process() = sequence { 
        request(r){
            hold(1)
        }
    }
}

```
* Implemented `Environment.toString` to provide json description
* Various bug-fixes


## v0.3

* Reimplemented monitors
* Continued salabim core API reimplementation
* Fixed: Decouple simulation with different koin application contxts

## v0.2

* Reimplement core salabim examples in kotlin
* Port all salabim examples
* Started MkDocs manual

## v0.1

* Reimplement salabim's main component lifecycle
* Add timing API
