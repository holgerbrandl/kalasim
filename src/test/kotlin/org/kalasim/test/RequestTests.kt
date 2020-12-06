package org.kalasim.test

import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.junit.Ignore
import org.junit.Test
import org.kalasim.*

class RequestTests {

    @Test
    fun `preemptive resources should bump claims`(){
        // see version 19.0.9  2019-10-08 in salabim change log for code snippets

        /**
        if the component has to start all over again (hold(1)) if it is bumped:
        def process(self):
        prio = sim.Pdf((1,2,3), 1)
        while True:
        yield self.request((preemptive_resource, 1, prio)
        yield self.hold(1)
        if self.isclaiming(preemptive_resource):
        break
        self.release(preemptive_resource)
         */


        createSimulation {
            val resource = Resource(preemptive = true)

            val prioPDF =  EnumeratedDistribution(listOf(1,2,3).map{ it to 1.0/3.0}.asCM())

            object : Component(){

                override suspend fun ProcContext.process() {
                    while(true){
                       yield(request(resource withQuantity 1 andPriority prioPDF.sample()))
                        yield(hold(1))
                        if(!isClaiming(resource)){
                            break
                        }else{
                            release(resource)
                        }
                    }
                }
            }
        }
    }
}


