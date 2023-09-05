package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.Localization


sealed class Condition(val text: ReadOnlyVar<String>, val priority: Int) : Comparable<Condition> {

    data class Lives(val maxLives: Int) : Condition(
        Localization.getVar(
            "desktop.pane.conditions.secondary.lives${if (maxLives == 1) ".one" else ""}",
            listOf(maxLives)
        ), 1
    )

    sealed class InputRestriction(text: ReadOnlyVar<String>) : Condition(text, 2) {
        data object AcesOnly : InputRestriction(Localization.getVar("desktop.pane.conditions.secondary.inputRestriction.acesOnly"))
        data object NoBarelies : InputRestriction(Localization.getVar("desktop.pane.conditions.secondary.inputRestriction.noBarelies"))
    }

    data object MonsterGoal : Condition(Localization.getVar("desktop.pane.conditions.secondary.monsterGoal"), 3)

    data class DefectiveRods(val maxLives: Int) : Condition(
        Localization.getVar(
            "desktop.pane.conditions.secondary.defectiveRods",
            listOf(maxLives)
        ), 4
    )

    data class TempoUp(val tempoChangePercent: Int) : Condition(
        Localization.getVar(
            if (tempoChangePercent >= 100)
                "desktop.pane.conditions.secondary.tempoUp"
            else "desktop.pane.conditions.secondary.tempoDown"
        ), 5
    )


    override fun compareTo(other: Condition): Int = this.priority.compareTo(other.priority)
}
