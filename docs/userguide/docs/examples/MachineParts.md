## Machine Parts

This model demonstrates the use of *stacked* interrupts. It is adopted from [here](https://github.com/salabim/salabim/blob/master/sample%20models/Demo%20interrupt%20resume.py).

Each of two machines has three parts, that will be subject to failure. If one or more of these parts has failed, the machine is stopped. Only when all parts are operational, the machine can continue its work (hold).

For a machine to work it needs the resource. If, during the requesting of this resource, one or more parts of that machine break down, the machine stops requesting until all parts are operational.

In this model the interrupt level frequently gets to 2 or 3 (all parts broken down).

Have a close look at the trace output to see what is going on.

```kotlin
//{!MachineWithParts.kt!}
```
