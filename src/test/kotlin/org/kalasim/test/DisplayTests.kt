package org.kalasim.test

import io.kotest.assertions.fail
import kotlinx.datetime.Instant
import kravis.GGPlot
import kravis.SessionPrefs
import kravis.render.LocalR
import org.junit.*
import org.junit.rules.TestName
import org.kalasim.examples.MM1Queue
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.plot.kravis.*
import java.io.*
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import kotlin.io.path.*

class DisplayTests : AbstractSvgPlotRegression() {

    override val testDataDir: File
        get() = File("src/test/resources/display/kravis")

    @Before
    fun beforeMethod() {
        // Only run tests that rely on for brandl or if users has exported KALASIM_RUN_DISPLAY_TESTS as 'true'
        // https://stackoverflow.com/questions/1689242/conditionally-ignoring-tests-in-junit-4
        val runDisplaytests =
            System.getProperty("KALASIM_RUN_DISPLAY_TESTS").toBoolean() || System.getProperty("user.name") == "brandl"
        Assume.assumeTrue(runDisplaytests);
    }

    @Test
    fun `is should display the mm1 server utilization`() {
        // todo use more exciting standard model here with more dynamics
        val mm1 = MM1Queue()

        mm1.run(50)
//        mm1.customers

//        USE_KRAVIS_VIEWER = true

        mm1.server.activities.display("MM1 Server Utilization")
            .apply { assertExpected(this, "activities") }

        with(mm1.componentGenerator.history) {
            displayStateProportions("MM1 Server Utilization")
                .apply { assertExpected(this, "proportions") }

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

    @OptIn(AmbiguousDuration::class)
    @Test
    fun `is should display the mm1 server utilization with walltime`() {

        val mm1 = MM1Queue()

        // redo but with set tick-transform
        mm1.startDate = Instant.parse("2021-01-01T00:00:00.00Z")

        mm1.run(50)


        mm1.server.activities.display("MM1 Server Utilization")
            .apply { assertExpected(this, "activities") }

        with(mm1.componentGenerator.history) {
            displayStateTimeline("MM1 Server Utilization")
                .apply { assertExpected(this, "timeline") }
        }

        mm1.server.claimedTimeline.display("Claimed Server Capacity")
            .apply { assertExpected(this, "claimed") }

        val customerTimeline =
            mm1.componentGenerator.history.first().stateTimeline

        customerTimeline.display("Arrival State Timeline", forceTickAxis = true)
            .apply { assertExpected(this, "arrival_state_forced") }

        customerTimeline.display("Arrival State Timeline")
            .apply { assertExpected(this, "arrival_state") }
    }
}

abstract class AbstractSvgPlotRegression {

    @Rule
    @JvmField
    val name = TestName()


    abstract val testDataDir: File

    protected fun assertExpected(plot: GGPlot, subtest: String? = null) {
//        if(true) return
        val plotFile = plot.save(createTempFile(suffix = ".svg"))

        Assert.assertTrue(plotFile.exists() && plotFile.fileSize() > 0)

        val svgDoc = plotFile.readText().run { prettyFormat(this, 4) }.trim()
        //        val obtained = prettyFormat(svgDoc, 4).trim()

        val methodName = name.methodName ?: return // because we're running not in test mode

        val file = File(testDataDir, methodName.replace(" ", "_") + "${subtest?.let { ".$it" } ?: ""}.svg")
        if (!file.exists()) {
            file.writeText(svgDoc)
            fail("could not find expected result.")
        }

        // maybe https://stackoverflow.com/questions/8596161/json-string-tidy-formatter-for-java

        val expected = file.readText().trim() //.run { prettyFormat(this, 4) }

        // note assertEquals would be cleaner but since its printing the complete diff, it's polluting the travis logs
//                assertEquals(expected, svgDoc)
        val failMsg = "svg mismatch got:\n${svgDoc.lines().take(30).joinToString("\n")}"
        Assert.assertTrue(failMsg, expected.equals(svgDoc))

        // compare actual images
        //        saveImage(File(testDataDir, name.methodName.replace(" ", "_") + ".png"))
    }

    private fun prettyFormat(input: String, indent: Int): String {
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
        } catch (e: Exception) {
            throw RuntimeException(e) // simple exception handling, please review it
        }

    }


    @Before
    fun setup() {
        // prevent these tests from running on github-CI
        // https://stackoverflow.com/questions/1689242/conditionally-ignoring-tests-in-junit-4
        // An assumption failure causes the test to be ignored.
        Assume.assumeTrue(canDisplay() && System.getenv("SKIP_DISPLAY_TESTS") == null)

//        SessionPrefs.RENDER_BACKEND = RserveEngine()
//        SessionPrefs.RENDER_BACKEND = Docker("holgerbrandl/kravis_core:3.5.1")
        SessionPrefs.RENDER_BACKEND = LocalR()
    }
}

