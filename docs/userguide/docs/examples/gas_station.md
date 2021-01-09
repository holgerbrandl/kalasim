# Gas Station

This example models a gas station and cars that arrive at the station for refueling.

Covers:

*Resources: Resource
* [Depletable Resources](../resource.md#)
* Process Interaction, particular [waiting](../component.md#wait) for other processes

The gas station has a limited number of fuel pumps and a fuel tank that is shared between the fuel pumps. The gas station is thus modeled as Resource. The shared fuel tank is modeled with a Container.

Vehicles arriving at the gas station first request a fuel pump from the station. Once they acquire one, they try to take the desired amount of fuel from the fuel pump. They leave when they are done.

The gas stations fuel level is reqularly monitored by gas station control. When the level drops below a certain threshold, a tank truck is called to refuel the gas station itself.

```kotlin
//{!GasStation.kt!}
```

Here,  we use both lazy injection with `inject<T>()` and instance retrieval with `get<T>()`. For details see [koin reference](https://doc.insert-koin.io/#/koin-core/injection-parameters)

The example is a true classic and was adopted from [salabim](https://github.com/salabim/salabim/blob/master/sample%20models/Gas%20station.py)'s and [SimPy](https://simpy.readthedocs.io/en/2.3.1/examples/gas_station_refuel.html)'s gas stations.
