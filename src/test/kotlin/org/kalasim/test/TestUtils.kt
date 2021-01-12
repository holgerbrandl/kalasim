package org.kalasim.test

import java.io.ByteArrayOutputStream
import java.io.PrintStream


// this class is simply copied from https://github.com/holgerbrandl/krangl

internal data class CapturedOutput(val stdout: String, val stderr: String)

internal fun captureOutput(expr: () -> Any): CapturedOutput {
    val origOut = System.out
    val origErr = System.err
    // https://stackoverflow.com/questions/216894/get-an-outputstream-into-a-string

    val baosOut = ByteArrayOutputStream()
    val baosErr = ByteArrayOutputStream()

    System.setOut(PrintStream(baosOut));
    System.setErr(PrintStream(baosErr));


    // run the expression
    expr()

    val stdout = String(baosOut.toByteArray()).trim().replace(System.lineSeparator() , "\n")
    val stderr = String(baosErr.toByteArray()).trim().replace(System.lineSeparator() , "\n")

    System.setOut(origOut)
    System.setErr(origErr)

    return CapturedOutput(stdout, stderr)
}

// since the test reference dat is typically provided as multi-line which is using \n by design, we adjust the
// out-err stream results accordingly here to become platform independent.
// https://stackoverflow.com/questions/48933641/kotlin-add-carriage-return-into-multiline-string
//internal fun String.trimAndReline() = trimIndent().replace("\n", System.getProperty("line.separator"))

