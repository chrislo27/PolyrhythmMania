package polyrhythmmania.achievements


enum class AchievementCategory(val id: String) {

    GENERAL("general"),
    ENDLESS_MODE("endlessMode"),
    DAILY("daily"),
    EDITOR("editor"),
    EXTRAS("extras"),
    ;
    
    companion object {
        val VALUES: List<AchievementCategory> = values().toList()
    }
    
    fun toLocalizationID(): String = "achievement.category.$id"
    
}