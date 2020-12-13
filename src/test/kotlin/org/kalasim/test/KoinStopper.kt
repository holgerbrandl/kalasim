package org.kalasim.test

import kotlin.test.BeforeTest

@Deprecated("Not needed any longer as new Environment will auto-stop the current one (until separate koin contexts are implemented")
open class KoinStopper {


//    companion object {
//        private lateinit var env: Environment
//
//        @BeforeClass
//        @JvmStatic
//        fun setup() {
//            GlobalContext.stop()
//
//            env = Environment()
//        }
//    }


    @BeforeTest
    fun stopKoin() {
        org.koin.core.context.GlobalContext.stop()
    }


}
