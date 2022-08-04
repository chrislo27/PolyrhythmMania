package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import paintbox.ui.Anchor
import paintbox.ui.StringConverter
import paintbox.ui.area.Insets
import paintbox.ui.control.CheckBox
import paintbox.ui.control.ComboBox
import paintbox.ui.control.ScrollPane
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.WindowSize
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.Settings
import polyrhythmmania.ui.PRManiaSkins


class VideoSettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    companion object {
        val FRAMERATES: List<Int> = listOf(
                30,
                60,
                75,
                120,
                144,
                240,
                360,
                0,
        )
    }

    private val settings: Settings = menuCol.main.settings

    val resolutionCombobox: ComboBox<WindowSize>
    val fullscreenCheck: CheckBox

    init {
        this.setSize(MMMenu.WIDTH_MID)
        this.titleText.bind { Localization.getVar("mainMenu.videoSettings.title").use() }
        this.contentPane.bounds.height.set(300f)

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

        val resolutionsList = PRMania.commonResolutions.toList()
        val (resolutionPane, resolutionCombobox) = createComboboxOption(resolutionsList, resolutionsList[1],
                { Localization.getVar("mainMenu.videoSettings.windowedResolution").use() })
        this.resolutionCombobox = resolutionCombobox

        val (fullscreenPane, fullscreenCheck) = createCheckboxOption({ Localization.getVar("mainMenu.videoSettings.fullscreen").use() })
        this.fullscreenCheck = fullscreenCheck
        this.fullscreenCheck.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.videoSettings.fullscreen.tooltip")))
        

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
            val (fpsPane, fpsCombobox) = createComboboxOption(FRAMERATES, settings.maxFramerate.getOrCompute(),
                    { Localization.getVar("mainMenu.videoSettings.maxFramerate").use() })
            vbox += fpsPane
            fpsCombobox.itemStringConverter.bind {
                return@bind StringConverter { fps: Int ->
                    if (fps <= 0) unlimitedFpsText.use() else "$fps"
                }
            }
            fpsCombobox.selectedItem.addListener { v ->
                val newValue = v.getOrCompute()
                val maxFps = if (newValue <= 0) 0 else newValue
                settings.maxFramerate.set(newValue)
                val gr = Gdx.graphics
                gr.setForegroundFPS(maxFps)
            }
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
                    val useFullscreen = fullscreenCheck.checkedState.get()
                    val res = resolutionCombobox.selectedItem.getOrCompute()
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
                val resList = resolutionCombobox.items.getOrCompute()
                resolutionCombobox.selectedItem.set(resList.first { it == defaultWindowSize })
                
                graphics.setWindowedMode(defaultWindowSize.width, defaultWindowSize.height)

                settings.fullscreen.set(false)
                settings.windowedResolution.set(defaultWindowSize)
            }
        }
    }

    fun prepareShow() {
        val resList = resolutionCombobox.items.getOrCompute()
        val savedWidth = settings.windowedResolution.getOrCompute().width
        val closestIndex = resList.binarySearch { it.width.compareTo(savedWidth) }
        if (closestIndex >= 0) {
            // Found exact match
            resolutionCombobox.selectedItem.set(resList[closestIndex])
        } else {
            // Closest index is "inverted insertion index" (-insertionPoint - 1)
            resolutionCombobox.selectedItem.set(resList[(-(closestIndex + 1)).coerceIn(0, resList.size - 1)])
        }
        fullscreenCheck.checkedState.set(Gdx.graphics.isFullscreen)
    }

}