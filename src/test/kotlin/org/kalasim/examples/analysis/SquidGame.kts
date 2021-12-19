//SquidGame.kts
package org.kalasim.examples.analysis

import kravis.geomBar
import kravis.geomCol
import kravis.plot
import org.apache.commons.math3.distribution.LogNormalDistribution
import org.kalasim.Component
import org.kalasim.Environment
import org.kalasim.enumerated
import org.kalasim.invoke
import java.lang.Double.min
import kotlin.random.Random


// Play the game of squid. Adopted from  https://www.jhelvy.com/posts/2021-10-19-monte-carlo-bridge-game/
class SquidGame(
    val numSteps: Int = 18,
    val numPlayers: Int = 16,
    val maxDuration: Int = 16 * 60
) : Environment(randomSeed = Random.nextInt()) {

    // randomization
    val stepTime = LogNormalDistribution(rg, 3.5, 0.88)
    val decision = enumerated(true, false)

    // state
    var playersLeft = numPlayers
    var stepsLeft = numSteps

    val numTrials: Int
        get() = numSteps - stepsLeft

    fun playerSurvived(playerNo: Int) = playersLeft < playerNo

    init {
        object : Component() {
            override fun process() = sequence {
                while (stepsLeft-- > 0) {
                    hold(min(stepTime(), 100.0)) // cap time at 100sec
                    if (decision()) playersLeft--
                    if (playersLeft == 0 || now > maxDuration) passivate()
                }
            }
        }
    }
}

// run once
val sim = SquidGame()
sim.run()
println("${sim.playersLeft} survived")

// run many times
val manyGames = org.kalasim.misc.repeat(1000) {
    SquidGame().apply { run() }
}

val avgSurvivors = manyGames.map { it.playersLeft }.average()

// or plot its distribution
manyGames.plot(x = { playersLeft }).geomBar().labs(
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
    x = "Player order number",
    y = "Probability"
).show()

// Calculate the probability of having less than two survivors
val probLT2Players = manyGames.count { it.playersLeft < 2 }.toDouble() / manyGames.size
println("the probability for less than 2 players is $probLT2Players")

println("On average $avgSurvivors players will survive the game of squid")

// re-run our simulations, but with an increasing number of steps. To keep things simple, I run 1,000 iterations of the game over an increasing number of steps from 10 to 30:
val stepSims = (10..30).flatMap { numSteps ->
    org.kalasim.misc.repeat(10000) {
        SquidGame(numSteps = numSteps).apply { run() }
    }
}

// Compute probability of having less than two survivors for each step siz
val stepSimSummary = stepSims.groupBy { it.numSteps }.map { (steps, games) ->
    steps to games.count { it.playersLeft < 2 }.toDouble() / games.size
}

stepSimSummary.plot(x = { it.first }, y = { it.second }).geomCol().labs(
    title = "Probability of having less than two remaining players",
    x = "Number of bridge steps",
    y = "Probability"
).show()
