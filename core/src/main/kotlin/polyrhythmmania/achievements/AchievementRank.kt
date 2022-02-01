package polyrhythmmania.achievements

import com.badlogic.gdx.graphics.Color


enum class AchievementRank(val id: String, val color: Color, val defaultIconID: String) {
    
    STATISTICAL("statistical", Color.valueOf("1BCC12"), "trophy1"), 
    OBJECTIVE("objective", Color.valueOf("497AFF"), "trophy2"), 
    CHALLENGE("challenge", Color.valueOf("FF0800"), "trophy2"),
    ;
    
    fun toAchievementLocalizationID(hidden: Boolean): String = "achievement.toast.rank.$id${if (hidden) ".hidden" else ""}"
    fun toNameLocalizationID(): String = "rank.$id"
    
}