# State

States together with the `Component.wait()` method provide a powerful way of process interaction.

A state will have a certain value at a given time. In its simplest form a component can then wait for
a specific value of a state. Once that value is reached, the component will be resumed.

Definition is simple, like `val doorOpen = State(false)`. The initial value is False, meaning
the door is closed.


Now we can say :

```kotlin
doorOpen.value = true
```

to open the door.

If we want a person to wait for an open door, we could say :

```kotlin
yield(wait(doorOpen, true))
```

If we just want at most one person to enter, we say `doorOpen.trigger(max=1)`.

We can obtain the current value by just calling the state, like in:

```kotlin
print("""door is ${if(doorOpen.value) "open" else "closed"}""")
```

The value of a state is automatically monitored in the `State<T>.value` level monitor.

All components waiting for a state are in a queue, called `waiters()`.

## Type Support

States support generics, so we could equally well use a string (or any type) to indicate state.

States can be used also for non values other than bool type. E.g.

```
val light = State("red")
...
light.value = "green"
```

Or define a int/float state :

```
val level = State(0.0)
        
level.value += 10
```

Since `State<T>` is a generic type, the compile will reject invalid level associations such as
```
level.value = "foo"
```
This won't compile because the type of level is `Double`.


States have a number of monitors:

* `valueMonitor`, where all the values are collected over time
* info.queueLengthStats,
* info.lengthOfStayStats

## Process interaction with `wait()`

A component can wait for a state to get a certain value. In its most simple form :

```kotlin
yield(wait(dooropen, true))
```

Once the dooropen state is `true`, the component will continue.

As with request() it is possible to set a timeout with fail_at or fail_delay :

```kotlin
yield(wait(dooropen, true, failDelay=10.0))
if(failed) print("impatient ...")
```

In the above example we tested for a state to be true.

There are three ways to test for a value:

### Value testing

It is possible to test for a certain value :

```kotlin
yield(wait(light, "green"))
```
    
Or more states at once :
    
```kotlin
yield(wait(light turns "green", light turns "yellow"))  // honored as soon is light is green OR yellow
```
    
It is also possible to wait for all conditions to be satisfied, by adding `all=true`:

```kotlin
yield(wait((light turns "green"), enginerunning turns true, all=true)) // honored as soon as light is green AND engine is running
```


### Predicate testing

This is a more complicated but also more versatile way of specifying the honor-condition. In that case, a predicate function `(T) -> Boolean` is required to specify the condition.

E.g.:
        
```kotlin
yield(wait(StateRequest(State("foo")) { listOf("bar", "test").contains(it) })
yield(wait(StateRequest(State(3.0)) { it*3 < 42 }))
```
