package org.kalasim.test

import io.kotest.assertions.fail
import junit.framework.Assert
import krangl.irisData
import kravis.*
import kravis.nshelper.plot
import kravis.render.LocalR
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.kalasim.ComponentGenerator
import org.kalasim.demo.MM1Queue
import org.kalasim.plot.kravis.canDisplay
import org.kalasim.plot.kravis.display
import org.koin.core.component.get
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class DisplayTests : AbstractSvgPlotRegression() {

    override val testDataDir: File
        get() = File("src/test/resources/display/kravis")


    @Test
    fun `is should display the mm1 server utilization`() {
        // todo use more exciting standard model here with more dynamics
        val mm1 = MM1Queue()

        mm1.run(100)
//        mm1.customers

        mm1.server.activities.display("MM1 Server Utilization").apply { assertExpected(this) }

        mm1.("MM1 Server Utilization").apply { assertExpected(this) }
    }
}

abstract class AbstractSvgPlotRegression {

    @Rule
    @JvmField
    val name = TestName()


    abstract val testDataDir: File

    protected fun assertExpected(plot: GGPlot, subtest: String? = null) {
        val plotFile = plot.save(createTempFile(suffix = ".svg"))

        Assert.assertTrue(plotFile.exists() && plotFile.length() > 0)

        val svgDoc = plotFile.readText().run { prettyFormat(this, 4) }.trim()
        //        val obtained = prettyFormat(svgDoc, 4).trim()

        val methodName = name.methodName

        if (methodName == null) return // because we're running not in test mode

        val file = File(testDataDir, methodName.replace(" ", "_") + "${subtest?.let { "." + it } ?: ""}.svg")
        if (!file.exists()) {
            file.writeText(svgDoc)
            fail("could not find expected result.")
        }

        // maybe https://stackoverflow.com/questions/8596161/json-string-tidy-formatter-for-java

        val expected = file.readText().trim() //.run { prettyFormat(this, 4) }

        // note assertEquals would be cleaner but since its printing the complete diff, it's polluting the travis logs
        //        assertEquals(expected, svgDoc)
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
        Assume.assumeTrue(canDisplay())

//        SessionPrefs.RENDER_BACKEND = RserveEngine()
//        SessionPrefs.RENDER_BACKEND = Docker("holgerbrandl/kravis_core:3.5.1")
        SessionPrefs.RENDER_BACKEND = LocalR()
    }
}

