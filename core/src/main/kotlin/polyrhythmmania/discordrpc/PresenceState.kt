package polyrhythmmania.discordrpc



/**
 * Data object of rich presence information. Default objects are in [DefaultPresences].
 */
sealed class PresenceState(open val state: String = "", open val smallIcon: String = "", open val smallIconText: String = state,
                           open val largeIcon: String? = null, open val largeIconText: String? = null) {

    open fun getPartyCount(): Pair<Int, Int> = RichPresence.DEFAULT_PARTY

    open fun modifyRichPresence(richPresence: RichPresence) {
    }

}
