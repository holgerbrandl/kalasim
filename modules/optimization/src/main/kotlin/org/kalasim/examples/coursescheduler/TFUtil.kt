package org.kalasim.examples.coursescheduler

import ai.timefold.solver.core.api.domain.variable.VariableListener
import ai.timefold.solver.core.api.score.director.ScoreDirector

open class BasicVariableListener<S, E>() : VariableListener<S, E> {
    override fun beforeEntityAdded(scoreDirector: ScoreDirector<S>, entity: E) {}
    override fun afterEntityAdded(scoreDirector: ScoreDirector<S>, entity: E) {}
    override fun beforeEntityRemoved(scoreDirector: ScoreDirector<S>, entity: E) {}
    override fun afterEntityRemoved(scoreDirector: ScoreDirector<S>, entity: E) {}
    override fun beforeVariableChanged(scoreDirector: ScoreDirector<S>, entity: E) {}
    override fun afterVariableChanged(scoreDirector: ScoreDirector<S>, entity: E) {}
}


fun <T, Solution_> ScoreDirector<Solution_>.change(name: String, task: T, block: T.(T) -> Unit) {
    beforeVariableChanged(task, name)
    task.block(task)
    afterVariableChanged(task, name)
}
