package org.kalasim.examples.zombies

import org.kalasim.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

// Define map dimensions and utility function for random direction
const val MAP_WIDTH = 10
const val MAP_HEIGHT = 10

fun randomDirection(): Pair<Int, Int> {
    return when(Random.nextInt(4)) {
        0 -> Pair(0, 1)   // move up
        1 -> Pair(0, -1)  // move down
        2 -> Pair(1, 0)   // move right
        3 -> Pair(-1, 0)  // move left
        else -> Pair(0, 0)
    }
}

// Define the Zombie Component
class Zombie(name: String) : Component(name) {
    var position = Pair(Random.nextInt(MAP_WIDTH), Random.nextInt(MAP_HEIGHT))

    override fun process() = sequence {
        while (true) {
            move()
            hold(1.seconds) // Each step takes 1 second
        }
    }

    private fun move() {
        val (dx, dy) = randomDirection()
        position = Pair((position.first + dx).coerceIn(0, MAP_WIDTH - 1), (position.second + dy).coerceIn(0, MAP_HEIGHT - 1))
        println("Zombie ${name} moved to $position at ${env.now}")
    }
}

// Define the ZombieKiller Component
class ZombieKiller(name: String) : Component(name) {
    var position = Pair(Random.nextInt(MAP_WIDTH), Random.nextInt(MAP_HEIGHT))

    override fun process() = sequence {
        while (true) {
            move()
            hold(1.seconds)

            // Check for collisions with zombies and kill them
            getZombies().forEach { zombie ->
                if (zombie.position == this@ZombieKiller.position) {
                    kill(zombie)
                }
            }
        }
    }

    private fun move() {
        val (dx, dy) = randomDirection()
        position = Pair((position.first + dx).coerceIn(0, MAP_WIDTH - 1), (position.second + dy).coerceIn(0, MAP_HEIGHT - 1))
        println("ZombieKiller ${name} moved to $position at ${env.now}")
    }

    private fun kill(zombie: Zombie) {
        println("ZombieKiller ${name} killed Zombie ${zombie.name} at ${env.now}")
        zombie.cancel()
    }

    private fun getZombies() = env.queue.filterIsInstance<Zombie>()
}

// Statistics reporter to emit statistics periodically
class StatisticsReporter : Component() {
    override fun process() = sequence {
        while (true) {
            hold(1.hours)
            emitStatistics()
        }
    }

    private fun emitStatistics() {
        println("Statistics at ${env.now}:")
        println("  Zombies remaining: ${env.queue.filterIsInstance<Zombie>().count()}")
        println("  Zombie Killers: ${env.queue.filterIsInstance<ZombieKiller>().count()}")
        println("  Current Time: ${env.now}")

        // Additional statistics can be added here as needed
    }
}

// Define simulation parameters
val simulationDuration = 24.hours // Total simulation time

fun main() {
    createSimulation {
        // Enable logging (optional)
        enableComponentLogger()

        // Initial zombies and killers on the map
        repeat(3) { Zombie("Zombie #$it") }
        repeat(2) { ZombieKiller("Killer #$it") }

        // Generate zombies at random intervals using ComponentGenerator and exponential distribution
        val spawnInterval = exponential(2.seconds)
        ComponentGenerator(iat = spawnInterval) {
            val zombie = Zombie("Zombie ${env.queue.filterIsInstance<Zombie>().count()}")
            zombie
        }

        // Initialize StatisticsReporter
        StatisticsReporter()
    }.run(simulationDuration)
}