package polyrhythmmania.discord

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import de.jcm.discordgamesdk.Core
import paintbox.Paintbox
import polyrhythmmania.util.TempFileUtils
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


object DiscordSDKNatives {
    
    private val firstTime: AtomicBoolean = AtomicBoolean(true)
    private val hasCoreBeenInited: AtomicBoolean = AtomicBoolean(false)
    
    @Synchronized
    fun getNativeLoc(): File? {
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
                Paintbox.LOGGER.warn("[DiscordSDKNatives] Cannot determine a supported OS, got $osName.")
                return null
            }
        }

        val nativeName = "discord_game_sdk.$suffix"
        val nativePath = "discord-gamesdk/$arch/$nativeName"
        val nativeFh = Gdx.files.internal(nativePath)
        if (!nativeFh.exists()) {
            Paintbox.LOGGER.warn("[DiscordSDKNatives] Cannot find the discord-gamesdk library \"$nativePath\" in game files.")
            return null
        }
        
        // Copy to temp location
        val tmpFolder = TempFileUtils.TEMP_FOLDER.resolve("discord-gamesdk/")
        tmpFolder.mkdirs()
        val tmpNativeFile = tmpFolder.resolve(nativeName)
        if (firstTime.get()) {
            firstTime.set(false)
            tmpNativeFile.delete() // Delete old one on first call
        }
        
        if (!tmpNativeFile.exists()) {
            try {
                nativeFh.copyTo(FileHandle(tmpNativeFile))
            } catch (e: Exception) {
                Paintbox.LOGGER.warn("[DiscordSDKNatives] Cannot copy the discord-gamesdk library \"$nativeName\" to \"${tmpNativeFile.absolutePath}\".")
                e.printStackTrace()
                return null
            }
        }
        
        return tmpNativeFile
    }
    
    @Synchronized
    fun initCore(force: Boolean = false): Boolean {
        if (hasCoreBeenInited.get() && !force) return false
        
        val nativeLoc = getNativeLoc()
        return if (nativeLoc != null && nativeLoc.exists()) {
            Core.init(nativeLoc)
            hasCoreBeenInited.set(true)
            true
        } else {
            false
        }
    }
}