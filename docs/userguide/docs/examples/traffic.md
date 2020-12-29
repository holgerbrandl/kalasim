<!--## Traffic-->

The following example integrates three simulation entities

* A gas station with a limited number of pumps
* A traffic light that prevents cars from driving
* Multiple cars that need to pass the cross with the traffic light to reach a gas station. There each car needs to refill before it is reaching its end of live within the simulation context.

The example illustrates how to establish a simple interplay of  [states](../state.md) and [resources](../resource.md). It is realized elegantly with [dependency injection](../basics.md#dependency-injection).

```kotlin
//{!ExtendedCars.kts!}
```

Here,  we use both lazy injection with `inject<T>()` and instance retrieval with `get<T>()`. For details see [koin reference](https://doc.insert-koin.io/#/koin-core/injection-parameters)

