package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.StoryL10N


sealed class Condition(val text: ReadOnlyVar<String>, val priority: Int) : Comparable<Condition> {
    
    data class Lives(val maxLives: Int)
        : Condition(StoryL10N.getVar("desktop.pane.conditions.secondary.lives${if (maxLives == 1) ".one" else ""}", listOf(maxLives)), 1)
    
    object MonsterGoal : Condition(StoryL10N.getVar("desktop.pane.conditions.secondary.monsterGoal"), 2)
    
    data class DefectiveRods(val maxLives: Int)
        : Condition(StoryL10N.getVar("desktop.pane.conditions.secondary.defectiveRods", listOf(maxLives)), 3)
    
    data class TempoUp(val tempoChangePercent: Float)
        : Condition(StoryL10N.getVar(if (tempoChangePercent >= 1f) "desktop.pane.conditions.secondary.tempoUp" else "desktop.pane.conditions.secondary.tempoDown"), 4)

    override fun compareTo(other: Condition): Int = this.priority.compareTo(other.priority)
}
