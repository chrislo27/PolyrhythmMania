package polyrhythmmania.achievements

import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.statistics.Stat


sealed class Achievement(
        val id: String,
        val quality: AchievementQuality, val category: AchievementCategory,
        val isHidden: Boolean
) {

    class Ordinary(id: String, quality: AchievementQuality, category: AchievementCategory, isHidden: Boolean)
        : Achievement(id, quality, category, isHidden)
    
    class ScoreThreshold(id: String, quality: AchievementQuality, category: AchievementCategory, isHidden: Boolean,
                         val scoreMinimum: Int)
        : Achievement(id, quality, category, isHidden) {
        
        override fun getLocalizedDesc(): ReadOnlyVar<String> {
            return AchievementsL10N.getVar("achievement.desc.$id", Var { listOf(scoreMinimum) })
        }
    }
    
    class StatTriggered(id: String, quality: AchievementQuality, category: AchievementCategory, isHidden: Boolean,
                        val stat: Stat, val threshold: Int)
        : Achievement(id, quality, category, isHidden)
    
    open fun getLocalizedName(): ReadOnlyVar<String> = AchievementsL10N.getVar("achievement.name.$id")
    open fun getLocalizedDesc(): ReadOnlyVar<String> = AchievementsL10N.getVar("achievement.desc.$id")
    
}