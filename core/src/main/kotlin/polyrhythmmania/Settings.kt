package polyrhythmmania

import com.badlogic.gdx.Preferences
import com.eclipsesource.json.Json
import paintbox.binding.ReadOnlyVar
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
import polyrhythmmania.PreferenceKeys.ENDLESS_HIGH_SCORE
import polyrhythmmania.PreferenceKeys.KEYMAP_KEYBOARD
import polyrhythmmania.PreferenceKeys.SETTINGS_CALIBRATION_AUDIO_OFFSET_MS
import polyrhythmmania.PreferenceKeys.SETTINGS_DISCORD_RPC
import polyrhythmmania.PreferenceKeys.SETTINGS_FULLSCREEN
import polyrhythmmania.PreferenceKeys.SETTINGS_GAMEPLAY_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_LOCALE
import polyrhythmmania.PreferenceKeys.SETTINGS_MAINMENU_FLIP_ANIMATION
import polyrhythmmania.PreferenceKeys.SETTINGS_MENU_MUSIC_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_MENU_SFX_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_MIXER
import polyrhythmmania.PreferenceKeys.SETTINGS_SHOW_INPUT_FEEDBACK_BAR
import polyrhythmmania.PreferenceKeys.SETTINGS_SHOW_SKILL_STAR
import polyrhythmmania.PreferenceKeys.SETTINGS_WINDOWED_RESOLUTION
import polyrhythmmania.editor.CameraPanningSetting
import polyrhythmmania.editor.EditorSetting
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.sidemodes.endlessmode.DailyChallengeScore
import polyrhythmmania.sidemodes.endlessmode.EndlessHighScore
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Suppress("PrivatePropertyName", "PropertyName")
class Settings(val main: PRManiaGame, val prefs: Preferences) {

    data class KeyValue<T>(val key: String, val value: Var<T>, val defaultValue: T) {
        constructor(key: String, defaultValue: T) : this(key, Var(defaultValue), defaultValue)
    }

    private val kv_locale: KeyValue<String> = KeyValue(SETTINGS_LOCALE, "")
    private val kv_gameplayVolume: KeyValue<Int> = KeyValue(SETTINGS_GAMEPLAY_VOLUME, 50)
    private val kv_menuMusicVolume: KeyValue<Int> = KeyValue(SETTINGS_MENU_MUSIC_VOLUME, 50)
    private val kv_menuSfxVolume: KeyValue<Int> = KeyValue(SETTINGS_MENU_SFX_VOLUME, 50)
    private val kv_windowedResolution: KeyValue<WindowSize> = KeyValue(SETTINGS_WINDOWED_RESOLUTION, PRMania.DEFAULT_SIZE)
    private val kv_fullscreen: KeyValue<Boolean> = KeyValue(SETTINGS_FULLSCREEN, false)
    private val kv_showInputFeedbackBar: KeyValue<Boolean> = KeyValue(SETTINGS_SHOW_INPUT_FEEDBACK_BAR, true)
    private val kv_showSkillStar: KeyValue<Boolean> = KeyValue(SETTINGS_SHOW_SKILL_STAR, true)
    private val kv_audioOffsetMs: KeyValue<Int> = KeyValue(SETTINGS_CALIBRATION_AUDIO_OFFSET_MS, 0)
    private val kv_discordRichPresence: KeyValue<Boolean> = KeyValue(SETTINGS_DISCORD_RPC, true)
    private val kv_mixer: KeyValue<String> = KeyValue(SETTINGS_MIXER, "")
    private val kv_mainMenuFlipAnimations: KeyValue<Boolean> = KeyValue(SETTINGS_MAINMENU_FLIP_ANIMATION, true)

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
    private val kv_endlessHighScore: KeyValue<EndlessHighScore> = KeyValue(ENDLESS_HIGH_SCORE, EndlessHighScore.ZERO)

    val locale: Var<String> = kv_locale.value
    val gameplayVolume: Var<Int> = kv_gameplayVolume.value
    val menuMusicVolume: Var<Int> = kv_menuMusicVolume.value
    val menuSfxVolume: Var<Int> = kv_menuSfxVolume.value
    val windowedResolution: Var<WindowSize> = kv_windowedResolution.value
    val fullscreen: Var<Boolean> = kv_fullscreen.value
    val showInputFeedbackBar: Var<Boolean> = kv_showInputFeedbackBar.value
    val showSkillStar: Var<Boolean> = kv_showSkillStar.value
    val calibrationAudioOffsetMs: Var<Int> = kv_audioOffsetMs.value
    val discordRichPresence: Var<Boolean> = kv_discordRichPresence.value
    val mixer: Var<String> = kv_mixer.value
    val mainMenuFlipAnimation: Var<Boolean> = kv_mainMenuFlipAnimations.value

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
    val endlessHighScore: Var<EndlessHighScore> = kv_endlessHighScore.value
    
    val inputCalibration: ReadOnlyVar<InputCalibration> = Var.bind { 
        InputCalibration(calibrationAudioOffsetMs.use().toFloat())
    }

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
        prefs.getIntCoerceIn(kv_audioOffsetMs, -500, 500)
        prefs.getBoolean(kv_discordRichPresence)
        prefs.getString(kv_mixer, "")
        prefs.getBoolean(kv_mainMenuFlipAnimations)
        prefs.getString(kv_locale, "")
        
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
        prefs.getEndlessHighScore(kv_endlessHighScore)
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
                .putInt(kv_audioOffsetMs)
                .putBoolean(kv_discordRichPresence)
                .putString(kv_mixer)
                .putBoolean(kv_mainMenuFlipAnimations)
                .putString(kv_locale)

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
                .putEndlessHighScore(kv_endlessHighScore)

                .flush()
    }

    private fun Preferences.getInt(kv: KeyValue<Int>) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            kv.value.set(prefs.getInteger(kv.key, kv.defaultValue))
        }
    }

    private fun Preferences.getIntCoerceIn(kv: KeyValue<Int>, min: Int, max: Int) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            kv.value.set(prefs.getInteger(kv.key, kv.defaultValue).coerceIn(min, max))
        }
    }

    private fun Preferences.putInt(kv: KeyValue<Int>): Preferences {
        val prefs: Preferences = this
        return prefs.putInteger(kv.key, kv.value.getOrCompute())
    }

    private fun Preferences.getBoolean(kv: KeyValue<Boolean>) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            kv.value.set(prefs.getBoolean(kv.key, kv.defaultValue))
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
    
    private fun Preferences.getEndlessHighScore(kv: KeyValue<EndlessHighScore>) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            val str = prefs.getString(kv.key)
            try {
                val delimited = str.split(';')
                val scoreStr = delimited[0]
                val seedStr = delimited[1]
                val seed: UInt = seedStr.toUInt(16)
                kv.value.set(EndlessHighScore(seed, scoreStr.toInt()))
            } catch (ignored: Exception) {
                kv.value.set(EndlessHighScore.ZERO)
            }
        }
    }

    private fun Preferences.putEndlessHighScore(kv: KeyValue<EndlessHighScore>): Preferences {
        val prefs: Preferences = this
        val pair = kv.value.getOrCompute()
        return prefs.putString(kv.key, "${pair.score};${pair.seed.toString(16)}")
    }
}