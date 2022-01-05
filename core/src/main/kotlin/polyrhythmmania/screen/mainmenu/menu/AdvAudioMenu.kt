package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.font.TextAlign
import paintbox.ui.Anchor
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.Slider
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.soundsystem.DumpAudioDebugInfo
import polyrhythmmania.soundsystem.SoundSystem
import javax.sound.sampled.Mixer


class AdvAudioMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
//    private val settings: Settings = menuCol.main.settings

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

        val mixerHandler = SoundSystem.defaultMixerHandler
        val mixers: List<Mixer> = mixerHandler.supportedMixers
        mixers.forEach { mixer ->
            Paintbox.LOGGER.info("Supported mixer: ${mixer.mixerInfo.name}")
        }

        val (mixerPane, mixerCombobox) = createComboboxOption(mixers, mixerHandler.recommendedMixer,
                { Localization.getVar("mainMenu.advancedAudio.mixer").use() },
                percentageContent = 1f, twoRowsTall = true, itemToString = { it.mixerInfo.name })
        mixerPane.label.textAlign.set(TextAlign.CENTRE)
        mixerPane.label.renderAlign.set(Align.center)
        mixerPane.label.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.advancedAudio.mixer.tooltip")))
        mixerCombobox.font.set(main.fontMainMenuRodin)
        mixerCombobox.setScaleXY(0.75f)
        mixerCombobox.selectedItem.addListener { m ->
            setSoundSystemMixer(m.getOrCompute())
        }

        vbox.temporarilyDisableLayouts {
            fun separator(): UIElement {
                return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                    this.bounds.height.set(10f)
                    this.margin.set(Insets(4f, 4f, 12f, 12f))
                }
            }
            
            vbox += TextLabel(binding = { Localization.getVar("mainMenu.advancedAudio.notice").use() }).apply {
                this.markup.set(this@AdvAudioMenu.markup)
                this.bounds.height.set(65f)    
                this.renderAlign.set(Align.topLeft)
                this.doLineWrapping.set(true)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
                this.setScaleXY(0.9f)
            }
            vbox += mixerPane
            
            vbox += separator()
            vbox += createLongButton { Localization.getVar("mainMenu.advancedAudio.dumpAudioDebugInfo").use() }.apply {
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.advancedAudio.dumpAudioDebugInfo.tooltip")))
                this.setOnAction {
                    DumpAudioDebugInfo.dump()
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
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.advancedAudio.resetMixer").use() }).apply {
                this.bounds.width.set(250f)
                this.setOnAction {
                    val def = mixerHandler.defaultMixer
                    mixerCombobox.selectedItem.set(def) // Listener will set accordingly.
                }
            }
        }
    }

    fun setSoundSystemMixer(newMixer: Mixer) {
        if (SoundSystem.defaultMixerHandler.recommendedMixer == newMixer) return
        SoundSystem.defaultMixerHandler.recommendedMixer = newMixer
        // Restart the main menu sound system.
        synchronized(mainMenu) {
            val oldMusicPos = mainMenu.soundSys.musicPlayer.position
            mainMenu.soundSys.shutdown()
            mainMenu.soundSys = mainMenu.SoundSys().apply {
                start()
                musicPlayer.position = oldMusicPos
            }
        }
        val name = newMixer.mixerInfo.name
        main.settings.mixer.set(name)
        Paintbox.LOGGER.info("Set mixer to ${name}")
    }
}