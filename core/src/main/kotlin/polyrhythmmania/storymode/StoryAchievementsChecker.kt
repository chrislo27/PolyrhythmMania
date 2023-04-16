package polyrhythmmania.storymode

import polyrhythmmania.achievements.Achievement
import polyrhythmmania.achievements.Achievements
import polyrhythmmania.storymode.inbox.InboxDB
import polyrhythmmania.storymode.inbox.InboxItemCompletion
import polyrhythmmania.storymode.inbox.InboxState


object StoryAchievementsChecker {
    
    private val achievementMapping: Map<String, Achievement> by lazy { 
        mapOf(
            InboxDB.ITEM_TO_TRIGGER_ACHIEVEMENT_JUNIOR_TECHNICIAN to Achievements.storyJuniorTechnician,
            InboxDB.ITEM_TO_TRIGGER_ACHIEVEMENT_INTERMEDIATE_TECHNICIAN to Achievements.storyIntermediateTechnician,
            InboxDB.ITEM_TO_TRIGGER_ACHIEVEMENT_DEFEATED_BOSS to Achievements.storyDefeatedBoss,
            InboxDB.ITEM_TO_TRIGGER_ACHIEVEMENT_COMPLETED_POSTGAME to Achievements.storyCompletedPostgame,
        )
    }
    
    fun check(inboxState: InboxState) {
        achievementMapping.forEach { (itemID, achievement) ->
            val itemState = inboxState.getItemState(itemID)
            if (itemState?.completion == InboxItemCompletion.COMPLETED) {
                Achievements.awardAchievement(achievement)
            }
        }
    }
    
}