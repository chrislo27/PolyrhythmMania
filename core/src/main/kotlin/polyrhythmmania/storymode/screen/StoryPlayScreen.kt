package polyrhythmmania.storymode.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import paintbox.PaintboxScreen
import paintbox.binding.*
import paintbox.font.Markup
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.transition.TransitionScreen
import paintbox.transition.WipeTransitionHead
import paintbox.transition.WipeTransitionTail
import paintbox.ui.*
import paintbox.ui.animation.Animation
import paintbox.ui.animation.AnimationHandler
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.TextLabel
import paintbox.ui.element.QuadElement
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.WindowSize
import paintbox.util.gdxutils.NestedFrameBuffer
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.grey
import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.Container
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.ResultFlag
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.gamemodes.CanPreventPausing
import polyrhythmmania.gamemodes.ChangeMusicVolMultiplierEvent
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.gamemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.screen.play.AbstractEnginePlayScreen
import polyrhythmmania.screen.play.pause.ArrowNode
import polyrhythmmania.screen.play.pause.PauseMenuHandler
import polyrhythmmania.screen.play.pause.PauseOption
import polyrhythmmania.screen.play.pause.TengokuBgPauseMenuHandler
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.StorySession
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.contract.Contracts
import polyrhythmmania.storymode.gamemode.AbstractStoryGameMode
import polyrhythmmania.storymode.gamemode.boss.BossModifierModule
import polyrhythmmania.ui.TextSlideInterp
import polyrhythmmania.util.FrameBufferManager
import polyrhythmmania.util.FrameBufferMgrSettings
import polyrhythmmania.world.EventEndState
import kotlin.math.max
import kotlin.math.roundToInt


class StoryPlayScreen(
        main: PRManiaGame,
        val storySession: StorySession,
        container: Container,
        challenges: Challenges, inputCalibration: InputCalibration,
        gameMode: GameMode?,
        val contract: Contract,
        isNotCompletedYet: Boolean,
        private val previousFailureCount: Int,
        val exitToScreen: (ExitReason) -> PaintboxScreen,
        val exitCallback: ExitCallback,
) : AbstractEnginePlayScreen(main, null, container, challenges, inputCalibration, gameMode) {

    override val pauseMenuHandler: PauseMenuHandler = TengokuBgPauseMenuHandler(this).apply { 
        this.pauseBg.gradientRenderer = StoryPlayGradientRenderer()
    }

    private var disableCatchingCursorOnHide: Boolean = false

    private val blurShader: ShaderProgram = GaussianBlur.createShaderProgram()
    private val framebufferMgr: FrameBufferManager = FrameBufferManager(2, FrameBufferMgrSettings(Pixmap.Format.RGB888), tag = "StoryPlayScreen", referenceWindowSize = WindowSize(1280, 720))
    
    private val animationHandler: AnimationHandler = AnimationHandler()
    
    private val engineBeat: FloatVar = FloatVar(0f)
    private val currentFailureCount: IntVar = IntVar(0)
    
    // Intro card
    private val introCardDefaultDuration: Float = 3f
    var introCardDuration: Float = introCardDefaultDuration
    private val introCardUnblurDuration: Float = 0.5f
    private val introCardSceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val introCardTime: FloatVar = FloatVar(0f) // Goes linearly from 1 to 0 when transitioning
    private val inIntroCard: ReadOnlyBooleanVar = BooleanVar(eager = true) { introCardTime.use() > 0f }
    private val blurStrength: FloatVar = FloatVar(0f)
    private val blackBarsAmount: FloatVar
    private val textSlide: TextSlideInterp
    
    // Score card
    private val scoreCardTransitionTime: Float = 0.5f
    private val scoreCardSceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val scoreCardTransition: FloatVar = FloatVar(0f) // Goes linearly from 1 to 0 when transitioning
    private val showingScoreCard: BooleanVar = BooleanVar(false)
    private val lastResultFlag: ReadOnlyVar<ResultFlag> = Var.eagerBind { engine.resultFlag.use() }
    private val scoreBar: FloatVar = FloatVar(0f)
    private val showScoreOnScoreCard: BooleanVar = BooleanVar(false) // Blank out score before hit
    
    private val failScoreCardOptions: List<PauseOption>
    private val successScoreCardOptions: List<PauseOption>
    private val currentScoreCardOptions: Var<List<PauseOption>> = Var(listOf())
    private val selectedScoreCardOption: Var<PauseOption?> = Var(null)
    
    private val gamemodePreventsPausing: ReadOnlyBooleanVar = BooleanVar(eager = true) { 
        (gameMode as? CanPreventPausing)?.preventPausing?.use() ?: false
    }
    private val canPauseGame: ReadOnlyBooleanVar = BooleanVar(eager = true) { 
        (!gamemodePreventsPausing.use()) && !(inIntroCard.use() || showingScoreCard.use())
    }
    private val couldSkipLevelEventually: Boolean = isNotCompletedYet && contract.canSkipLevel
    private var skippableScoreBarAnimation: Animation? = null
    
    private lateinit var successExitReason: ExitReason
    
    init {
        val optionList = mutableListOf<PauseOption>()
        optionList += PauseOption("play.pause.resume", true) {
            unpauseGame(true)
        }
        optionList += PauseOption("play.pause.startOver", !(gameMode is EndlessPolyrhythm && gameMode.dailyChallenge != null)) {
            startOverPauseAction()
        }
        optionList += PauseOption(StoryL10N.getVar("play.pause.quit"), true) {
            quitPauseAction(ExitReason.Quit(currentFailureCount.get()))
        }
        this.pauseOptions.set(optionList)
    }
    
    init {
        introCardSceneRoot.doClipping.set(true)
        
        blackBarsAmount = FloatVar {
            val effectInLastSec = 0.5f // Effect only in the first and last few seconds
            val time = introCardTime.use() * introCardDuration // Counts down from duration to 0.0 sec
            
            val interpolation = Interpolation.smoother
            val firstThreshold = introCardDuration - effectInLastSec
            when {
                time <= 0f -> 0f
                time > firstThreshold -> {
                    val timePercent = 1f - (time - firstThreshold) / effectInLastSec
                    interpolation.apply(0f, 1f, timePercent)
                }
                time < effectInLastSec -> {
                    val timePercent = 1f - time / effectInLastSec
                    interpolation.apply(1f, 0f, timePercent)
                }
                else -> 1f
            }
        }
        textSlide = TextSlideInterp(introCardTime, interpPower = 4.5f)
        
        // Black bars: Full is 16:9 = 1.778, cinema is 2.35:1 = 2.35. About 25% less height
        val slantAmount = 1f
        val barHeight = 0.125f * 1.25f
        introCardSceneRoot += QuadElement(Color.BLACK).apply { 
            this.bindHeightToParent(multiplier = barHeight * slantAmount)
            Anchor.TopLeft.yConfigure(this) {
                val h = bounds.height.use()
                -h + (blackBarsAmount.use() * h)
            }
            
            this.bottomLeftOffsetV.set(0f)
            this.bottomRightOffsetV.set(1f - slantAmount)
        }
        introCardSceneRoot += QuadElement(Color.BLACK).apply { 
            this.bindHeightToParent(multiplier = barHeight * slantAmount)
            Anchor.BottomLeft.yConfigure(this) {
                val h = bounds.height.use()
                h - (blackBarsAmount.use() * h)
            }

            this.topLeftOffsetV.set(1f)
            this.topRightOffsetV.set(slantAmount)
        }
        // Title and tagline
        introCardSceneRoot += TextLabel(contract.name.getOrCompute(), font = PRManiaGame.instance.fontGamePracticeClear).apply { 
            Anchor.CentreLeft.configure(this, offsetY = {
                -(bounds.height.use() / 2f)
            })
            this.bindHeightToParent(multiplier = 0.125f)
            
            this.margin.set(Insets(0f, 0f, 50f, 50f))
            this.renderAlign.set(RenderAlign.center)
            this.textColor.set(Color.WHITE.cpy())
            
            this.bounds.x.bind {
                val parentW = parent.use()?.bounds?.width?.use() ?: 0f    
                MathUtils.lerp(-(bounds.width.use()), parentW, textSlide.textSlideAmount.use())
            }
        }
        introCardSceneRoot += TextLabel(contract.tagline.getOrCompute(), font = PRManiaGame.instance.fontGamePracticeClear).apply {
            Anchor.CentreLeft.configure(this, offsetY = {
                (bounds.height.use() / 2f)
            })
            this.bindHeightToParent(multiplier = 0.2f)
            
            this.margin.set(Insets(0f, 0f, 50f, 50f))
            this.renderAlign.set(RenderAlign.center)
            this.textColor.set(Color.WHITE.cpy())
            this.setScaleXY(0.75f)
            
            this.opacity.bind {
                val time = textSlide.textSlideAmount.use()
                when {
                    time < 0.25f -> 0f
                    time in 0.25f..0.4f -> (time - 0.25f) / 0.15f
                    time in 0.4f..0.75f -> 1f
                    time > 0.75f -> 1f - ((time - 0.75f) / 0.25f)
                    else -> 0f
                }
            }
            this.bounds.x.bind {
                val parentW = parent.use()?.bounds?.width?.use() ?: 0f
                MathUtils.lerp(-1f, 0.5f, textSlide.textSlideAmount.use()) * (parentW * 0.1f)
            }
        }
    }
    
    init {
        currentScoreCardOptions.addListener {
            val l = it.getOrCompute()
            selectedScoreCardOption.set(l.firstOrNull())
        }
        
        scoreCardSceneRoot.doClipping.set(true)
        
        val mainPane = Pane().apply { 
            this.contentOffsetY.bind { 
                Interpolation.pow3In.apply(scoreCardTransition.use()) * -40f
            }
        }
        val fadeInPane = Pane().apply {
            this.opacity.bind {
                Interpolation.smoother.apply(0f, 1f, 1f - scoreCardTransition.use())
            }
        }
        val staticPane = Pane()
        scoreCardSceneRoot += RectElement(Color(0.25f, 0.25f, 0.25f, 0.35f)).apply { 
            this.opacity.bind {
                Interpolation.smoother.apply(0f, 1f, (1f - scoreCardTransition.use()) * 2).coerceIn(0f, 1f)
            }
        }
        scoreCardSceneRoot += fadeInPane
        scoreCardSceneRoot += mainPane
        scoreCardSceneRoot += staticPane
        
        fun createTextLabelOption(option: PauseOption): TextLabel {
            val selectedLabelColor = Color(0f, 1f, 1f, 1f)
            val unselectedLabelColor = Color(1f, 1f, 1f, 1f)
            return TextLabel(binding = { option.text.use() }, font = main.fontMainMenuMain).apply {
                Anchor.TopLeft.configure(this)
                this.disabled.bind { !option.enabled.use() }
                this.textColor.bind {
                    if (apparentDisabledState.use()) {
                        Color.GRAY
                    } else if (selectedScoreCardOption.use() == option) {
                        selectedLabelColor
                    } else {
                        unselectedLabelColor
                    }
                }
                this.bounds.height.set(32f)
                this.padding.set(Insets(2f, 2f, 12f, 2f))
                this.renderAlign.set(Align.left)
                this.textAlign.set(TextAlign.LEFT)
                this += ArrowNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["arrow_pointer_finger"])).apply {
                    Anchor.CentreLeft.configure(this, offsetY = 4f)
                    this.bounds.height.set(48f)
                    this.bindWidthToSelfHeight()
                    this.bounds.x.bind { -(bounds.width.use() + 12f) }
                    this.visible.bind { selectedScoreCardOption.use() == option }
                }
            }
        }
        
        val dialog = RectElement(Color().grey(0f, 0.85f)).apply {
            Anchor.Centre.configure(this)    
            this.border.set(Insets(8f))
            this.borderStyle.set(SolidBorder(Color.WHITE).apply { 
                this.roundedCorners.set(true)
            })
            this.bounds.width.set(500f)
            this.bounds.height.set(350f)
            this.padding.set(Insets(16f))
        }
        mainPane += dialog

        failScoreCardOptions = listOfNotNull(
                PauseOption("play.pause.startOver", true) { startOverPauseAction() },
                if (!couldSkipLevelEventually) null else PauseOption(StoryL10N.getVar("play.pause.skipThisLevel"), false) {
                    quitPauseAction(ExitReason.Skipped)
                }.apply {
                    this.enabled.bind { previousFailureCount + currentFailureCount.use() >= contract.skipAfterNFailures }
                },
                PauseOption(StoryL10N.getVar("play.pause.quit"), true) {
                    quitPauseAction(ExitReason.Quit(currentFailureCount.get()))
                },
        )
        successScoreCardOptions = listOf(
                PauseOption(StoryL10N.getVar("play.pause.continue"), true) {
                    quitPauseAction(successExitReason)
                },
//                PauseOption("play.pause.startOver", true) { startOverPauseAction() }, // TODO Callback problem: Starting over with a passing score means it doesn't get reflected back in the callback
        )
        
        val failPane = VBox().apply { 
            this.visible.bind { lastResultFlag.use() is ResultFlag.Fail }
            this.spacing.set(8f)
            
            this.temporarilyDisableLayouts { 
                this += TextLabel(binding = {
                    val resultFlag = (lastResultFlag.use() as? ResultFlag.Fail) ?: ResultFlag.Fail.Generic
                    resultFlag.tagline.use()
                }, font = main.fontMainMenuHeading).apply {
                    this.bounds.height.set(80f)    
                    this.textColor.set(Color.WHITE)
                    this.renderAlign.set(RenderAlign.center)
                    this.doLineWrapping.set(true)
                    this.margin.set(Insets(1f, 1f, 4f, 4f))
                }
                this += RectElement(Color().grey(0.8f)).apply { 
                    this.bounds.height.set(2f)
                }
                
                this += HBox().apply { 
                    this.bounds.height.set(70f)
                    this.spacing.set(4f)
                    this.margin.set(Insets(16f, 18f, 2f, 2f))
                    this.align.set(HBox.Align.CENTRE)
                    
                    this.temporarilyDisableLayouts { 
                        val bossModifierModule = engine.modifiers.getModifierModuleByType<BossModifierModule>()
                        val progress: ReadOnlyIntVar = if (bossModifierModule != null) {
                            val bossHealth = bossModifierModule.bossHealth
                            IntVar(eager = true) {
                                ((1f - (bossHealth.currentHP.use() / bossHealth.maxHP.use().coerceAtLeast(1).toFloat())) * 100)
                                    .roundToInt().coerceIn(0, 99)
                            }
                        } else IntVar(eager = true) {
                            (engineBeat.use() / container.stopPosition.use().coerceAtLeast(1f) * 100).roundToInt()
                                .coerceIn(0, 99)
                        }
                        this += TextLabel(StoryL10N.getVar("play.scoreCard.progress"), font = main.fontMainMenuMain).apply {
                            this.bindWidthToParent(multiplier = 0.275f)
                            this.textColor.set(Color.WHITE)
                            this.renderAlign.set(RenderAlign.right)
                            this.margin.set(Insets(1f, 1f, 4f, 6f))
                        }
                        this += Pane().apply { 
                            this.bindWidthToParent(multiplier = 0.55f)
                            val borderColor = Color.WHITE
                            val borderSize = 4f
                            this.border.set(Insets(borderSize))
                            this.borderStyle.set(SolidBorder(borderColor))
                            this.padding.set(Insets(borderSize))
                            this += RectElement(borderColor).apply {
                                this.bindWidthToParent(multiplierBinding = { progress.use() / 100f }) { 0f }
                            }
                        }
                        this += TextLabel(StoryL10N.getVar("play.scoreCard.percentage", Var { listOf(progress.use()) }), font = main.fontMainMenuMain).apply {
                            this.bindWidthToParent(multiplier = 0.125f)
                            this.textColor.set(Color.WHITE)
                            this.renderAlign.set(RenderAlign.left)
                            this.margin.set(Insets(1f, 1f, 6f, 4f))
                        }
                    }
                }

                val allOptions = failScoreCardOptions
                allOptions.forEach { opt ->
                    this += createTextLabelOption(opt).apply {
                        Anchor.TopCentre.xConfigure(this, offsetX = 48f)    
                        this.bindWidthToParent(multiplier = 0.5f)
                    }
                }
            }
        }
        dialog += failPane
        
        val scorePane = VBox().apply {
            this.visible.bind { !failPane.visible.use() }
            this.spacing.set(8f)

            this.temporarilyDisableLayouts {
                this += TextLabel(StoryL10N.getVar("play.scoreCard.scoreCardTitle"), font = main.fontMainMenuHeading).apply {
                    this.bounds.height.set(50f)
                    this.textColor.set(Color.WHITE)
                    this.renderAlign.set(RenderAlign.center)
                    this.doLineWrapping.set(true)
                    this.margin.set(Insets(1f, 1f, 4f, 4f))
                }
                this += RectElement(Color().grey(0.8f)).apply {
                    this.bounds.height.set(2f)
                }


                this += HBox().apply {
                    this.bounds.height.set(90f)
                    this.spacing.set(4f)
                    this.margin.set(Insets(16f, 18f, 2f, 2f))
                    this.align.set(HBox.Align.CENTRE)

                    this.temporarilyDisableLayouts {
                        val progress: ReadOnlyFloatVar = scoreBar
                        this += Pane().apply {
                            this.bindWidthToParent(multiplier = 0.85f)
                            
                            val borderColor = Color.WHITE
                            val borderSize = 4f
                            this += Pane().apply {
                                this.border.set(Insets(borderSize))
                                this.borderStyle.set(SolidBorder(borderColor))
                                this.padding.set(Insets(borderSize * 1.25f))
                                
                                this += RectElement(borderColor).apply {
                                    this.bindWidthToParent(multiplierBinding = { progress.use() / 100f }) { 0f }
                                }
                            }
                            this += TextLabel(binding = {
                                if (contract.immediatePass) {
                                    if (progress.use() < 100f) "" else StoryL10N.getValue("play.scoreCard.pass")
                                } else {
                                    val minToPass = contract.minimumScore
                                    val minToPassDenom = "[scale=0.5]/${minToPass}[]"
                                    if (!showScoreOnScoreCard.use()) {
                                        ""
                                    } else {
                                        // Invisible minToPassDenom on the left so everything is centred still
                                        // Spaces at end prevent a vertical alignment issue
                                        " [color=#FFFFFF00]${minToPassDenom}[]${progress.use().toInt()}${minToPassDenom} "
                                    }
                                }
                            }).apply {
                                this.renderAlign.set(RenderAlign.center)
                                this.padding.set(Insets(0f, 6f * 0.5f, 16f, 16f))
                                this.textColor.set(Color.WHITE)
                                this.markup.set(Markup.createWithSingleFont(main.fontResultsScore, lenientMode = true))
                                this.setScaleXY(0.5f)
                            }
                        }
                    }
                }
            }

            val optionsFade = FloatVar(1f)
            val successOptions: List<UIElement> = listOf(Pane().apply { 
                this.bounds.height.set(16f)
            }) + successScoreCardOptions.map { opt ->
                createTextLabelOption(opt).apply {
                    Anchor.TopCentre.xConfigure(this, offsetX = 48f)
                    this.bindWidthToParent(multiplier = 0.5f)
                }
            }
            val failOptions: List<UIElement> = failScoreCardOptions.map { opt ->
                createTextLabelOption(opt).apply {
                    Anchor.TopCentre.xConfigure(this, offsetX = 48f)
                    this.bindWidthToParent(multiplier = 0.5f)
                }
            }
            (successOptions + failOptions).forEach { 
                it.opacity.bind { optionsFade.use() }
            }
            currentScoreCardOptions.addListener { vl ->
                this.temporarilyDisableLayouts {
                    (successOptions + failOptions).forEach { this.removeChild(it) }
                    
                    val list = vl.getOrCompute()
                    optionsFade.set(0f)
                    if (list.isNotEmpty()) {
                        animationHandler.enqueueAnimation(Animation(Interpolation.smoother, 0.25f, 0f, 1f), optionsFade)
                        optionsFade.set(0f)
                    } else {
                        animationHandler.cancelAnimationFor(optionsFade)
                    }
                    when (list) {
                        successScoreCardOptions -> successOptions
                        failScoreCardOptions -> failOptions
                        else -> emptyList()
                    }.forEach { this.addChild(it) }
                }
            }
        }
        dialog += scorePane
        
        fadeInPane += TextLabel(this.keyboardKeybinds.toScoreCardKeyboardString(), font = main.fontMainMenuRodin).apply {
            Anchor.BottomCentre.configure(this)
            this.textColor.set(Color.WHITE)
            this.bounds.width.set(300f)
            this.bounds.height.set(128f)
            this.bgPadding.set(Insets(10f))
            this.renderAlign.set(Align.bottom)
            this.textAlign.set(TextAlign.LEFT)
            this.backgroundColor.set(Color(0f, 0f, 0f, 0.75f))
            this.renderBackground.set(true)
            this.setScaleXY(0.875f)
            this.opacity.set(0f)
            currentScoreCardOptions.addListener {
                val l = it.getOrCompute()
                if (l.isEmpty()) {
                    animationHandler.cancelAnimationFor(this.opacity)
                    this.opacity.set(0f)
                } else {
                    animationHandler.enqueueAnimation(Animation(Interpolation.smoother, 0.5f, 0f, 1f), this.opacity)
                }
            }
        }
    }
    
    private fun startOverPauseAction() {
        playMenuSound("sfx_menu_enter_game")

        val thisScreen: StoryPlayScreen = this
        val resetAction: () -> Unit = {
            resetAndUnpause()
            disableCatchingCursorOnHide = false
        }
        if (shouldCatchCursor()) {
            disableCatchingCursorOnHide = true
            Gdx.input.isCursorCatched = true
        }
        main.screen = TransitionScreen(main, thisScreen, thisScreen,
                WipeTransitionHead(Color.BLACK.cpy(), 0.4f), WipeTransitionTail(Color.BLACK.cpy(), 0.4f)).apply {
            onEntryEnd = resetAction
        }
    }
    
    private fun quitPauseAction(exitReason: ExitReason) {
        quitToScreen(exitToScreen(exitReason), longTransition = exitReason is ExitReason.Passed && contract.id == Contracts.ID_BOSS)
        exitCallback.onExit(exitReason)
        Gdx.app.postRunnable {
            playMenuSound("sfx_pause_exit")
        }
    }

    override fun initializeGameplay() {
        super.initializeGameplay()
        engine.inputter.areInputsLocked = false
        cancelIntroCard()
        closeScoreCard()
        Gdx.app.postRunnable {
            listOf(StoryAssets.get<Sound>("score_jingle_pass"), StoryAssets.get<Sound>("score_jingle_tryagain"),
                    StoryAssets.get<Sound>("score_jingle_pass_hard")).forEach { it.stop() }
        }
    }

    override fun getSecondsToDelayAtStart(): Float {
        return (gameMode as? AbstractStoryGameMode)?.getSecondsToDelayAtStartOverride() ?: super.getSecondsToDelayAtStart()
    }
    
    private fun shouldPauseDuringIntroCard(): Boolean {
        return (gameMode as? AbstractStoryGameMode)?.shouldPauseWhileInIntroCardOverride() ?: true
    }

    fun initializeIntroCard() {
        introCardTime.set(1f)
        blurStrength.set(1f)
        animationHandler.cancelAnimationFor(blurStrength)
        animationHandler.enqueueAnimation(Animation(Interpolation.pow2Out, 0.5f, 1f, 0f, delay = introCardDuration - introCardUnblurDuration), blurStrength)
        
        if (shouldPauseDuringIntroCard()) {
            shouldUpdateTiming.set(false)
            soundSystem.setPaused(true)
        } else {
            shouldUpdateTiming.set(true)
            soundSystem.setPaused(false)
        }
        
        // Play jingle
        val jingleID = contract.jingleType.soundID
        if (jingleID != null) {
            playMenuSound(StoryAssets.get<Sound>(jingleID))
        }

        animationHandler.enqueueAnimation(Animation(Interpolation.linear, introCardDuration, 1f, 0f).apply { 
            this.onComplete = {
                cancelIntroCard()
            }                                                                                                     
        }, introCardTime)
    }
    
    fun cancelIntroCard() {
        introCardTime.set(0f)
        animationHandler.cancelAnimationFor(blurStrength)
        blurStrength.set(0f)
        animationHandler.cancelAnimationFor(introCardTime)
        shouldUpdateTiming.set(true)
        soundSystem.setPaused(false)
    }
    
    fun openScoreCard() {
        showingScoreCard.set(true)
        showScoreOnScoreCard.set(false)
        animationHandler.cancelAnimationFor(scoreCardTransition)
        scoreCardTransition.set(1f)
        animationHandler.enqueueAnimation(Animation(Interpolation.linear, scoreCardTransitionTime, 1f, 0f), scoreCardTransition)
    }
    
    fun closeScoreCard() {
        showingScoreCard.set(false)
        scoreCardTransition.set(0f)
        animationHandler.cancelAnimationFor(scoreBar)
        skippableScoreBarAnimation = null
    }
    
    override fun renderGameplay(delta: Float) {
        this.engineBeat.set(engine.beat)
        
        framebufferMgr.frameUpdate()
        
        val batch = this.batch
        val frameBuffer = this.framebufferMgr.getFramebuffer(0)
        val frameBuffer2 = this.framebufferMgr.getFramebuffer(1)
        
        if (frameBuffer != null && frameBuffer2 != null) {
            frameBuffer.begin()
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            worldRenderer.render(batch)
            frameBuffer.end()


            // Do blur
            val passes = 7
            val blurStrength = this.blurStrength.get()
            var readBuffer: NestedFrameBuffer = frameBuffer
            var writeBuffer: NestedFrameBuffer = frameBuffer2

            val cam = this.uiCamera // 1280x720 camera always
            val shader = this.blurShader
            batch.projectionMatrix = cam.combined

            if (blurStrength > 0f) {
                batch.shader = shader
                batch.begin()
                batch.setColor(1f, 1f, 1f, 1f)

                shader.setUniformf("resolution", max(cam.viewportHeight, cam.viewportWidth))

                for (i in 0 until passes * 2) {
                    val radius = (passes - i / 2) * blurStrength

                    writeBuffer.begin()
                    if (i % 2 == 0) {
                        shader.setUniformf("dir", radius, 0f)
                    } else {
                        shader.setUniformf("dir", 0f, radius)
                    }
                    val bufferTex = readBuffer.colorBufferTexture
                    batch.draw(bufferTex, 0f, 0f, cam.viewportWidth, cam.viewportHeight, 0, 0, bufferTex.width, bufferTex.height, false, true)
                    batch.flush()
                    writeBuffer.end()


                    // Swap buffers
                    val tmp = readBuffer
                    readBuffer = writeBuffer
                    writeBuffer = tmp
                }

                batch.end()
                batch.shader = null // Reset shader
            }


            // Render final buffer to screen
            uiViewport.apply() // 1280x720 viewport always
            batch.begin()
            val bufferTex = readBuffer.colorBufferTexture
            batch.draw(bufferTex, 0f, 0f, cam.viewportWidth, cam.viewportHeight, 0, 0, bufferTex.width, bufferTex.height, false, true)
            batch.end()

            main.resetViewportToScreen()
        }
    }

    override fun renderAfterGameplay(delta: Float, camera: OrthographicCamera) {
        super.renderAfterGameplay(delta, camera)

        animationHandler.frameUpdate(delta)
        
        if (inIntroCard.get()) {
            introCardSceneRoot.renderAsRoot(batch)
            main.resetViewportToScreen()
        }
        if (showingScoreCard.get()) {
            scoreCardSceneRoot.renderAsRoot(batch)
            main.resetViewportToScreen()
        }
    }

    override fun onResultFlagChanged(flag: ResultFlag) {
        super.onResultFlagChanged(flag)
        
        if (flag is ResultFlag.Fail) {
            engine.inputter.areInputsLocked = true // Unlocked in initialize()
            engine.removeEvents(engine.events.filterIsInstance<EventEndState>())
            
            val currentBeat = engine.beat
            val currentSec = engine.seconds
            
            val endAfterSec: Float
            val silentAfterSec: Float
            if (flag is ResultFlag.Fail.MonsterGoal) {
                silentAfterSec = 0.25f
                endAfterSec = 1.75f
            } else {
                endAfterSec = 1.5f
                silentAfterSec = endAfterSec / 2f
            }


            val endAtBeat = engine.tempos.secondsToBeats(currentSec + endAfterSec, disregardSwing = true)
            val silentAtBeat = engine.tempos.secondsToBeats(currentSec + silentAfterSec, disregardSwing = true)

            engine.addEvent(EventEndState(engine, endAtBeat))
            engine.addEvent(ChangeMusicVolMultiplierEvent(engine, 1f, 0f, currentBeat, silentAtBeat - currentBeat))

            // Special handling for monster goal failure
            if (flag is ResultFlag.Fail.MonsterGoal) {
                val startCrushAfterSec = 0.5f
                val fadeOutDurationSec = worldRenderer.monsterGoalRendering.crushDurationSec
                val crushAfterBeat = engine.tempos.secondsToBeats(currentSec + startCrushAfterSec, disregardSwing = true)
                val finishFadeOutAtBeat = engine.tempos.secondsToBeats(currentSec + startCrushAfterSec + fadeOutDurationSec, disregardSwing = true)

                engine.addEvent(object : Event(engine) {
                    override fun onStart(currentBeat: Float) {
                        worldRenderer.monsterGoalRendering.startCrush()
                        playMenuSound("sfx_monster_goal_fail", volume = 0.9f) // This is a menu SFX b/c it has to play even after the end event is fired
                    }

                    override fun onUpdate(currentBeat: Float) {
                        val percentage = if (this.width <= 0f) 1f else ((currentBeat - this.beat) / this.width).coerceIn(0f, 1f)
                        this.engine.soundInterface.setGlobalGain(1f - percentage)
                    }
                }.apply { 
                    this.beat = crushAfterBeat
                    this.width = finishFadeOutAtBeat - this.beat
                })
            }
        }
    }

    override fun onEndSignalFired() {
        super.onEndSignalFired()
        
        this.shouldUpdateTiming.set(false)

        engine.inputter.areInputsLocked = true // Unlocked in initialize()
        animationHandler.enqueueAnimation(Animation(Interpolation.smoother, 0.25f, 0f, 1f), blurStrength)
        
        if (engine.resultFlag.getOrCompute() is ResultFlag.Fail) {
            incrementCurrentFailureCount()
            currentScoreCardOptions.set(failScoreCardOptions)
        } else {
            engine.resultFlag.set(ResultFlag.None)
            currentScoreCardOptions.set(emptyList())
            scoreBar.set(0f)
            
            val delay = scoreCardTransitionTime + 0.25f
            
            if (contract.immediatePass) {
                val inputter = engine.inputter
                val scoreBase = inputter.computeScore()
                // Just a delay, then show Pass! with hit + music, options
                successExitReason = ExitReason.Passed(scoreBase.scoreInt, scoreBase.skillStar, scoreBase.noMiss)
                
                animationHandler.enqueueAnimation(Animation(Interpolation.linear, 0f, 0f, 100f, delay).apply {
                    this.onComplete = {
                        playMenuSound(StoryAssets.get<Sound>("score_finish"))
                        playMenuSound(StoryAssets.get<Sound>("score_jingle_pass"))
                        
                        animationHandler.enqueueAnimation(Animation(Interpolation.linear, 0f, 100f, 100f, 0.5f).apply {
                            this.onComplete = {
                                currentScoreCardOptions.set(successScoreCardOptions)
                                if (contract.shouldPlayPostResultsMixOnPass) {
                                    storySession.musicHandler.transitionToPostResultsMix()
                                }
                            }
                        }, scoreBar)
                    }
                }, scoreBar)
            } else {
                // Delay, roll up score, hit+music, options
                val inputter = engine.inputter
                val scoreBase = inputter.computeScore()
                val scoreInt: Int = scoreBase.scoreInt
                
                successExitReason = ExitReason.Passed(scoreInt, scoreBase.skillStar, scoreBase.noMiss)

                val animation = Animation(Interpolation.linear, (145f / 60f) * (scoreInt / 100f), 0f, scoreInt.toFloat(), delay).apply {
                    val fillingSound = StoryAssets.get<Sound>("score_filling")
                    var fillingSoundID = -1L

                    this.onStart = {
                        skippableScoreBarAnimation = this
                        
                        fillingSoundID = playMenuSound(fillingSound).second
                        showScoreOnScoreCard.set(true)
                    }
                    this.onComplete = {
                        skippableScoreBarAnimation = null
                        
                        val passed = scoreInt >= contract.minimumScore

                        if (!passed) {
                            incrementCurrentFailureCount()
                        }

                        fillingSound.stop(fillingSoundID)
                        playMenuSound(StoryAssets.get<Sound>("score_finish"))
                        playMenuSound(StoryAssets.get<Sound>(if (passed) "score_jingle_pass" else "score_jingle_tryagain"))

                        if (passed && contract.shouldPlayPostResultsMixOnPass) {
                            storySession.musicHandler.transitionToPostResultsMix()
                        }

                        animationHandler.enqueueAnimation(Animation(Interpolation.linear, 0f, scoreInt.toFloat(), scoreInt.toFloat(), 0.5f).apply {
                            this.onComplete = {
                                currentScoreCardOptions.set(if (passed) successScoreCardOptions else failScoreCardOptions)
                            }
                        }, scoreBar)
                    }
                }
                animationHandler.enqueueAnimation(animation, scoreBar)
            }
            
        }
        openScoreCard()
    }

    override fun renderUpdate() {
        super.renderUpdate()
        storySession.renderUpdate()
    }
    
    private fun incrementCurrentFailureCount() {
        currentFailureCount.incrementAndGet()
    }

    private fun pauseGameNoCheck(playSound: Boolean) {
        super.pauseGame(playSound)
    }

    private fun unpauseGameNoCheck(playSound: Boolean) {
        super.unpauseGame(playSound)
    }

    override fun pauseGame(playSound: Boolean) {
        if (canPauseGame.get()) {
            pauseGameNoCheck(playSound)
        }
    }

    override fun unpauseGame(playSound: Boolean) {
        if (canPauseGame.get()) {
            unpauseGameNoCheck(playSound)
        }
    }

    private fun attemptSelectCurrentScoreCardOption() {
        val pauseOp = selectedScoreCardOption.getOrCompute()
        if (pauseOp != null && pauseOp.enabled.get()) {
            pauseOp.action()
        } else {
            val animation = skippableScoreBarAnimation
            if (animation != null) {
                animationHandler.cancelAnimation(animation)
            }
        }
    }

    private fun changeScoreCardSelectionTo(option: PauseOption): Boolean {
        if (selectedScoreCardOption.getOrCompute() != option && option.enabled.get()) {
            selectedScoreCardOption.set(option)
            playMenuSound("sfx_menu_blip")
            return true
        }
        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        if (this.inIntroCard.get()) {
            return false
        }
        
        var consumed = false
        if (main.screen === this && !isPaused.get() && showingScoreCard.get()) {

            fun moveArrow(up: Boolean) {
                val options = this.currentScoreCardOptions.getOrCompute()
                if (options.isNotEmpty() && !options.all { !it.enabled.get() }) {
                    val maxSelectionSize = options.size
                    val incrementAmt = if (up) -1 else 1
                    val currentSelected = this.selectedScoreCardOption.getOrCompute()
                    val currentIndex = options.indexOf(currentSelected)
                    var increment = incrementAmt
                    var nextIndex: Int
                    do {
                        nextIndex = (currentIndex + increment + maxSelectionSize) % maxSelectionSize
                        if (changeScoreCardSelectionTo(options[nextIndex])) {
                            consumed = true
                            break
                        }
                        increment += incrementAmt
                    } while (nextIndex != currentIndex)
                }
            }

            fun select() {
                attemptSelectCurrentScoreCardOption()
                consumed = true
            }


            when (keycode) {
                keyboardKeybinds.buttonDpadUp -> moveArrow(up = true)
                keyboardKeybinds.buttonDpadDown -> moveArrow(up = false)
                keyboardKeybinds.buttonA -> select()
                
                // Note: These defaults are last in case up/down/enter are rebound for some reason
                Keys.UP -> moveArrow(up = true)
                Keys.DOWN -> moveArrow(up = false)
                Keys.ENTER -> select()
            }
            
            if (consumed) {
                return true
            }
        }
        
        return super.keyDown(keycode)
    }

    override fun uncatchCursorOnHide(): Boolean {
        return super.uncatchCursorOnHide() && !disableCatchingCursorOnHide
    }

    override fun _dispose() {
        super._dispose()
        this.framebufferMgr.disposeQuietly()
        this.blurShader.disposeQuietly()
    }
}