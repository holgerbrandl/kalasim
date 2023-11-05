package org.kalasim.test

import kotlinx.datetime.Instant
import org.apache.commons.math3.distribution.ConstantRealDistribution
import org.kalasim.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.time.DurationUnit

fun SimulationEntity.printInfo() = println(this)

// this class is simply copied from https://github.com/holgerbrandl/krangl

internal data class CapturedOutput(val stdout: String, val stderr: String)

internal fun captureOutput(expr: () -> Any): CapturedOutput {
    val origOut = System.out
    val origErr = System.err
    // https://stackoverflow.com/questions/216894/get-an-outputstream-into-a-string

    val baosOut = ByteArrayOutputStream()
    val baosErr = ByteArrayOutputStream()

    System.setOut(PrintStream(baosOut))
    System.setErr(PrintStream(baosErr))

    // run the expression
    expr()

    val stdout = String(baosOut.toByteArray()).trim().replace(System.lineSeparator(), "\n")
    val stderr = String(baosErr.toByteArray()).trim().replace(System.lineSeparator(), "\n")

    System.setOut(origOut)
    System.setErr(origErr)

    return CapturedOutput(stdout, stderr)
}

// since the test reference dat is typically provided as multi-line which is using \n by design, we adjust the
// out-err stream results accordingly here to become platform independent.
// https://stackoverflow.com/questions/48933641/kotlin-add-carriage-return-into-multiline-string
//internal fun String.trimAndReline() = trimIndent().replace("\n", System.getProperty("line.separator"))


internal fun createTestSimulation(
    startDate: Instant = Instant.fromEpochMilliseconds(0),
    enableComponentLogger: Boolean = true,
    /** The duration unit of this environment. Every tick corresponds to a unit duration. See https://www.kalasim.org/basics/#running-a-simulation */
    tickDurationUnit: DurationUnit = DurationUnit.MINUTES,
    builder: Environment.() -> Unit,
) {
    createSimulation(startDate, tickDurationUnit = tickDurationUnit) {
        if(enableComponentLogger) enableComponentLogger()
        builder()
    }
}

internal fun <S : Environment> testModel(sim: S, smthg: S.() -> Unit): Unit = smthg(sim)


//relates to https://github.com/holgerbrandl/kalasim/issues/11
//internal fun testComponent(enableComponentLogger: Boolean = true, block:  suspend SequenceScope<Component>.() -> Unit) = createTestSimulation {
//
//    object : Component() {
//        override fun process() = sequence {
//            block()
//        }
//    }
//
//}
//
//fun main() {
//    testComponent {
//        hold(1)
//    }
//}


/** Converts a list of fixed history into a inter-arrival distribution. Once the list is exhausted it will throw an
 * error. This is mainly useful for testing.
 *
 * Note: the thrown NoSuchElementException will cause the event-loop to terminate a consuming ComponentGenerator
 */
internal fun Environment.inversedIatDist(vararg arrivalTimes: Number) = object : ConstantRealDistribution(-1.0) {

    val values = (listOf(now) + arrivalTimes.map { asSimTime(TickTime(it.toDouble())) })
        .zipWithNext()
        .map { (prev, curVal) -> (curVal - prev) }
        .iterator()


    override fun sample(): Double = values.next().ticks
}


// see https://github.com/kotest/kotest/issues/1084
infix fun String.shouldBeDiff(expected: String) = assertEquals(expected, this)
