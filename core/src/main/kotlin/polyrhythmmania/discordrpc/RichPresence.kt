package polyrhythmmania.discordrpc

import club.minnced.discord.rpc.DiscordRichPresence
import polyrhythmmania.PRMania


class RichPresence(state: String = "",
                   party: Pair<Int, Int> = DEFAULT_PARTY,
                   smallIcon: String = "",
                   smallIconText: String = state,
                   largeIcon: String? = null,
                   largeIconText: String? = null)
    : DiscordRichPresence() {

    companion object {
        val DEFAULT_PARTY: Pair<Int, Int> = 0 to 0
    }

    constructor(presenceState: PresenceState)
            : this(presenceState.state, presenceState.getPartyCount(), presenceState.smallIcon, presenceState.smallIconText,
            presenceState.largeIcon, presenceState.largeIconText) {
        presenceState.modifyRichPresence(this)
    }

    init {
        details = "${PRMania.VERSION}"
        startTimestamp = DiscordHelper.initTime / 1000L // Epoch seconds
        largeImageKey = largeIcon ?: DiscordHelper.DEFAULT_LARGE_IMAGE
        largeImageText = largeIconText ?: PRMania.GITHUB
        smallImageKey = smallIcon
        smallImageText = smallIconText
        this.state = state
        if (party.first > 0 && party.second > 0) {
            partySize = party.first
            partyMax = party.second
        }
    }

}