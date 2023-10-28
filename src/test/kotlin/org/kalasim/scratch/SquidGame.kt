//SquidGame.kts
package org.kalasim.scratch

import kravis.*
import org.apache.commons.math3.distribution.LogNormalDistribution
import org.kalasim.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun main() {

// Play the game of squid. Adopted from  https://www.jhelvy.com/posts/2021-10-19-monte-carlo-bridge-game/
    val random = Random(0)

    class SquidGame(
        val numSteps: Int = 18,
        val numPlayers: Int = 16,
        val maxDuration: Duration = 12 .minutes
    ) : Environment(randomSeed = random.nextInt()) {

        // randomization
        val stepTime = LogNormalDistribution(rg, 3.5, 0.88).seconds
//    val stepTime = uniform(10,30)

        val decision = enumerated(true, false)

        // state
        var stepsLeft = numSteps
        var survivors = mutableListOf<Int>()

        val numTrials: Int
            get() = numSteps - survivors.size

        val numSurvivors: Int
            get() = survivors.size

        fun playerSurvived(playerNo: Int) = survivors.contains(playerNo)

        init {
            object : Component() {

                val simStart = now
                override fun process() = sequence {
                    queue@
                    for (player in 1..numPlayers) {
                        hold(minOf(stepTime(), 100.seconds)) // cap time at 100sec

                        while (stepsLeft-- > 0) {
                            if (decision()) continue@queue
                        }

                        survivors.add(player)

                        if ((now-simStart) > maxDuration) break // this wrong, here we need to model
                    }
                }
            }
        }
    }
// run once
    val sim = SquidGame()
    sim.run()

    println("${sim.numSurvivors} survived")

// run many times
    val manyGames = org.kalasim.misc.repeat(1000) {
        SquidGame().apply { run() }
    }

    val avgSurvivors = manyGames.map { it.numSurvivors }.average()
    println("The average number of survivors is ${avgSurvivors}")

// or plot its distribution
    manyGames.plot(x = { numSurvivors }).geomBar().labs(
        title = "Outcomes of 10,000 trials",
        x = "Number of survivors",
        y = "Count"
    ).show()

// Plot the probability of survival based on the player order number
    val survivalProbByNo = (1..manyGames.first().numPlayers).map { playerNo ->
        playerNo to manyGames.count { it.playerSurvived(playerNo) }.toDouble() / manyGames.size
    }
    survivalProbByNo.plot(x = { it.first }, y = { it.second }).geomCol().labs(
        title = "Probability of survival based on player order number",
        x = "Player Order Number",
        y = "Probability"
    ).show()

// Calculate the probability of having less than two survivors
    val probLT2Players = manyGames.count { it.numSurvivors < 2 }.toDouble() / manyGames.size
    println("the probability for less than 2 players is ${probLT2Players}")

    println("On average ${avgSurvivors} players will survive the game of squid")

// re-run our simulations, but with an increasing number of steps. To keep things simple, I run 1,000 iterations of the game over an increasing number of steps from 10 to 30:
    val stepSims = (10..30).flatMap { numSteps ->
        org.kalasim.misc.repeat(10000) {
            SquidGame(numSteps = numSteps).apply { run() }
        }
    }

    val stepSimSummary = stepSims.groupBy { it.numSteps }.map { (steps, games) ->
        steps to games.count { it.numSurvivors < 2 }.toDouble() / games.size
    }

// Compute probability of having less than two survivors for each step siz
    stepSimSummary.plot(x = { it.first }, y = { it.second }).geomCol().labs(
        title = "Probability of having less than two remaining players",
        x = "Number of bridge steps",
        y = "Probability"
    ).show()
}
