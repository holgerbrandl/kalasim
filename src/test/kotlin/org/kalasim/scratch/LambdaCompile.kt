// see https://youtrack.jetbrains.com/issue/KT-44631

//class Something{
//    fun now() = 3.0
//}
//
//
//fun createSmthg(
//
//    builder: Something.() -> Unit
//): Something = Something().apply(builder)
//
//object EventLog2 {
//
//    @JvmStatic
//    fun main(args: Array<String>) {
//        val sim = createSmthg {
//            class MyEvent(msg: String, time: Double = now())
//
//            MyEvent("something magical happened", now())
//        }
//    }
//}
