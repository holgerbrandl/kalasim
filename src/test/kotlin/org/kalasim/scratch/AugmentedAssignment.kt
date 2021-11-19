package org.kalasim.scratch

import kotlin.properties.Delegates


//object IntMonitoringExamples {
//    @JvmStatic
//    fun main() {
//
//        //        https://stackoverflow.com/questions/54313839/how-to-set-up-a-listener-for-a-variable-in-kotlin
//
//        val env = Environment()
//
//        val barMonitor = MetricTimeline()
//
//        var bar: Int by Delegates.observable(0) { property, oldValue, newValue ->
//            println("New Value $newValue")
//            println("Old Value $oldValue")
//            barMonitor.addValue(newValue)
//        }
//
//        bar++
//
//        barMonitor.printHistogram()
//
//    }
//
//}

// Tracked as https://youtrack.jetbrains.com/issue/KT-43912
//object SimplifiedIntMonitor {
//
//    @JvmStatic
//    fun main(args: Array<String>) {
//
//        //        https://stackoverflow.com/questions/54313839/how-to-set-up-a-listener-for-a-variable-in-kotlin
//        class Monitor {
//            var myValue: Int = 0
//        }
//
//        val timeline = Monitor()
//
//        var bar: Int by Delegates.observable(0) { property, oldValue, newValue ->
//            println("New Value $newValue")
//            println("Old Value $oldValue")
//            timeline.myValue = newValue
//        }
//
//        bar++
//
//        println(timeline.myValue)
//
//    }
//}

object SimplfiedIntMonitorWithWrapperClass {

    @JvmStatic
    fun main(args: Array<String>) {
        class Monitor {
            var myValue: Int = 0
        }

        class Foo {
            //        https://stackoverflow.com/questions/54313839/how-to-set-up-a-listener-for-a-variable-in-kotlin

            val timeline = Monitor()

            var bar: Int by Delegates.observable(0) { property, oldValue, newValue ->
                println("New Value $newValue")
                println("Old Value $oldValue")
                timeline.myValue = newValue
            }


        }

        val foo = Foo()
        foo.bar++

        println(foo.bar)

    }
}
