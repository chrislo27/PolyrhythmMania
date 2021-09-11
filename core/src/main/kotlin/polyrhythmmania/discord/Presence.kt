package polyrhythmmania.discord


data class Presence(val details: String = "" /*"${PRMania.VERSION}"*/,
                    val state: String = "",
                    val partySize: Int = 0, val partyMax: Int = 0, val partyID: String? = null,
                    val smallIcon: String = "", val smallIconText: String = state,
                    val largeIcon: String = "square_logo", val largeIconText: String = "" /*PRMania.GITHUB*/,
                    val startTimestamp: Long = 0L /*DiscordCore.initTime / 1000*/, val endTimestamp: Long? = null,
                    val joinSecret: String? = null, val spectateSecret: String? = null, val matchSecret: String? = null)