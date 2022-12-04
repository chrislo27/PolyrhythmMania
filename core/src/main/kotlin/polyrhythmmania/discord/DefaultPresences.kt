package polyrhythmmania.discord

import java.time.LocalDate
import java.time.format.DateTimeFormatter


object DefaultPresences {
    
    fun idle(): PresenceData = PresenceData(state = "")
    fun inEditor(): PresenceData = PresenceData(state = "Editing a project", smallIcon = "editor")
    fun playingLevel(): PresenceData = PresenceData(state = "Playing a level")
    fun playingPractice(): PresenceData = PresenceData(state = "Playing a practice mode", smallIcon = "practice")
    fun playingEndlessMode(): PresenceData = PresenceData(state = "Playing Endless Mode", startTimestamp = System.currentTimeMillis() / 1000L, smallIcon = "endless")
    fun playingDailyChallenge(date: LocalDate): PresenceData {
        val dateStr = date.format(DateTimeFormatter.ISO_DATE)
        return PresenceData(state = "Playing the Daily Challenge ($dateStr)", smallIconText = dateStr, startTimestamp = System.currentTimeMillis() / 1000L, smallIcon = "daily")
    }
    fun playingDunk(): PresenceData = PresenceData(state = "Playing Polyrhythm: Dunk", startTimestamp = System.currentTimeMillis() / 1000L, smallIcon = "dunk")
    fun playingAssemble(): PresenceData = PresenceData(state = "Playing Polyrhythm: Assemble", smallIcon = "assemble")
    fun playingSolitaire(): PresenceData = PresenceData(state = "Playing Built to Scale: Solitaire", startTimestamp = System.currentTimeMillis() / 1000L, smallIcon = "solitaire")
    fun playingStoryMode(): PresenceData = PresenceData(state = "Playing Story Mode", startTimestamp = System.currentTimeMillis() / 1000L, smallIcon = "story_mode") // TODO add to rich presence assets

}