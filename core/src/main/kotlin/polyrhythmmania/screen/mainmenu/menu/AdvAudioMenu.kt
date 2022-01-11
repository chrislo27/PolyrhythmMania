package polyrhythmmania.screen.mainmenu.menu

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
import polyrhythmmania.Settings
import polyrhythmmania.soundsystem.beads.DaemonJavaSoundAudioIO
import polyrhythmmania.soundsystem.beads.OpenALAudioIO
import polyrhythmmania.soundsystem.javasound.DumpAudioDebugInfo
import polyrhythmmania.soundsystem.javasound.MixerHandler
import javax.sound.sampled.Mixer
import kotlin.math.max
import kotlin.math.roundToInt


class AdvAudioMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private val settings: Settings = menuCol.main.settings
    private val soundSystemUpdatedFlag: BooleanVar = BooleanVar(false) // Toggled to indicate a sound system change
    private val doneTextTimer: FloatVar = FloatVar(0f)

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
            this.padding.set(Insets(2f))
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
        
        val legacyAudioEnabled = BooleanVar { use(settings.useLegacyAudio) }
        
        val audioSettingsPane = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
        }
        
        audioSettingsPane.temporarilyDisableLayouts {
            audioSettingsPane += longSeparator()
        }
        audioSettingsPane.sizeHeightToChildren(10f)
        audioSettingsPane.visible.bind { !legacyAudioEnabled.use() }
        
        // Legacy audio settings
        
        val legacySettingsPane = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
        }

        val mixerHandler = MixerHandler.defaultMixerHandler
        val supportedMixers: MixerHandler.SupportedMixers = mixerHandler.supportedMixers
        supportedMixers.mixers.forEach { mixer ->
            Paintbox.LOGGER.info("Supported JavaSound mixer: ${mixer.mixerInfo.name}")
        }

        val (mixerPane, mixerCombobox) = createComboboxOption(supportedMixers.mixers, mixerHandler.recommendedMixer,
                { Localization.getVar("mainMenu.advancedAudio.legacyOutputInterface").use() },
                percentageContent = 1f, twoRowsTall = true, itemToString = { it.mixerInfo.name })
        mixerPane.label.textAlign.set(TextAlign.CENTRE)
        mixerPane.label.renderAlign.set(Align.center)
        mixerPane.label.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.advancedAudio.legacyOutputInterface.tooltip")))
        mixerCombobox.font.set(main.fontMainMenuRodin)
        mixerCombobox.setScaleXY(0.75f)
        mixerCombobox.selectedItem.addListener { m ->
            setSoundSystemMixer(m.getOrCompute())
        }
        
        legacySettingsPane.temporarilyDisableLayouts {
            legacySettingsPane += mixerPane
        }
        legacySettingsPane.sizeHeightToChildren(10f)
        legacySettingsPane.visible.bind { legacyAudioEnabled.use() }
        
        // End of legacy audio settings

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
                            (DaemonJavaSoundAudioIO.NUM_OUTPUT_BUFFERS * ((audioIO.systemBufferSizeInFrames / 4) / audioIO.context.sampleRate) * 1000).roundToInt()
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
                if (newState) {
                    Paintbox.LOGGER.info("Now using OpenAL audio system: ${settings.audioDeviceSettings.getOrCompute()}")
                }
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

    fun setSoundSystemMixer(newMixer: Mixer) {
        if (MixerHandler.defaultMixerHandler.recommendedMixer == newMixer) return
        MixerHandler.defaultMixerHandler.recommendedMixer = newMixer
        restartMainMenuAudio()
        val name = newMixer.mixerInfo.name
        main.settings.mixer.set(name)
        Paintbox.LOGGER.info("Now using JavaSound audio system; set JavaSound mixer to $name")
    }
    
    fun restartMainMenuAudio() {
        // Restart the main menu sound system.
        synchronized(mainMenu) {
            val oldMusicPos = mainMenu.soundSys.musicPlayer.position
            mainMenu.soundSys.shutdown()
            mainMenu.soundSys = mainMenu.SoundSys().apply {
                start()
                musicPlayer.position = oldMusicPos
            }
            soundSystemUpdatedFlag.invert()
        }
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelf(originX, originY, batch)
        val timer = doneTextTimer.get()
        if (timer > 0f) {
            doneTextTimer.set((timer - Gdx.graphics.deltaTime).coerceAtLeast(0f))
        }
    }
}