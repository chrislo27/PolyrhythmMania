package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.Slider
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.screen.mainmenu.MainMenuScreen
import polyrhythmmania.soundsystem.MixerHandler
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.beads.toJavaAudioFormat
import javax.sound.sampled.Mixer


class AudioSettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private val settings: Settings = menuCol.main.settings
    
    val gameplayVolSlider: Slider = Slider().apply { 
        this.bindWidthToParent(multiplier = 0.85f)
        this.minimum.set(0f)
        this.maximum.set(100f)
        this.tickUnit.set(5f)
        this.setValue(settings.gameplayVolume.getOrCompute().toFloat())
        this.value.addListener { v ->
            settings.gameplayVolume.set(v.getOrCompute().toInt())
        }
    }
    val menuMusicVolSlider: Slider = Slider().apply { 
        this.bindWidthToParent(multiplier = 0.85f)
        this.minimum.set(0f)
        this.maximum.set(100f)
        this.tickUnit.set(5f)
        this.setValue(settings.menuMusicVolume.getOrCompute().toFloat())
        this.value.addListener { v ->
            settings.menuMusicVolume.set(v.getOrCompute().toInt())
        }
    }
    val menuSfxVolSlider: Slider = Slider().apply { 
        this.bindWidthToParent(multiplier = 0.85f)
        this.minimum.set(0f)
        this.maximum.set(100f)
        this.tickUnit.set(5f)
        this.setValue(settings.menuSfxVolume.getOrCompute().toFloat())
        this.value.addListener { v ->
            settings.menuSfxVolume.set(v.getOrCompute().toInt())
        }
    }

    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.audioSettings.title").use() }
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

        val (mixerPane, mixerControl) = createCycleOption(mixers, mixerHandler.recommendedMixer,
                { Localization.getVar("mainMenu.audioSettings.mixer").use() },
                percentageContent = 1f, twoRowsTall = true, itemToString = { it.mixerInfo.name })
        mixerPane.label.textAlign.set(TextAlign.CENTRE)
        mixerPane.label.renderAlign.set(Align.center)
        mixerControl.label.font.set(main.fontMainMenuRodin)
        mixerControl.label.setScaleXY(0.75f)
        mixerControl.currentItem.addListener { m ->
            setSoundSystemMixer(m.getOrCompute())
        }
        
        vbox.temporarilyDisableLayouts {
            vbox += mixerPane
            
            vbox += createSliderPane(gameplayVolSlider) { Localization.getVar("mainMenu.audioSettings.gameplayVol").use() }
            vbox += createSliderPane(menuMusicVolSlider) { Localization.getVar("mainMenu.audioSettings.menuMusicVol").use() }
            vbox += createSliderPane(menuSfxVolSlider) { Localization.getVar("mainMenu.audioSettings.menuSfxVol").use() }
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.audioSettings.resetLevels").use() }).apply {
                this.bounds.width.set(200f)
                this.setOnAction {
                    listOf(gameplayVolSlider, menuMusicVolSlider, menuSfxVolSlider).forEach { 
                        it.setValue(50f)
                    }
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.audioSettings.resetMixer").use() }).apply {
                this.bounds.width.set(200f)
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
        main.settings.mixer.set(newMixer.mixerInfo.name)
    }

}