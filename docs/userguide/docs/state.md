# State

States together with the `Component.wait()` method provide a powerful way of process interaction.

A state will have a certain value at a given time. In its simplest form a component can then wait for
a specific value of a state. Once that value is reached, the component will be resumed.

Definition is simple, like `val doorOpen = State(false)`. The initial value is False, meaning
the door is closed.


Now we can say ::

```kotlin
doorOpen.value = true
```

to open the door.

If we want a person to wait for an open door, we could say ::

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

Or define a int/float state ::

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

A component can wait for a state to get a certain value. In its most simple form ::

```kotlin
yield(wait(dooropen, true))
```

Once the dooropen state is True, the component will continue.

As with request() it is possible to set a timeout with fail_at or fail_delay ::

```kotlin
yield(wait(dooropen, true, failDelay=10.0))
if(failed) print("impatient ...")
```

In the above example we tested for a state to be True.

There are three ways to test for a value:

Scalar testing
~~~~~~~~~~~~~~
It is possible to test for a certain value ::

    yield self.wait((light, "green"))
    
Or more states at once ::
    
    yield self.wait((light, "green"), night)  # honored as soon as light is green OR it"s night
    yield self.wait((light, "green"), (light, "yellow"))  # honored as soon is light is green OR yellow
    
It is also possible to wait for all conditions to be satisfied, by adding ``all=True``::

    yield self.wait((light,"green"), enginerunning, all=True)  # honored as soon as light is green AND engine is running
    
Evaluation testing
~~~~~~~~~~~~~~~~~~
Here, we use a string containing an expression that can evaluate to True or False. This is
done by specifying at least one ``$`` in the test-string. This ``$`` will be replaced at run time by
``state.value()``, where state is the state under test. Here are some examples ::

    yield self.wait((light, "$ in ("green","yellow")")) 
        # if at run time light.value() is "green", test for eval(state.value() in ("green,"yellow")) ==> True
    yield self.wait((level, "$ < 30"))
        # if at run time level.value() is 50, test for eval(state.value() < 30) ==> False

During the evaluation, ``self`` refers to the component under test and ``state`` to the state under test.
E.g. ::

    self.limit = 30
    yield self.wait((level, "self.limit >= $"))
        # if at run time level.value() is 10, test for eval(self.limit >= state.get()) ==> True, so honored

Function testing
~~~~~~~~~~~~~~~~
This is a more complicated but also more versatile way of specifying the honor-condition.
In that case, a function is required to specify the condition. The function needs to accept three
arguments:

* x = state.get()
* component component under test
* state under test

E.g.::
        
    yield self.wait((light, lambda x, component, state x in ("green", "yellow"))
        # x is light.get()
    yield self.wait((level, lambda x, *_: x >= 30))
        # x is level.get(), other two parameters are "dummied"
        
And, of course, it is possible to define a function ::

    def levelreached(value, component, state):
        return value < component.limit
        
    ...
    
    self.limit = 30
    yield self.wait((level, levelreached))
    
Combination of testing methods
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
It is possible to mix scalar, evaluation and function testing. And it"s also possible to specify all=True
in any case.