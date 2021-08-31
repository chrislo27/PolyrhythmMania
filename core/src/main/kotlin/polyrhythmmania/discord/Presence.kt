package polyrhythmmania.discord

import polyrhythmmania.PRMania


data class Presence(val details: String = "${PRMania.VERSION}",
                    val state: String = "",
                    val partySize: Int = 0, val partyMax: Int = 0,
                    val smallIcon: String = "", val smallIconText: String = state,
                    val largeIcon: String? = "square_logo", val largeIconText: String? = PRMania.GITHUB,
                    val startTimestamp: Long = DiscordCore.initTime / 1000, val endTimestamp: Long? = null)
