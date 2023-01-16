package polyrhythmmania.storymode.screen.title

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import paintbox.binding.*
import paintbox.font.Markup
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.ui.*
import paintbox.ui.animation.Animation
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.ButtonSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.ColumnarPane
import paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.StorySavefile
import polyrhythmmania.storymode.StorySession
import polyrhythmmania.storymode.inbox.InboxDB
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemCompletion
import polyrhythmmania.storymode.screen.StoryDesktopScreen
import polyrhythmmania.storymode.screen.StoryTitleScreen
import polyrhythmmania.storymode.screen.desktop.DesktopAnimations
import polyrhythmmania.storymode.screen.desktop.DesktopScenario
import polyrhythmmania.storymode.screen.desktop.DesktopUI.Companion.UI_SCALE
import polyrhythmmania.ui.PRManiaSkins


class TitleUI(private val titleLogic: TitleLogic, val sceneRoot: SceneRoot) {

    private sealed class Operation {
        interface IHasFromTarget {
            val from: Var<StorySavefile.LoadedState>
            val fromNumber: Int get() = from.getOrCompute().number
            val originalShowContext: BooleanVar
        }

        object None : Operation()

        data class Copy(
                override val from: Var<StorySavefile.LoadedState>, override val originalShowContext: BooleanVar
        ) : Operation(), IHasFromTarget

        data class Move(
                override val from: Var<StorySavefile.LoadedState>, override val originalShowContext: BooleanVar
        ) : Operation(), IHasFromTarget

        data class Delete(
                override val from: Var<StorySavefile.LoadedState>, override val originalShowContext: BooleanVar
        ) : Operation(), IHasFromTarget
    }

    private val main: PRManiaGame = titleLogic.main
    private val storySession: StorySession = titleLogic.storySession

    private val savefiles: List<Var<StorySavefile.LoadedState>> = titleLogic.savefiles

    private val robotoMarkup: Markup = Markup.createWithBoldItalic(main.fontRoboto, main.fontRobotoBold, main.fontRobotoItalic, main.fontRobotoBoldItalic)

    private val fullTitleTransition: FloatVar = FloatVar(if (titleLogic.fullTitle.get()) 1f else 0f)
    private val titleScaleAnimation: FloatVar = FloatVar(fullTitleTransition.get())
    private val panelAnimation: FloatVar = FloatVar(1f - fullTitleTransition.get())

    private val currentOperation: Var<Operation> = Var(Operation.None)

    init {
        val titleFullHeight = 0.75f
        val titleSmallHeight = 0.55f

        sceneRoot += NoInputPane().apply {
            this += ImageIcon(TextureRegion(StoryAssets.get<Texture>("logo"))).apply {
                Anchor.TopCentre.configure(this)
                this.margin.set(Insets(10f, 0f, 4f, 4f))
                this.bindHeightToParent(multiplierBinding = {
                    MathUtils.lerp(titleSmallHeight, titleFullHeight, titleScaleAnimation.use())
                }, adjustBinding = { 0f })
                this.bindWidthToParent(multiplier = 0.8f)
            }

            this += Pane().apply {
                Anchor.BottomCentre.configure(this)
                this.bindHeightToParent(multiplier = 1f - titleFullHeight)
                this.opacity.bind { Interpolation.exp10.apply(0f, 1f, fullTitleTransition.use()) }

                this += TextLabel(StoryL10N.getVar("titleScreen.clickAnywhereToContinue"), font = main.fontMainMenuHeadingBordered).apply {
                    Anchor.Centre.configure(this)
                    this.renderAlign.set(RenderAlign.center)
                    this.bounds.height.set(64f)
                    this.textColor.set(Color.WHITE.cpy())
                    this.bindWidthToParent(multiplier = 0.75f)
                }
            }
        }

        val overallPane = ActionablePane()

        with(overallPane) {
            this += Button("").apply {
                Anchor.TopRight.configure(this)
                this.bounds.width.set(64f)
                this.bounds.height.set(64f)
                (this.skin.getOrCompute() as ButtonSkin).apply {
                    this.roundedRadius.set(4)
                    this.roundedCorners.clear()
                    this.roundedCorners.add(Corner.BOTTOM_LEFT)
                }
                this.setOnAction {
                    quitToMainMenu()
                }
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_linear")["x"])).apply {
                    this.margin.set(Insets(6f))
                    this.tint.set(Color.BLACK.cpy())
                }
            }

            this.onAction = {
                if (titleLogic.fullTitle.get()) {
                    titleLogic.fullTitle.set(false)
                    main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_select"))
                    true
                } else false
            }
        }
        sceneRoot += overallPane

        val mainPane = Pane().apply {
            this.bindHeightToParent(multiplier = 1f - titleSmallHeight)
            this.bounds.y.bind {
                (parent.use()?.bounds?.height?.use() ?: 0f) - (bounds.height.use() * panelAnimation.use())
            }
        }
        overallPane += mainPane
        with(mainPane) {
            this += ColumnarPane(listOf(6, 4), true).apply {
                this[0] += ColumnarPane(savefiles.size, false).apply {
                    this.bindWidthToParent(multiplier = 0.8f)
                    Anchor.Centre.configure(this)
                    this.spacing.set(32f)

                    val inboxItemIDsThatAreContracts = InboxDB().items
                            .filterIsInstance<InboxItem.ContractDoc>()
                            .map { it.id }
                            .toSet()

                    this.columnBoxes.zip(savefiles).forEach { (pane, savefileStateVar) ->
                        pane += createSavefileElement(inboxItemIDsThatAreContracts, savefileStateVar)
                    }
                }

                this[1] += Button(Localization.getVar("common.back"), font = main.fontRobotoBold).apply {
                    Anchor.Centre.configure(this)
                    this.bounds.width.set(200f)
                    this.bounds.height.set(64f)
                    (this.skin.getOrCompute() as ButtonSkin).apply {
                        this.roundedRadius.set(8)
                    }
                    this.disabled.bind { currentOperation.use() != Operation.None }
                    this.setOnAction {
                        titleLogic.fullTitle.set(true)
                        main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_deselect"))
                    }
                }
            }
        }
    }

    init {
        titleLogic.fullTitle.addListener {
            val value = it.getOrCompute()
            sceneRoot.animations.enqueueAnimation(Animation(Interpolation.linear, 0.25f, if (value) 0f else 1f, if (value) 1f else 0f), fullTitleTransition)
        }
        titleLogic.fullTitle.addListener {
            val value = it.getOrCompute()
            sceneRoot.animations.enqueueAnimation(Animation(Interpolation.pow3Out, 0.25f, if (value) 0f else 1f, if (value) 1f else 0f), titleScaleAnimation)
        }
        titleLogic.fullTitle.addListener {
            val value = it.getOrCompute()
            sceneRoot.animations.enqueueAnimation(Animation(Interpolation.pow3Out, 0.25f, if (value) 1f else 0f, if (value) 0f else 1f), panelAnimation)
        }
    }


    private fun quitToMainMenu() {
        main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_deselect"))

        val doAfterUnload: () -> Unit = {
            val mainMenu = main.mainMenuScreen.prepareShow(doFlipAnimation = true)

            main.screen = TransitionScreen(main, main.screen, mainMenu, FadeToOpaque(0.125f, Color.BLACK), null)
        }
        storySession.musicHandler.fadeOutAndDispose(0.375f)
        main.screen = TransitionScreen(main, main.screen, storySession.createExitLoadingScreen(main, doAfterUnload),
                FadeToOpaque(0.25f, Color.BLACK), FadeToTransparent(0.125f, Color.BLACK))
    }

    private fun launchSavefile(savefileState: StorySavefile.LoadedState) {
        val isBrandNew = savefileState is StorySavefile.LoadedState.NoSavefile
        val savefile = when (savefileState) {
            is StorySavefile.LoadedState.FailedToLoad -> return
            is StorySavefile.LoadedState.Loaded -> savefileState.savefile
            is StorySavefile.LoadedState.NoSavefile -> savefileState.blankFile
        }

        storySession.useSavefile(savefile)

        val inboxDB = InboxDB()
        val desktopScreen = StoryDesktopScreen(main, storySession, {
            storySession.stopUsingSavefile()
            StoryTitleScreen(main, storySession)
        }, DesktopScenario(inboxDB, inboxDB.progression, savefile), isBrandNew)

        main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_enter_game"))
        val holdDuration = 0.75f
        if (isBrandNew) {
            val desktopUI = desktopScreen.desktopUI
            val animations = desktopUI.animations

            storySession.musicHandler.transitionToDesktopMix()

            main.screen = TransitionScreen(main, main.screen, desktopScreen,
                    FadeToOpaque(2.5f, Color.BLACK, holdDuration = holdDuration), FadeToTransparent(0.5f, Color.BLACK)).apply {
                this.onDestEnd = {
                    animations.enqueueAnimation(animations.AnimLockInputs(true))
                    animations.enqueueAnimation(DesktopAnimations.AnimDelay(1f))
                    animations.enqueueAnimation(DesktopAnimations.AnimGeneric(0f) { _, _ ->
                        desktopUI.updateAndShowNewlyAvailableInboxItems(lockInputs = true)
                    })
                }
            }
        } else {
            storySession.musicHandler.transitionToDesktopMix()
            main.screen = TransitionScreen(main, main.screen, desktopScreen,
                    FadeToOpaque(0.5f, Color.BLACK, holdDuration = holdDuration), FadeToTransparent(0.25f, Color.BLACK))
        }
    }
    
    private fun createSavefileElement(inboxItemIDsThatAreContracts: Set<String>, savefileStateVar :Var<StorySavefile.LoadedState>): ImageNode {
        return ImageNode(TextureRegion(StoryAssets.get<Texture>("title_file_blank"))).apply {
            Anchor.Centre.configure(this)
            this.bounds.width.set(76f * UI_SCALE)
            this.bounds.height.set(48f * UI_SCALE)

            val showContext = BooleanVar(false)
            val isInFailedState = BooleanVar {
                savefileStateVar.use() is StorySavefile.LoadedState.FailedToLoad
            }.asReadOnly()
            val isOperationOnMe: ReadOnlyBooleanVar = BooleanVar {
                val op = currentOperation.use()
                op is Operation.IHasFromTarget && op.fromNumber == savefileStateVar.use().number
            }
            val isOperationNotOnMe: ReadOnlyBooleanVar = BooleanVar {
                val op = currentOperation.use()
                op is Operation.IHasFromTarget && op.fromNumber != savefileStateVar.use().number
            }

            val filePane = Pane()
            this += filePane
            filePane += TextLabel(StoryL10N.getVar("titleScreen.file.fileNumber", Var {
                listOf(savefileStateVar.use().number)
            }), main.fontRobotoBold).apply {
                this.bounds.x.set(3f * UI_SCALE)
                this.bounds.y.set(2f * UI_SCALE)
                this.bounds.width.set(21f * UI_SCALE)
                this.bounds.height.set(8f * UI_SCALE)

                this.textColor.set(Color.BLACK.cpy())
                this.renderAlign.set(RenderAlign.center)
                this.setScaleXY(1.1f)
            }

            val regularLabelText: ReadOnlyVar<String> = Var {
                when (val savefileState = savefileStateVar.use()) {
                    is StorySavefile.LoadedState.Loaded -> {
                        val numContractsCompleted = savefileState.savefile.inboxState.getAllItemStates().count { (itemID, state) ->
                            state.completion == InboxItemCompletion.COMPLETED && itemID in inboxItemIDsThatAreContracts
                        }
                        StoryL10N.getVar("titleScreen.file.contractsCompleted", listOf(numContractsCompleted))
                    }
                    is StorySavefile.LoadedState.FailedToLoad -> StoryL10N.getVar("titleScreen.file.failedToLoad")
                    is StorySavefile.LoadedState.NoSavefile -> StoryL10N.getVar("titleScreen.file.noData")
                }.use()
            }
            filePane += TextLabel(binding = {
                when (val op = currentOperation.use()) {
                    is Operation.Copy -> {
                        if (op.fromNumber == savefileStateVar.use().number) {
                            StoryL10N.getVar("titleScreen.file.operation.copy.source").use()
                        } else {
                            val canCopyHere = savefileStateVar.use() is StorySavefile.LoadedState.NoSavefile
                            if (!canCopyHere) {
                                StoryL10N.getVar("titleScreen.file.operation.copy.dest.cannot").use()
                            } else regularLabelText.use()
                        }
                    }
                    is Operation.Delete -> {
                        if (op.fromNumber == savefileStateVar.use().number) {
                            StoryL10N.getVar("titleScreen.file.operation.delete.confirm").use()
                        } else ""
                    }
                    is Operation.Move -> {
                        if (op.fromNumber == savefileStateVar.use().number) {
                            StoryL10N.getVar("titleScreen.file.operation.move.source").use()
                        } else {
                            val isThisEmpty = savefileStateVar.use() is StorySavefile.LoadedState.NoSavefile
                            StoryL10N.getVar(if (isThisEmpty) "titleScreen.file.operation.move.dest.doesNotExist" else "titleScreen.file.operation.move.dest.exists").use()
                        }
                    }
                    Operation.None -> regularLabelText.use()
                }
            }).apply {
                this.bounds.x.set(3f * UI_SCALE)
                this.bounds.y.set(14f * UI_SCALE)
                this.bounds.width.set(70f * UI_SCALE)
                this.bounds.height.set(19f * UI_SCALE)

                this.markup.set(robotoMarkup)
                this.textColor.set(Color.BLACK.cpy())
                this.renderAlign.set(RenderAlign.center)
            }

            fun Button.applyBottomButtonStyling() {
                this.bounds.width.set(34f * UI_SCALE)
                this.bounds.height.set(9f * UI_SCALE)
                this.skinID.set(PRManiaSkins.BUTTON_SKIN_STORY_DARK)
            }

            val playButton = Button(binding = {
                StoryL10N.getVar(when (savefileStateVar.use()) {
                    is StorySavefile.LoadedState.NoSavefile -> "titleScreen.file.start"
                    else -> "titleScreen.file.play"
                }).use()
            }, main.fontRobotoBold).apply {
                this.bounds.x.set(21f * UI_SCALE)
                this.bounds.y.set(37f * UI_SCALE)
                this.applyBottomButtonStyling()

                this.visible.bind {
                    if (savefileStateVar.use() is StorySavefile.LoadedState.FailedToLoad) {
                        false
                    } else currentOperation.use() == Operation.None
                }

                this.setOnAction {
                    launchSavefile(savefileStateVar.getOrCompute())
                }
            }
            filePane += playButton
            val cancelOperationButton = Button(Localization.getVar("common.cancel"), main.fontRobotoItalic).apply {
                this.bounds.x.set(21f * UI_SCALE)
                this.bounds.y.set(37f * UI_SCALE)
                this.applyBottomButtonStyling()

                this.visible.bind {
                    val op = currentOperation.use()
                    op is Operation.IHasFromTarget && op.fromNumber == savefileStateVar.use().number && op !is Operation.Delete
                }

                this.setOnAction {
                    currentOperation.set(Operation.None)
                }
            }
            filePane += cancelOperationButton
            val copyHereOperationButton = Button(StoryL10N.getVar("titleScreen.file.operation.copy.action"), main.fontRobotoBoldItalic).apply {
                this.bounds.x.set(21f * UI_SCALE)
                this.bounds.y.set(37f * UI_SCALE)
                this.applyBottomButtonStyling()

                this.visible.bind {
                    val op = currentOperation.use()
                    op is Operation.Copy && op.fromNumber != savefileStateVar.use().number && savefileStateVar.use() is StorySavefile.LoadedState.NoSavefile
                }

                this.setOnAction {
                    val op = currentOperation.getOrCompute() as? Operation.Copy
                    val thisSavefile = savefileStateVar.getOrCompute()
                    val savefileNum = thisSavefile.number
                    if (op != null && op.fromNumber != savefileNum && thisSavefile is StorySavefile.LoadedState.NoSavefile) {
                        val src = op.from.getOrCompute()
                        if (src is StorySavefile.LoadedState.Loaded) {
                            src.savefile.persistTo(thisSavefile.storageLoc)
                            savefileStateVar.set(StorySavefile.attemptLoad(savefileNum))
                        }
                        op.originalShowContext.set(false)
                        showContext.set(false)
                    }
                    currentOperation.set(Operation.None)
                }
            }
            filePane += copyHereOperationButton
            val moveHereOperationButton = Button(binding = {
                StoryL10N.getVar(if (savefileStateVar.use() is StorySavefile.LoadedState.NoSavefile)
                    "titleScreen.file.operation.move.action.move"
                else "titleScreen.file.operation.move.action.swap").use()
            }, main.fontRobotoBoldItalic).apply {
                this.bounds.x.set(21f * UI_SCALE)
                this.bounds.y.set(37f * UI_SCALE)
                this.applyBottomButtonStyling()

                this.visible.bind {
                    val op = currentOperation.use()
                    op is Operation.Move && op.fromNumber != savefileStateVar.use().number
                }

                this.setOnAction {
                    val op = currentOperation.getOrCompute() as? Operation.Move
                    val thisSavefile = savefileStateVar.getOrCompute()
                    val savefileNum = thisSavefile.number
                    if (op != null && op.fromNumber != savefileNum) {
                        val src = op.from.getOrCompute()
                        val srcLoc = src.storageLoc
                        val dst = thisSavefile
                        val dstLoc = dst.storageLoc

                        if (src is StorySavefile.LoadedState.Loaded) {
                            // Source can only be Loaded, cannot be NoSavefile
                            src.savefile.persistTo(dstLoc)
                        }

                        if (dst is StorySavefile.LoadedState.Loaded) {
                            dst.savefile.persistTo(srcLoc)
                        } else if (dst is StorySavefile.LoadedState.NoSavefile) {
                            src.storageLoc.delete()
                        }

                        savefileStateVar.set(StorySavefile.attemptLoad(savefileNum))
                        op.from.set(StorySavefile.attemptLoad(op.fromNumber))

                        op.originalShowContext.set(false)
                        showContext.set(false)
                    }
                    currentOperation.set(Operation.None)
                }
            }
            filePane += moveHereOperationButton
            val deleteOperationsHBox = HBox().apply {
                this.bounds.x.set(8f * UI_SCALE)
                this.bounds.y.set(37f * UI_SCALE)
                this.bounds.width.set(58f * UI_SCALE)
                this.bounds.height.set(9f * UI_SCALE)

                this.spacing.set(2f * UI_SCALE)
                this.align.set(HBox.Align.CENTRE)

                this.visible.bind {
                    val op = currentOperation.use()
                    op is Operation.Delete && op.fromNumber == savefileStateVar.use().number
                }

                this += Button(Localization.getVar("common.cancel"), main.fontRobotoItalic).apply {
                    this.applyBottomButtonStyling()
                    this.bounds.width.set(28f * UI_SCALE)

                    this.setOnAction {
                        currentOperation.set(Operation.None)
                    }
                }
                this += Button(StoryL10N.getVar("titleScreen.file.operation.delete.action"), main.fontRobotoBoldItalic).apply {
                    this.applyBottomButtonStyling()
                    this.bounds.width.set(28f * UI_SCALE)

                    this.setOnAction {
                        val savefile = savefileStateVar.getOrCompute()
                        val fh = savefile.storageLoc
                        if (fh.exists()) {
                            fh.delete()
                        }
                        val savefileNum = savefile.number
                        savefileStateVar.set(StorySavefile.LoadedState.NoSavefile(savefileNum, StorySavefile.newSaveFile(savefileNum)))
                        currentOperation.set(Operation.None)
                        showContext.set(false)
                    }
                }
            }
            filePane += deleteOperationsHBox

            // Operations (copy/move/delete)
            filePane += HBox().apply {
                this.bounds.x.set(33f * UI_SCALE)
                this.bounds.y.set(3f * UI_SCALE)
                this.bounds.width.set(40f * UI_SCALE)
                this.bounds.height.set(8f * UI_SCALE)

                this.spacing.set(1f * UI_SCALE)
                this.align.set(HBox.Align.RIGHT)

                this.temporarilyDisableLayouts {
                    this += Button("").apply {
                        this.bounds.width.set(8f * UI_SCALE)
                        this.bounds.height.set(8f * UI_SCALE)
                        this.visible.bind {
                            showContext.use() && !isInFailedState.use() && !isOperationNotOnMe.use() && savefileStateVar.use() !is StorySavefile.LoadedState.NoSavefile
                        }

                        this += ImageNode(TextureRegion(StoryAssets.get<Texture>("title_icon_copy")))

                        this.setOnAction {
                            if (currentOperation.getOrCompute() is Operation.Copy) {
                                currentOperation.set(Operation.None)
                            } else {
                                currentOperation.set(Operation.Copy(savefileStateVar, showContext))
                            }
                        }
                    }
                    this += Button("").apply {
                        this.bounds.width.set(8f * UI_SCALE)
                        this.bounds.height.set(8f * UI_SCALE)
                        this.visible.bind {
                            showContext.use() && !isInFailedState.use() && !isOperationNotOnMe.use() && savefileStateVar.use() !is StorySavefile.LoadedState.NoSavefile
                        }

                        this += ImageNode(TextureRegion(StoryAssets.get<Texture>("title_icon_move")))

                        this.setOnAction {
                            if (currentOperation.getOrCompute() is Operation.Move) {
                                currentOperation.set(Operation.None)
                            } else {
                                currentOperation.set(Operation.Move(savefileStateVar, showContext))
                            }
                        }
                    }
                    this += Button("").apply {
                        this.bounds.width.set(8f * UI_SCALE)
                        this.bounds.height.set(8f * UI_SCALE)
                        this.visible.bind {
                            showContext.use() && !isOperationNotOnMe.use() && savefileStateVar.use() !is StorySavefile.LoadedState.NoSavefile
                        }

                        this += ImageNode(TextureRegion(StoryAssets.get<Texture>("title_icon_delete")))

                        this.setOnAction {
                            if (currentOperation.getOrCompute() is Operation.Delete) {
                                currentOperation.set(Operation.None)
                            } else {
                                currentOperation.set(Operation.Delete(savefileStateVar, showContext))
                            }
                        }
                    }
                    this += Button("").apply {
                        this.bounds.width.set(8f * UI_SCALE)
                        this.bounds.height.set(8f * UI_SCALE)
                        this.disabled.bind { isOperationOnMe.use() }
                        this.visible.bind {
                            !isOperationNotOnMe.use() && savefileStateVar.use() !is StorySavefile.LoadedState.NoSavefile
                        }

                        this += ImageNode(TextureRegion(StoryAssets.get<Texture>("title_icon_dotdotdot")))

                        this.setOnAction {
                            if (showContext.invert()) {
                                main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_select"))
                            } else {
                                main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_deselect"))
                            }
                        }
                    }
                }
            }
        }
    }

}
