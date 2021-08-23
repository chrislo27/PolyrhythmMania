package polyrhythmmania

import com.badlogic.gdx.Preferences
import com.eclipsesource.json.Json
import paintbox.binding.Var
import paintbox.util.WindowSize
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_ARROW_KEYS_LIKE_SCROLL
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_AUTOSAVE_INTERVAL
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_CAMERA_PAN_ON_DRAG_EDGE
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_DETAILED_MARKER_UNDO
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_HIGHER_ACCURACY_PREVIEW
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_MUSIC_WAVEFORM_OPACITY
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_PANNING_DURING_PLAYBACK
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_PLAYTEST_STARTS_PLAY
import polyrhythmmania.PreferenceKeys.ENDLESS_DAILY_CHALLENGE
import polyrhythmmania.PreferenceKeys.ENDLESS_DUNK_HIGHSCORE
import polyrhythmmania.PreferenceKeys.KEYMAP_KEYBOARD
import polyrhythmmania.PreferenceKeys.SETTINGS_DISCORD_RPC
import polyrhythmmania.PreferenceKeys.SETTINGS_FULLSCREEN
import polyrhythmmania.PreferenceKeys.SETTINGS_GAMEPLAY_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_MENU_MUSIC_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_MENU_SFX_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_MIXER
import polyrhythmmania.PreferenceKeys.SETTINGS_MUSIC_OFFSET_MS
import polyrhythmmania.PreferenceKeys.SETTINGS_SHOW_INPUT_FEEDBACK_BAR
import polyrhythmmania.PreferenceKeys.SETTINGS_SHOW_SKILL_STAR
import polyrhythmmania.PreferenceKeys.SETTINGS_WINDOWED_RESOLUTION
import polyrhythmmania.editor.CameraPanningSetting
import polyrhythmmania.editor.EditorSetting
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.sidemodes.endlessmode.DailyChallengeScore
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Suppress("PrivatePropertyName", "PropertyName")
class Settings(val main: PRManiaGame, val prefs: Preferences) {

    data class KeyValue<T>(val key: String, val value: Var<T>, val defaultValue: T) {
        constructor(key: String, defaultValue: T) : this(key, Var(defaultValue), defaultValue)
    }

    private val kv_gameplayVolume: KeyValue<Int> = KeyValue(SETTINGS_GAMEPLAY_VOLUME, 50)
    private val kv_menuMusicVolume: KeyValue<Int> = KeyValue(SETTINGS_MENU_MUSIC_VOLUME, 50)
    private val kv_menuSfxVolume: KeyValue<Int> = KeyValue(SETTINGS_MENU_SFX_VOLUME, 50)
    private val kv_windowedResolution: KeyValue<WindowSize> = KeyValue(SETTINGS_WINDOWED_RESOLUTION, PRMania.DEFAULT_SIZE)
    private val kv_fullscreen: KeyValue<Boolean> = KeyValue(SETTINGS_FULLSCREEN, false)
    private val kv_showInputFeedbackBar: KeyValue<Boolean> = KeyValue(SETTINGS_SHOW_INPUT_FEEDBACK_BAR, true)
    private val kv_showSkillStar: KeyValue<Boolean> = KeyValue(SETTINGS_SHOW_SKILL_STAR, true)
    private val kv_musicOffsetMs: KeyValue<Int> = KeyValue(SETTINGS_MUSIC_OFFSET_MS, 0)
    private val kv_discordRichPresence: KeyValue<Boolean> = KeyValue(SETTINGS_DISCORD_RPC, true)
    private val kv_mixer: KeyValue<String> = KeyValue(SETTINGS_MIXER, "")

    val kv_editorDetailedMarkerUndo: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_DETAILED_MARKER_UNDO, false)
    val kv_editorCameraPanOnDragEdge: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_CAMERA_PAN_ON_DRAG_EDGE, true)
    val kv_editorPanningDuringPlayback: KeyValue<CameraPanningSetting> = KeyValue(EDITORSETTINGS_PANNING_DURING_PLAYBACK, CameraPanningSetting.PAN)
    val kv_editorAutosaveInterval: KeyValue<Int> = KeyValue(EDITORSETTINGS_AUTOSAVE_INTERVAL, 5)
    val kv_editorMusicWaveformOpacity: KeyValue<Int> = KeyValue(EDITORSETTINGS_MUSIC_WAVEFORM_OPACITY, 10)
    val kv_editorHigherAccuracyPreview: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_HIGHER_ACCURACY_PREVIEW, true)
    val kv_editorPlaytestStartsPlay: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_PLAYTEST_STARTS_PLAY, true)
    val kv_editorArrowKeysLikeScroll: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_ARROW_KEYS_LIKE_SCROLL, false)
    
    private val kv_keymapKeyboard: KeyValue<InputKeymapKeyboard> = KeyValue(KEYMAP_KEYBOARD, InputKeymapKeyboard())
            
    private val kv_endlessDunkHighScore: KeyValue<Int> = KeyValue(ENDLESS_DUNK_HIGHSCORE, 0)
    private val kv_endlessDailyChallenge: KeyValue<DailyChallengeScore> = KeyValue(ENDLESS_DAILY_CHALLENGE, DailyChallengeScore.ZERO)

    val gameplayVolume: Var<Int> = kv_gameplayVolume.value
    val menuMusicVolume: Var<Int> = kv_menuMusicVolume.value
    val menuSfxVolume: Var<Int> = kv_menuSfxVolume.value
    val windowedResolution: Var<WindowSize> = kv_windowedResolution.value
    val fullscreen: Var<Boolean> = kv_fullscreen.value
    val showInputFeedbackBar: Var<Boolean> = kv_showInputFeedbackBar.value
    val showSkillStar: Var<Boolean> = kv_showSkillStar.value
    val musicOffsetMs: Var<Int> = kv_musicOffsetMs.value
    val discordRichPresence: Var<Boolean> = kv_discordRichPresence.value
    val mixer: Var<String> = kv_mixer.value

    val editorDetailedMarkerUndo: Var<Boolean> = kv_editorDetailedMarkerUndo.value
    val editorCameraPanOnDragEdge: Var<Boolean> = kv_editorCameraPanOnDragEdge.value
    val editorPanningDuringPlayback: Var<CameraPanningSetting> = kv_editorPanningDuringPlayback.value
    val editorAutosaveInterval: Var<Int> = kv_editorAutosaveInterval.value
    val editorMusicWaveformOpacity: Var<Int> = kv_editorMusicWaveformOpacity.value
    val editorHigherAccuracyPreview: Var<Boolean> = kv_editorHigherAccuracyPreview.value
    val editorPlaytestStartsPlay: Var<Boolean> = kv_editorPlaytestStartsPlay.value
    val editorArrowKeysLikeScroll: Var<Boolean> = kv_editorArrowKeysLikeScroll.value
    
    val inputKeymapKeyboard: Var<InputKeymapKeyboard> = kv_keymapKeyboard.value
    
    val endlessDunkHighScore: Var<Int> = kv_endlessDunkHighScore.value
    val endlessDailyChallenge: Var<DailyChallengeScore> = kv_endlessDailyChallenge.value

    @Suppress("UNCHECKED_CAST")
    fun load() {
        val prefs = this.prefs
        prefs.getIntCoerceIn(kv_gameplayVolume, 0, 100)
        prefs.getIntCoerceIn(kv_menuMusicVolume, 0, 100)
        prefs.getIntCoerceIn(kv_menuSfxVolume, 0, 100)
        prefs.getWindowSize(kv_windowedResolution)
        prefs.getBoolean(kv_fullscreen)
        prefs.getBoolean(kv_showInputFeedbackBar)
        prefs.getBoolean(kv_showSkillStar)
        prefs.getInt(kv_musicOffsetMs)
        prefs.getBoolean(kv_discordRichPresence)
        prefs.getString(kv_mixer, "")
        
        prefs.getBoolean(kv_editorDetailedMarkerUndo)
        prefs.getBoolean(kv_editorCameraPanOnDragEdge)
        prefs.getEditorSetting(kv_editorPanningDuringPlayback as KeyValue<EditorSetting>,
                CameraPanningSetting.MAP, CameraPanningSetting.PAN)
        prefs.getIntCoerceIn(kv_editorAutosaveInterval, 0, Short.MAX_VALUE.toInt())
        prefs.getIntCoerceIn(kv_editorMusicWaveformOpacity, 0, 10)
        prefs.getBoolean(kv_editorHigherAccuracyPreview)
        prefs.getBoolean(kv_editorPlaytestStartsPlay)
        prefs.getBoolean(kv_editorArrowKeysLikeScroll)
        
        prefs.getInputKeymapKeyboard(kv_keymapKeyboard)
        
        prefs.getInt(kv_endlessDunkHighScore)
        prefs.getDailyChallenge(kv_endlessDailyChallenge)
    }

    fun persist() {
        prefs
                .putInt(kv_gameplayVolume)
                .putInt(kv_menuMusicVolume)
                .putInt(kv_menuSfxVolume)
                .putWindowSize(kv_windowedResolution)
                .putBoolean(kv_fullscreen)
                .putBoolean(kv_showInputFeedbackBar)
                .putBoolean(kv_showSkillStar)
                .putInt(kv_musicOffsetMs)
                .putBoolean(kv_discordRichPresence)
                .putString(kv_mixer)

                .putBoolean(kv_editorDetailedMarkerUndo)
                .putBoolean(kv_editorCameraPanOnDragEdge)
                .putEditorSetting(kv_editorPanningDuringPlayback)
                .putInt(kv_editorAutosaveInterval)
                .putInt(kv_editorMusicWaveformOpacity)
                .putBoolean(kv_editorHigherAccuracyPreview)
                .putBoolean(kv_editorPlaytestStartsPlay)
                .putBoolean(kv_editorArrowKeysLikeScroll)

                .putInputKeymapKeyboard(kv_keymapKeyboard)

                .putInt(kv_endlessDunkHighScore)
                .putDailyChallenge(kv_endlessDailyChallenge)

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

    private fun Preferences.getString(kv: KeyValue<String>, defaultValue: String) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            kv.value.set(prefs.getString(kv.key, defaultValue))
        }
    }

    private fun Preferences.putString(kv: KeyValue<String>): Preferences {
        val prefs: Preferences = this
        return prefs.putString(kv.key, kv.value.getOrCompute())
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

    private fun Preferences.getEditorSetting(kv: KeyValue<EditorSetting>,
                                             map: Map<String, EditorSetting>, defaultValue: EditorSetting) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            kv.value.set(map[prefs.getString(kv.key, defaultValue.persistValueID)] ?: defaultValue)
        }
    }

    private fun Preferences.putEditorSetting(kv: KeyValue<out EditorSetting>): Preferences {
        val prefs: Preferences = this
        return prefs.putString(kv.key, kv.value.getOrCompute().persistValueID)
    }

    private fun Preferences.getLocalDate(kv: KeyValue<LocalDate>) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            val str = prefs.getString(kv.key)
            try {
                val localDate: LocalDate = LocalDate.parse(str, DateTimeFormatter.ISO_DATE)
                kv.value.set(localDate)
            } catch (ignored: Exception) {
                kv.value.set(LocalDate.MIN)
            }
        }
    }

    private fun Preferences.putLocalDate(kv: KeyValue<LocalDate>): Preferences {
        val prefs: Preferences = this
        return prefs.putString(kv.key, kv.value.getOrCompute().format(DateTimeFormatter.ISO_DATE))
    }

    private fun Preferences.getDailyChallenge(kv: KeyValue<DailyChallengeScore>) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            val str = prefs.getString(kv.key)
            try {
                val delimited = str.split(';')
                val scoreStr = delimited[0]
                val dateStr = delimited[1]
                val localDate: LocalDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE)
                kv.value.set(DailyChallengeScore(localDate, scoreStr.toInt()))
            } catch (ignored: Exception) {
                kv.value.set(DailyChallengeScore.ZERO)
            }
        }
    }

    private fun Preferences.putDailyChallenge(kv: KeyValue<DailyChallengeScore>): Preferences {
        val prefs: Preferences = this
        val pair = kv.value.getOrCompute()
        return prefs.putString(kv.key, "${pair.score};${pair.date.format(DateTimeFormatter.ISO_DATE)}")
    }
}