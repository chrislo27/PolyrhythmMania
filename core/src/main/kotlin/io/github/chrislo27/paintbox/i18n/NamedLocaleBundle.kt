package io.github.chrislo27.paintbox.i18n

import com.badlogic.gdx.utils.I18NBundle
import io.github.chrislo27.paintbox.Paintbox
import java.util.*


data class NamedLocaleBundle(val locale: NamedLocale, val bundle: I18NBundle) {

    val missingKeys: MutableSet<String> = mutableSetOf()
    
    fun getValue(key: String): String {
        if (checkMissing(key)) return key
        return bundle[key]
    }

    fun getValue(key: String, vararg args: Any?): String {
        if (checkMissing(key)) return key
        return bundle.format(key, *args)
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