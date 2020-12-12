package org.kalasim.test

import org.junit.Assert
import org.junit.Test
import org.kalasim.Component
import org.kalasim.misc.printThis
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternal
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.definition.BeanDefinition
import org.koin.core.instance.InstanceFactory
import org.koin.core.logger.Level
import org.koin.core.scope.Scope
import org.koin.dsl.module
import kotlin.reflect.KClass

class ComponentTests {

    @Test
    fun `it should create components outside of an environment`() {
        Component("foo").info.printThis()
    }
}

