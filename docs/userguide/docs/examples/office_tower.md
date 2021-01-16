<!--## Elevators-->

Here, we simulate logistics in an office tower. There are 3 lifts with capacity limited cars. Passengers arrive at different floors with different rates and press buttons indicating the direction of their target floor. The cars have a defined speed, and clearly it takes time to open/close its doors before passengers can enter & leaver.

Parameters

* Origin/destination distribution of visitors
* Number of elevators
* Capacity of each elevator

<!--```kotlin-->
<!--//{!elevator/Elevator.kt!}-->
<!--```-->
See [here](https://github.com/holgerbrandl/kalasim/blob/master/src/test/kotlin/org/kalasim/examples/elevator/Elevator.kt) for the implementation

The implementation is inspired by [salabim's elevator](https://github.com/salabim/salabim/blob/master/sample%20models/Elevator.py) example.