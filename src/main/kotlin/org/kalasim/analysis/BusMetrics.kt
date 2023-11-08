package org.kalasim.analysis;

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.kalasim.*
import org.kalasim.misc.*
import org.kalasim.monitors.*
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Logs metrics for measuring load and event distribution on the internal bus..
 *
 * @property timelineInterval The duration interval in simulation time coordinates for measuring event rate in the timeline.
 * @property walltimeInterval The duration interval in walltime coordinates for measuring event rate in wall time.
 * @constructor Creates a BusMetrics instance.
 */
class BusMetrics(
    val timelineInterval: Duration = 1.minutes,
    val walltimeInterval: Duration? = 10.seconds
) : Component("BusMetrics") {

    // we measure 2 aspects overall event load and event distribution
    val eventDistribution = CategoryMonitor<String>("Event Types")

    var eventCount = 0
    var eventCountWT = 0

    var eventsTotal = 0

    val evenRateTimeline = MetricTimeline("#Events/${timelineInterval}", 0)
    val eventRateWT = mutableListOf<Pair<Instant, Double>>()
    val wtLogger: Thread?

    init {
        env.addEventListener {
            eventDistribution.addValue(it.eventType)

            eventCount++
            eventsTotal++
        }

        wtLogger = if(walltimeInterval != null) {
            thread(isDaemon = true) {
                while(!isInterrupted) {
                    try {
                        Thread.sleep(walltimeInterval.inWholeMilliseconds)


                        if(env.running) continue // do not run if simulation has stopped

                        val eventsPerSecond = eventCountWT.toDouble() / walltimeInterval.inWholeSeconds
                        eventRateWT.add(Clock.System.now() to eventsPerSecond)

                        logger.info {
                            "${name}: ${eventsPerSecond.roundAny(2)} events processed on average per wall-time second"
                        }

                        // reset counter
                        eventCountWT = 0
                    } catch(ie: InterruptedException) {
                        logger.info { "Stopped bus metrics" }
                    }
                }
            }
        } else null

    }

    override fun repeatedProcess() = sequence {
        hold(timelineInterval)

        evenRateTimeline.addValue(eventCount)

        logger.info { "${name}: $eventCount events processed in last $timelineInterval" }
        eventCount = 0
    }

    fun stop() {
        wtLogger?.interrupt()
        Thread.sleep(2.seconds.inWholeMilliseconds)
    }
}
