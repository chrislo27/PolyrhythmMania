package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.ui.Anchor
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.Slider
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.ui.PRManiaSkins
import kotlin.math.roundToInt


class StoryModeMusicVolumeWarningMenu(menuCol: MenuCollection, private val continueAction: () -> Unit) : StandardMenu(menuCol) {

    private val settings: Settings = menuCol.main.settings
    
    private val masterVolSlider: Slider = Slider().apply {
        this.bindWidthToParent(multiplier = 0.85f)
        this.minimum.set(0f)
        this.maximum.set(100f)
        this.tickUnit.set(5f)
        this.setValue(settings.masterVolumeSetting.getOrCompute().toFloat())
        this.value.addListener { v ->
            settings.masterVolumeSetting.set(v.getOrCompute().toInt())
        }
        this.tooltipElement.set(createTooltip { "${value.use().roundToInt()}" })
    }
    private val menuMusicVolSlider: Slider = Slider().apply {
        this.bindWidthToParent(multiplier = 0.85f)
        this.minimum.set(0f)
        this.maximum.set(100f)
        this.tickUnit.set(5f)
        this.setValue(settings.menuMusicVolumeSetting.getOrCompute().toFloat())
        this.value.addListener { v ->
            settings.menuMusicVolumeSetting.set(v.getOrCompute().toInt())
        }
        this.tooltipElement.set(createTooltip { "${value.use().roundToInt()}" })
    }
    
    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.storyModeMusicVolumeWarning.title").use() }
        this.contentPane.bounds.height.set(300f)
        this.deleteWhenPopped.set(true)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)

            this.vBar.unitIncrement.set(10f)
            this.vBar.blockIncrement.set(40f)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(4f, 0f, 2f, 2f))
            this.bounds.height.set(40f)
        }

        contentPane.addChild(scrollPane)
        contentPane.addChild(hbox)

        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
        }
        fun separator(): UIElement {
            return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                this.bounds.height.set(10f)
                this.margin.set(Insets(4f, 4f, 0f, 0f))
            }
        }
        vbox.temporarilyDisableLayouts {
            vbox += TextLabel(binding = { Localization.getVar("mainMenu.storyModeMusicVolumeWarning.desc").use() }).apply {
                this.markup.set(this@StoryModeMusicVolumeWarningMenu.markup)
                this.bounds.height.set(100f)
                this.renderAlign.set(Align.topLeft)
                this.doLineWrapping.set(true)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
            }
            vbox += separator()
            vbox += createSliderPane(masterVolSlider, percentageContent = 0.6f) { Localization.getVar("mainMenu.audioSettings.masterVol").use() }
            vbox += createSliderPane(menuMusicVolSlider, percentageContent = 0.6f) { Localization.getVar("mainMenu.audioSettings.menuMusicVol").use() }
        }
        
        vbox.sizeHeightToChildren(100f)
        scrollPane.setContent(vbox)

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.storyModeMusicVolumeWarning.continue").use() }).apply {
                this.bounds.width.set(300f)
                this.setOnAction {
                    continueAction()
                    settings.persist()
                }
            }
        }
    }
}