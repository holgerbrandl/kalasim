# State

States provide a powerful tool for process interaction.

A state will have a value at any given time. In its simplest form a component can [`wait()`](component.md#wait) for a specific value of a state. Once that value is reached, the component will be resumed.

## Examples

* [Traffic](examples/traffic.md)
* [Bank Office with 1 clerk](examples/bank_office.md#bank-office-with-states)
* [Bank Office With Balking And Reneging](examples/bank_office.md#bank-office-with-balking-and-reneging)

## Usage

New States are defined as `val doorOpen = State(false)`. The initial value is `false`, meaning
the door is closed.

Now we can say :

```kotlin
doorOpen.value = true
```

to open the door.

If we want a person to wait for an open door, we could say :

```kotlin
wait(doorOpen, true)
```

The person's [process definition](component.md#creation-of-a-component) will be suspended until the door is open.

We can obtain the current value (e.g. for logging) with:

```kotlin
print("""door is ${if(doorOpen.value) "open" else "closed"}""")
```

The value of a state is automatically monitored in the `State<T>.valueMonitor` level monitor.

All components waiting for a state are tracked in a (internal) queue, that can be obtained with `doorOpen.waiters`.

If we just want at most one person to enter, we can use `trigger()` (which is a simple convenience wrapper around `wait)` with `doorOpen.trigger(true, max=1)`. The following will happen:

1. Temporarily change the state to the provided value,
2. Reschedule `max` components (or less if there are fewer/no `waiters`) for immediate process continuation,
3. and finally restore the previous state value


## Type Support

States support generics, so we could equally well use any other type to model the value. For example, a traffic light could be modelled with a `String` state:

```
// initially the traffic light is red
val light = State("red")
...
// toggle its value to green
light.value = "green"
```

Or define a int/float state :

```
val level = State(0.0)
        
level.value += 10
```

Since `State<T>` is a generic type, the compiler will reject invalid level associations such as
```
level.value = "red"
```
This won't compile because the type of level is `Double`.


## Metrics

States have a number of metrics endpoints:

* `valueMonitor` tracks state changes over time
* `queueLength` tracks the queue length level across time
* `lengthOfStay` tracks the length of stay in the queue over time


## Process interaction with `wait()`

A component can [`wait()`](component.md#wait) for a state to get a certain value. In its most simple form this is done with

```kotlin
wait(doorOpen, true)
```

Once the `doorOpen` state is `true`, the component will be scheduled for process continuation.

As with [`request`](component.md#request) it is possible to set a timeout with `failAt` or `failDelay` :

```kotlin
wait(dooropen, true, failDelay=10.0)
if(failed) print("impatient ...")
```

In this example, the process will wait at max `10` ticks. If the state predicate was not met until then, the `failed` flag will be set and be consumed by the user.

There are two ways to test for a value

* Value testing
* Predicate testing

### Value Testing

It is possible to test for a certain value:

```kotlin
wait(light, "green")
```
    
Or more states at once:
    
```kotlin
wait(light turns "green", light turns "yellow")  
```
where the wait is honored as soon is light is `green` OR `yellow`.
    
It is also possible to wait for all conditions to be satisfied, by adding `all=true`:

```kotlin
wait(light turns "green", engineRunning turns true, all=true) 
```
Here, the wait is honored as soon as light is `green` AND  the engine is running.


### Predicate testing

This is a more complicated but also more versatile way of specifying the honor-condition. In that case, a predicate function `(T) -> Boolean` must be provided required to specify the condition.

#### Example 1

```kotlin
wait(StateRequest(State("foo")) { listOf("bar", "test").contains(it) })
```
The wait is honored if the `String` State becomes either `bar` or `test`.

#### Example 2

```kotlin
wait(StateRequest(State(3.0)) { it*3 < 42 })
```

In this last example the wait is honored as soon as the value fulfils `it*3 < 42`.
