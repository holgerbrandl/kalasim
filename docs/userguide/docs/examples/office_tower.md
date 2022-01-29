<!--## Elevators-->

Here, we simulate logistics in an office tower. There are 3 lifts with capacity limited cars. Passengers arrive at different floors with different rates and press buttons indicating the direction of their target floor. The cars have a defined speed, and clearly it takes time to open/close its doors before passengers can enter & leave.

Parameters

* Origin/destination distribution of visitors
* Number of elevators
* Capacity of each elevator

See [here](https://github.com/holgerbrandl/kalasim/blob/master/src/test/kotlin/org/kalasim/examples/elevator/Elevator.kt) for the implementation.

The implementation is inspired by [salabim's elevator](https://github.com/salabim/salabim/blob/master/sample%20models/Elevator.py) example.


The model was also animated ([source](https://github.com/holgerbrandl/kalasim/blob/master/modules/animation/src/test/kotlin/org/kalasim/animation/examples/elevator/ElevatorAnimated.kt)) to illustrate the power of kalasim's [animation](../animation/animation.md) API.

<div class="video-wrapper">
  <iframe width="700" height="500" src="https://www.youtube.com/watch?v=KwBeon-rXdw" frameborder="0" allowfullscreen></iframe>
</div>


