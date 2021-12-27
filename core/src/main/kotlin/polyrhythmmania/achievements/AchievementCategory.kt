package polyrhythmmania.achievements


enum class AchievementCategory(val id: String, val iconID: String?) {

    GENERAL("general", null),
    ENDLESS_MODE("endlessMode", "endless"),
    DAILY("daily", "daily"),
    EDITOR("editor", null),
    EXTRAS("extras", null),
    ;
    
    companion object {
        val VALUES: List<AchievementCategory> = values().toList()
    }
    
    fun toLocalizationID(): String = "achievement.category.$id"
    
}