package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import paintbox.binding.Var
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.CheckBox
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.Slider
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.WindowSize
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.Settings
import polyrhythmmania.ui.PRManiaSkins
import kotlin.math.roundToInt


class VideoSettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    private val settings: Settings = menuCol.main.settings

    val resolutionCycle: CycleControl<WindowSize>
    val fullscreenCheck: CheckBox
    private val minimumMaxFps: Int = 30
    private val maximumMaxFps: Int = 360
    private val stepMaxFps: Int = 10
    private val unlimitedMaxFps: Int = maximumMaxFps + stepMaxFps
    val maxFramerateSlider: Slider = Slider().apply {
        this.bindWidthToParent(multiplier = 0.85f)
        this.minimum.set(minimumMaxFps.toFloat())
        this.maximum.set(unlimitedMaxFps.toFloat())
        this.tickUnit.set(stepMaxFps.toFloat())
        this.setValue((settings.maxFramerate.getOrCompute().takeUnless { it <= 0f }?.coerceAtMost(maximumMaxFps) ?: unlimitedMaxFps).toFloat())
        this.value.addListener { v ->
            val newValue = v.getOrCompute().toInt()
            val maxFps = if (newValue > maximumMaxFps) 0 else newValue
            settings.maxFramerate.set(newValue)
            val gr = Gdx.graphics
            gr.setForegroundFPS(maxFps)
        }
        this.tooltipElement.set(createTooltip {
            val fps = value.useF().roundToInt()
            if (fps > maximumMaxFps) Localization.getVar("mainMenu.videoSettings.maxFramerate.unlimited").use() else "$fps"
        })
    }

    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.videoSettings.title").use() }
        this.contentPane.bounds.height.set(300f)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

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
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }

        contentPane.addChild(scrollPane)
        contentPane.addChild(hbox)
        
        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
        }

        val resolutionsList = PRMania.commonResolutions.toList()
        val (resolutionPane, resolutionCycle) = createCycleOption(resolutionsList, resolutionsList[1],
                { Localization.getVar("mainMenu.videoSettings.windowedResolution").use() })
        this.resolutionCycle = resolutionCycle

        val (fullscreenPane, fullscreenCheck) = createCheckboxOption({ Localization.getVar("mainMenu.videoSettings.fullscreen").use() })
        this.fullscreenCheck = fullscreenCheck
        

        vbox.temporarilyDisableLayouts {
            vbox += resolutionPane
            vbox += fullscreenPane
            
            val (vsyncPane, vsyncCheck) = createCheckboxOption({ Localization.getVar("mainMenu.videoSettings.vsync").use() })
            vsyncCheck.selectedState.set(main.settings.vsyncEnabled.getOrCompute())
            vsyncCheck.onCheckChanged = {
                main.settings.vsyncEnabled.set(it)
                Gdx.graphics.setVSync(it)
            }
            vbox += vsyncPane
            val unlimitedFpsText = Localization.getVar("mainMenu.videoSettings.maxFramerate.unlimited")
            val maxFpsText = Localization.getVar("mainMenu.videoSettings.maxFramerate", Var { 
                val fps = maxFramerateSlider.value.useF().roundToInt()
                listOf(if (fps > maximumMaxFps) unlimitedFpsText.use() else "$fps")
            })
            vbox += createSliderPane(maxFramerateSlider, percentageContent = 0.6f) { maxFpsText.use() }
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.videoSettings.applySettings").use() }).apply {
                this.bounds.width.set(280f)
                this.setOnAction {
                    val graphics = Gdx.graphics
                    val useFullscreen = fullscreenCheck.checkedState.getOrCompute()
                    val res = resolutionCycle.currentItem.getOrCompute()
                    if (useFullscreen) {
                        graphics.setFullscreenMode(graphics.displayMode)
                    } else {
                        graphics.setWindowedMode(res.width, res.height)
                    }
                    
                    settings.fullscreen.set(useFullscreen)
                    settings.windowedResolution.set(res)
                }
            }
        }
        vbox.sizeHeightToChildren(100f)
        scrollPane.setContent(vbox)
        
        hbox += createSmallButton(binding = { Localization.getVar("common.reset").use() }).apply {
            this.bounds.width.set(80f)
            this.setOnAction {
                val graphics = Gdx.graphics
                val defaultWindowSize = PRMania.DEFAULT_SIZE
                fullscreenCheck.checkedState.set(false)
                val resList = resolutionCycle.list
                resolutionCycle.currentItem.set(resList.first { it == defaultWindowSize })
                
                graphics.setWindowedMode(defaultWindowSize.width, defaultWindowSize.height)

                settings.fullscreen.set(false)
                settings.windowedResolution.set(defaultWindowSize)
            }
        }
    }

    fun prepareShow() {
        val resList = resolutionCycle.list
        val savedWidth = settings.windowedResolution.getOrCompute().width
        val closestIndex = resList.binarySearch { it.width.compareTo(savedWidth) }
        if (closestIndex >= 0) {
            // Found exact match
            resolutionCycle.currentItem.set(resList[closestIndex])
        } else {
            // Closest index is "inverted insertion index" (-insertionPoint - 1)
            resolutionCycle.currentItem.set(resList[(-(closestIndex + 1)).coerceIn(0, resList.size - 1)])
        }
        fullscreenCheck.checkedState.set(Gdx.graphics.isFullscreen)
    }

}