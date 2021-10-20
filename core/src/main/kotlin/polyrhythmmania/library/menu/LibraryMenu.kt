package polyrhythmmania.library.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import net.lingala.zip4j.ZipFile
import paintbox.Paintbox
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyBooleanVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.TinyFDWrapper
import paintbox.util.Version
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.grey
import paintbox.util.gdxutils.openFileExplorer
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.container.Container
import polyrhythmmania.container.LevelMetadata
import polyrhythmmania.container.manifest.ExportStatistics
import polyrhythmmania.container.manifest.LibraryRelevantData
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.Ranking
import polyrhythmmania.library.LevelEntry
import polyrhythmmania.library.score.GlobalScoreCache
import polyrhythmmania.library.score.LevelScoreAttempt
import polyrhythmmania.screen.mainmenu.menu.LoadSavedLevelMenu
import polyrhythmmania.screen.mainmenu.menu.MenuCollection
import polyrhythmmania.screen.mainmenu.menu.StandardMenu
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.ui.ScrollingTextLabelSkin
import polyrhythmmania.util.DecimalFormats
import polyrhythmmania.util.TempFileUtils
import polyrhythmmania.util.TimeUtils
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.thread


class LibraryMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private var workerThread: Thread? = null

    private val currentBanner: Var<Texture?> = Var(null) // Note: A Texture will not be disposed unless it is swapped out.
    private val toggleGroup: ToggleGroup = ToggleGroup()
    private val selectedLevelEntry: ReadOnlyVar<LevelEntry?> = Var {
        val at = toggleGroup.activeToggle.use()
        if (at != null && at.selectedState.useB()) {
            (at as? LibraryEntryButton)?.levelEntry
        } else null
    }
    private val sortFilter: Var<LibrarySortFilter> = Var(LibrarySortFilter.DEFAULT)
    private val levelList: Var<List<LevelEntryData>> = Var(emptyList())
    private val activeLevelList: Var<List<LevelEntryData>> = Var(emptyList())
    
    private val vbox: VBox
    private val contentPaneLeft: RectElement
    private val contentPaneRight: RectElement
    
    init {
        PRMania.DEFAULT_LEVELS_FOLDER // Invoke to mkdirs
        
        this.setSize(percentage = 0.975f)
        this.titleText.bind { Localization.getVar("mainMenu.library.title").use() }
        this.showLogo.set(false)
        this.contentPane.bounds.height.set(520f)
        this.contentPane.padding.set(Insets.ZERO)
        this.contentPane.color.set(Color(0f, 0f, 0f, 0f))
        
        contentPaneLeft = RectElement(grey).apply {
            Anchor.TopLeft.configure(this)
            contentPane += this
            this.bindWidthToParent(multiplier = 0.5f, adjust = 64f)
            this.padding.set(Insets(16f))
        }
        contentPaneRight = RectElement(grey).apply {
            Anchor.BottomRight.configure(this)
            contentPane += this
            this.bounds.height.bind { 
                // NOTE: the top part overflows its parent container, but the only element there is the level banner.
                (parent.use()?.bounds?.height?.useF() ?: 0f) + titleLabel.bounds.height.useF()
            }
            this.bindWidthToParent(multiplier = 0.5f, adjust = -100f)
            this.padding.set(Insets(0f))
            this.border.set(Insets(16f))
            this.borderStyle.set(SolidBorder(grey).also { border ->
                border.roundedCorners.set(true)
            })
        }

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)

            this.vBar.unitIncrement.set(48f)
            this.vBar.blockIncrement.set(48f * 4)
        }
        val leftBottomHbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }


        contentPaneLeft.addChild(scrollPane)
        contentPaneLeft.addChild(leftBottomHbox)

        // No levels label
        scrollPane.addChild(TextLabel(binding = {
            val ll = levelList.use()
            val active = activeLevelList.use()
            if (ll.isEmpty()) {
                Localization.getVar("mainMenu.library.noLevelsInFolder").use()
            } else if (active.isEmpty()) {
                Localization.getVar("mainMenu.library.noLevelsFiltered").use()
            } else ""
        }, font = main.fontMainMenuMain).apply {
            scrollPane.addChild(this) // Intentional, not part of content
            Anchor.Centre.configure(this, offsetX = -(scrollPane.barSize.get()))
            this.bindWidthToParent(adjust = -(64f + scrollPane.barSize.get()))
            this.bindHeightToParent(adjust = -(64f))
            this.doLineWrapping.set(true)
            this.renderAlign.set(Align.center)
            this.visible.bind {
                activeLevelList.use().isEmpty()
            }
        })
        
        val anyLevelSelected: ReadOnlyBooleanVar = BooleanVar {
            selectedLevelEntry.use() != null
        }
        // Select from the left! label
        contentPaneRight.addChild(TextLabel(binding = { Localization.getVar("mainMenu.library.selectFromLeft").use() }, font = main.fontMainMenuMain).apply {
            this.doLineWrapping.set(true)
            this.renderAlign.set(Align.center)
            this.visible.bind {
                !anyLevelSelected.useB()
            }
        })
        
        // Level details pane
        val levelDetailsPane = Pane().apply {
            val levelEntryModern: ReadOnlyVar<LevelEntry.Modern?> = Var.bind { selectedLevelEntry.use() as? LevelEntry.Modern }
            val levelEntryLegacy: ReadOnlyVar<LevelEntry.Legacy?> = Var.bind { selectedLevelEntry.use() as? LevelEntry.Legacy }
            this.visible.bind {
                selectedLevelEntry.use() != null
            }
            this.doClipping.set(true)
            
            val bannerRatio = 3.2f // 512 x 160
            val spacing = 2f
            this += ImageNode(binding = {
                val tex: Texture = currentBanner.use() ?: AssetRegistry["library_default_banner"]
                TextureRegion(tex)
            }, renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO).apply {
                Anchor.TopLeft.configure(this)
                this.bindHeightToSelfWidth(multiplier = 1f / bannerRatio)
            }
            val bottomBarSize = 40f
            // Bottom bar
            this += Pane().apply {
                Anchor.BottomLeft.configure(this)
                this.bounds.height.set(bottomBarSize)
                val hbox = HBox().apply {
                    Anchor.BottomLeft.configure(this)
                    this.spacing.set(8f)
                    this.padding.set(Insets(2f))
                    this.align.set(HBox.Align.CENTRE)
                }
                this += hbox
                hbox += createSmallButton(binding = { Localization.getVar("mainMenu.play.playAction").use() }).apply {
                    this.bounds.width.set(150f)
                    this.setOnAction {
                        val l = selectedLevelEntry.getOrCompute()
                        if (l != null) {
                            if (!l.file.exists()) {
                                startSearchThread() // Re-search the list. The level was deleted but this should be a very rare case.
                            } else {
                                val previousHighScore = GlobalScoreCache.scoreCache.getOrCompute().map[l.uuid]?.attempts?.maxOrNull()?.score ?: 0
                                val loadMenu = LoadSavedLevelMenu(menuCol, l.file, GlobalScoreCache.createConsumer(l.uuid),
                                        previousHighScore)
                                menuCol.addMenu(loadMenu)
                                menuCol.pushNextMenu(loadMenu)
                            }
                        }
                    }
                }
            }
            // Main content area
            this += VBox().apply {
                Anchor.BottomLeft.configure(this, offsetY = -bottomBarSize)
                val thisBounds = this.bounds
                thisBounds.height.bind {
                    @Suppress("SimpleRedundantLet")
                    (parent.use()?.let { p -> p.contentZone.height.useF() } ?: 0f) - (thisBounds.width.useF() * (1f / bannerRatio) + spacing * 2) - bottomBarSize
                }
                this.spacing.set(spacing)
                this.temporarilyDisableLayouts {
                    fun createRodinTooltip(binding: Var.Context.() -> String): Tooltip {
                        return Tooltip(binding = binding, font = main.fontMainMenuRodin).apply { 
                            setScaleXY(0.9f)
                        }
                    }
                    
                    // Song name/filename label
                    this += TextLabel(binding = {
                        selectedLevelEntry.use()?.getTitle() ?: ""
                    }, font = main.fontMainMenuRodin).apply { 
                        this.renderAlign.set(Align.center)
                        this.doXCompression.set(false)
                        this.skinID.set(PRManiaSkins.SCROLLING_TEXTLABEL)
                        this.bounds.height.set(34f)
                        this.margin.set(Insets(0f, 2f, 0f, 0f))
                        this.tooltipElement.set(createRodinTooltip { 
                            text.use()
                        })
                    }
                    // Level creator or [Legacy Level]
                    this += TextLabel(binding = {
                        levelEntryModern.use()?.levelMetadata?.levelCreator ?: Localization.getVar("mainMenu.library.legacyIndicator").use()
                    }, font = main.fontMainMenuRodin).apply { 
                        this.renderAlign.set(Align.center)
                        this.doXCompression.set(false)
                        this.skinID.set(PRManiaSkins.SCROLLING_TEXTLABEL)
                        this.bounds.height.set(30f)
                        this.margin.set(Insets(0f, 2f, 0f, 0f))
                        setScaleXY(0.9f)
                        this.tooltipElement.set(createRodinTooltip {
                            val t = text.use()
                            Localization.getVar("mainMenu.library.levelCreator", Var { listOf(t) }).use()
                        })
                    }
                    
                    val leftRatio = 0.6f
                    val rightRatio = 1f - leftRatio
                    val labelHeight = 24f
                    val dataHeight = 24f
                    val labelColor = Color().grey(0.2f, 1f)
                    // Left: Song Artist; Right: Duration 
                    this += Pane().apply {
                        this.bounds.height.set(labelHeight + dataHeight + 2f)
                        this.margin.set(Insets(0f, 2f, 0f, 0f))
//                        this.visible.bind { selectedLevelEntry.use() is LevelEntry.Modern }
                        
                        this += TextLabel(binding = { Localization.getVar("levelMetadata.songArtist").use() }, font = main.fontMainMenuThin).apply { 
                            Anchor.TopLeft.configure(this)
                            this.bounds.height.set(labelHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(adjust = -4f, multiplier = 0.5f)
                            this.renderAlign.set(Align.left)
                            this.textColor.set(labelColor)
                        }
                        this += TextLabel(binding = { Localization.getVar("mainMenu.library.duration").use() }, font = main.fontMainMenuThin).apply { 
                            Anchor.TopRight.configure(this)
                            this.bounds.height.set(labelHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(adjust = -4f, multiplier = 0.5f)
                            this.renderAlign.set(Align.right)
                            this.textColor.set(labelColor)
                        }
                        
                        this += TextLabel(binding = {
                            levelEntryModern.use()?.levelMetadata?.songArtist ?: Localization.getVar("mainMenu.library.levelMetadataNotAvailable").use()
                        }, font = main.fontMainMenuRodin).apply {
                            Anchor.BottomLeft.configure(this)
                            this.bounds.height.set(dataHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(multiplier = leftRatio, adjust = -4f)
                            this.renderAlign.set(Align.left)
                            this.tooltipElement.set(createRodinTooltip {
                                text.use()
                            })
                        }
                        this += TextLabel(binding = {
                            val l = levelEntryModern.use()
                            if (l != null) {
                                TimeUtils.convertMsToTimestamp(l.exportStatistics.durationSec * 1000, noMs = true)
                            } else Localization.getVar("mainMenu.library.levelMetadataNotAvailable").use()
                        }, font = main.fontMainMenuRodin).apply {
                            Anchor.BottomRight.configure(this)
                            this.bounds.height.set(dataHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(multiplier = rightRatio, adjust = -4f)
                            this.renderAlign.set(Align.right)
                        }
                    }
                    // Left: Album Name / Album Year / Album Name (Album Year); Right: BPM
                    this += Pane().apply {
                        this.bounds.height.set(labelHeight + dataHeight + 2f)
                        this.margin.set(Insets(0f, 2f, 0f, 0f))
                        this += TextLabel(binding = { Localization.getVar("levelMetadata.albumName.short").use() }, font = main.fontMainMenuThin).apply {
                            Anchor.TopLeft.configure(this)
                            this.bounds.height.set(labelHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(adjust = -4f, multiplier = 0.5f)
                            this.renderAlign.set(Align.left)
                            this.textColor.set(labelColor)
                        }
                        this += TextLabel(binding = { Localization.getVar("mainMenu.library.averageTempo").use() }, font = main.fontMainMenuThin).apply {
                            Anchor.TopRight.configure(this)
                            this.bounds.height.set(labelHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(adjust = -4f, multiplier = 0.5f)
                            this.renderAlign.set(Align.right)
                            this.textColor.set(labelColor)
                        }
                        
                        this += TextLabel(binding = {
                            levelEntryModern.use()?.levelMetadata?.getFullAlbumInfo()?.takeUnless { it.isBlank() } ?: Localization.getVar("mainMenu.library.levelMetadataNotAvailable").use()
                        }, font = main.fontMainMenuRodin).apply {
                            Anchor.BottomLeft.configure(this)
                            this.bounds.height.set(dataHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(multiplier = leftRatio, adjust = -4f)
                            this.renderAlign.set(Align.left)
                            this.tooltipElement.set(createRodinTooltip {
                                text.use()
                            })
                            
                            // Special case: this one scrolls.
                            this.skinID.set(PRManiaSkins.SCROLLING_TEXTLABEL)
                            this.doClipping.set(true)
                            this.doXCompression.set(false)
                            (this.skin.getOrCompute() as ScrollingTextLabelSkin).gapBetween.set(24f)
                        }
                        this += TextLabel(binding = {
                            val l = levelEntryModern.use()
                            if (l != null) {
                                Localization.getVar("editor.bpm", Var { 
                                    listOf(DecimalFormats["0.#"].format(l.exportStatistics.averageBPM))
                                }).use()
                            } else Localization.getVar("mainMenu.library.levelMetadataNotAvailable").use()
                        }, font = main.fontMainMenuRodin).apply {
                            Anchor.BottomRight.configure(this)
                            this.bounds.height.set(dataHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(multiplier = rightRatio, adjust = -4f)
                            this.renderAlign.set(Align.right)
                        }
                    }
                    // Left: Genre (may be blank); Right: Difficulty (may be blank)
                    this += Pane().apply {
                        this.bounds.height.set(labelHeight + dataHeight + 2f)
                        this.margin.set(Insets(0f, 2f, 0f, 0f))
                        this += TextLabel(binding = { Localization.getVar("levelMetadata.genre").use() }, font = main.fontMainMenuThin).apply {
                            Anchor.TopLeft.configure(this)
                            this.bounds.height.set(labelHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(adjust = -4f, multiplier = 0.5f)
                            this.renderAlign.set(Align.left)
                            this.textColor.set(labelColor)
                        }
                        this += TextLabel(binding = { Localization.getVar("levelMetadata.difficulty").use() }, font = main.fontMainMenuThin).apply {
                            Anchor.TopRight.configure(this)
                            this.bounds.height.set(labelHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(adjust = -4f, multiplier = 0.5f)
                            this.renderAlign.set(Align.right)
                            this.textColor.set(labelColor)
                        }
                        
                        this += TextLabel(binding = {
                            levelEntryModern.use()?.levelMetadata?.genre?.takeUnless { it.isBlank() } ?: Localization.getVar("mainMenu.library.levelMetadataNotAvailable").use()
                        }, font = main.fontMainMenuRodin).apply {
                            Anchor.BottomLeft.configure(this)
                            this.bounds.height.set(dataHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(multiplier = leftRatio, adjust = -4f)
                            this.renderAlign.set(Align.left)
                            this.tooltipElement.set(createRodinTooltip {
                                text.use()
                            })
                        }
                        this += TextLabel(binding = {
                            val l = levelEntryModern.use()
                            if (l != null && l.levelMetadata.difficulty > 0) {
                                "${l.levelMetadata.difficulty} / ${LevelMetadata.LIMIT_DIFFICULTY.last}"
                            } else Localization.getVar("mainMenu.library.levelMetadataNotAvailable").use()
                        }, font = main.fontMainMenuRodin).apply {
                            Anchor.BottomRight.configure(this)
                            this.bounds.height.set(dataHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(multiplier = rightRatio, adjust = -4f)
                            this.renderAlign.set(Align.right)
                        }
                    }
                    // Left: Genre (may be blank); Right: Difficulty (may be blank)
                    this += Pane().apply {
                        this.bounds.height.set(labelHeight + dataHeight + 2f)
                        this.margin.set(Insets(0f, 2f, 0f, 0f))
                        this += TextLabel(binding = { Localization.getVar("mainMenu.library.inputs").use() }, font = main.fontMainMenuThin).apply {
                            Anchor.TopLeft.configure(this)
                            this.bounds.height.set(labelHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(adjust = -4f, multiplier = 0.5f)
                            this.renderAlign.set(Align.left)
                            this.textColor.set(labelColor)
                        }
                        this += TextLabel(binding = { Localization.getVar("mainMenu.library.averageInputs").use() }, font = main.fontMainMenuThin).apply {
                            Anchor.TopRight.configure(this)
                            this.bounds.height.set(labelHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(adjust = -4f, multiplier = 0.5f)
                            this.renderAlign.set(Align.right)
                            this.textColor.set(labelColor)
                        }
                        
                        this += TextLabel(binding = {
                            val inputCount = levelEntryModern.use()?.exportStatistics?.inputCount
                            if (inputCount != null) DecimalFormats["0"].format(inputCount)
                            else Localization.getVar("mainMenu.library.levelMetadataNotAvailable").use()
                        }, font = main.fontMainMenuRodin).apply {
                            Anchor.BottomLeft.configure(this)
                            this.bounds.height.set(dataHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(multiplier = leftRatio, adjust = -4f)
                            this.renderAlign.set(Align.left)
                        }
                        this += TextLabel(binding = {
                            val inputsPerMin = levelEntryModern.use()?.exportStatistics?.averageInputsPerMinute
                            if (inputsPerMin != null)
                                Localization.getVar("mainMenu.library.averageInputs.value", Var {
                                    listOf(DecimalFormats.format("0.0", inputsPerMin))
                                }).use()
                            else Localization.getVar("mainMenu.library.levelMetadataNotAvailable").use()
                        }, font = main.fontMainMenuRodin).apply {
                            Anchor.BottomRight.configure(this)
                            this.bounds.height.set(dataHeight)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(multiplier = rightRatio, adjust = -4f)
                            this.renderAlign.set(Align.right)
                        }
                    }
                    
                    // Description scroll pane / high scores
                    val descExists = BooleanVar {
                        levelEntryModern.use()?.levelMetadata?.description?.takeUnless { it.isBlank() } != null
                    }
                    val showDesc = BooleanVar(true)
                    this += Pane().apply {
                        this.bounds.height.set(labelHeight)

                        this += Button(binding = {
                            Localization.getVar(if (showDesc.useB()) "mainMenu.library.switchToHighScores" else "mainMenu.library.switchToDesc").use()
                        }, font = main.fontMainMenuThin).apply {
                            Anchor.TopRight.configure(this)
                            this.setScaleXY(0.75f)
                            this.bindWidthToParent(adjust = -4f, multiplier = 0.55f)
                            this.setOnAction {
                                showDesc.invert()
                            }
                            this.visible.bind {
                                descExists.useB()
                            }
                        }
                        this += TextLabel(binding = {
                            Localization.getVar(if (descExists.useB() && showDesc.useB())
                                "levelMetadata.description" else "mainMenu.library.highScores").use()
                        }, font = main.fontMainMenuThin).apply {
                            Anchor.TopLeft.configure(this)
                            this.setScaleXY(0.8f)
                            this.bindWidthToParent(adjust = -4f, multiplier = 0.45f)
                            this.renderAlign.set(Align.left)
                            this.textColor.set(labelColor)
                        }
                    }
                    this += Pane().apply {
                        this.bounds.height.set(96f - labelHeight)

                        // Description
                        val descScrollPane = ScrollPane().apply {
                            this.visible.bind {
                                descExists.useB() && showDesc.useB()
                            }

                            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))
                            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
                            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)
                            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
                            this.vBar.skinID.set(scrollBarSkinID)
                            this.hBar.skinID.set(scrollBarSkinID)
                            this.vBar.unitIncrement.set(24f)
                            this.vBar.blockIncrement.set(24f)

                            this.setContent(TextLabel(binding = {
                                levelEntryModern.use()?.levelMetadata?.description ?: ""
                            }, font = main.fontMainMenuRodin).apply {
                                this.setScaleXY(0.85f)
                                this.textColor.set(Color().grey(0.25f))
                                this.doXCompression.set(false)
                                this.doLineWrapping.set(true)
                                this.internalTextBlock.addListener {
                                    this.resizeBoundsToContent(affectWidth = false)
                                }
                            })
                        }
                        this += descScrollPane
                        
                        // High scores
                        this += ScrollPane().apply ScrollPane@{
                            this.visible.bind {
                                !descScrollPane.visible.useB()
                            }

                            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))
                            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
                            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)
                            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
                            this.vBar.skinID.set(scrollBarSkinID)
                            this.hBar.skinID.set(scrollBarSkinID)
                            this.vBar.unitIncrement.set(24f)
                            this.vBar.blockIncrement.set(24f)

                            val noHighScoresLabel = TextLabel(binding = {
                                Localization.getVar("mainMenu.library.noHighScores").use()
                            }, font = main.fontMainMenuThin).apply {
                                this.textColor.set(Color().grey(0.25f))
                                this.bounds.height.set(60f)
                                this.renderAlign.set(Align.center)
                            }
                            val vbox = VBox().apply {
                                this.bounds.height.set(10f)
                            }
                            val highScoresListener: (levelEntry: LevelEntry?) -> Unit = { l ->
                                vbox.children.toList().forEach { c -> vbox.removeChild(c) }
                                if (l != null) {
                                    val scoreCache = GlobalScoreCache.scoreCache.getOrCompute()
                                    val levelScore = scoreCache.map[l.uuid]
                                    val attempts: List<LevelScoreAttempt>? = levelScore?.attempts?.sortedDescending()
                                    if (attempts == null || attempts.isEmpty()) {
                                        vbox += noHighScoresLabel
                                    } else {
                                        vbox.disableLayouts.set(true)
                                        val anySkillStar: Boolean = (l is LevelEntry.Modern && l.exportStatistics.hasSkillStar) || (attempts.any { it.skillStar })
                                        attempts.forEachIndexed { index, attempt ->
                                            vbox += HBox().apply {
                                                this.bounds.height.set(24f)
                                                this.spacing.set(4f)
                                                this.temporarilyDisableLayouts {
                                                    this += TextLabel(text = "${index + 1}.", font = main.fontMainMenuMain).apply {
                                                        this.bounds.width.set(36f)
                                                        this.margin.set(Insets(0f, 0f, 0f, 4f))
                                                        this.setScaleXY(0.85f)
                                                        this.textColor.set(Color().grey(0.25f))
                                                        this.renderAlign.set(Align.right)
                                                        this.tooltipElement.set(createTooltip {
                                                            val datetime = Instant.ofEpochMilli(attempt.playTime).atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault())
                                                            DateTimeFormatter.RFC_1123_DATE_TIME.format(datetime)
                                                        })
                                                    }
                                                    this += TextLabel(text = "${attempt.score}", font = main.fontMainMenuRodin).apply {
                                                        this.bounds.width.set(54f)
                                                        this.margin.set(Insets(0f, 0f, 0f, 4f))
                                                        this.textColor.set(Color().grey(0.25f))
                                                        this.renderAlign.set(Align.center)
                                                    }
                                                    this += ImageNode(AssetRegistry.get<PackedSheet>("results_ranking")[Ranking.getRanking(attempt.score).rankingIconID]).apply {
                                                        this.bounds.width.set(90f)
                                                    }
                                                    if (anySkillStar) {
                                                        this += ImageNode(AssetRegistry.get<PackedSheet>("tileset_ui")[if (attempt.skillStar) "skill_star" else "skill_star_grey"]).apply {
                                                            this.bindWidthToSelfHeight()
                                                        }
                                                    }
                                                    this += ImageNode(AssetRegistry.get<PackedSheet>("tileset_ui")[if (attempt.noMiss) "perfect" else "perfect_failed"]).apply {
                                                        this.bindWidthToSelfHeight()
                                                        this.visible.set(attempt.challenges.goingForPerfect)
                                                    }
                                                    if (attempt.challenges.tempoUp != 100) {
                                                        this += TextLabel(text = Localization.getValue(
                                                                if (attempt.challenges.tempoUp >= 100) "play.results.tempoUp"
                                                                else "play.results.tempoDown", "${attempt.challenges.tempoUp}"
                                                        ), font = main.fontMainMenuThin).apply {
                                                            this.bounds.width.set(146f)
                                                            this.setScaleXY(0.85f)
                                                            this.margin.set(Insets(0f, 0f, 0f, 4f))
                                                            this.textColor.set((if (attempt.challenges.tempoUp >= 100) Challenges.TEMPO_UP_COLOR else Challenges.TEMPO_DOWN_COLOR).cpy())
                                                            this.renderAlign.set(Align.left)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        vbox += TextLabel(binding = { Localization.getVar("mainMenu.library.playCount", Var {
                                            val playCount = GlobalScoreCache.scoreCache.use().map[l.uuid]?.playCount ?: 0
                                            listOf(playCount)
                                        }).use() }, font = main.fontMainMenuThin).apply {
                                            this.bounds.height.set(24f)
                                            this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.library.playCount.tooltip")))
                                            this.textColor.set(Color().grey(0.25f))
                                            this.setScaleXY(0.9f)
                                            this.renderAlign.set(Align.center)
                                        }
                                        vbox.disableLayouts.set(false)
                                    }
                                }
                                vbox.sizeHeightToChildren(10f)
                                this@ScrollPane.vBar.setValue(0f)
                                this@ScrollPane.setContent(vbox) // Forces refresh of bounds inside scroll pane
                            }
                            selectedLevelEntry.addListener {
                                Gdx.app.postRunnable {
                                    highScoresListener.invoke(it.getOrCompute())
                                }
                            }
                            GlobalScoreCache.scoreCache.addListener {
                                Gdx.app.postRunnable {
                                    highScoresListener.invoke(selectedLevelEntry.getOrCompute())
                                }
                            }
                            this.setContent(vbox)
                        }
                    }
                }
            }
        }
        contentPaneRight += levelDetailsPane

        val vbox = VBox().apply {
            this.spacing.set(0f)
            this.bounds.height.set(100f)
            this.margin.set(Insets(0f, 0f, 0f, 2f))
        }
        this.vbox = vbox

        scrollPane.setContent(vbox)

        leftBottomHbox.temporarilyDisableLayouts {
            leftBottomHbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            leftBottomHbox += createSmallButton(binding = {""}).apply {
                this.bindWidthToSelfHeight()
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["refresh"]))
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.library.refresh")))
                this.setOnAction {
                    startSearchThread()
                }
            }
            leftBottomHbox += createSmallButton(binding = {""}).apply {
                this.bindWidthToSelfHeight()
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["filter"]))
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.library.sortAndFilter")))
                this.setOnAction {
                    // TODO
                }
            }
            leftBottomHbox += createSmallButton(binding = {""}).apply {
                this.bindWidthToSelfHeight()
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_open"]))
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.library.openLibraryLocation.tooltip")))
                this.setOnAction {
                    Gdx.net.openFileExplorer(getLibraryFolder())
                }
            }
            leftBottomHbox += createSmallButton(binding = { Localization.getVar("mainMenu.library.changeLibraryLocation").use() }).apply {
                this.bounds.width.set(160f)
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.library.changeLibraryLocation.tooltip")))
                this.setOnAction {
                    val oldFolder = getLibraryFolder()
                    main.restoreForExternalDialog { completionCallback ->
                        thread(isDaemon = true) {
                            TinyFDWrapper.selectFolder(Localization.getValue("fileChooser.libraryFolderChange.title"), oldFolder) { file: File? ->
                                completionCallback()
                                if (file != null && file.isDirectory && file != oldFolder) {
                                    main.persistDirectory(PreferenceKeys.FILE_CHOOSER_LIBRARY_VIEW, file)
                                    startSearchThread()
                                }
                            }
                        }
                    }
                }
            }
            
        }
        
        selectedLevelEntry.addListener {
            val level = it.getOrCompute()
            if (level is LevelEntry.Modern && level.file.exists()) {
                Gdx.app.postRunnable {
                    unloadBanner()
                    try {
                        val zipFile = ZipFile(level.file)
                        val bannerHeader = zipFile.getFileHeader("banner.png")
                        if (bannerHeader != null) {
                            val tempFile = TempFileUtils.createTempFile("banner", ".png")
                            zipFile.getInputStream(bannerHeader).use { zipInputStream ->
                                val out = tempFile.outputStream()
                                zipInputStream.copyTo(out)
                            }

                            val tex = Texture(FileHandle(tempFile))
                            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                            tempFile.delete()

                            if (!Container.isBannerTextureWithinSize(tex)) {
                                Paintbox.LOGGER.warn("Ignoring banner texture because it is not the right size (${tex.width}x${tex.height})")
                                tex.disposeQuietly()
                            } else {
                                unloadBanner()
                                this.currentBanner.set(tex)
                            }
                        }
                    } catch (e: Exception) {
                        Paintbox.LOGGER.warn("Failed to load level banner for ${level.file.absolutePath}")
                        e.printStackTrace()
                    }
                }
            } else {
                Gdx.app.postRunnable {
                    unloadBanner()
                }
            }
        }
    }

    fun prepareShow(): LibraryMenu {
        toggleGroup.activeToggle.getOrCompute()?.selectedState?.set(false)
        startSearchThread()
        return this
    }
    
    private fun getLibraryFolder(): File {
        val prefName = PreferenceKeys.FILE_CHOOSER_LIBRARY_VIEW
        return main.attemptRememberDirectory(prefName)?.takeIf { it.isDirectory } ?: run {
            PRMania.DEFAULT_LEVELS_FOLDER.also { defFolder ->
                defFolder.mkdirs()
                main.persistDirectory(prefName, defFolder)
            }
        }
    }
    
    private fun createLibraryEntryButton(levelEntry: LevelEntry): LibraryEntryButton {
        return LibraryEntryButton(this, levelEntry).apply {
            this@LibraryMenu.toggleGroup.addToggle(this)
            this.padding.set(Insets(4f, 4f, 12f, 12f))
            this.bounds.height.set(48f)
            this.textAlign.set(TextAlign.LEFT)
            this.renderAlign.set(Align.left)
            this.setOnHoverStart(blipSoundListener)
        }
    }
    
    private fun addLevels(list: List<LevelEntry>) {
        val levelEntryData = list.map { LevelEntryData(it, createLibraryEntryButton(it)) }
        
        val newLeveLList = levelList.getOrCompute() + levelEntryData
        levelList.set(newLeveLList)
        
        filterAndSortLevelList()
        updateLevelListVbox()
    }
    
    private fun removeLevels(list: List<LevelEntryData>) {
        levelList.set(levelList.getOrCompute() - list)
        activeLevelList.set(activeLevelList.getOrCompute() - list)
        list.forEach { toggleGroup.removeToggle(it.button) }
        
        filterAndSortLevelList()
        updateLevelListVbox()
    }
    
    private fun filterAndSortLevelList() {
        val sf = this.sortFilter.getOrCompute()
        activeLevelList.set(sf.sortAndFilter(levelList.getOrCompute()))
    }
    
    private fun updateLevelListVbox() {
        val buttonBorder = Insets(1f, 0f, 0f, 0f)
        vbox.temporarilyDisableLayouts {
            vbox.children.forEach { vbox.removeChild(it) }
            activeLevelList.getOrCompute().forEachIndexed { index, it ->
                val button = it.button
                button.border.set(if (index == 0) Insets.ZERO else buttonBorder)
                vbox.addChild(button)
            }
        }
        vbox.sizeHeightToChildren(100f)
    }
    
    fun interruptSearchThread() {
        try {
            synchronized(this) {
                this.workerThread?.interrupt()
                this.workerThread = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun startSearchThread(): Thread {
        interruptSearchThread()
        
        removeLevels(levelList.getOrCompute().toList())
        
        val searchFolder = getLibraryFolder()
        val thread = thread(start = false, isDaemon = true, name = "Library Search") {
            Paintbox.LOGGER.info("Starting Library search in ${searchFolder.absolutePath}")
            val startNano = System.nanoTime()
            try {
                val potentialFiles = searchFolder.listFiles { file: File ->
                    val lowerName = file.name.lowercase(Locale.ROOT)
                    file.extension.lowercase(Locale.ROOT) == Container.LEVEL_FILE_EXTENSION 
                            && !lowerName.endsWith(".autosave.${Container.LEVEL_FILE_EXTENSION}")
                }?.toList() ?: emptyList()
                Paintbox.LOGGER.info("[Library Search] Possible files found: ${potentialFiles.size}")
                
                var lastUIPushTime = 0L
                val entriesToAdd = mutableListOf<LevelEntry>()
                
                fun pushEntriesToUI() {
                    if (entriesToAdd.isNotEmpty()) {
                        val copy = entriesToAdd.toList()
                        entriesToAdd.clear()
                        Gdx.app.postRunnable { 
                            addLevels(copy)
                        }
                        lastUIPushTime = System.currentTimeMillis()
                    }
                }
                
                var levelsAdded = 0
                for (file: File in potentialFiles) {
                    try {
                        val levelEntry: LevelEntry = loadLevelEntry(file) ?: continue
                        entriesToAdd += levelEntry
                        levelsAdded++
                        
                        if (System.currentTimeMillis() - lastUIPushTime >= 100L) {
                            pushEntriesToUI()
                        }
                    } catch (e: Exception) {
                        Paintbox.LOGGER.warn("Exception when scanning level in library: ${file.absolutePath}")
                        e.printStackTrace()
                    }
                }
                
                pushEntriesToUI()
                Paintbox.LOGGER.info("[Library Search] Levels read: $levelsAdded (took ${(System.nanoTime() - startNano) / 1_000_000f} ms)")
            } catch (ignored: InterruptedException) {
            } catch (e: Exception) {
                Paintbox.LOGGER.error("Exception when searching for files in library directory ${searchFolder.absolutePath}")
                e.printStackTrace()
            }
        }
        synchronized(this) {
            this.workerThread = thread
        }
        thread.start()
        return thread
    }

    private fun unloadBanner() {
        val ct = this.currentBanner.getOrCompute()
        if (ct != null) {
            this.currentBanner.set(null)
            ct.disposeQuietly()
        }
    }
    
    private fun loadLevelEntry(file: File): LevelEntry? {
        val zipFile = ZipFile(file)
        val json: JsonObject
        zipFile.getInputStream(zipFile.getFileHeader("manifest.json")).use { zipInputStream ->
            val reader = zipInputStream.reader()
            json = Json.parse(reader).asObject()
        }

        val libraryRelevantDataLoad = LibraryRelevantData.fromManifestJson(json, file.lastModified())
        val libraryRelevantData: LibraryRelevantData = libraryRelevantDataLoad.first
        
        val containerVersion: Int = libraryRelevantData.containerVersion
        val programVersion: Version = libraryRelevantData.programVersion
        val uuid: UUID? = libraryRelevantData.levelUUID
        val levelMetadata: LevelMetadata? = libraryRelevantData.levelMetadata
        val exportStatistics: ExportStatistics? = libraryRelevantData.exportStatistics
        
        return if (uuid != null && levelMetadata != null && exportStatistics != null) {
            if (libraryRelevantData.isAutosave || libraryRelevantData.isProject) return null
            LevelEntry.Modern(libraryRelevantData.levelUUID, file, containerVersion, programVersion, levelMetadata, exportStatistics)
        } else {
            LevelEntry.Legacy(file, containerVersion, programVersion)
        }
    }
    
}