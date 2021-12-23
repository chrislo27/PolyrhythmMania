package polyrhythmmania.achievements

import polyrhythmmania.statistics.Stat


sealed class Achievement(val id: String, val quality: AchievementQuality, val isHidden: Boolean) {
    
    class Normal(id: String, quality: AchievementQuality, isHidden: Boolean)
        : Achievement(id, quality, isHidden)
    
    class StatTriggered(id: String, quality: AchievementQuality, isHidden: Boolean,
                        val stat: Stat, val threshold: Int)
        : Achievement(id, quality, isHidden)
    
}