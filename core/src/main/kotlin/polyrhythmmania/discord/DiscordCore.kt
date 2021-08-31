package polyrhythmmania.discord

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable
import de.jcm.discordgamesdk.Core
import de.jcm.discordgamesdk.CreateParams
import de.jcm.discordgamesdk.Result
import de.jcm.discordgamesdk.activity.Activity
import de.jcm.discordgamesdk.user.DiscordUser
import paintbox.Paintbox
import paintbox.binding.Var
import polyrhythmmania.util.TempFileUtils
import java.time.Instant
import java.util.*


object DiscordCore : Disposable {
    
    const val DISCORD_APP_ID: Long = 869413093665558589L
    
    var loaded: Boolean = false
        private set
    
    val initTime: Long = System.currentTimeMillis()
    
    val enableRichPresence: Var<Boolean> = Var(true)
    private lateinit var core: Core
    
    init {
        synchronized(this) {
            attemptLoadCore()
        }
    }
    
    private fun isLoaded(): Boolean = this.loaded
    
    private fun attemptLoadCore() {
        // Unpack the gamesdk libraries
        val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
        var arch = System.getProperty("os.arch").lowercase(Locale.ROOT)
        if (arch == "amd64") {
            arch = "x86_64"
        }
        
        val suffix: String = when {
            "windows" in osName -> "dll"
            "linux" in osName -> "so"
//            "mac os" in osName || "macos" in osName -> "dylib"
            else -> {
                Paintbox.LOGGER.warn("[DiscordCore] Cannot determine a supported OS, got $osName. Discord functionality disabled.")
                return
            }
        }
        
        val nativeName = "discord_game_sdk.$suffix"
        val nativePath = "discord-gamesdk/$arch/$nativeName"
        val nativeFh = Gdx.files.internal(nativePath)
        if (!nativeFh.exists()) {
            Paintbox.LOGGER.warn("[DiscordCore] Cannot find the discord-gamesdk library \"$nativePath\". Discord functionality disabled.")
            return
        }
        
        // Copy to temp location
        val tmpFolder = TempFileUtils.TEMP_FOLDER.resolve("discord-gamesdk/")
        tmpFolder.mkdirs()
        val tmpNativeFile = tmpFolder.resolve(nativeName)
        tmpNativeFile.delete()
        try {
            nativeFh.copyTo(FileHandle(tmpNativeFile))
        } catch (e: Exception) {
            Paintbox.LOGGER.warn("[DiscordCore] Cannot copy the discord-gamesdk library \"$nativeName\" to \"${tmpNativeFile.absolutePath}\". Discord functionality disabled.")
            e.printStackTrace()
            return
        }

        try {
            Core.init(tmpNativeFile)
            
            val createParams = CreateParams().apply { 
                this.clientID = DISCORD_APP_ID
                this.setFlags(CreateParams.Flags.NO_REQUIRE_DISCORD)
            }
            this.core = Core(createParams)
            
            this.loaded = true
            Paintbox.LOGGER.info("[DiscordCore] Loaded successfully.")
        } catch (t: Throwable) {
            Paintbox.LOGGER.warn("[DiscordCore] Could not init Core (native: \"${tmpNativeFile.absolutePath}\"). Discord functionality disabled.")
            t.printStackTrace()
            return
        }
    }
    
    fun runCallbacks() {
        if (!isLoaded()) return
        try {
            this.core.runCallbacks() // May throw exception if Discord is closed?
        } catch (e: Exception) {
            this.loaded = false
            Paintbox.LOGGER.warn("[DiscordCore] Error while running callbacks. Discord functionality has been disabled.")
            e.printStackTrace()
        }
    }
    
    fun updateActivity(presence: Presence) {
        if (!isLoaded()) return
        try {
            Activity().use { activity ->
                activity.details = presence.details
                activity.state = presence.state
                activity.timestamps().start = Instant.ofEpochSecond(presence.startTimestamp)
                if (presence.endTimestamp != null) {
                    activity.timestamps().end = Instant.ofEpochSecond(presence.endTimestamp)
                }
                activity.party().size().currentSize = presence.partySize
                activity.party().size().maxSize = presence.partyMax
                activity.assets().largeImage = presence.largeIcon
                activity.assets().largeText = presence.largeIconText
                activity.assets().smallImage = presence.smallIcon
                activity.assets().smallText = presence.smallIconText


                this.core.activityManager().updateActivity(activity) { result ->
                    if (result != Result.OK) {
                        Paintbox.LOGGER.warn("[DiscordCore] Non-OK result when updating activity: $result")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getCurrentUser(): DiscordUser? {
        if (!isLoaded()) return null
        return try {
            this.core.userManager().currentUser
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun dispose() {
        if (!isLoaded()) return
        this.loaded = false
        
        try {
            // TODO kill off Activity.java non-daemon thread until https://github.com/JnCrMx/discord-game-sdk4j/issues/33 is resolved
            val field = Activity::class.java.getDeclaredField("QUEUE_THREAD")
            field.isAccessible = true
            val thread: Thread = field.get(null) as Thread
            thread.stop()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        
        this.core.close()
    }
}