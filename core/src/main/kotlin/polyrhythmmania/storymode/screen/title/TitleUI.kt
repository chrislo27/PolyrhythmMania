package polyrhythmmania.storymode.screen.title

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import paintbox.binding.FloatVar
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.ui.*
import paintbox.ui.animation.Animation
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.element.RectElement
import paintbox.ui.layout.ColumnarPane
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.StorySavefile
import polyrhythmmania.storymode.StorySession
import polyrhythmmania.storymode.inbox.InboxDB
import polyrhythmmania.storymode.screen.StoryDesktopScreen
import polyrhythmmania.storymode.screen.StoryTitleScreen
import polyrhythmmania.storymode.screen.desktop.DesktopAnimations
import polyrhythmmania.storymode.screen.desktop.DesktopScenario


class TitleUI(val titleLogic: TitleLogic, val sceneRoot: SceneRoot) {

    private val main: PRManiaGame = titleLogic.main
    private val storySession: StorySession = titleLogic.storySession

    private val savefiles: List<StorySavefile.LoadedState> = titleLogic.savefiles

    private val titleBigSize: FloatVar = FloatVar(if (titleLogic.fullTitle.get()) 1f else 0f)

    init {
        sceneRoot += NoInputPane().apply {
            this += RectElement(PRManiaColors.debugColor)
            this += ImageIcon(TextureRegion(StoryAssets.get<Texture>("logo"))).apply {
                this.margin.set(Insets(4f))
                this.bindHeightToParent(multiplierBinding = { MathUtils.lerp(0.55f, 0.75f, titleBigSize.use()) }, adjustBinding = { 0f })
                this.visible.set(false) // FIXME
            }
        }

        /*
        - When logic.fullTitle == true, then title image should be big
            - "Click anywhere to start"
            - Have a Quit to Main Menu button
        - Otherwise:
            - Show smaller title image
            - Show save file buttons
            - Still have the Quit to Main Menu button available
            - Have a back button
            
         */

        val pane = Pane()
        with(pane) {
            this += ColumnarPane(listOf(5, 5), true).apply {
                this.spacing.set(48f)
                this.columnBoxes.forEach { pane ->
                    pane.margin.set(Insets(4f))
                }
                this[1] += ColumnarPane(listOf(6, 4), true).apply {
                    this[0] += ColumnarPane(savefiles.size, false).apply {
                        this.bindWidthToParent(multiplier = 0.5f)
                        this.bindHeightToParent(multiplier = 0.75f)
                        Anchor.Centre.configure(this)
                        this.spacing.set(10f)

                        this.columnBoxes.zip(savefiles).forEachIndexed { index, (pane, savefileState) ->
                            pane += Button("File ${savefileState.number}\n\n${savefileState.javaClass.simpleName}").apply {
                                Anchor.Centre.configure(this)
                                this.bindWidthToSelfHeight()
                                if (savefileState is StorySavefile.LoadedState.FailedToLoad) {
                                    this.disabled.set(true)
                                }

                                this.setOnAction {
                                    val isBrandNew = savefileState is StorySavefile.LoadedState.NoSavefile
                                    val savefile = when (savefileState) {
                                        is StorySavefile.LoadedState.FailedToLoad -> return@setOnAction
                                        is StorySavefile.LoadedState.Loaded -> savefileState.savefile
                                        is StorySavefile.LoadedState.NoSavefile -> savefileState.blankFile
                                    }

                                    storySession.useSavefile(savefile)

                                    val inboxDB = InboxDB()
                                    val desktopScreen = StoryDesktopScreen(main, storySession, {
                                        storySession.stopUsingSavefile()
                                        StoryTitleScreen(main, storySession)
                                    }, DesktopScenario(inboxDB, inboxDB.progression, savefile), isBrandNew)

                                    if (isBrandNew) {
                                        val desktopUI = desktopScreen.desktopUI
                                        val animations = desktopUI.animations

                                        main.screen = TransitionScreen(main, main.screen, desktopScreen,
                                                FadeToOpaque(2.5f, Color.BLACK), FadeToTransparent(0.5f, Color.BLACK)).apply {
                                            this.onDestEnd = {
                                                animations.enqueueAnimation(animations.AnimLockInputs(true))
                                                animations.enqueueAnimation(DesktopAnimations.AnimDelay(1f))
                                                animations.enqueueAnimation(DesktopAnimations.AnimGeneric(0f) { _, _ ->
                                                    desktopUI.updateAndShowNewlyAvailableInboxItems(lockInputs = true)
                                                })
                                            }
                                        }
                                    } else {
                                        main.screen = TransitionScreen(main, main.screen, desktopScreen,
                                                FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                                    }
                                }
                            }
                        }
                    }
//                    this[1] += Button("Toggle").apply {
//                        Anchor.Centre.configure(this)
//                        this.bounds.width.set(300f)
//                        this.bounds.height.set(64f)
//                        this.setOnAction {
//                            titleLogic.fullTitle.invert()
//                        }
//                    }
                    this[1] += Button(StoryL10N.getVar("titleScreen.quitToMainMenu")).apply {
                        Anchor.Centre.configure(this)
                        this.bounds.width.set(300f)
                        this.bounds.height.set(64f)
                        this.setOnAction {
                            main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_deselect"))

                            val doAfterUnload: () -> Unit = {
                                val mainMenu = main.mainMenuScreen.prepareShow(doFlipAnimation = true)

                                main.screen = TransitionScreen(main, main.screen, mainMenu, FadeToOpaque(0.125f, Color.BLACK), null)
                            }
                            main.screen = TransitionScreen(main, main.screen, storySession.createExitLoadingScreen(main, doAfterUnload),
                                    FadeToOpaque(0.25f, Color.BLACK), FadeToTransparent(0.125f, Color.BLACK))
                        }
                    }
                }
            }
        }
        sceneRoot += pane
    }

    init {
        titleLogic.fullTitle.addListener {
            val value = it.getOrCompute()
            sceneRoot.animations.enqueueAnimation(Animation(Interpolation.pow5Out, 0.5f, if (value) 0f else 1f, if (value) 1f else 0f), titleBigSize)
        }
    }

}
