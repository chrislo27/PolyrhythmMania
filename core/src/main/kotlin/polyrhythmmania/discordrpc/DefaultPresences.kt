package polyrhythmmania.discordrpc

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
    
    object PlayingDunk
        : PresenceState("Playing Polyrhythm DUNK!!")


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