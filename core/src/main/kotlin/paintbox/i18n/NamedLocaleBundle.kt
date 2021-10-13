package paintbox.i18n

import com.badlogic.gdx.utils.I18NBundle
import paintbox.Paintbox
import java.util.*


data class NamedLocaleBundle(val locale: NamedLocale, val bundle: I18NBundle) {

    /**
     * Keys with missing information.
     */
    val missingKeys: MutableSet<String> = mutableSetOf()

    /**
     * Keys with [IllegalArgumentException]s due to bad formatting.
     * Future IAEs are suppressed.
     */
    val caughtIAEs: MutableSet<String> = mutableSetOf()
    
    fun getValue(key: String): String {
        if (checkMissing(key)) return key
        return bundle[key]
    }

    fun getValue(key: String, vararg args: Any?): String {
        if (checkMissing(key)) return key
        return try {
            bundle.format(key, *args)
        } catch (iae: IllegalArgumentException) {
            if (key !in caughtIAEs) {
                caughtIAEs += key
                Paintbox.LOGGER.error("IllegalArgumentException thrown when calling getValue on key $key (args [${args.toList()}]). Future IAEs will be suppressed.")
                iae.printStackTrace()
            }
            key
        }
    }
    
    fun checkMissing(key: String): Boolean {
        if (key in missingKeys) return true
        try {
            bundle[key]
        } catch (e: MissingResourceException) {
            missingKeys += key
            Paintbox.LOGGER.warn("Missing content for I18N key $key in bundle $locale")
            return true
        }
        return false
    }
    
}