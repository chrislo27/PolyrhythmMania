package polyrhythmmania.discordrpc

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong


object DefaultPresences {
    
    object Idle
        : PresenceState("")

    object InEditor
        : PresenceState("Editing a level")

    object PlayingLevel
        : PresenceState("Playing a level")
    
    object PlayingPractice
        : PresenceState("Playing a practice mode")
    
    class PlayingEndlessMode
        : PresenceState("Playing Endless Mode") {
        override fun modifyRichPresence(richPresence: RichPresence) {
            super.modifyRichPresence(richPresence)
            richPresence.startTimestamp = System.currentTimeMillis() / 1000L
        }
    }
    
    class PlayingDailyChallenge(val localDate: LocalDate)
        : PresenceState("Playing the Daily Challenge (${localDate.format(DateTimeFormatter.ISO_DATE)})") {
        override fun modifyRichPresence(richPresence: RichPresence) {
            super.modifyRichPresence(richPresence)
            richPresence.startTimestamp = System.currentTimeMillis() / 1000L
        }
    }
    
    class PlayingDunk
        : PresenceState("Playing Polyrhythm: Dunk") {
        override fun modifyRichPresence(richPresence: RichPresence) {
            super.modifyRichPresence(richPresence)
            richPresence.startTimestamp = System.currentTimeMillis() / 1000L
        }
    }


    sealed class Elapsable(state: String, val duration: Float, smallIcon: String = "", smallIconText: String = state)
        : PresenceState(state, smallIcon, smallIconText) {
        override fun modifyRichPresence(richPresence: RichPresence) {
            super.modifyRichPresence(richPresence)
            if (duration > 0f) {
                richPresence.endTimestamp = System.currentTimeMillis() / 1000L + duration.roundToLong()
            }
        }

    }

}