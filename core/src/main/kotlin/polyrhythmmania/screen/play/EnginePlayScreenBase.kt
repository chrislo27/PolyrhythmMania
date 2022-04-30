package polyrhythmmania.screen.play

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Align
import paintbox.binding.FloatVar
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.transition.*
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.animation.Animation
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.fillRect
import paintbox.util.gdxutils.prepareStencilMask
import paintbox.util.gdxutils.useStencilMask
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.achievements.Achievements
import polyrhythmmania.container.Container
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.Score
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.gamemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.world.EndlessType
import polyrhythmmania.world.WorldType


class EnginePlayScreenBase(
        main: PRManiaGame, playTimeType: PlayTimeType?,
        container: Container,
        challenges: Challenges, inputCalibration: InputCalibration,
        gameMode: GameMode?, resultsBehaviour: ResultsBehaviour
) : AbstractEnginePlayScreen(main, playTimeType, container, challenges, inputCalibration, gameMode, resultsBehaviour) {

    companion object; // Used for early init

    private var endlessPrPauseTime: Float = 0f


    // TODO remove these
    private val pauseBg: PauseBackground = PauseBackground()
    private val panelAnimationValue: FloatVar = FloatVar(0f)
    private var activePanelAnimation: Animation? = null
    private val topPane: Pane
    private val bottomPane: Pane
    private val titleLabel: TextLabel
    
    init {
        // TODO move me
        // Score achievements for endless-type modes
        engine.inputter.endlessScore.score.addListener { scoreVar ->
            if (engine.world.worldMode.endlessType == EndlessType.REGULAR_ENDLESS && engine.areStatisticsEnabled) {
                val newScore = scoreVar.getOrCompute()
                when (engine.world.worldMode.type) {
                    WorldType.POLYRHYTHM -> {
                        if (gameMode is EndlessPolyrhythm) {
                            if (gameMode.dailyChallenge != null) {
                                listOf(Achievements.dailyScore25, Achievements.dailyScore50,
                                        Achievements.dailyScore75, Achievements.dailyScore100,
                                        Achievements.dailyScore125).forEach {
                                    Achievements.attemptAwardScoreAchievement(it, newScore)
                                }
                            } else {
                                listOf(Achievements.endlessScore25, Achievements.endlessScore50,
                                        Achievements.endlessScore75, Achievements.endlessScore100,
                                        Achievements.endlessScore125).forEach {
                                    Achievements.attemptAwardScoreAchievement(it, newScore)
                                }

                                if (gameMode.disableLifeRegen) {
                                    Achievements.attemptAwardScoreAchievement(Achievements.endlessNoLifeRegen100, newScore)
                                }
                                if (engine.inputter.endlessScore.maxLives.get() == 1) { // Daredevil
                                    Achievements.attemptAwardScoreAchievement(Achievements.endlessDaredevil100, newScore)
                                }
                                if (main.settings.masterVolumeSetting.getOrCompute() == 0) {
                                    Achievements.attemptAwardScoreAchievement(Achievements.endlessSilent50, newScore)
                                }
                            }
                        }

                    }
                    WorldType.DUNK -> {
                        listOf(Achievements.dunkScore10, Achievements.dunkScore20, Achievements.dunkScore30,
                                Achievements.dunkScore50).forEach {
                            Achievements.attemptAwardScoreAchievement(it, newScore)
                        }
                    }
                    WorldType.ASSEMBLE -> {
                        // NO-OP
                    }
                }
            }
        }
    }
    
    init {
        val optionList = mutableListOf<PauseOption>()
        optionList += PauseOption(if (engine.autoInputs) "play.pause.resume.robotMode" else "play.pause.resume", true) {
            unpauseGame(true)
        }
        optionList += PauseOption("play.pause.startOver", !(gameMode is EndlessPolyrhythm && gameMode.dailyChallenge != null)) {
            playMenuSound("sfx_menu_enter_game")

            val thisScreen: EnginePlayScreenBase = this
            val resetAction: () -> Unit = {
                resetAndUnpause()
            }
            main.screen = TransitionScreen(main, thisScreen, thisScreen,
                    WipeTransitionHead(Color.BLACK.cpy(), 0.4f), WipeTransitionTail(Color.BLACK.cpy(), 0.4f)).apply {
                onEntryEnd = resetAction
                onStart = {
                    Gdx.input.isCursorCatched = true
                }
            }
        }
        optionList += PauseOption("play.pause.quitToMainMenu", true) {
            quitToMainMenu()
            Gdx.app.postRunnable {
                playMenuSound("sfx_pause_exit")
            }
        }
        this.pauseOptions.set(optionList)
    }


    init { // TODO remove these
        var nextLayer: UIElement = sceneRoot
        fun addLayer(element: UIElement) {
            nextLayer += element
            nextLayer = element
        }
        addLayer(RectElement(Color(0f, 0f, 0f, 0f)))

        topPane = Pane().apply {
            Anchor.TopLeft.configure(this, offsetY = {
                val h = bounds.height.use()
                -h + panelAnimationValue.use() * h
            })
            this.bindHeightToParent(multiplier = 0.3333f)
            this.bindWidthToSelfHeight(multiplier = 1f / pauseBg.triangleSlope)
            this.padding.set(Insets(36f, 0f, 64f, 0f))
        }
        nextLayer += topPane
        bottomPane = Pane().apply {
            Anchor.BottomRight.configure(this, offsetY = {
                val h = bounds.height.use()
                h + panelAnimationValue.use() * -h
            })
            this.bindWidthToParent(multiplier = 0.6666f)
            this.bindHeightToSelfWidth(multiplier = pauseBg.triangleSlope)
        }
        nextLayer += bottomPane

        val leftVbox = VBox().apply {
            this.spacing.set(16f)
            this.bounds.height.set(300f)
        }
        topPane += leftVbox

        titleLabel = TextLabel(binding = { Localization.getVar("play.pause.title").use() }, font = main.fontPauseMenuTitle).apply {
            this.textColor.set(Color.WHITE)
            this.bounds.height.set(128f)
            this.renderAlign.set(Align.left)
        }

        leftVbox.temporarilyDisableLayouts {
            leftVbox += titleLabel
        }

        val transparentBlack = Color(0f, 0f, 0f, 0.75f)
        bottomPane += TextLabel(keyboardKeybinds.toKeyboardString(true, true), font = main.fontMainMenuRodin).apply {
            Anchor.BottomRight.configure(this)
            this.textColor.set(Color.WHITE)
            this.bounds.width.set(550f)
            this.bounds.height.set(80f)
            this.bgPadding.set(Insets(12f))
            this.renderAlign.set(Align.bottomRight)
            this.textAlign.set(TextAlign.LEFT)
            this.backgroundColor.set(transparentBlack)
            this.renderBackground.set(true)
        }

        val optionsBorderSize = 12f
        val optionsContentHeight = 144f
        val optionsBg = RectElement(transparentBlack).apply {
            Anchor.BottomRight.configure(this, offsetY = -80f, offsetX = -15f)
            this.bounds.width.set(275f + optionsBorderSize * 2)
            this.bounds.height.set(optionsContentHeight + optionsBorderSize * 2)
            this.border.set(Insets(optionsBorderSize))
            this.borderStyle.set(SolidBorder(transparentBlack).apply {
                this.roundedCorners.set(true)
            })
        }
        bottomPane += optionsBg

        val selectedLabelColor = Color(0f, 1f, 1f, 1f)
        val unselectedLabelColor = Color(1f, 1f, 1f, 1f)
        fun createTextLabelOption(option: PauseOption, index: Int, allOptions: List<PauseOption>): TextLabel {
            return TextLabel(binding = { Localization.getVar(option.localizationKey).use() }, font = main.fontMainMenuMain).apply {
                Anchor.TopLeft.configure(this)
                this.disabled.set(!option.enabled)
                this.textColor.bind {
                    if (apparentDisabledState.use()) {
                        Color.GRAY
                    } else if (selectedPauseOption.use() == option) {
                        selectedLabelColor
                    } else {
                        unselectedLabelColor
                    }
                }
                this.bounds.height.set(optionsContentHeight / allOptions.size)
                this.padding.set(Insets(2f, 2f, 12f, 12f))
                this.renderAlign.set(Align.left)
                this.textAlign.set(TextAlign.LEFT)
                this += ArrowNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["arrow_pointer_finger"])).apply {
                    Anchor.CentreLeft.configure(this, offsetY = 4f)
                    this.bounds.height.set(64f)
                    this.bindWidthToSelfHeight()
                    this.bounds.x.bind { -(bounds.width.use() + optionsBorderSize * 2 + 2f) }
                    this.visible.bind { selectedPauseOption.use() == option }
                }
                this.setOnAction {
                    attemptSelectCurrentPauseOption()
                }
                this.setOnHoverStart {
                    changeSelectionTo(option)
                }
            }
        }

        optionsBg += VBox().apply {
            this.spacing.set(0f)

            fun refreshPauseOptions(optionList: List<PauseOption>) {
                this.removeAllChildren()
                this.temporarilyDisableLayouts {
                    optionList.forEachIndexed { index, op ->
                        this += createTextLabelOption(op, index, optionList)
                    }
                }
            }
            pauseOptions.addListener {
                val optionList = it.getOrCompute()
                refreshPauseOptions(optionList)
            }
            refreshPauseOptions(pauseOptions.getOrCompute())
        }
    }
    
    override fun copyThisScreenForResults(scoreObj: Score, resultsBehaviour: ResultsBehaviour): AbstractEnginePlayScreen {
        return EnginePlayScreenBase(main, playTimeType, container, challenges, inputCalibration, gameMode,
                if (resultsBehaviour is ResultsBehaviour.ShowResults)
                    resultsBehaviour.copy(previousHighScore = if (scoreObj.newHighScore) 
                        scoreObj.scoreInt 
                    else resultsBehaviour.previousHighScore)
                else resultsBehaviour)
    }

    override fun renderGameplay(delta: Float) {
        super.renderGameplay(delta)

        if (isPaused.get()) {
            endlessPrPauseTime += Gdx.graphics.deltaTime
        }
    }

    override fun renderAfterGameplay(delta: Float, camera: OrthographicCamera) {
        super.renderAfterGameplay(delta, camera)
        
        if (isPaused.get()) { // TODO move me
            val width = camera.viewportWidth
            val height = camera.viewportHeight
            val shapeRenderer = main.shapeRenderer
            shapeRenderer.projectionMatrix = camera.combined
            uiViewport.apply()

            batch.setColor(1f, 1f, 1f, 0.5f)
            batch.fillRect(0f, 0f, width, height)
            batch.setColor(1f, 1f, 1f, 1f)

            val pauseBg = this.pauseBg

            val topLeftX1 = topPane.bounds.x.get()
            val topLeftY1 = height - (topPane.bounds.y.get() + topPane.bounds.height.get())
            val topLeftX2 = topLeftX1
            val topLeftY2 = height - (topPane.bounds.y.get())
            val topLeftY3 = topLeftY2
            val topLeftX3 = topPane.bounds.x.get() + topPane.bounds.width.get()
            val botRightX1 = bottomPane.bounds.x.get()
            val botRightY1 = height - (bottomPane.bounds.y.get() + bottomPane.bounds.height.get())
            val botRightX2 = bottomPane.bounds.x.get() + bottomPane.bounds.width.get()
            val botRightY2 = botRightY1
            val botRightX3 = botRightX2
            val botRightY3 = height - (bottomPane.bounds.y.get())
            val triLineWidth = 12f
            shapeRenderer.prepareStencilMask(batch) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                shapeRenderer.triangle(topLeftX1, topLeftY1, topLeftX2, topLeftY2, topLeftX3, topLeftY3)
                shapeRenderer.triangle(botRightX1, botRightY1, botRightX2, botRightY2, botRightX3, botRightY3)
                shapeRenderer.end()
            }.useStencilMask {
                batch.setColor(1f, 1f, 1f, 1f)
                pauseBg.render(delta, batch, camera)
                batch.setColor(1f, 1f, 1f, 1f)
            }

            // Draw lines to hide aliasing
            val shapeDrawer = this.shapeDrawer
            shapeDrawer.setColor(0f, 0f, 0f, 1f)
            shapeDrawer.line(topLeftX1 - triLineWidth, topLeftY1 - triLineWidth * pauseBg.triangleSlope,
                    topLeftX3 + triLineWidth, topLeftY3 + triLineWidth * pauseBg.triangleSlope, triLineWidth, false)
            shapeDrawer.line(botRightX1 - triLineWidth, botRightY1 - triLineWidth * pauseBg.triangleSlope,
                    botRightX3 + triLineWidth, botRightY3 + triLineWidth * pauseBg.triangleSlope, triLineWidth, false)
            shapeDrawer.setColor(1f, 1f, 1f, 1f)
            batch.setColor(1f, 1f, 1f, 1f)

            batch.flush()
            shapeRenderer.projectionMatrix = main.nativeCamera.combined

            sceneRoot.renderAsRoot(batch)
        }
    }

    override fun pauseGame(playSound: Boolean) {
        super.pauseGame(playSound)

        endlessPrPauseTime = 0f

        // TODO move me
        pauseBg.randomizeSeed()
        panelAnimationValue.set(0f)
        val ani = Animation(Interpolation.smoother, 0.25f, 0f, 1f).apply {
            onComplete = {
                activePanelAnimation = null
            }
        }
        val oldAni = this.activePanelAnimation
        if (oldAni != null) {
            sceneRoot.animations.cancelAnimation(oldAni)
        }
        this.activePanelAnimation = ani
        sceneRoot.animations.enqueueAnimation(ani, panelAnimationValue)
    }

    override fun unpauseGame(playSound: Boolean) {
        super.unpauseGame(playSound)

        if (gameMode != null && gameMode is EndlessPolyrhythm) {
            gameMode.submitPauseTime(this.endlessPrPauseTime)
        }
        
        // TODO move me
        panelAnimationValue.set(0f)
    }
}