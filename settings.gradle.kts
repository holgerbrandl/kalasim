
rootProject.name = "kalasim"

//include(":modules:json")
//include(":modules:k5")
include(":modules:animation")
include(":modules:logistics")
include(":modules:notebook")
include(":modules:letsplot")
include(":modules:kravis")

//having a module name that is identical to one of your imported dependencies,
// especially when the group ID is also the same, can indeed lead to issues in Gradle.
// This is because Gradle uses the combination of the group, name,
// and version (often called GAV) to uniquely identify dependencies and projects.
// If a module in your project has the same GAV identifier as an external dependency,
// it can confuse Gradle
findProject(":modules:kravis")?.name = "kravis4klsm"

//include(":modules:benchmarks")
//include("modules:sparksim")
//findProject(":modules:sparksim")?.name = "sparksim"
