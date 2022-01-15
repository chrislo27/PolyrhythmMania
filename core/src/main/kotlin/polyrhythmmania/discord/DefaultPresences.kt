package polyrhythmmania.discord

import java.time.LocalDate
import java.time.format.DateTimeFormatter


object DefaultPresences {
    
    fun idle(): PresenceData = PresenceData(state = "")
    fun inEditor(): PresenceData = PresenceData(state = "Editing a level")
    fun playingLevel(): PresenceData = PresenceData(state = "Playing a level")
    fun playingPractice(): PresenceData = PresenceData(state = "Playing a practice mode")
    fun playingEndlessMode(): PresenceData = PresenceData(state = "Playing Endless Mode", startTimestamp = System.currentTimeMillis() / 1000L)
    fun playingDailyChallenge(date: LocalDate): PresenceData = PresenceData(state = "Playing the Daily Challenge (${date.format(DateTimeFormatter.ISO_DATE)})", startTimestamp = System.currentTimeMillis() / 1000L)
    fun playingDunk(): PresenceData = PresenceData(state = "Playing Polyrhythm: Dunk", startTimestamp = System.currentTimeMillis() / 1000L)
    fun playingAssemble(): PresenceData = PresenceData(state = "Playing Polyrhythm: Assemble")
    fun playingSolitaire(): PresenceData = PresenceData(state = "Playing Built to Scale: Solitaire", startTimestamp = System.currentTimeMillis() / 1000L, smallIcon = "solitaire")

}