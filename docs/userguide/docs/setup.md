# Installation

`kalasim` requires Java11 or higher.

## Gradle

To get started simply add it as a dependency via Jcenter:
```
repositories {
    jcenter()
}

dependencies {
    implementation "com.github.holgerbrandl:kalasim:0.6.7"
}
```


## Jitpack Integration

You can also use [JitPack with Maven or Gradle](https://jitpack.io/#holgerbrandl/kalasim) to build the latest snapshot as a dependency in your project.

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
        implementation 'com.github.holgerbrandl:kalasim:-SNAPSHOT'
}
```

## How to build it from sources?

To build and install it into your local maven cache, simply clone the repo and run
```bash
./gradlew install
```