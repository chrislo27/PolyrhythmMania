package polyrhythmmania

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.eclipsesource.json.Json
import paintbox.Paintbox
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.util.MonitorInfo
import paintbox.util.Version
import paintbox.util.WindowSize
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_ARROW_KEYS_LIKE_SCROLL
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_AUTOSAVE_INTERVAL
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_CAMERA_PAN_ON_DRAG_EDGE
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_DETAILED_MARKER_UNDO
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_HIGHER_ACCURACY_PREVIEW
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_MUSIC_WAVEFORM_OPACITY
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_PANNING_DURING_PLAYBACK
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_PLAYTEST_STARTS_PLAY
import polyrhythmmania.PreferenceKeys.EDITORSETTINGS_UI_SCALE
import polyrhythmmania.PreferenceKeys.ENDLESS_DAILY_CHALLENGE
import polyrhythmmania.PreferenceKeys.ENDLESS_DAILY_CHALLENGE_STREAK
import polyrhythmmania.PreferenceKeys.ENDLESS_DUNK_HIGHSCORE
import polyrhythmmania.PreferenceKeys.ENDLESS_HIGH_SCORE
import polyrhythmmania.PreferenceKeys.KEYMAP_KEYBOARD
import polyrhythmmania.PreferenceKeys.LAST_UPDATE_NOTES
import polyrhythmmania.PreferenceKeys.LAST_VERSION
import polyrhythmmania.PreferenceKeys.NEW_INDICATOR_EDITORHELP_EXPORTING
import polyrhythmmania.PreferenceKeys.NEW_INDICATOR_EDITORHELP_PRMPROJ
import polyrhythmmania.PreferenceKeys.NEW_INDICATOR_EDITORHELP_SPOTLIGHTS
import polyrhythmmania.PreferenceKeys.NEW_INDICATOR_EDITORHELP_TEXPACK
import polyrhythmmania.PreferenceKeys.NEW_INDICATOR_ENDLESS_MODE_HELP
import polyrhythmmania.PreferenceKeys.NEW_INDICATOR_EXTRAS_ASM
import polyrhythmmania.PreferenceKeys.NEW_INDICATOR_EXTRAS_SOLITAIRE
import polyrhythmmania.PreferenceKeys.NEW_INDICATOR_LIBRARY
import polyrhythmmania.PreferenceKeys.NEW_INDICATOR_STORY_MODE
import polyrhythmmania.PreferenceKeys.SETTINGS_ACHIEVEMENT_NOTIFICATIONS
import polyrhythmmania.PreferenceKeys.SETTINGS_AUDIODEVICE_BUFFER_COUNT
import polyrhythmmania.PreferenceKeys.SETTINGS_CALIBRATION_AUDIO_OFFSET_MS
import polyrhythmmania.PreferenceKeys.SETTINGS_CALIBRATION_DISABLE_INPUT_SFX
import polyrhythmmania.PreferenceKeys.SETTINGS_DISCORD_RPC
import polyrhythmmania.PreferenceKeys.SETTINGS_FORCE_TEXTURE_PACK
import polyrhythmmania.PreferenceKeys.SETTINGS_FORCE_TILESET_PALETTE
import polyrhythmmania.PreferenceKeys.SETTINGS_FULLSCREEN
import polyrhythmmania.PreferenceKeys.SETTINGS_FULLSCREEN_MONITOR
import polyrhythmmania.PreferenceKeys.SETTINGS_GAMEPLAY_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_LOCALE
import polyrhythmmania.PreferenceKeys.SETTINGS_MAINMENU_FLIP_ANIMATION
import polyrhythmmania.PreferenceKeys.SETTINGS_MASTER_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_MAX_FPS
import polyrhythmmania.PreferenceKeys.SETTINGS_MENU_MUSIC_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_MENU_SFX_VOLUME
import polyrhythmmania.PreferenceKeys.SETTINGS_MIXER
import polyrhythmmania.PreferenceKeys.SETTINGS_ONLY_DEFAULT_PALETTE_OLD
import polyrhythmmania.PreferenceKeys.SETTINGS_SHOW_INPUT_FEEDBACK_BAR
import polyrhythmmania.PreferenceKeys.SETTINGS_SHOW_SKILL_STAR
import polyrhythmmania.PreferenceKeys.SETTINGS_USE_LEGACY_SOUND
import polyrhythmmania.PreferenceKeys.SETTINGS_VSYNC
import polyrhythmmania.PreferenceKeys.SETTINGS_WINDOWED_RESOLUTION
import polyrhythmmania.PreferenceKeys.SIDEMODE_ASSEMBLE_NORMAL
import polyrhythmmania.PreferenceKeys.SIDEMODE_SOLITAIRE_GAME_MUSIC
import polyrhythmmania.PreferenceKeys.SIDEMODE_SOLITAIRE_GAME_SFX
import polyrhythmmania.editor.CameraPanningSetting
import polyrhythmmania.editor.EditorSetting
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.gamemodes.endlessmode.DailyChallengeScore
import polyrhythmmania.gamemodes.endlessmode.EndlessHighScore
import polyrhythmmania.soundsystem.AudioDeviceSettings
import polyrhythmmania.soundsystem.javasound.MixerHandler
import polyrhythmmania.world.render.ForceTexturePack
import polyrhythmmania.world.render.ForceTilesetPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt


@Suppress("PrivatePropertyName", "PropertyName")
class Settings(val main: PRManiaGame, val prefs: Preferences) { // Note: this predates PaintboxPreferences
    
    companion object {
        private fun determineMaxRefreshRate(): Int {
            return try {
                Gdx.graphics.displayMode.refreshRate.coerceAtLeast(24)
            } catch (e: Exception) {
                Paintbox.LOGGER.warn("Failed to detect refresh rate for current display mode")
                e.printStackTrace()
                60
            }
        }
    }

    data class KeyValue<T>(val key: String, val value: Var<T>, val defaultValue: T) {
        constructor(key: String, defaultValue: T) : this(key, Var(defaultValue), defaultValue)
    }
    
    class NewIndicator(val key: String, val newAsOf: Version, val newEvenIfFirstPlay: Boolean) {
        val value: BooleanVar = BooleanVar(true)
    }
    
    var lastVersion: Version? = null
        private set

    private val kv_locale: KeyValue<String> = KeyValue(SETTINGS_LOCALE, "")
    private val kv_masterVolumeSetting: KeyValue<Int> = KeyValue(SETTINGS_MASTER_VOLUME, 100)
    private val kv_gameplayVolumeSetting: KeyValue<Int> = KeyValue(SETTINGS_GAMEPLAY_VOLUME, 50)
    private val kv_menuMusicVolumeSetting: KeyValue<Int> = KeyValue(SETTINGS_MENU_MUSIC_VOLUME, 50)
    private val kv_menuSfxVolumeSetting: KeyValue<Int> = KeyValue(SETTINGS_MENU_SFX_VOLUME, 50)
    private val kv_windowedResolution: KeyValue<WindowSize> = KeyValue(SETTINGS_WINDOWED_RESOLUTION, PRMania.DEFAULT_SIZE)
    private val kv_fullscreen: KeyValue<Boolean> = KeyValue(SETTINGS_FULLSCREEN, true)
    private val kv_fullscreenMonitor: KeyValue<MonitorInfo?> = KeyValue(SETTINGS_FULLSCREEN_MONITOR, null)
    private val kv_showInputFeedbackBar: KeyValue<Boolean> = KeyValue(SETTINGS_SHOW_INPUT_FEEDBACK_BAR, true)
    private val kv_showSkillStar: KeyValue<Boolean> = KeyValue(SETTINGS_SHOW_SKILL_STAR, true)
    private val kv_discordRichPresence: KeyValue<Boolean> = KeyValue(SETTINGS_DISCORD_RPC, true)
    // Note: Not private due to being used by AdvAudioMenu
    val kv_audioDeviceBufferCount: KeyValue<Int> = KeyValue(SETTINGS_AUDIODEVICE_BUFFER_COUNT, AudioDeviceSettings.getDefaultBufferCount())
    private val kv_mixer: KeyValue<String> = KeyValue(SETTINGS_MIXER, "")
    private val kv_useLegacyAudio: KeyValue<Boolean> = KeyValue(SETTINGS_USE_LEGACY_SOUND, false)
    private val kv_mainMenuFlipAnimations: KeyValue<Boolean> = KeyValue(SETTINGS_MAINMENU_FLIP_ANIMATION, true)
    private val kv_achievementNotifications: KeyValue<Boolean> = KeyValue(SETTINGS_ACHIEVEMENT_NOTIFICATIONS, true)
    private val kv_calibrationAudioOffsetMs: KeyValue<Int> = KeyValue(SETTINGS_CALIBRATION_AUDIO_OFFSET_MS, 0)
    private val kv_calibrationDisableInputSFX: KeyValue<Boolean> = KeyValue(SETTINGS_CALIBRATION_DISABLE_INPUT_SFX, false)
    private val kv_vsyncEnabled: KeyValue<Boolean> = KeyValue(SETTINGS_VSYNC, false)
    private val kv_maxFramerate: KeyValue<Int> = KeyValue(SETTINGS_MAX_FPS, determineMaxRefreshRate())
    private val kv_forceTexturePack: KeyValue<ForceTexturePack> = KeyValue(SETTINGS_FORCE_TEXTURE_PACK, ForceTexturePack.NO_FORCE)
    private val kv_forceTilesetPalette: KeyValue<ForceTilesetPalette> = KeyValue(SETTINGS_FORCE_TILESET_PALETTE, ForceTilesetPalette.NO_FORCE)
    private val kv_lastUpdateNotes: KeyValue<String> = KeyValue(LAST_UPDATE_NOTES, "")

    val kv_editorDetailedMarkerUndo: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_DETAILED_MARKER_UNDO, false)
    val kv_editorCameraPanOnDragEdge: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_CAMERA_PAN_ON_DRAG_EDGE, true)
    val kv_editorPanningDuringPlayback: KeyValue<CameraPanningSetting> = KeyValue(EDITORSETTINGS_PANNING_DURING_PLAYBACK, CameraPanningSetting.PAN)
    val kv_editorAutosaveInterval: KeyValue<Int> = KeyValue(EDITORSETTINGS_AUTOSAVE_INTERVAL, 5)
    val kv_editorMusicWaveformOpacity: KeyValue<Int> = KeyValue(EDITORSETTINGS_MUSIC_WAVEFORM_OPACITY, 10)
    val kv_editorHigherAccuracyPreview: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_HIGHER_ACCURACY_PREVIEW, true)
    val kv_editorPlaytestStartsPlay: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_PLAYTEST_STARTS_PLAY, true)
    val kv_editorArrowKeysLikeScroll: KeyValue<Boolean> = KeyValue(EDITORSETTINGS_ARROW_KEYS_LIKE_SCROLL, true)
    val kv_editorUIScale: KeyValue<Int> = KeyValue(EDITORSETTINGS_UI_SCALE, 1)
    
    private val kv_keymapKeyboard: KeyValue<InputKeymapKeyboard> = KeyValue(KEYMAP_KEYBOARD, InputKeymapKeyboard())
            
    private val kv_endlessDunkHighScore: KeyValue<Int> = KeyValue(ENDLESS_DUNK_HIGHSCORE, 0)
    private val kv_endlessDailyChallenge: KeyValue<DailyChallengeScore> = KeyValue(ENDLESS_DAILY_CHALLENGE, DailyChallengeScore.ZERO)
    private val kv_endlessDailyChallengeStreak: KeyValue<Int> = KeyValue(ENDLESS_DAILY_CHALLENGE_STREAK, 0)
    private val kv_endlessHighScore: KeyValue<EndlessHighScore> = KeyValue(ENDLESS_HIGH_SCORE, EndlessHighScore.ZERO)
    private val kv_assembleNormalHighScore: KeyValue<Int> = KeyValue(SIDEMODE_ASSEMBLE_NORMAL, 0)
    private val kv_solitaireSFX: KeyValue<Boolean> = KeyValue(SIDEMODE_SOLITAIRE_GAME_SFX, true)
    private val kv_solitaireMusic: KeyValue<Boolean> = KeyValue(SIDEMODE_SOLITAIRE_GAME_MUSIC, true)

    val audioDeviceSettings: ReadOnlyVar<AudioDeviceSettings> = Var {
        PRMania.audioDeviceSettings
                ?: AudioDeviceSettings(AudioDeviceSettings.getDefaultBufferSize(), use(kv_audioDeviceBufferCount.value).coerceAtLeast(3))
    }

    val locale: Var<String> = kv_locale.value
    val masterVolumeSetting: Var<Int> = kv_masterVolumeSetting.value
    val gameplayVolumeSetting: Var<Int> = kv_gameplayVolumeSetting.value
    val menuMusicVolumeSetting: Var<Int> = kv_menuMusicVolumeSetting.value
    val menuSfxVolumeSetting: Var<Int> = kv_menuSfxVolumeSetting.value
    val windowedResolution: Var<WindowSize> = kv_windowedResolution.value
    val fullscreen: Var<Boolean> = kv_fullscreen.value
    val fullscreenMonitor: Var<MonitorInfo?> = kv_fullscreenMonitor.value
    val showInputFeedbackBar: Var<Boolean> = kv_showInputFeedbackBar.value
    val showSkillStar: Var<Boolean> = kv_showSkillStar.value
    val discordRichPresence: Var<Boolean> = kv_discordRichPresence.value
    val mixer: Var<String> = kv_mixer.value
    val useLegacyAudio: Var<Boolean> = kv_useLegacyAudio.value
    val mainMenuFlipAnimation: Var<Boolean> = kv_mainMenuFlipAnimations.value
    val achievementNotifications: Var<Boolean> = kv_achievementNotifications.value
    val calibrationAudioOffsetMs: Var<Int> = kv_calibrationAudioOffsetMs.value
    val calibrationDisableInputSFX: Var<Boolean> = kv_calibrationDisableInputSFX.value
    val vsyncEnabled: Var<Boolean> = kv_vsyncEnabled.value
    val maxFramerate: Var<Int> = kv_maxFramerate.value
    val forceTexturePack: Var<ForceTexturePack> = kv_forceTexturePack.value
    val forceTilesetPalette: Var<ForceTilesetPalette> = kv_forceTilesetPalette.value
    val lastUpdateNotes: Var<String> = kv_lastUpdateNotes.value

    val editorDetailedMarkerUndo: Var<Boolean> = kv_editorDetailedMarkerUndo.value
    val editorCameraPanOnDragEdge: Var<Boolean> = kv_editorCameraPanOnDragEdge.value
    val editorPanningDuringPlayback: Var<CameraPanningSetting> = kv_editorPanningDuringPlayback.value
    val editorAutosaveInterval: Var<Int> = kv_editorAutosaveInterval.value
    val editorMusicWaveformOpacity: Var<Int> = kv_editorMusicWaveformOpacity.value
    val editorHigherAccuracyPreview: Var<Boolean> = kv_editorHigherAccuracyPreview.value
    val editorPlaytestStartsPlay: Var<Boolean> = kv_editorPlaytestStartsPlay.value
    val editorArrowKeysLikeScroll: Var<Boolean> = kv_editorArrowKeysLikeScroll.value
    val editorUIScale: Var<Int> = kv_editorUIScale.value
    
    val inputKeymapKeyboard: Var<InputKeymapKeyboard> = kv_keymapKeyboard.value
    
    val endlessDunkHighScore: Var<Int> = kv_endlessDunkHighScore.value
    val sidemodeAssembleHighScore: Var<Int> = kv_assembleNormalHighScore.value
    val endlessDailyChallenge: Var<DailyChallengeScore> = kv_endlessDailyChallenge.value
    val dailyChallengeStreak: Var<Int> = kv_endlessDailyChallengeStreak.value
    val endlessHighScore: Var<EndlessHighScore> = kv_endlessHighScore.value
    val solitaireSFX: Var<Boolean> = kv_solitaireSFX.value
    val solitaireMusic: Var<Boolean> = kv_solitaireMusic.value
    
    val inputCalibration: ReadOnlyVar<InputCalibration> = Var.bind { 
        InputCalibration(use(calibrationAudioOffsetMs).toFloat(), use(calibrationDisableInputSFX))
    }
    val gameplayVolume: ReadOnlyVar<Int> = Var { (use(gameplayVolumeSetting) * (use(masterVolumeSetting) / 100f)).roundToInt().coerceIn(0, 100) }
    val menuMusicVolume: ReadOnlyVar<Int> = Var { (use(menuMusicVolumeSetting) * (use(masterVolumeSetting) / 100f)).roundToInt().coerceIn(0, 100) }
    val menuSfxVolume: ReadOnlyVar<Int> = Var { (use(menuSfxVolumeSetting) * (use(masterVolumeSetting) / 100f)).roundToInt().coerceIn(0, 100) }
    
    val newIndicatorLibrary: NewIndicator = NewIndicator(NEW_INDICATOR_LIBRARY, Version(1, 1, 0), newEvenIfFirstPlay = false)
    val newIndicatorEditorHelpTexpack: NewIndicator = NewIndicator(NEW_INDICATOR_EDITORHELP_TEXPACK, Version(1, 1, 0), newEvenIfFirstPlay = false)
    val newIndicatorEditorHelpExporting: NewIndicator = NewIndicator(NEW_INDICATOR_EDITORHELP_EXPORTING, Version(1, 1, 0), newEvenIfFirstPlay = false)
    val newIndicatorEditorHelpPrmproj: NewIndicator = NewIndicator(NEW_INDICATOR_EDITORHELP_PRMPROJ, Version(1, 1, 0), newEvenIfFirstPlay = false)
    val newIndicatorEditorHelpSpotlights: NewIndicator = NewIndicator(NEW_INDICATOR_EDITORHELP_SPOTLIGHTS, Version(2, 0, 0), newEvenIfFirstPlay = false)
    val newIndicatorExtrasAssemble: NewIndicator = NewIndicator(NEW_INDICATOR_EXTRAS_ASM, Version(1, 1, 0), newEvenIfFirstPlay = false)
    val newIndicatorExtrasSolitaire: NewIndicator = NewIndicator(NEW_INDICATOR_EXTRAS_SOLITAIRE, Version(1, 2, 0), newEvenIfFirstPlay = false)
    val newIndicatorStoryMode: NewIndicator = NewIndicator(NEW_INDICATOR_STORY_MODE, Version(2, 0, 0), newEvenIfFirstPlay = true)
    val newIndicatorEndlessModeHelp: NewIndicator = NewIndicator(NEW_INDICATOR_ENDLESS_MODE_HELP, Version(2, 0, 0), newEvenIfFirstPlay = true)
    val allNewIndicators: List<NewIndicator> = listOf(newIndicatorLibrary, newIndicatorEditorHelpTexpack,
            newIndicatorEditorHelpExporting, newIndicatorEditorHelpPrmproj, newIndicatorExtrasAssemble,
            newIndicatorExtrasSolitaire, newIndicatorStoryMode, newIndicatorEndlessModeHelp)

    
    init {
        LocalePicker.currentLocale.addListener {
            val locale = it.getOrCompute()
            this.locale.set(locale.locale.toString())
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun load() {
        val prefs = this.prefs
        prefs.getIntCoerceIn(kv_masterVolumeSetting, 0, 100)
        prefs.getIntCoerceIn(kv_gameplayVolumeSetting, 0, 100)
        prefs.getIntCoerceIn(kv_menuMusicVolumeSetting, 0, 100)
        prefs.getIntCoerceIn(kv_menuSfxVolumeSetting, 0, 100)
        prefs.getWindowSize(kv_windowedResolution)
        prefs.getBoolean(kv_fullscreen)
        prefs.getMonitorInfo(kv_fullscreenMonitor)
        prefs.getBoolean(kv_showInputFeedbackBar)
        prefs.getBoolean(kv_showSkillStar)
        prefs.getBoolean(kv_discordRichPresence)
        prefs.getInt(kv_audioDeviceBufferCount)
        prefs.getString(kv_mixer)
        prefs.getBoolean(kv_useLegacyAudio)
        prefs.getBoolean(kv_mainMenuFlipAnimations)
        prefs.getBoolean(kv_achievementNotifications)
        prefs.getString(kv_locale)
        prefs.getIntCoerceIn(kv_calibrationAudioOffsetMs, -500, 500)
        prefs.getBoolean(kv_calibrationDisableInputSFX)
        prefs.getBoolean(kv_vsyncEnabled)
        prefs.getIntCoerceIn(kv_maxFramerate, 0, 1000)
        prefs.getForceTexturePack(kv_forceTexturePack)
        if (prefs.contains(SETTINGS_ONLY_DEFAULT_PALETTE_OLD)) {
            // Legacy setting that is migrated
            val oldSetting = prefs.getBoolean(SETTINGS_ONLY_DEFAULT_PALETTE_OLD)
            kv_forceTilesetPalette.value.set(if (oldSetting) ForceTilesetPalette.FORCE_PR1 else ForceTilesetPalette.NO_FORCE)
            prefs.remove(SETTINGS_ONLY_DEFAULT_PALETTE_OLD)
            Paintbox.LOGGER.info("[Settings] Migrated old setting $SETTINGS_ONLY_DEFAULT_PALETTE_OLD, previously was $oldSetting")
        } else {
            prefs.getForceTilesetPalette(kv_forceTilesetPalette)
        }
        prefs.getString(kv_lastUpdateNotes)
        
        prefs.getBoolean(kv_editorDetailedMarkerUndo)
        prefs.getBoolean(kv_editorCameraPanOnDragEdge)
        prefs.getEditorSetting(kv_editorPanningDuringPlayback as KeyValue<EditorSetting>,
                CameraPanningSetting.MAP, CameraPanningSetting.PAN)
        prefs.getIntCoerceIn(kv_editorAutosaveInterval, 0, Short.MAX_VALUE.toInt())
        prefs.getIntCoerceIn(kv_editorMusicWaveformOpacity, 0, 10)
        prefs.getBoolean(kv_editorHigherAccuracyPreview)
        prefs.getBoolean(kv_editorPlaytestStartsPlay)
        prefs.getBoolean(kv_editorArrowKeysLikeScroll)
        prefs.getInt(kv_editorUIScale)
        
        prefs.getInputKeymapKeyboard(kv_keymapKeyboard)
        
        prefs.getInt(kv_endlessDunkHighScore)
        prefs.getInt(kv_assembleNormalHighScore)
        prefs.getDailyChallenge(kv_endlessDailyChallenge)
        prefs.getEndlessHighScore(kv_endlessHighScore)
        prefs.getInt(kv_endlessDailyChallengeStreak)
        prefs.getBoolean(kv_solitaireSFX)
        prefs.getBoolean(kv_solitaireMusic)
        
        val lastVersion: Version? = Version.parse(prefs.getString(LAST_VERSION) ?: "")
        this.lastVersion = lastVersion
        allNewIndicators.forEach { prefs.getNewIndicator(it, lastVersion) }
    }

    fun persist() {
        prefs
                .putInt(kv_masterVolumeSetting)
                .putInt(kv_gameplayVolumeSetting)
                .putInt(kv_menuMusicVolumeSetting)
                .putInt(kv_menuSfxVolumeSetting)
                .putWindowSize(kv_windowedResolution)
                .putBoolean(kv_fullscreen)
                .putMonitorInfo(kv_fullscreenMonitor)
                .putBoolean(kv_showInputFeedbackBar)
                .putBoolean(kv_showSkillStar)
                .putBoolean(kv_discordRichPresence)
                .putInt(kv_audioDeviceBufferCount)
                .putString(kv_mixer)
                .putBoolean(kv_useLegacyAudio)
                .putBoolean(kv_mainMenuFlipAnimations)
                .putBoolean(kv_achievementNotifications)
                .putString(kv_locale)
                .putInt(kv_calibrationAudioOffsetMs)
                .putBoolean(kv_calibrationDisableInputSFX)
                .putBoolean(kv_vsyncEnabled)
                .putInt(kv_maxFramerate)
                .putForceTexturePack(kv_forceTexturePack)
                .putForceTilesetPalette(kv_forceTilesetPalette)
                .putString(kv_lastUpdateNotes)

                .putBoolean(kv_editorDetailedMarkerUndo)
                .putBoolean(kv_editorCameraPanOnDragEdge)
                .putEditorSetting(kv_editorPanningDuringPlayback)
                .putInt(kv_editorAutosaveInterval)
                .putInt(kv_editorMusicWaveformOpacity)
                .putBoolean(kv_editorHigherAccuracyPreview)
                .putBoolean(kv_editorPlaytestStartsPlay)
                .putBoolean(kv_editorArrowKeysLikeScroll)
                .putInt(kv_editorUIScale)

                .putInputKeymapKeyboard(kv_keymapKeyboard)

                .putInt(kv_endlessDunkHighScore)
                .putInt(kv_assembleNormalHighScore)
                .putDailyChallenge(kv_endlessDailyChallenge)
                .putEndlessHighScore(kv_endlessHighScore)
                .putInt(kv_endlessDailyChallengeStreak)
                .putBoolean(kv_solitaireSFX)
                .putBoolean(kv_solitaireMusic)

        allNewIndicators.forEach { prefs.putNewIndicator(it) }

        prefs.flush()
    }
    
    fun setStartupSettings(game: PRManiaGame) {
        LocalePicker.attemptToSetNamedLocale(this.locale.getOrCompute())

        Paintbox.LOGGER.info("Using ${if (useLegacyAudio.getOrCompute()) "legacy" else "OpenAL"} sound system")
        // Set correct JavaSound mixer
        val mixerHandler = MixerHandler.defaultMixerHandler
        val mixerString = this.mixer.getOrCompute()
        if (mixerString.isNotEmpty()) {
            val found = mixerHandler.supportedMixers.mixers.find {
                it.mixerInfo.name == mixerString
            }
            if (found != null) {
                Paintbox.LOGGER.info("Setting recommended legacy audio JavaSound mixer to mixer from settings: ${found.mixerInfo.name}")
                mixerHandler.recommendedMixer = found
            } else {
                Paintbox.LOGGER.warn("Could not find JavaSound mixer from settings: settings = $mixerString")
            }
        } else {
            val mixerName = mixerHandler.recommendedMixer.mixerInfo.name
            this.mixer.set(mixerName)
            Paintbox.LOGGER.info("No saved JavaSound mixer string, recommended will be $mixerName")
        }
        
        // LauncherSettings override properties
        val fps = game.launcherSettings.fps
        if (fps != null) {
            this.maxFramerate.set(fps.coerceAtLeast(0))
        }
        val vsync = game.launcherSettings.vsync
        if (vsync != null) {
            this.vsyncEnabled.set(vsync)
        }
        Gdx.app.postRunnable { 
            val gr = Gdx.graphics
            gr.setForegroundFPS(this.maxFramerate.getOrCompute())
            gr.setVSync(this.vsyncEnabled.getOrCompute())
            Paintbox.LOGGER.debug("DEBUG MONITORS: Primary=${gr.primaryMonitor?.name}, current=${gr.monitor?.name}, allMonitors=${gr.monitors.joinToString { it.name }}")
        }
    }
    
    private fun Preferences.getNewIndicator(newIndicator: NewIndicator, lastLoadedVersion: Version?) {
        val containsKey = this.contains(newIndicator.key)
        val defaultValue: Boolean = when {
            lastLoadedVersion == null -> newIndicator.newEvenIfFirstPlay
            lastLoadedVersion <= newIndicator.newAsOf -> true
            else -> false
        }
        if (containsKey) {
            newIndicator.value.set(this.getBoolean(newIndicator.key, defaultValue))
        } else {
            newIndicator.value.set(defaultValue)
            this.putBoolean(newIndicator.key, defaultValue)
        }
    }
    
    private fun Preferences.putNewIndicator(newIndicator: NewIndicator) {
        this.putBoolean(newIndicator.key, newIndicator.value.get())
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

    private fun Preferences.getString(kv: KeyValue<String>, defaultValue: String = kv.defaultValue) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            kv.value.set(prefs.getString(kv.key, defaultValue))
        }
    }

    private fun Preferences.putString(kv: KeyValue<String>): Preferences {
        val prefs: Preferences = this
        return prefs.putString(kv.key, kv.value.getOrCompute())
    }

    @JvmName("getStringNullable")
    private fun Preferences.getString(kv: KeyValue<String?>, defaultValue: String? = kv.defaultValue) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            kv.value.set(prefs.getString(kv.key, defaultValue))
        }
    }

    @JvmName("putStringNullable")
    private fun Preferences.putString(kv: KeyValue<String?>): Preferences {
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

    private fun Preferences.getMonitorInfo(kv: KeyValue<MonitorInfo?>) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            val str = prefs.getString(kv.key)
            kv.value.set(MonitorInfo.fromEncodedString(str))
        }
    }

    private fun Preferences.putMonitorInfo(kv: KeyValue<MonitorInfo?>): Preferences {
        val prefs: Preferences = this
        val mi = kv.value.getOrCompute()
        return prefs.putString(kv.key, mi?.toEncodedString() ?: "")
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
    
    private fun Preferences.getForceTexturePack(kv: KeyValue<ForceTexturePack>) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            val i = prefs.getInteger(kv.key, ForceTexturePack.NO_FORCE.jsonId)
            kv.value.set(ForceTexturePack.JSON_MAP[i] ?: ForceTexturePack.NO_FORCE)
        }
    }

    private fun Preferences.putForceTexturePack(kv: KeyValue<ForceTexturePack>): Preferences {
        val prefs: Preferences = this
        return prefs.putInteger(kv.key, kv.value.getOrCompute().jsonId)
    }
    
    private fun Preferences.getForceTilesetPalette(kv: KeyValue<ForceTilesetPalette>) {
        val prefs: Preferences = this
        if (prefs.contains(kv.key)) {
            val i = prefs.getInteger(kv.key, ForceTexturePack.NO_FORCE.jsonId)
            kv.value.set(ForceTilesetPalette.JSON_MAP[i] ?: ForceTilesetPalette.NO_FORCE)
        }
    }

    private fun Preferences.putForceTilesetPalette(kv: KeyValue<ForceTilesetPalette>): Preferences {
        val prefs: Preferences = this
        return prefs.putInteger(kv.key, kv.value.getOrCompute().jsonId)
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