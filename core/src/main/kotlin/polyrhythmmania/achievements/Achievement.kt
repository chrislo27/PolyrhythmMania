package polyrhythmmania.achievements

import polyrhythmmania.statistics.Stat


sealed class Achievement(
        val id: String,
        val quality: AchievementQuality, val category: AchievementCategory,
        val isHidden: Boolean
) {

    class Normal(id: String, quality: AchievementQuality, category: AchievementCategory, isHidden: Boolean)
        : Achievement(id, quality, category, isHidden)
    
    class StatTriggered(id: String, quality: AchievementQuality, category: AchievementCategory, isHidden: Boolean,
                        val stat: Stat, val threshold: Int)
        : Achievement(id, quality, category, isHidden)
    
}