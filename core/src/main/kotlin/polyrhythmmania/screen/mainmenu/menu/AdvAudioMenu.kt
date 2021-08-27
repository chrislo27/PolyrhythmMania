package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.font.TextAlign
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.Slider
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.soundsystem.SoundSystem
import javax.sound.sampled.Mixer


class AdvAudioMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private val settings: Settings = menuCol.main.settings

    init {
        this.setSize(MMMenu.WIDTH_MID)
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

        val (mixerPane, mixerControl) = createCycleOption(mixers, mixerHandler.recommendedMixer,
                { Localization.getVar("mainMenu.advancedAudio.mixer").use() },
                percentageContent = 1f, twoRowsTall = true, itemToString = { it.mixerInfo.name })
        mixerPane.label.textAlign.set(TextAlign.CENTRE)
        mixerPane.label.renderAlign.set(Align.center)
        mixerPane.label.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.advancedAudio.mixer.tooltip")))
        mixerControl.label.font.set(main.fontMainMenuRodin)
        mixerControl.label.setScaleXY(0.75f)
        mixerControl.currentItem.addListener { m ->
            setSoundSystemMixer(m.getOrCompute())
        }

        vbox.temporarilyDisableLayouts {
            vbox += TextLabel(binding = { Localization.getVar("mainMenu.advancedAudio.notice").use() }).apply {
                this.markup.set(this@AdvAudioMenu.markup)
                this.bounds.height.set(75f)    
                this.renderAlign.set(Align.topLeft)
                this.doLineWrapping.set(true)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
            }
            vbox += mixerPane
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
                    mixerControl.currentItem.set(def) // Listener will set accordingly.
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