package org.kalasim.test

import kravis.GGPlot
import kravis.SessionPrefs
import kravis.render.LocalR
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.kalasim.examples.MM1Queue
import org.kalasim.plot.kravis.*
import java.io.*
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import kotlin.io.path.createTempFile
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.minutes

class DisplayTests : AbstractSvgPlotRegression() {

    override val testDataDir: File
        get() = File("src/test/resources/display/kravis")

    @BeforeEach
    fun beforeMethod() {
        // Only run tests that rely on for brandl or if users has exported KALASIM_RUN_DISPLAY_TESTS as 'true'
        // https://stackoverflow.com/questions/1689242/conditionally-ignoring-tests-in-junit-4
        val runDisplayTests =
            System.getProperty("KALASIM_RUN_DISPLAY_TESTS").toBoolean() || System.getProperty("user.name") == "brandl"
        assumeTrue(runDisplayTests)
    }

    @Test
    fun `it should display the mm1 server utilization`() {
        val mm1 = MM1Queue()

        mm1.run(50.minutes)
//        mm1.customers

        mm1.server.requesters.queueLengthTimeline
            .display("Trimmed queue timeline", mm1.startDate + 5.minutes, mm1.startDate + 10.minutes)
//            .showFile()
            .apply {
                assertExpected(this, "trimmed_queue_timeline")
            }


//        USE_KRAVIS_VIEWER = true

//        mm1.server.activities.display("MM1 Server Utilization")
//            .apply { assertExpected(this, "activities") }

        with(mm1.componentGenerator.history) {
            displayStateProportions("MM1 Server Utilization")
                .apply {
//                     workaround for https://github.com/Kotlin/kotlin-jupyter/issues/352
//                        kravis.SessionPrefs.OUTPUT_DEVICE = kravis.device.SwingPlottingDevice()
//                    show()
//                    Thread.sleep(10000)

                    assertExpected(this, "proportions")
                }

            displayStateTimeline("MM1 Server Utilization")
                .apply { assertExpected(this, "timeline") }
        }

        mm1.server.claimedTimeline.display("Claimed Server Capacity")
            .apply { assertExpected(this, "claimed") }


        val customerTimeline =
            mm1.componentGenerator.history.first().stateTimeline

        customerTimeline.display("Arrival State Timeline")
            .apply { assertExpected(this, "arrival_state") }

    }

    @Test
    fun `it should display the mm1 server utilization with walltime`() {

        val mm1 = MM1Queue()

        // redo but with set tick-transform
//        mm1.startDate = Instant.parse("2021-01-01T00:00:00.00Z")

        mm1.run(50.minutes)

//
//        mm1.server.activities.display("MM1 Server Utilization")
//            .apply { assertExpected(this, "activities") }
//
//        with(mm1.componentGenerator.history) {
//            displayStateTimeline("MM1 Server Utilization")
//                .apply { assertExpected(this, "timeline") }
//        }

        // todo bring back
//        mm1.server.claimedTimeline.display("Claimed Server Capacity")
//            .apply { assertExpected(this, "claimed") }

        val customerTimeline =
            mm1.componentGenerator.history.first().stateTimeline

        customerTimeline.display("Arrival State Timeline", forceTickAxis = true)
            .apply { assertExpected(this, "arrival_state_forced") }

        customerTimeline.display("Arrival State Timeline")
            .apply { assertExpected(this, "arrival_state") }
    }
}

abstract class AbstractSvgPlotRegression {

    lateinit var testName: String

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        testName = testInfo.displayName.replace(" ", "_").replace("[()]+".toRegex(), "")
    }


    abstract val testDataDir: File

    protected fun assertExpected(plot: GGPlot, subtest: String? = null) {
//        if(true) return
        val plotFile = plot.save(createTempFile(suffix = ".svg"))

//        assertTrue(plotFile.exists() && plotFile.fileSize() > 0)

        val svgDoc = plotFile.readText().run { prettyFormat(this) }.trim()
        //        val obtained = prettyFormat(svgDoc, 4).trim()

        @Suppress("USELESS_ELVIS")
        val methodName = testName ?: return // because we're running not in test mode

        val file = File(testDataDir, methodName + "${subtest?.let { ".$it" } ?: ""}.svg")
        if(!file.exists()) {
            file.writeText(svgDoc)
            fail("could not find expected result.")
        }

        // maybe https://stackoverflow.com/questions/8596161/json-string-tidy-formatter-for-java

        val expected = file.readText().trim() //.run { prettyFormat(this, 4) }

        // note assertEquals would be cleaner but since its printing the complete diff, it's polluting the travis logs
        assertEquals(expected, svgDoc)
        val failMsg = "svg mismatch got:\n${svgDoc.lines().take(30).joinToString("\n")}"
        assertTrue(expected == svgDoc, failMsg)

        // compare actual images
        //        saveImage(File(testDataDir, name.methodName.replace(" ", "_") + ".png"))
    }

    private fun prettyFormat(input: String, indent: Int = 4): String {
        try {
            val xmlInput = StreamSource(StringReader(input))
            val stringWriter = StringWriter()
            val xmlOutput = StreamResult(stringWriter)
            val transformerFactory = TransformerFactory.newInstance()
//            transformerFactory.setAttribute("{http://xml.apache.org/xslt}indent-amount", indent)
            val transformer = transformerFactory.newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indent.toString())
            transformer.transform(xmlInput, xmlOutput)
            return xmlOutput.writer.toString()
        } catch(e: Exception) {
            throw RuntimeException(e) // simple exception handling, please review it
        }

    }


    @BeforeEach
    fun setup() {
        // prevent these tests from running on github-CI
        // https://stackoverflow.com/questions/1689242/conditionally-ignoring-tests-in-junit-4
        // An assumption failure causes the test to be ignored.
        assumeTrue(canDisplay() && System.getenv("SKIP_DISPLAY_TESTS") == null)

//        SessionPrefs.RENDER_BACKEND = RserveEngine()
//        SessionPrefs.RENDER_BACKEND = Docker("holgerbrandl/kravis_core:3.5.1")
        SessionPrefs.RENDER_BACKEND = LocalR()
    }
}

