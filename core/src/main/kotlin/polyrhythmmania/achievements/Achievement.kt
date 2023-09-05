package polyrhythmmania.achievements

import paintbox.binding.IntVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.Localization
import polyrhythmmania.statistics.Stat


sealed class Achievement(
        val id: String,
        val rank: AchievementRank, val category: AchievementCategory,
        val isHidden: Boolean
) {

    open class Ordinary(id: String, rank: AchievementRank, category: AchievementCategory, isHidden: Boolean)
        : Achievement(id, rank, category, isHidden) {
        
        override fun toString(): String {
            return "Ordinary($id, $rank, $category, hidden=$isHidden)"
        }
    }
    
    class ScoreThreshold(id: String, rank: AchievementRank, category: AchievementCategory, isHidden: Boolean,
                         val scoreMinimum: Int)
        : Achievement(id, rank, category, isHidden) {
        
        override fun getLocalizedDesc(): ReadOnlyVar<String> {
            return Localization.getVar("achievement.desc.$id", Var { listOf(scoreMinimum) })
        }
        
        override fun toString(): String {
            return "ScoreThreshold($id, $rank, $category, hidden=$isHidden, scoreMin=$scoreMinimum)"
        }
    }
    
    class NumericalThreshold(id: String, rank: AchievementRank, category: AchievementCategory, isHidden: Boolean,
                             val minimumValue: Int)
        : Achievement(id, rank, category, isHidden) {
        
        override fun getLocalizedDesc(): ReadOnlyVar<String> {
            return Localization.getVar("achievement.desc.$id", Var { listOf(minimumValue) })
        }
        
        override fun toString(): String {
            return "NumericalThreshold($id, $rank, $category, hidden=$isHidden, minValue=$minimumValue)"
        }
    }
    
    class StatTriggered(id: String, rank: AchievementRank, category: AchievementCategory, isHidden: Boolean,
                        val stat: Stat, val threshold: Int, val showProgress: Boolean = true)
        : Achievement(id, rank, category, isHidden) {

        override fun getLocalizedDesc(): ReadOnlyVar<String> {
            return Localization.getVar("achievement.desc.$id", Var { listOf(stat.formatter.format(IntVar(threshold)).use()) })
        }
        
        override fun toString(): String {
            return "ScoreThreshold($id, $rank, $category, hidden=$isHidden, stat=${stat.id}, threshold=${threshold})"
        }
    }
    
    var overrideIconID: String? = null
    
    open fun getLocalizedName(): ReadOnlyVar<String> = Localization.getVar("achievement.name.$id")
    open fun getLocalizedDesc(): ReadOnlyVar<String> = Localization.getVar("achievement.desc.$id")
    
    fun getIconID(): String {
        return overrideIconID ?: category.iconID ?: rank.defaultIconID
    }
    
}