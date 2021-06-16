package polyrhythmmania

import com.badlogic.gdx.Preferences
import com.eclipsesource.json.Json
import paintbox.binding.Var
import paintbox.util.WindowSize
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_CAMERA_PAN_ON_DRAG_EDGE
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_DETAILED_MARKER_UNDO
import polyrhythmmania.PreferenceKeys.KEYMAP_KEYBOARD
import polyrhythmmania.PreferenceKeys.SETTINGS_FULLSCREEN
import polyrhythmmania.PreferenceKeys.SETTINGS_GAMEPLAY_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_MENU_MUSIC_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_MENU_SFX_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_WINDOWED_RESOLUTION
import polyrhythmmania.engine.input.InputKeymapKeyboard


@Suppress("PrivatePropertyName")
class Settings(val main: PRManiaGame, val prefs: Preferences) {

    data class KeyValue<T>(val key: String, val value: Var<T>)

    private val kv_gameplayVolume: KeyValue<Int> = KeyValue(SETTINGS_GAMEPLAY_VOLUME, Var(50))
    private val kv_menuMusicVolume: KeyValue<Int> = KeyValue(SETTINGS_MENU_MUSIC_VOLUME, Var(50))
    private val kv_menuSfxVolume: KeyValue<Int> = KeyValue(SETTINGS_MENU_SFX_VOLUME, Var(50))
    private val kv_windowedResolution: KeyValue<WindowSize> = KeyValue(SETTINGS_WINDOWED_RESOLUTION, Var(PRMania.DEFAULT_SIZE))
    private val kv_fullscreen: KeyValue<Boolean> = KeyValue(SETTINGS_FULLSCREEN, Var(false))

    private val kv_editorDetailedMarkerUndo: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_DETAILED_MARKER_UNDO, Var(false))
    private val kv_editorCameraPanOnDragEdge: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_CAMERA_PAN_ON_DRAG_EDGE, Var(true))
    
    private val kv_keymapKeyboard: KeyValue<InputKeymapKeyboard> = KeyValue(KEYMAP_KEYBOARD, Var(InputKeymapKeyboard()))

    val gameplayVolume: Var<Int> = kv_gameplayVolume.value
    val menuMusicVolume: Var<Int> = kv_menuMusicVolume.value
    val menuSfxVolume: Var<Int> = kv_menuSfxVolume.value
    val windowedResolution: Var<WindowSize> = kv_windowedResolution.value
    val fullscreen: Var<Boolean> = kv_fullscreen.value

    val editorDetailedMarkerUndo: Var<Boolean> = kv_editorDetailedMarkerUndo.value
    val editorCameraPanOnDragEdge: Var<Boolean> = kv_editorCameraPanOnDragEdge.value
    
    val inputKeymapKeyboard: Var<InputKeymapKeyboard> = kv_keymapKeyboard.value

    fun load() {
        val prefs = this.prefs
        prefs.getIntCoerceIn(kv_gameplayVolume, 0, 100)
        prefs.getIntCoerceIn(kv_menuMusicVolume, 0, 100)
        prefs.getIntCoerceIn(kv_menuSfxVolume, 0, 100)
        prefs.getWindowSize(kv_windowedResolution)
        prefs.getBoolean(kv_fullscreen)
        
        prefs.getBoolean(kv_editorDetailedMarkerUndo)
        prefs.getBoolean(kv_editorCameraPanOnDragEdge)
        
        prefs.getInputKeymapKeyboard(kv_keymapKeyboard)
    }

    fun persist() {
        prefs
                .putInt(kv_gameplayVolume)
                .putInt(kv_menuMusicVolume)
                .putInt(kv_menuSfxVolume)
                .putWindowSize(kv_windowedResolution)
                .putBoolean(kv_fullscreen)
                
                .putBoolean(kv_editorDetailedMarkerUndo)
                .putBoolean(kv_editorCameraPanOnDragEdge)
                
                .putInputKeymapKeyboard(kv_keymapKeyboard)

                .flush()
    }

    private fun Preferences.getInt(kv: KeyValue<Int>) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            kv.value.set(prefs.getInteger(kv.key))
        }
    }

    private fun Preferences.getIntCoerceIn(kv: KeyValue<Int>, min: Int, max: Int) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            kv.value.set(prefs.getInteger(kv.key).coerceIn(min, max))
        }
    }

    private fun Preferences.putInt(kv: KeyValue<Int>): Preferences {
        val prefs: Preferences = this
        return prefs.putInteger(kv.key, kv.value.getOrCompute())
    }

    private fun Preferences.getBoolean(kv: KeyValue<Boolean>) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            kv.value.set(prefs.getBoolean(kv.key))
        }
    }

    private fun Preferences.putBoolean(kv: KeyValue<Boolean>): Preferences {
        val prefs: Preferences = this
        return prefs.putBoolean(kv.key, kv.value.getOrCompute())
    }

    private fun Preferences.getWindowSize(kv: KeyValue<WindowSize>) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            val str = prefs.getString(kv.key)
            try {
                val width = str.substringBefore('x').toInt()
                val height = str.substringAfter('x').toInt()
                kv.value.set(WindowSize(width, height))
            } catch (ignored: Exception) {
                kv.value.set(PRMania.DEFAULT_SIZE)
            }
        }
    }

    private fun Preferences.putWindowSize(kv: KeyValue<WindowSize>): Preferences {
        val prefs: Preferences = this
        val windowSize = kv.value.getOrCompute()
        return prefs.putString(kv.key, "${windowSize.width}x${windowSize.height}")
    }

    private fun Preferences.getInputKeymapKeyboard(kv: KeyValue<InputKeymapKeyboard>) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            try {
                val json = Json.parse(prefs.getString(kv.key, ""))
                kv.value.set(InputKeymapKeyboard.fromJson(json.asObject()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun Preferences.putInputKeymapKeyboard(kv: KeyValue<InputKeymapKeyboard>): Preferences {
        val prefs: Preferences = this
        return prefs.putString(kv.key, kv.value.getOrCompute().toJson().toString())
    }
}