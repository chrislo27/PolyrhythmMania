package polyrhythmmania.discord

import de.jcm.discordgamesdk.activity.Activity
import polyrhythmmania.PRMania
import java.time.Instant


data class PresenceData(
        val details: String = "${PRMania.VERSION}",
        val state: String = "",
        val partySize: Int = 0, val partyMax: Int = 0, val partyID: String? = null,
        val smallIcon: String = "", val smallIconText: String = state,
        val largeIcon: String = "square_logo", val largeIconText: String = PRMania.GITHUB,
        val startTimestamp: Long = DiscordRichPresence.initTime / 1000, val endTimestamp: Long? = null,
        val joinSecret: String? = null, val spectateSecret: String? = null, val matchSecret: String? = null
) {

    fun mutateActivity(activity: Activity) {
        val presence = this
        
        activity.details = presence.details
        activity.state = presence.state
        if (presence.startTimestamp > 0) {
            activity.timestamps().start = Instant.ofEpochSecond(presence.startTimestamp)
        }
        if (presence.endTimestamp != null) {
            activity.timestamps().end = Instant.ofEpochSecond(presence.endTimestamp)
        }
        activity.party().size().currentSize = presence.partySize
        activity.party().size().maxSize = presence.partyMax
        if (presence.partyID != null) {
            activity.party().id = presence.partyID
        }
        if (presence.joinSecret != null) {
            activity.secrets().joinSecret = presence.joinSecret
        }
        if (presence.spectateSecret != null) {
            activity.secrets().spectateSecret = presence.spectateSecret
        }
        if (presence.matchSecret != null) {
            activity.secrets().matchSecret = presence.matchSecret
        }
        activity.assets().largeImage = presence.largeIcon
        activity.assets().largeText = presence.largeIconText
        activity.assets().smallImage = presence.smallIcon
        activity.assets().smallText = presence.smallIconText
    }
    
}