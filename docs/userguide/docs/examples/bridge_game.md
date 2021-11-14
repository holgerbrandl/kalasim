<!--## Squid Game-->

The glass bridge is a game in the [Netflix](https://netflix.com/) series [The Squid Game](https://www.imdb.com/title/tt10919420/). The series is Netflix's most-watched series to date, becoming the top-viewed program in 94 countries and attracting more than 142 million member households during its first four weeks from launch. (Source [Wikipedia](https://en.wikipedia.org/wiki/Squid_Game))

**Spoiler Alert** Don't read the article if you intend to watch the series!

![Squid Game Copyright Netflix 2021](squid_game_poster.png){: .center}

<p align="center">
<i><a href="https://www.netflix.com/de/title/81040344">Squid Game</a> - © Netflix 2021</i>
</p>


In one scene in Episode 7, 16 players have to cross a bridge made of two rows of glass tiles. The bridge is 18 steps long. They have to jump to one tile per row, but just one will last whereas the other one is made of tempered glass, which breaks under impact. The players start in an ordered fashion, whereby players with higher numbers will avoid broken tiles. To penalize players with higher numbers, there is a time-limit after which players which have not passed the bridge have lost as well (and pay with their lives). 

**Disclaimer** The author considers the game purely from a scientific/fictional perspective. The game as well as  the concept of the series are immoral, wrong, and detestable.

![Squid Game Copyright Netflix 2021](squid_game_bridge_scene.png){: .center}

<p align="center">
<i><a href="https://www.netflix.com/de/title/81040344">Squid Game</a> - © Netflix 2021</i>
</p>


Inspired by another [simulation](https://www.jhelvy.com/posts/2021-10-19-monte-carlo-bridge-game/) this example illustrates how to run simulations in different configurations many times to work out process parameter. Here, the key parameter of interest is the **number of surviving players**.

As players in the show, can pick their start number, the episode and also the internet community circles around the question regarding an optimal start number to _optimize the chance of survival_.

At its heart - which is its [process definition](../component.md#creation-of-a-component) -  it is a very simplistic model that centers around simulating the participants stepping on the tiles one after another, while considering the _learning experience_ of earlier participants with lower start numbers.

```kotlin hl_lines="9"
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
                while(stepsLeft-- > 0) {
                    hold(min(stepTime(), 100.0)) // cap time at 100sec
                    hold(min(stepTime(), 100.0)) // cap time at 100sec
                    if(decision()) playersLeft--
                    if(playersLeft == 0 || now > maxDuration) passivate()
                }
            }
        }
    }
}

```


Here, we also cap the time it takes a player to cross the bridge (or just part of it) at 100 seconds. Move times are modelled using a log-normal distribution with the parameters from [here](https://www.jhelvy.com/posts/2021-10-19-monte-carlo-bridge-game/).




See [here]()
Complete source:

```kotlin
//{!analysis/SquidGame.kts!}
```

Here,  we use both lazy injection with `inject<T>()` and instance retrieval with `get<T>()`. For details see [koin reference](https://doc.insert-koin.io/#/koin-core/injection-parameters)

