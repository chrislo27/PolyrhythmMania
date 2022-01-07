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
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.soundsystem.javasound.DumpAudioDebugInfo
import polyrhythmmania.soundsystem.javasound.MixerHandler
import javax.sound.sampled.Mixer


class AdvAudioMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private val settings: Settings = menuCol.main.settings
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
        
        // Legacy audio settings
        
        val legacyPane = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
        }

        val mixerHandler = MixerHandler.defaultMixerHandler
        val supportedMixers: MixerHandler.SupportedMixers = mixerHandler.supportedMixers
        supportedMixers.mixers.forEach { mixer ->
            Paintbox.LOGGER.info("Supported JavaSound mixer: ${mixer.mixerInfo.name}")
        }

        val (mixerPane, mixerCombobox) = createComboboxOption(supportedMixers.mixers, mixerHandler.recommendedMixer,
                { Localization.getVar("mainMenu.advancedAudio.outputInterface").use() },
                percentageContent = 1f, twoRowsTall = true, itemToString = { it.mixerInfo.name })
        mixerPane.label.textAlign.set(TextAlign.CENTRE)
        mixerPane.label.renderAlign.set(Align.center)
        mixerPane.label.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.advancedAudio.outputInterface.tooltip")))
        mixerCombobox.font.set(main.fontMainMenuRodin)
        mixerCombobox.setScaleXY(0.75f)
        mixerCombobox.selectedItem.addListener { m ->
            setSoundSystemMixer(m.getOrCompute())
        }
        
        val legacyAudioEnabled = BooleanVar { use(settings.useLegacyAudio) }
        legacyPane.temporarilyDisableLayouts {
            legacyPane += mixerPane
        }
        legacyPane.sizeHeightToChildren(10f)
        legacyPane.visible.bind { legacyAudioEnabled.use() }
        
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

            val (legacyAudioCheckPane, legacyAudioCheck) = createCheckboxOption({ Localization.getVar("mainMenu.advancedAudio.useLegacyAudio").use() })
            legacyAudioCheck.checkedState.set(settings.useLegacyAudio.getOrCompute())
            legacyAudioCheck.onCheckChanged = { newState ->
                settings.useLegacyAudio.set(newState)
                restartMainMenuAudio()
                if (newState) {
                    Paintbox.LOGGER.info("Now using OpenAL audio system")
                }
            }
            legacyAudioCheck.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.advancedAudio.useLegacyAudio.tooltip")))
            vbox += legacyAudioCheckPane
            
            val dumpAudioDebugInfoText = Var {
                if (doneTextTimer.use() <= 0f) {
                    Localization.getVar("mainMenu.advancedAudio.dumpAudioDebugInfo").use()
                } else {
                    Localization.getVar("mainMenu.advancedAudio.dumpAudioDebugInfo.done").use()
                }
            }
            vbox += createLongButton { 
                dumpAudioDebugInfoText.use()
            }.apply {
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.advancedAudio.dumpAudioDebugInfo.tooltip")))
                this.setOnAction {
                    DumpAudioDebugInfo.dump()
                    doneTextTimer.set(3f)
                }
            }
            
            vbox += longSeparator()
            vbox += legacyPane
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.advancedAudio.resetMixer").use() }).apply {
                this.bounds.width.set(250f)
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