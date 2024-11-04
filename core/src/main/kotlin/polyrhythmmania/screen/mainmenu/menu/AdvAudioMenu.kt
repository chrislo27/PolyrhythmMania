package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Audio
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.binding.BooleanVar
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.Settings
import polyrhythmmania.screen.mainmenu.SoundSys
import polyrhythmmania.soundsystem.AudioDeviceSettings
import polyrhythmmania.soundsystem.RealtimeOutput
import polyrhythmmania.soundsystem.beads.DaemonJavaSoundAudioIO
import polyrhythmmania.soundsystem.beads.OpenALAudioIO
import polyrhythmmania.soundsystem.javasound.DumpAudioDebugInfo
import polyrhythmmania.soundsystem.javasound.MixerHandler
import javax.sound.sampled.Mixer
import kotlin.math.max
import kotlin.math.roundToInt


class AdvAudioMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    companion object {
        private const val OPENAL_SOFT_ON_PREFIX: String = "OpenAL Soft on "
    }
    
    private data class OpenALOutputDevice(val rawName: String) {
        
        companion object {
            val DEFAULT_DEVICE: OpenALOutputDevice? = null // Null on purpose

            fun getOpenALOutputDevices(): List<OpenALOutputDevice?> {
                return listOf(DEFAULT_DEVICE) + Gdx.audio.availableOutputDevices.map(::OpenALOutputDevice)
            }
        }
        
        val readableName: String = rawName.removePrefix(OPENAL_SOFT_ON_PREFIX)
    }
    
    private val settings: Settings = menuCol.main.settings
    private val audioDeviceSettingsOverridden = PRMania.audioDeviceSettings != null
    private val soundSystemUpdatedFlag: BooleanVar = BooleanVar(false) // Toggled to indicate a sound system change
    private val doneTextTimer: FloatVar = FloatVar(0f)
    private val legacyAudioEnabled = BooleanVar { bindAndGet(settings.useLegacyAudio) }
    
    private val openalOutputDevices: Var<List<OpenALOutputDevice?>> = Var(listOf(null))

    init {
        this.setSize(MMMenu.WIDTH_MEDIUM)
        this.titleText.bind { Localization.getVar("mainMenu.advancedAudio.title").use() }
        this.contentPane.bounds.height.set(300f)

        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(4f, 0f, 2f, 2f))
            this.bounds.height.set(40f)
        }

        contentPane.addChild(vbox)
        contentPane.addChild(hbox)
        
        fun longSeparator(): UIElement {
            return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                this.bounds.height.set(10f)
                this.margin.set(Insets(4f, 4f, 4f, 4f))
            }
        }
        
        val audioSettingsPane = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
        }

        audioSettingsPane.temporarilyDisableLayouts {
            // OpenAL audio devices
            val (mixerPane, mixerCombobox) = createComboboxOption(
                openalOutputDevices.getOrCompute(), OpenALOutputDevice.DEFAULT_DEVICE,
                { Localization.getVar("mainMenu.advancedAudio.audioDeviceSettings.audioDevice").use() },
                percentageContent = 0.725f, twoRowsTall = false,
                itemToString = { device ->
                    device?.readableName
                        ?: Localization.getValue("mainMenu.advancedAudio.audioDeviceSettings.audioDevice.default")
                })
            mixerCombobox.items.bind { openalOutputDevices.use() }
            mixerCombobox.font.set(main.fontUnifontMainMenu)
            mixerCombobox.selectedItem.addListener { dev ->
                switchOpenALOutputDevice(dev.getOrCompute())
            }
            audioSettingsPane += mixerPane
            
            // Buffer count: Should be disabled if the launch argument --audio-device-buffer-count was set (check PRMania.audioDeviceSettings != null)
            val (bufCountPane, bufCountCombobox) = createComboboxOption((3..30).toList(), settings.audioDeviceSettings.getOrCompute().bufferCount,
                    { Localization.getVar("mainMenu.advancedAudio.audioDeviceSettings.bufferCount").use() },
                    percentageContent = 0.5f)
            bufCountPane.label.tooltipElement.set(createTooltip {
                Localization.getVar("mainMenu.advancedAudio.audioDeviceSettings.bufferCount.tooltip").use() + if (audioDeviceSettingsOverridden) {
                    "\n${
                        Localization.getVar("mainMenu.advancedAudio.audioDeviceSettings.bufferCount.tooltip.overridden", Var {
                            listOf("--audio-device-buffer-count")
                        }).use()
                    }"
                } else ""
            })
            bufCountCombobox.selectedItem.addListener { m ->
                settings.kv_audioDeviceBufferCount.value.set(m.getOrCompute().coerceAtLeast(3))
                restartMainMenuAudio()
            }
            bufCountPane.content -= bufCountCombobox
            bufCountPane.content += HBox().apply {
                this.spacing.set(8f)
                this.align.set(HBox.Align.RIGHT)
                this += bufCountCombobox.apply { 
                    this.bounds.width.set(100f)
                    this.disabled.set(audioDeviceSettingsOverridden)
                }
                this += createSmallButton { Localization.getVar("common.reset").use() }.apply { 
                    this.bounds.width.set(90f)
                    this.setOnAction { 
                        bufCountCombobox.selectedItem.set(AudioDeviceSettings.getDefaultBufferCount())
                    }
                    this.disabled.set(audioDeviceSettingsOverridden)
                }
            }
            audioSettingsPane += bufCountPane
        }
        audioSettingsPane.sizeHeightToChildren(10f)
        audioSettingsPane.visible.bind { !legacyAudioEnabled.use() }
        
        //region Legacy audio settings
        
        val legacySettingsPane = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
        }

        val mixerHandler = MixerHandler.defaultMixerHandler
        val supportedMixers: MixerHandler.SupportedMixers = mixerHandler.supportedMixers
        supportedMixers.mixers.forEach { mixer ->
            Paintbox.LOGGER.info("Legacy audio: Supported JavaSound mixer: ${mixer.mixerInfo.name}")
        }

        val (mixerPane, mixerCombobox) = createComboboxOption(supportedMixers.mixers, mixerHandler.recommendedMixer,
                { Localization.getVar("mainMenu.advancedAudio.legacyOutputInterface").use() },
                percentageContent = 1f, twoRowsTall = true, itemToString = { it.mixerInfo.name })
        mixerPane.label.textAlign.set(TextAlign.CENTRE)
        mixerPane.label.renderAlign.set(Align.center)
        mixerPane.label.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.advancedAudio.legacyOutputInterface.tooltip")))
        mixerCombobox.font.set(main.fontUnifontMainMenu)
        mixerCombobox.selectedItem.addListener { m ->
            setSoundSystemMixer(m.getOrCompute())
        }
        
        legacySettingsPane.temporarilyDisableLayouts {
            legacySettingsPane += mixerPane
        }
        legacySettingsPane.sizeHeightToChildren(10f)
        legacySettingsPane.visible.bind { legacyAudioEnabled.use() }
        
        //endregion

        vbox.temporarilyDisableLayouts {
            vbox += TextLabel(binding = { Localization.getVar("mainMenu.advancedAudio.notice").use() }).apply {
                this.markup.set(this@AdvAudioMenu.markup)
                this.bounds.height.set(50f)    
                this.renderAlign.set(Align.topLeft)
                this.doLineWrapping.set(true)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
                this.setScaleXY(0.9f)
            }

            vbox += longSeparator()
            
            vbox += TextLabel(binding = {
                Localization.getVar("mainMenu.advancedAudio.internalLatency", Var {
                    soundSystemUpdatedFlag.use()
                    val audioIO = mainMenu.soundSys.soundSystem.audioContext.audioIO
                    val latency: Int = when (audioIO) {
                        is OpenALAudioIO -> {
                            val s = audioIO.audioDeviceSettings
                            (s.bufferCount * ((s.bufferSize / 4) / audioIO.context.sampleRate) * 1000).roundToInt()
                        }
                        is DaemonJavaSoundAudioIO -> {
                            (DaemonJavaSoundAudioIO.NUM_OUTPUT_BUFFERS * (audioIO.systemBufferSizeInFrames / audioIO.context.sampleRate) * 1000).roundToInt()
                        }
                        else -> -1
                    }
                    listOf(latency)
                }).use()
            }).apply {
                this.padding.set(Insets(4f, 4f, 12f, 12f))
                this.bounds.height.set(36f)
                this.markup.set(this@AdvAudioMenu.markup)
                this.renderAlign.set(Align.center)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.advancedAudio.internalLatency.tooltip")))
            }

            val (legacyAudioCheckPane, legacyAudioCheck) = createCheckboxOption({ Localization.getVar("mainMenu.advancedAudio.useLegacyAudio").use() })
            legacyAudioCheck.checkedState.set(settings.useLegacyAudio.getOrCompute())
            legacyAudioCheck.onCheckChanged = { newState ->
                settings.useLegacyAudio.set(newState)
                restartMainMenuAudio()
            }
            legacyAudioCheck.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.advancedAudio.useLegacyAudio.tooltip")))
            vbox += legacyAudioCheckPane
            
            vbox += longSeparator()
            vbox += Pane().apply { 
                this += audioSettingsPane
                this += legacySettingsPane
                
                this.bounds.height.bind { 
                    max(audioSettingsPane.bounds.height.use(), legacySettingsPane.bounds.height.use())
                }
            }
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            val dumpAudioDebugInfoText = Var {
                if (doneTextTimer.use() <= 0f) {
                    Localization.getVar("mainMenu.advancedAudio.dumpAudioDebugInfo").use()
                } else {
                    Localization.getVar("mainMenu.advancedAudio.dumpAudioDebugInfo.done").use()
                }
            }
            hbox += createSmallButton(binding = { dumpAudioDebugInfoText.use() }).apply {
                this.bounds.width.set(300f)
                this.setOnAction {
                    DumpAudioDebugInfo.dump()
                    doneTextTimer.set(3f)
                }
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.advancedAudio.dumpAudioDebugInfo.tooltip")))
            }
            
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.advancedAudio.resetMixer").use() }).apply {
                this.bounds.width.set(200f)
                this.setOnAction {
                    val def = mixerHandler.defaultMixer
                    mixerCombobox.selectedItem.set(def) // Listener will set accordingly.
                }
                this.visible.bind { legacyAudioEnabled.use() }
            }
        }
    }
    
    init {
        openalOutputDevices.addListener { varr ->
            Paintbox.LOGGER.info("OpenAL audio: Output devices: ${varr.getOrCompute().drop(1).map { it?.readableName }}")
        }
    }
    
    private fun setSoundSystemMixer(newMixer: Mixer) {
        if (MixerHandler.defaultMixerHandler.recommendedMixer == newMixer) return
        MixerHandler.defaultMixerHandler.recommendedMixer = newMixer
        restartMainMenuAudio()
        val name = newMixer.mixerInfo.name
        main.settings.mixer.set(name)
        Paintbox.LOGGER.info("Now using JavaSound audio system; set JavaSound mixer to $name")
    }
    
    private fun switchOpenALOutputDevice(newDevice: OpenALOutputDevice?) {
        Gdx.audio.switchOutputDevice(newDevice)
        Paintbox.LOGGER.info("Switched OpenAL output device to ${newDevice?.rawName ?: "<default device (null)>"}")
    }
    
    private fun restartMainMenuAudio() {
        // Restart the main menu sound system.
        synchronized(mainMenu) {
            val oldMusicPos = mainMenu.soundSys.titleMusicPlayer.position
            mainMenu.soundSys.shutdown()
            mainMenu.soundSys = SoundSys(mainMenu).apply {
                start()
                titleMusicPlayer.position = oldMusicPos
            }
            soundSystemUpdatedFlag.invert()
            if (mainMenu.soundSys.soundSystem.realtimeOutput is RealtimeOutput.OpenAL) {
                Paintbox.LOGGER.info("Now using OpenAL audio system: ${settings.audioDeviceSettings.getOrCompute()}")
            }
        }
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelf(originX, originY, batch)
        val timer = doneTextTimer.get()
        if (timer > 0f) {
            doneTextTimer.set((timer - Gdx.graphics.deltaTime).coerceAtLeast(0f))
        }
    }

    override fun onMenuEntered() {
        super.onMenuEntered()
        openalOutputDevices.set(OpenALOutputDevice.getOpenALOutputDevices())
    }

    private fun Audio.switchOutputDevice(device: OpenALOutputDevice?): Boolean =
        this.switchOutputDevice(device?.rawName)
}