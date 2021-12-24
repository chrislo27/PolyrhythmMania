package polyrhythmmania.achievements

import com.badlogic.gdx.graphics.Color


enum class AchievementRank(val id: String, val color: Color) {
    
    STATISTICAL("statistical", Color.GOLD), 
    OBJECTIVE("objective", Color.GOLD), 
    CHALLENGE("challenge", Color.GOLD),
    ;
    
    fun toLocalizationID(hidden: Boolean): String = "achievement.toast.rank.$id${if (hidden) ".hidden" else ""}"
    
}