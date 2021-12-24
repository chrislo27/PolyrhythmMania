package polyrhythmmania.achievements

import com.badlogic.gdx.graphics.Color


enum class AchievementRank(val id: String, val color: Color) {
    
    STATISTICAL("statistical", Color.valueOf("1BCC12")), 
    OBJECTIVE("objective", Color.valueOf("497AFF")), 
    CHALLENGE("challenge", Color.valueOf("FF0800")),
    ;
    
    fun toLocalizationID(hidden: Boolean): String = "achievement.toast.rank.$id${if (hidden) ".hidden" else ""}"
    
}