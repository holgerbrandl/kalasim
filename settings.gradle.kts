
rootProject.name = "kalasim"

//include(":modules:json")
//include(":modules:k5")
include(":modules:animation")
include("modules:sparksim")
findProject(":modules:sparksim")?.name = "sparksim"
