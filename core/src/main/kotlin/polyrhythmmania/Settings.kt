package polyrhythmmania

import com.badlogic.gdx.Preferences
import paintbox.binding.Var
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_DETAILED_MARKER_UNDO
import polyrhythmmania.PreferenceKeys.SETTINGS_GAMEPLAY_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_MENU_MUSIC_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_MENU_SFX_VOLUME


@Suppress("PrivatePropertyName")
class Settings(val main: PRManiaGame, val prefs: Preferences) {

    data class KeyValue<T>(val key: String, val value: Var<T>)

    private val kv_gameplayVolume: KeyValue<Int> = KeyValue(SETTINGS_GAMEPLAY_VOLUME, Var(50))
    private val kv_menuMusicVolume: KeyValue<Int> = KeyValue(SETTINGS_MENU_MUSIC_VOLUME, Var(50))
    private val kv_menuSfxVolume: KeyValue<Int> = KeyValue(SETTINGS_MENU_SFX_VOLUME, Var(50))

    private val kv_editorDetailedMarkerUndo: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_DETAILED_MARKER_UNDO, Var(false))

    val gameplayVolume: Var<Int> = kv_gameplayVolume.value
    val menuMusicVolume: Var<Int> = kv_menuMusicVolume.value
    val menuSfxVolume: Var<Int> = kv_menuSfxVolume.value

    val editorDetailedMarkerUndo: Var<Boolean> = kv_editorDetailedMarkerUndo.value

    fun load() {
        val prefs = this.prefs
        prefs.getIntCoerceIn(kv_gameplayVolume, 0, 100)
        prefs.getIntCoerceIn(kv_menuMusicVolume, 0, 100)
        prefs.getIntCoerceIn(kv_menuSfxVolume, 0, 100)
        
        prefs.getBoolean(kv_editorDetailedMarkerUndo)
    }

    fun persist() {
        prefs
                .putInt(kv_gameplayVolume)
                .putInt(kv_menuMusicVolume)
                .putInt(kv_menuSfxVolume)
                
                .putBoolean(kv_editorDetailedMarkerUndo)

                .flush()
    }

    private fun Preferences.getInt(kv: KeyValue<Int>) {
        if (prefs.contains(kv.key)) {
            kv.value.set(prefs.getInteger(kv.key))
        }
    }

    private fun Preferences.getIntCoerceIn(kv: KeyValue<Int>, min: Int, max: Int) {
        if (prefs.contains(kv.key)) {
            kv.value.set(prefs.getInteger(kv.key).coerceIn(min, max))
        }
    }

    private fun Preferences.putInt(kv: KeyValue<Int>): Preferences {
        return prefs.putInteger(kv.key, kv.value.getOrCompute())
    }

    private fun Preferences.getBoolean(kv: KeyValue<Boolean>) {
        if (prefs.contains(kv.key)) {
            kv.value.set(prefs.getBoolean(kv.key))
        }
    }

    private fun Preferences.putBoolean(kv: KeyValue<Boolean>): Preferences {
        return prefs.putBoolean(kv.key, kv.value.getOrCompute())
    }
}