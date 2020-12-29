<!--## Machine Shop-->

In this example, we'll learn how to interrupt a process because of more important tasks. The example is adopted from the [SimPy example](https://simpy.readthedocs.io/en/latest/examples/machine_shop.htm).

The example covers [interrupt](../component.md#interrupt) and [preemptive resources](../resource.md#pre-emptive-resources).

This example comprises a workshop with `n` identical machines. A stream of jobs (enough to keep the machines busy) arrives. Each machine breaks down periodically. Repairs are carried out by one repairman. The repairman has other, less important tasks to perform, too. Broken machines preempt these tasks. The repairman continues them when he is done with the machine repair. The workshop works continuously.

A machine has two processes:

1. *working* implements the actual behaviour of the machine (producing parts).
2. *break_machine*  periodically interrupts the working process to simulate the machine failure.

The repairman’s other job is also a process (implemented by otherJob). The repairman itself is a [preemptive resource]() with a capacity of `1`. The machine repairing has a priority of 1, while the other job has a priority of `2` (the smaller the number, the higher the priority).

```kotlin
//{!MachineShop.kt!}
```