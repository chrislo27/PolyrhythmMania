package polyrhythmmania.storymode.screen.title

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import paintbox.binding.FloatVar
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
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.ColumnarPane
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StorySavefile
import polyrhythmmania.storymode.StorySession
import polyrhythmmania.storymode.inbox.InboxDB
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemCompletion
import polyrhythmmania.storymode.screen.StoryDesktopScreen
import polyrhythmmania.storymode.screen.StoryTitleScreen
import polyrhythmmania.storymode.screen.desktop.DesktopAnimations
import polyrhythmmania.storymode.screen.desktop.DesktopScenario


class TitleUI(val titleLogic: TitleLogic, val sceneRoot: SceneRoot) {

    private val main: PRManiaGame = titleLogic.main
    private val storySession: StorySession = titleLogic.storySession

    private val savefiles: List<StorySavefile.LoadedState> = titleLogic.savefiles

    private val robotoMarkup: Markup = Markup.createWithBoldItalic(main.fontRoboto, main.fontRobotoBold, main.fontRobotoItalic, main.fontRobotoBoldItalic)

    private val fullTitleTransition: FloatVar = FloatVar(if (titleLogic.fullTitle.get()) 1f else 0f)
    private val titleScaleAnimation: FloatVar = FloatVar(fullTitleTransition.get())
    private val panelAnimation: FloatVar = FloatVar(1f - fullTitleTransition.get())

    init {
        val titleFullHeight = 0.75f
        val titleSmallHeight = 0.55f
        
        sceneRoot += NoInputPane().apply {
            this += ImageIcon(TextureRegion(StoryAssets.get<Texture>("logo"))).apply {
                Anchor.TopCentre.configure(this)
                this.margin.set(Insets(4f))
                this.bindHeightToParent(multiplierBinding = {
                    MathUtils.lerp(titleSmallHeight, titleFullHeight, titleScaleAnimation.use())
                }, adjustBinding = { 0f })
                this.bindWidthToParent(multiplier = 0.8f)
            }
            
            this += Pane().apply {
                Anchor.BottomCentre.configure(this)
                this.bindHeightToParent(multiplier = 1f - titleFullHeight)
                this.opacity.bind { Interpolation.smoother.apply(0f, 1f, fullTitleTransition.use()) }
                
                this += TextLabel("Click anywhere to continue", font = main.fontMainMenuHeading).apply {
                    Anchor.Centre.configure(this)
                    this.renderAlign.set(RenderAlign.center)
                    this.bounds.height.set(64f)
                    this.bindWidthToParent(multiplier = 0.75f)
                }
            }
        }

        val overallPane = ActionablePane()
        val mainPane = Pane().apply {
            this.bindHeightToParent(multiplier = 1f - titleSmallHeight)
            this.bounds.y.bind { (parent.use()?.bounds?.height?.use() ?: 0f) - (bounds.height.use() * panelAnimation.use()) }
        }
        
        with(overallPane) {
            this += mainPane
            
            this += Button("").apply {
                Anchor.TopRight.configure(this)
                this.bounds.width.set(64f)
                this.bounds.height.set(64f)
                this.setOnAction {
                    quitToMainMenu()
                }
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_linear")["x"])).apply {
                    this.margin.set(Insets(6f))
                    this.tint.set(Color.BLACK.cpy())
                }
            }
//            this += Button("Toggle").apply {
//                Anchor.TopLeft.configure(this)
//                this.bounds.width.set(64f)
//                this.bounds.height.set(64f)
//                this.setOnAction {
//                    titleLogic.fullTitle.invert()
//                }
//            }
            
            this.onAction = {
                if (titleLogic.fullTitle.get()) {
                    titleLogic.fullTitle.set(false)
                    true
                } else false
            }
        }
        sceneRoot += overallPane
        
        with(mainPane) {
            this += ColumnarPane(listOf(1, 50), true).apply {
                this.spacing.set(32f)
                this.columnBoxes.forEach { pane ->
                    pane.margin.set(Insets(4f))
                }
                this[1] += ColumnarPane(listOf(6, 4), true).apply {
                    this[0] += ColumnarPane(savefiles.size, false).apply {
                        this.bindWidthToParent(multiplier = 0.6f)
                        this.bindHeightToParent(multiplier = 0.875f)
                        Anchor.Centre.configure(this)
                        this.spacing.set(32f)

                        val inboxItemIDsThatAreContracts = InboxDB().items
                                .filterIsInstance<InboxItem.ContractDoc>()
                                .map { it.id }
                                .toSet()
                        
                        this.columnBoxes.zip(savefiles).forEach { (pane, savefileState) ->
                            var buttonText = "[b]File ${savefileState.number}[][scale=0.4]\n[]\n"
                            buttonText += when (savefileState) {
                                is StorySavefile.LoadedState.Loaded -> {
                                    val numContractsCompleted = savefileState.savefile.inboxState.getAllItemStates().count { (itemID, state) ->
                                        state.completion == InboxItemCompletion.COMPLETED && itemID in inboxItemIDsThatAreContracts
                                    }
                                    "$numContractsCompleted Contracts\ncompleted"
                                }
                                is StorySavefile.LoadedState.FailedToLoad -> "[i]Failed to load![]"
                                is StorySavefile.LoadedState.NoSavefile -> "[i]No data[]"
                            }
                            pane += Button(buttonText).apply {
                                Anchor.Centre.configure(this)
                                this.markup.set(robotoMarkup)
                                
                                if (savefileState is StorySavefile.LoadedState.FailedToLoad) {
                                    this.disabled.set(true)
                                }

                                this.setOnAction {
                                    launchSavefile(savefileState)
                                }
                            }
                        }
                    }
                    this[1] += Button(Localization.getVar("common.back"), font = main.fontRobotoBold).apply {
                        Anchor.Centre.configure(this)
                        this.bounds.width.set(200f)
                        this.bounds.height.set(64f)
                        this.setOnAction {
                            titleLogic.fullTitle.set(true)
                        }
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

}
