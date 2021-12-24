package polyrhythmmania.achievements

import paintbox.binding.IntVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.Localization
import polyrhythmmania.statistics.Stat


sealed class Achievement(
        val id: String,
        val quality: AchievementQuality, val category: AchievementCategory,
        val isHidden: Boolean
) {

    class Ordinary(id: String, quality: AchievementQuality, category: AchievementCategory, isHidden: Boolean)
        : Achievement(id, quality, category, isHidden) {
        
        override fun toString(): String {
            return "Ordinary($id, $quality, $category, hidden=$isHidden)"
        }
    }
    
    class ScoreThreshold(id: String, quality: AchievementQuality, category: AchievementCategory, isHidden: Boolean,
                         val scoreMinimum: Int)
        : Achievement(id, quality, category, isHidden) {
        
        override fun getLocalizedDesc(): ReadOnlyVar<String> {
            return AchievementsL10N.getVar("achievement.desc.$id", Var { listOf(scoreMinimum) })
        }
        
        override fun toString(): String {
            return "ScoreThreshold($id, $quality, $category, hidden=$isHidden, scoreMin=$scoreMinimum)"
        }
    }
    
    class NumericalThreshold(id: String, quality: AchievementQuality, category: AchievementCategory, isHidden: Boolean,
                         val minimumValue: Int)
        : Achievement(id, quality, category, isHidden) {
        
        override fun getLocalizedDesc(): ReadOnlyVar<String> {
            return AchievementsL10N.getVar("achievement.desc.$id", Var { listOf(minimumValue) })
        }
        
        override fun toString(): String {
            return "NumericalThreshold($id, $quality, $category, hidden=$isHidden, minValue=$minimumValue)"
        }
    }
    
    class StatTriggered(id: String, quality: AchievementQuality, category: AchievementCategory, isHidden: Boolean,
                        val stat: Stat, val threshold: Int, val showProgress: Boolean = true)
        : Achievement(id, quality, category, isHidden) {

        override fun getLocalizedDesc(): ReadOnlyVar<String> {
            return AchievementsL10N.getVar("achievement.desc.$id", Var { listOf(stat.formatter.format(IntVar(threshold)).use()) })
        }
        
        override fun toString(): String {
            return "ScoreThreshold($id, $quality, $category, hidden=$isHidden, stat=${stat.id}, threshold=${threshold})"
        }
    }
    
    open fun getLocalizedName(): ReadOnlyVar<String> = AchievementsL10N.getVar("achievement.name.$id")
    open fun getLocalizedDesc(): ReadOnlyVar<String> = AchievementsL10N.getVar("achievement.desc.$id")
    
}