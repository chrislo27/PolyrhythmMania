package polyrhythmmania.achievements


enum class AchievementCategory(val id: String, val iconID: String) {

    GENERAL("general", "trophy2"),
    ENDLESS_MODE("endlessMode", "trophy2"),
    DAILY("daily", "trophy2"),
    EDITOR("editor", "trophy2"),
    EXTRAS("extras", "trophy2"),
    ;
    
    companion object {
        val VALUES: List<AchievementCategory> = values().toList()
    }
    
    fun toLocalizationID(): String = "achievement.category.$id"
    
}