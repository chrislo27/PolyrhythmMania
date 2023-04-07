package polyrhythmmania.storymode.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.binding.toConstVar
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.ui.SceneRoot
import paintbox.ui.Tooltip
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.ComboBox
import paintbox.ui.element.RectElement
import paintbox.ui.layout.ColumnarPane
import paintbox.ui.layout.ColumnarVBox
import paintbox.ui.layout.HBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.editor.EditorScreen
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.EditorSpecialParams
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.StorySession
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.contract.Contracts
import polyrhythmmania.storymode.contract.JingleType
import polyrhythmmania.storymode.contract.Requester
import polyrhythmmania.storymode.gamemode.boss.StoryBossGameMode
import polyrhythmmania.storymode.inbox.IContractDoc.ContractSubtype
import polyrhythmmania.storymode.inbox.InboxDB
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItems
import polyrhythmmania.storymode.inbox.progression.Progression
import polyrhythmmania.storymode.inbox.progression.UnlockStage
import polyrhythmmania.storymode.inbox.progression.UnlockStageChecker
import polyrhythmmania.storymode.screen.StoryLoadingScreen
import polyrhythmmania.storymode.screen.StoryTitleScreen
import polyrhythmmania.storymode.screen.cutscene.PostBossCutsceneScreen
import polyrhythmmania.storymode.test.gamemode.*
import java.util.*


class TestStoryGimmickDebugScreen(main: PRManiaGame, val storySession: StorySession) : PRManiaScreen(main) {

    val batch: SpriteBatch = main.batch
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val processor: InputProcessor = sceneRoot.inputSystem


    init {
        sceneRoot += RectElement(PRManiaColors.debugColor).apply {
            this.padding.set(Insets(24f, 10f, 12f, 12f))
            val columns = ColumnarVBox(2, false).apply { 
                this.spacing.set(16f)
            }
            this += columns
            
            fun separator(): UIElement {
                return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                    this.bounds.height.set(6f)
                    this.margin.set(Insets(2f, 2f, 0f, 0f))
                }
            }
            
            columns[0].apply {
                this.spacing.set(4f)
                this.temporarilyDisableLayouts {
                    this += Button("Back to Main Menu").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_deselect"))

                            val doAfterUnload: () -> Unit = {
                                val mainMenu = main.mainMenuScreen.prepareShow(doFlipAnimation = true)

                                main.screen = TransitionScreen(main, main.screen, mainMenu, FadeToOpaque(0.125f, Color.BLACK), null)
                            }
                            storySession.musicHandler.fadeOutAndDispose(0.375f)
                            main.screen = TransitionScreen(main, main.screen, storySession.createExitLoadingScreen(main, doAfterUnload),
                                    FadeToOpaque(0.25f, Color.BLACK), FadeToTransparent(0.125f, Color.BLACK))
                        }
                    }
                    this += separator()
                    this += Button("Story Mode Editor").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            Gdx.app.postRunnable {
                                storySession.musicHandler.fadeOut(0f)
                                val editorScreen = EditorScreen(main, EnumSet.of(EditorSpecialFlags.STORY_MODE), EditorSpecialParams(storySession))
                                main.screen = TransitionScreen(main, main.screen, editorScreen,
                                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    this += separator()
                    this += Button("Story Mode title screen/file select").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            Gdx.app.postRunnable {
                                val titleScreen = StoryTitleScreen(main, storySession)
                                main.screen = TransitionScreen(main, main.screen, titleScreen,
                                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    this += separator()
                    this += Button("Debug \"desktop\" screen with main inbox items (all unlocked)").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            val inboxItems: InboxItems = InboxDB()
                            val progression = Progression(listOf(
                                    UnlockStage("all", UnlockStageChecker.alwaysUnlocked(), inboxItems.items.map { it.id })
                            ))

                            Gdx.app.postRunnable {
                                val titleScreen = TestStoryDesktopScreen(main, storySession, this@TestStoryGimmickDebugScreen, inboxItems, progression)
                                main.screen = TransitionScreen(main, main.screen, titleScreen,
                                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    this += Button("Debug \"desktop\" screen with main inbox items (progression)").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            val inboxItems = InboxDB()
                            val progression = inboxItems.progression

                            Gdx.app.postRunnable {
                                val titleScreen = TestStoryDesktopScreen(main, storySession, this@TestStoryGimmickDebugScreen, inboxItems, progression)
                                main.screen = TransitionScreen(main, main.screen, titleScreen,
                                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    this += separator()
                    this += Button("Debug \"all inbox items\" screen (old UI)").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            Gdx.app.postRunnable {
                                val titleScreen = TestStoryAllInboxItemsScreen(main, storySession, this@TestStoryGimmickDebugScreen)
                                main.screen = TransitionScreen(main, main.screen, titleScreen,
                                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    this += Button("Debug \"all inbox items\" screen (new desktop UI) (all unlocked)").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            Gdx.app.postRunnable {
                                val inboxItems = DebugAllInboxItemsDB
                                val titleScreen = TestStoryDesktopScreen(main, storySession, this@TestStoryGimmickDebugScreen, inboxItems,
                                        Progression(inboxItems.items.map {
                                            UnlockStage.singleItem(it.id, UnlockStageChecker.alwaysUnlocked())
                                        }))
                                main.screen = TransitionScreen(main, main.screen, titleScreen,
                                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    this += Button("Debug \"all inbox items\" screen (new desktop UI) (progression)").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            Gdx.app.postRunnable {
                                val inboxItems = DebugAllInboxItemsDB
                                val titleScreen = TestStoryDesktopScreen(main, storySession, this@TestStoryGimmickDebugScreen, inboxItems, Progression.debugItemsInOrder(inboxItems))
                                main.screen = TransitionScreen(main, main.screen, titleScreen,
                                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    this += separator()
                    this += Button("Debug \"progression overview\" screen").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            Gdx.app.postRunnable {
                                val titleScreen = TestStoryProgressionOverviewScreen(main, storySession, this@TestStoryGimmickDebugScreen)
                                main.screen = TransitionScreen(main, main.screen, titleScreen,
                                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    class ProgressionTest(val name: String, val inboxProgressionFactory: () -> Pair<InboxItems, Progression>)
                    val progressions: List<ProgressionTest> = listOf(
                            ProgressionTest("Complex progression logic") {
                                val inboxItems = InboxItems(listOf(
                                        InboxItem.Debug("debug0", "1st item", InboxItem.Debug.DebugSubtype.PROGRESSION_ADVANCER, "This item is always unlocked to start. This will unlock the 2nd item once COMPLETED"),
                                        InboxItem.Debug("debug1", "2nd item", InboxItem.Debug.DebugSubtype.PROGRESSION_ADVANCER, "This will unlock both the 3rd and 4th items once COMPLETED"),
                                        InboxItem.Debug("debug2a", "3rd item", InboxItem.Debug.DebugSubtype.PROGRESSION_ADVANCER, "This unlocks the 5th item once this and 4th item are COMPLETED"),
                                        InboxItem.Debug("debug2b", "4th item", InboxItem.Debug.DebugSubtype.PROGRESSION_ADVANCER, "This unlocks the 5th item once this and 3rd item are COMPLETED"),
                                        InboxItem.Debug("debug3", "5th item", InboxItem.Debug.DebugSubtype.PROGRESSION_ADVANCER),
                                        InboxItem.ContractDoc(Contract("debugcontract_1", StoryL10N.getVar("test.name"), StoryL10N.getVar("test.listingName"), StoryL10N.getVar("test.desc"), StoryL10N.getVar("test.tagline"), Requester("test"), JingleType.GBA, null, 60, gamemodeFactory = Contracts["fillbots"].gamemodeFactory)),
                                        InboxItem.ContractDoc(Contract("debugcontract_2", StoryL10N.getVar("test.name"), StoryL10N.getVar("test.listingName"), StoryL10N.getVar("test.desc"), StoryL10N.getVar("test.tagline"), Requester("test"), JingleType.GBA, null, 60, gamemodeFactory = Contracts["fillbots"].gamemodeFactory)),
                                ))
                                val progression = Progression(listOf(
                                        UnlockStage.singleItem("debug0", UnlockStageChecker.alwaysUnlocked(), stageID = "stage0"),
                                        UnlockStage.singleItem("debug1", UnlockStageChecker.stageToBeCompleted("stage0"), stageID = "stage1"),
                                        UnlockStage("stage2", UnlockStageChecker.stageToBeCompleted("stage1"), listOf("debug2a", "debug2b")),
                                        UnlockStage.singleItem("debug3", UnlockStageChecker.stageToBeCompleted("stage2"), stageID = "stage3"),
                                        UnlockStage.singleItem("contract_debugcontract_1", UnlockStageChecker.alwaysUnlocked(), stageID = "stage_debugcontract_1"),
                                        UnlockStage.singleItem("contract_debugcontract_2", UnlockStageChecker.stageToBeCompleted("stage_debugcontract_1"), stageID = "stage_debugcontract_2"),
                                ))
                                Pair(inboxItems, progression)
                            },
                            ProgressionTest("Straight-down progression logic") {
                                val inboxItems = InboxItems((0 until 30).map { i ->
                                    InboxItem.Debug("debug$i", "item #$i", InboxItem.Debug.DebugSubtype.PROGRESSION_ADVANCER, "no desc")
                                })
                                val progression = Progression.debugItemsInOrder(inboxItems)
                                Pair(inboxItems, progression)
                            },
                            ProgressionTest("Zig-zag progression logic") {
                                val inboxItems = InboxItems((0 until 20).map { i ->
                                    InboxItem.Debug("debug$i", "item #$i", InboxItem.Debug.DebugSubtype.PROGRESSION_ADVANCER, "no desc")
                                })
//                                val progression = Progression(inboxItems.items.subList(0, 15).map {
//                                    UnlockStage.singleItem(it.id, UnlockStageChecker.alwaysUnlocked())
//                                } + (0 until inboxItems.items.size / 2).map { i ->
//                                    UnlockStage.singleItem(inboxItems.items[30 - i - 1].id, UnlockStageChecker.stageToBeCompleted(inboxItems.items[i].id))
//                                })
                                val progression = Progression(listOf(
                                        UnlockStage.singleItem("debug0", UnlockStageChecker.alwaysUnlocked()),

                                        UnlockStage.singleItem("debug19", UnlockStageChecker.stageToBeCompleted("debug0")),
                                        UnlockStage.singleItem("debug1", UnlockStageChecker.stageToBeCompleted("debug19")),
                                        UnlockStage.singleItem("debug18", UnlockStageChecker.stageToBeCompleted("debug1")),
                                        UnlockStage.singleItem("debug2", UnlockStageChecker.stageToBeCompleted("debug18")),
                                        UnlockStage.singleItem("debug17", UnlockStageChecker.stageToBeCompleted("debug2")),
                                        UnlockStage.singleItem("debug3", UnlockStageChecker.stageToBeCompleted("debug17")),
                                        UnlockStage.singleItem("debug16", UnlockStageChecker.stageToBeCompleted("debug3")),
                                        UnlockStage.singleItem("debug4", UnlockStageChecker.stageToBeCompleted("debug16")),
                                        UnlockStage.singleItem("debug15", UnlockStageChecker.stageToBeCompleted("debug4")),
                                        UnlockStage.singleItem("debug5", UnlockStageChecker.stageToBeCompleted("debug15")),
                                        UnlockStage.singleItem("debug14", UnlockStageChecker.stageToBeCompleted("debug5")),
                                        UnlockStage.singleItem("debug6", UnlockStageChecker.stageToBeCompleted("debug14")),
                                        UnlockStage.singleItem("debug13", UnlockStageChecker.stageToBeCompleted("debug6")),
                                        UnlockStage.singleItem("debug7", UnlockStageChecker.stageToBeCompleted("debug13")),
                                        UnlockStage.singleItem("debug12", UnlockStageChecker.stageToBeCompleted("debug7")),
                                        UnlockStage.singleItem("debug8", UnlockStageChecker.stageToBeCompleted("debug12")),
                                        UnlockStage.singleItem("debug11", UnlockStageChecker.stageToBeCompleted("debug8")),
                                        UnlockStage.singleItem("debug9", UnlockStageChecker.stageToBeCompleted("debug11")),
                                        UnlockStage.singleItem("debug10", UnlockStageChecker.stageToBeCompleted("debug9")),
                                ))
                                Pair(inboxItems, progression)
                            }
                    )
                    this += HBox().apply {
                        this.bounds.height.set(32f)
                        this.spacing.set(8f)
                        val combobox = ComboBox(progressions, progressions.first()).apply {
                            this.bindWidthToParent(multiplier = 0.7f)
                            this.itemStringConverter.set { it.name }
                        }
                        this += combobox
                        this += Button("Open Selected").apply {
                            this.bindWidthToParent(multiplier = 0.3f, adjust = -8f)
                            this.setOnAction {
                                val (inboxItems, progression) = combobox.selectedItem.getOrCompute().inboxProgressionFactory()
                                Gdx.app.postRunnable {
                                    val titleScreen = TestStoryDesktopScreen(main, storySession, this@TestStoryGimmickDebugScreen, inboxItems, progression)
                                    main.screen = TransitionScreen(main, main.screen, titleScreen,
                                            FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                                }
                            }
                        }
                    }
                    this += separator()
                    this += Button("Debug \"desktop\" screen background only").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            Gdx.app.postRunnable {
                                val titleScreen = TestStoryDesktopBgScreen(main, this@TestStoryGimmickDebugScreen)
                                main.screen = TransitionScreen(main, main.screen, titleScreen,
                                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    this += Button("Debug story mode desktop music stems").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            Gdx.app.postRunnable {
                                val titleScreen = TestStoryMusicScreen(main, this@TestStoryGimmickDebugScreen)
                                main.screen = TransitionScreen(main, main.screen, titleScreen,
                                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    this += Button("Debug \"desktop\" screen to demo inbox item types").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            val inboxItems = InboxItems(listOf(
                                    InboxItem.ContractDoc(Contract("debugcontract_1", StoryL10N.getVar("test.name"), StoryL10N.getVar("test.listingName"), StoryL10N.getVar("test.desc"), StoryL10N.getVar("test.tagline"), Requester("test"), JingleType.GBA, null, 60, gamemodeFactory = Contracts["fillbots"].gamemodeFactory)),
                                    InboxItem.ContractDoc(Contract("debugcontract_2", "TRAINING-099".toConstVar(), StoryL10N.getVar("test.listingName"), StoryL10N.getVar("test.desc"), StoryL10N.getVar("test.tagline"), Requester("test"), JingleType.GBA, null, 60, gamemodeFactory = Contracts["fillbots"].gamemodeFactory), subtype = ContractSubtype.TRAINING),
                                    InboxItem.Memo("test_memo", true, true)
                            ))
                            val progression = Progression(listOf(
                                    UnlockStage("all", UnlockStageChecker.alwaysUnlocked(), inboxItems.items.map { it.id })
                            ))

                            Gdx.app.postRunnable {
                                val titleScreen = TestStoryDesktopScreen(main, storySession, this@TestStoryGimmickDebugScreen, inboxItems, progression)
                                main.screen = TransitionScreen(main, main.screen, titleScreen,
                                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    this += separator()
                    class GamemodeTest(val name: String, val gamemodeFactory: () -> TestStoryGameMode)
                    val gamemodes: List<GamemodeTest> = listOf(
                            GamemodeTest("Polyrhythm 2 no changes") { object : TestStoryGameMode(main) {} },
                            GamemodeTest("Aces Only PR2") { TestStoryAcesOnlyGameMode(main) },
                            GamemodeTest("No Barelies PR2") { TestStoryNoBarelyGameMode(main) },
                            GamemodeTest("Continuous (8-ball) PR2") { TestStory8BallGameMode(main) },
                            GamemodeTest("PR2 with lives") { TestStoryLivesGameMode(main) },
                            GamemodeTest("All Defective Rods PR2") { TestStoryDefectiveGameMode(main) },
                    )
                    this += HBox().apply { 
                        this.bounds.height.set(32f)
                        this.spacing.set(8f)
                        val combobox = ComboBox(gamemodes, gamemodes.first()).apply { 
                            this.bindWidthToParent(multiplier = 0.7f)
                            this.itemStringConverter.set { it.name }
                        }
                        this += combobox
                        this += Button("Play Selected").apply {
                            this.bindWidthToParent(multiplier = 0.3f, adjust = -8f)
                            this.setOnAction {
                                enterGimmickGameMode(combobox.selectedItem.getOrCompute().gamemodeFactory())
                            }
                        }
                    }
                    this += separator()
                    this += Button("Endless UI debug").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            enterGimmickGameMode(TestStoryNoOpEndlessUIGameMode(main))
                        }
                        this.tooltipElement.set(Tooltip("C - increment score\nShift+C - reset score to 0\nL - decrement lives\nShift+L - increment lives\nHold Ctrl w/ (Shift+)L - change max lives"))
                    }
                    this += Button("Lives UI debug").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            enterGimmickGameMode(TestStoryNoOpLivesUIGameMode(main))
                        }
                        this.tooltipElement.set(Tooltip("L - increment max lives\nShift+L - decrement max lives\nF - lose a life\nShift+F - gain a life"))
                    }
                    this += Button("Boss UI debug").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            enterGimmickGameMode(TestStoryNoOpBossUIGameMode(main))
                        }
                        this.tooltipElement.set(Tooltip("B - boss takes 2 damage\nShift+B - boss takes 1 damage\nP - player takes damage\nR - Reset damages"))
                    }
                }
            }
            columns[1].apply {
                this.spacing.set(4f)
                this.temporarilyDisableLayouts {
                    this += Button("Boss fight from beginning").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            enterGameMode(StoryBossGameMode.getFactory())
                        }
                    }
                    this += separator()
                    this += ColumnarPane(2, useRows = false).apply {
                        this.spacing.set(8f)
                        this.bounds.height.set(32f)
                        listOf(
                            StoryBossGameMode.DebugPhase.A1,
                            StoryBossGameMode.DebugPhase.A2,
                        ).forEachIndexed { index, debugPhase ->
                            this[index] += Button("Boss fight, looping phase A${index + 1}").apply {
                                this.setOnAction {
                                    enterGameMode(StoryBossGameMode.getFactory(debugPhase = debugPhase))
                                }
                            }
                        }
                    }
                    this += separator()
                    this += ColumnarPane(2, useRows = false).apply {
                        this.spacing.set(8f)
                        this.bounds.height.set(32f)
                        listOf(
                            StoryBossGameMode.DebugPhase.B1,
                            StoryBossGameMode.DebugPhase.B2,
                        ).forEachIndexed { index, debugPhase ->
                            this[index] += Button("Boss fight, looping phase B${index + 1}").apply {
                                this.setOnAction {
                                    enterGameMode(StoryBossGameMode.getFactory(debugPhase = debugPhase))
                                }
                            }
                        }
                    }
                    this += separator()
                    this += Button("Boss fight, looping phase C (variants 1-3)").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            enterGameMode(StoryBossGameMode.getFactory(debugPhase = StoryBossGameMode.DebugPhase.C))
                        }
                    }
                    this += ColumnarPane(3, useRows = false).apply {
                        this.spacing.set(8f)
                        this.bounds.height.set(32f)
                        listOf(
                            StoryBossGameMode.DebugPhase.C_VAR1,
                            StoryBossGameMode.DebugPhase.C_VAR2,
                            StoryBossGameMode.DebugPhase.C_VAR3
                        ).forEachIndexed { index, debugPhase ->
                            this[index] += Button("Looping variant ${index + 1}").apply {
                                this.setOnAction {
                                    enterGameMode(StoryBossGameMode.getFactory(debugPhase = debugPhase))
                                }
                            }
                        }
                    }
                    this += separator()
                    this += Button("Boss fight, looping phase D (variants 1-3)").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            enterGameMode(StoryBossGameMode.getFactory(debugPhase = StoryBossGameMode.DebugPhase.D))
                        }
                    }
                    this += ColumnarPane(3, useRows = false).apply {
                        this.spacing.set(8f)
                        this.bounds.height.set(32f)
                        listOf(
                            StoryBossGameMode.DebugPhase.D_VAR1,
                            StoryBossGameMode.DebugPhase.D_VAR2,
                            StoryBossGameMode.DebugPhase.D_VAR3,
                        ).forEachIndexed { index, debugPhase ->
                            this[index] += Button("Looping variant ${index + 1}").apply {
                                this.setOnAction {
                                    enterGameMode(StoryBossGameMode.getFactory(debugPhase = debugPhase))
                                }
                            }
                        }
                    }
                    this += separator()
                    this += ColumnarPane(2, useRows = false).apply {
                        this.spacing.set(8f)
                        this.bounds.height.set(32f)
                        listOf(
                            StoryBossGameMode.DebugPhase.E1,
                            StoryBossGameMode.DebugPhase.E2,
                        ).forEachIndexed { index, debugPhase ->
                            this[index] += Button("Boss fight, looping phase $debugPhase").apply {
                                this.setOnAction {
                                    enterGameMode(StoryBossGameMode.getFactory(debugPhase = debugPhase))
                                }
                            }
                        }
                    }
                    this += separator()
                    this += Button("Boss fight, looping phase F").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            enterGameMode(StoryBossGameMode.getFactory(debugPhase = StoryBossGameMode.DebugPhase.F))
                        }
                    }
                    this += ColumnarPane(3, useRows = false).apply {
                        this.spacing.set(8f)
                        this.bounds.height.set(32f)
                        listOf(
                            StoryBossGameMode.DebugPhase.F_VAR1,
                            StoryBossGameMode.DebugPhase.F_VAR2,
                            StoryBossGameMode.DebugPhase.F_VAR3,
                        ).forEachIndexed { index, debugPhase ->
                            this[index] += Button("Looping variant ${index + 1}").apply {
                                this.setOnAction {
                                    enterGameMode(StoryBossGameMode.getFactory(debugPhase = debugPhase))
                                }
                            }
                        }
                    }
                    this += separator()
                    this += separator()
                    this += Button("Post-boss cutscene").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            val titleScreen = PostBossCutsceneScreen(main, storySession) {
                                main.screen = TransitionScreen(
                                    main, main.screen, this@TestStoryGimmickDebugScreen,
                                    FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK)
                                )
                            }
                            main.screen = TransitionScreen(
                                main, main.screen, titleScreen,
                                FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun enterGimmickGameMode(gameMode: GameMode) {
        main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_enter_game"))
        val playScreen = TestStoryGimmickPlayScreen(main, storySession, Challenges.NO_CHANGES, main.settings.inputCalibration.getOrCompute(), gameMode)
        main.screen = TransitionScreen(main, main.screen, playScreen,
                FadeToOpaque(0.25f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK)).apply {
            this.onEntryEnd = {
                gameMode.prepareFirstTime()
                playScreen.resetAndUnpause()
            }
        }
    }

    private fun enterGameMode(gamemodeFactory: Contract.GamemodeFactory) {
        val main = this.main
        val gamemode: GameMode? = gamemodeFactory.load(1 / 30f, main)
        
        if (gamemode != null) {
            // Go immediately
            enterGimmickGameMode(gamemode)
        } else {
            // Go to loading screen to finish loading
            val loadingScreen = StoryLoadingScreen<TestStoryGimmickPlayScreen>(main, { delta ->
                val gameMode: GameMode? = gamemodeFactory.load(delta, main)

                if (gameMode != null) {
                    val playScreen = TestStoryGimmickPlayScreen(main, storySession, Challenges.NO_CHANGES, main.settings.inputCalibration.getOrCompute(), gameMode)

                    gameMode.prepareFirstTime()
                    playScreen.resetAndUnpause()

                    StoryLoadingScreen.LoadResult(playScreen)
                } else null
            }) { playScreen ->
                playScreen.unpauseGameNoSound()
                main.screen = TransitionScreen(main, main.screen, playScreen,
                    FadeToOpaque(0.1f, Color.BLACK), FadeToTransparent(0.1f, Color.BLACK))
            }.apply {
                this.minimumShowTime = 0f
                this.minWaitTimeBeforeLoadStart = 0f
                this.minWaitTimeAfterLoadFinish = 0f
            }

            main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_enter_game"))
            main.screen = TransitionScreen(main, main.screen, loadingScreen,
                FadeToOpaque(0.1f, Color.BLACK), FadeToTransparent(0.1f, Color.BLACK))
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render(delta)

        val camera = uiCamera
        batch.projectionMatrix = camera.combined
        batch.begin()

        sceneRoot.renderAsRoot(batch)

        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        uiViewport.update(width, height)
    }

    override fun show() {
        super.show()
        main.inputMultiplexer.removeProcessor(processor)
        main.inputMultiplexer.addProcessor(processor)

        storySession.musicHandler.fadeOut(0f)
    }

    override fun hide() {
        super.hide()
        main.inputMultiplexer.removeProcessor(processor)
    }

    override fun dispose() {
    }
}