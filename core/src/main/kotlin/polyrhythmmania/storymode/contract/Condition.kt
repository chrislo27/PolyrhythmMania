package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.StoryL10N


sealed class Condition(val text: ReadOnlyVar<String>, val priority: Int) : Comparable<Condition> {
    
    data class Lives(val maxLives: Int)
        : Condition(StoryL10N.getVar("desktop.pane.conditions.secondary.lives${if (maxLives == 1) ".one" else ""}", listOf(maxLives)), 1)

    sealed class InputRestriction(text: ReadOnlyVar<String>) : Condition(text, 2) {
        object AcesOnly : InputRestriction(StoryL10N.getVar("desktop.pane.conditions.secondary.inputRestriction.acesOnly"))
        object NoBarelies : InputRestriction(StoryL10N.getVar("desktop.pane.conditions.secondary.inputRestriction.noBarelies"))
    }
    
    object MonsterGoal : Condition(StoryL10N.getVar("desktop.pane.conditions.secondary.monsterGoal"), 3)
    
    data class DefectiveRods(val maxLives: Int)
        : Condition(StoryL10N.getVar("desktop.pane.conditions.secondary.defectiveRods", listOf(maxLives)), 4)
    
    data class TempoUp(val tempoChangePercent: Int)
        : Condition(StoryL10N.getVar(if (tempoChangePercent >= 100) "desktop.pane.conditions.secondary.tempoUp" else "desktop.pane.conditions.secondary.tempoDown"), 5)
    

    override fun compareTo(other: Condition): Int = this.priority.compareTo(other.priority)
}
