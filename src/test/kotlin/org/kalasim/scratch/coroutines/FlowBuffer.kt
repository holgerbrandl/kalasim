package org.kalasim.scratch.coroutines

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

object BufferTakeAll {
    fun dressMaker(): Flow<Int> = flow {
        for(i in 1..3) {
            println("Dress $i in the making")
            delay(100)
            println("Dress $i ready for sale")
            emit(i)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        dressMaker()
            .buffer()
            .collect { value ->
                println("Dress $value bought for use")
                delay(300)
                println("Dress $value completely used")
            }
    }
}


object ConflateThrifty {
    fun dressMaker(): Flow<Int> = flow {
        for(i in 1..3) {
            println("Dress $i in the making")
            delay(100)
            println("Dress $i ready for sale")
            emit(i)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) =
        runBlocking {
            dressMaker()
                .conflate()
                .collect { value ->
                    println("Dress $value bought for use")
                    delay(300)
                    println("Dress $value completely used")
                }
        }

}


object CollectLatest {
    fun dressMaker(): Flow<Int> = flow {
        for(i in 1..3) {
            println("Dress $i in the making")
            delay(100)
            println("Dress $i ready for sale")
            emit(i)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) =
        runBlocking {
            dressMaker()
                .collectLatest { value ->
                    println("Dress $value bought for use")
                    delay(300)
                    println("Dress $value completely used")
                }
        }
}