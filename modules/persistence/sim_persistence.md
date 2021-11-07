## Save and Load Simulation and Branching

https://www.baeldung.com/java-serialization-approaches

https://stackoverflow.com/questions/239280/which-is-the-best-alternative-for-java-serialization

https://stackoverflow.com/questions/14011467/java-serialization-alternative-with-better-performance/14012205

fst --> https://github.com/RuedigerMoeller/fast-serialization/wiki/JSON-serialization


or use built-in to enable simple branching

**TODO** kyro (modern )


https://betterprogramming.pub/the-top-11-trending-kotlin-libraries-for-2021-71dc6d65af5a

> When you think of a JSON parser, only these names come to mind, Jackson, Gson, or Moshi. And those are the most widely used parsers with Spring and Java applications as well. Kotlin serialization is


## Moshi

circular references https://stackoverflow.com/questions/10209959/gson-tojson-throws-stackoverflowerror

name vs name? `has a constructor parameter of type` https://github.com/square/moshi/issues/1146


custom adapater howto https://www.valueof.io/blog/writing-a-custom-moshi-adapter


primer code-gen https://github.com/square/moshi#codegen

great overview code-gen https://www.zacsweers.dev/exploring-moshis-kotlin-code-gen/

benefits code-gen https://stackoverflow.com/questions/58501918/whats-the-use-of-moshis-kotlin-codegen

> Earlier versions of Moshi didn't support "codegen", so they completely relied on reflection (i.e., the ability to introspect classes at runtime). However, that could be a problem for applications that require very high performance so they've added a "codegen" capability that takes advantage of annotation processing.


How To Deserialize Generic Types with Moshi?
https://newbedev.com/how-to-deserialize-generic-types-with-moshi

```kotlin
  private val moshi = Moshi.Builder()
    .add(
      PolymorphicJsonAdapterFactory.of(Vehicle::class.java, "__typename")
        .withSubtype(Car::class.java, "Car")
        .withSubtype(Boat::class.java, "Boat")
    )
    .build()
```
great ref about PolymorphicJsonAdapterFactory. https://proandroiddev.com/moshi-polymorphic-adapter-is-d25deebbd7c5

also see https://github.com/square/moshi/issues/309

## Kryo

Kotlin forum recommendation for kryo https://discuss.kotlinlang.org/t/new-serialization-library-for-kotlin/9509/8

How to ignore private fields https://github.com/EsotericSoftware/kryo/issues/289

## Builtin

Java Serialization Caveats https://www.baeldung.com/java-serialization-approaches

> When a class implements the Serializable interface, all its sub-classes are serializable as well. However, when an object has a reference to another object, these objects must implement the Serializable interface separately, or else a NotSerializableException will be thrown