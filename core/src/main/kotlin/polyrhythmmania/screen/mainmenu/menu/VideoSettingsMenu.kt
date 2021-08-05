package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.CheckBox
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.WindowSize
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.Settings


class VideoSettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    private val settings: Settings = menuCol.main.settings

    val resolutionCycle: CycleControl<WindowSize>
    val fullscreenCheck: CheckBox

    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.videoSettings.title").use() }
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

        val resolutionsList = PRMania.commonResolutions.toList()
        val (resolutionPane, resolutionCycle) = createCycleOption(resolutionsList, resolutionsList[1],
                { Localization.getVar("mainMenu.videoSettings.windowedResolution").use() })
        this.resolutionCycle = resolutionCycle

        val (fullscreenPane, fullscreenCheck) = createCheckboxOption({ Localization.getVar("mainMenu.videoSettings.fullscreen").use() })
        this.fullscreenCheck = fullscreenCheck

        vbox.temporarilyDisableLayouts {
            vbox += resolutionPane
            vbox += fullscreenPane
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.videoSettings.applySettings").use() }).apply {
                this.bounds.width.set(300f)
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