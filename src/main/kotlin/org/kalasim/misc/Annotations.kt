package org.kalasim.misc


@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is not recommended as the duration unit is not specified. Remove any doubt by adding .minutes, .hours, .days accordingly. The default duration unit of a simulation environment is MINUTES."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class AmbiguousDuration

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is not recommended and deprecated in kalasim (v0.12+). Extend org.kalasim.Component directly. It's strongly encouraged to use kotlinx.datetime Instant instead of Number to configure and express times in your simulation model."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class AmbiguousDurationComponent

//@RequiresOptIn(level = RequiresOptIn.Level.WARNING, message = "This API is not recommended as the duration unit is not specified. Remove any doubt by specifing the tick duration unit of this simulation environment.")
//@Retention(AnnotationRetention.BINARY)
//@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION)
//annotation class EnvironmentWithoutDurationUnit