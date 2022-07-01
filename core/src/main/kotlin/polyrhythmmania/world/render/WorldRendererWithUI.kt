package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import paintbox.binding.*
import paintbox.font.Markup
import paintbox.font.TextAlign
import paintbox.font.TextRun
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.animation.Animation
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.VBox
import paintbox.util.MathHelper
import paintbox.util.gdxutils.drawCompressed
import paintbox.util.gdxutils.grey
import paintbox.util.gdxutils.scaleMul
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.TextBoxStyle
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.ui.TextboxPane
import polyrhythmmania.util.RodinSpecialChars
import polyrhythmmania.world.World
import polyrhythmmania.world.tileset.Tileset
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class WorldRendererWithUI(world: World, tileset: Tileset, val engine: Engine)
    : WorldRenderer(world, tileset) {

    data class SongInfoCard(var text: String = "", var secondsStart: Float = -10000f) {
        companion object {
            const val TRANSITION_TIME: Float = 0.5f
        }

        var deployed: Boolean = false

        fun isVisible(currentSeconds: Float): Boolean {
            return currentSeconds >= secondsStart && (deployed || (currentSeconds - secondsStart) <= TRANSITION_TIME)
        }

        fun reset() {
            deployed = false
            text = ""
            secondsStart = -10000f
        }
    }

    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        setToOrtho(false, 1280f, 720f)
        update()
    }
    var renderUI: BooleanVar = BooleanVar(true)
    
    private var hudRedFlash: Float = 0f

    private val uiSceneRoot: SceneRoot = SceneRoot(uiCamera)
    private val baseMarkup: Markup = Markup(mapOf(
            "prmania_icons" to PRManiaGame.instance.fontIcons,
            "moretimes" to PRManiaGame.instance.fontGameMoreTimes,
            "bordered" to PRManiaGame.instance.fontGameUIText,
            "practiceclear" to PRManiaGame.instance.fontGamePracticeClear,
            "mainmenu_main" to PRManiaGame.instance.fontMainMenuMain,
            "mainmenu_thin" to PRManiaGame.instance.fontMainMenuThin,
            "mainmenu_heading" to PRManiaGame.instance.fontMainMenuHeading,
            "mainmenu_rodin" to PRManiaGame.instance.fontMainMenuRodin,
    ), TextRun(PRManiaGame.instance.fontGameTextbox, ""), lenientMode = true)
    
    val endlessModeRendering: EndlessModeRendering
    private val practiceRendering: PracticeRendering
    private val perfectRendering: PerfectRendering
    val songCardRendering: SongCardRendering
    private val skillStarRendering: SkillStarRendering
    private val textboxRendering: TextBoxRendering
    
    private val allInnerRenderers: List<InnerRendering>

    init {
        this.endlessModeRendering = this.EndlessModeRendering()
        uiSceneRoot += this.endlessModeRendering.uiElement

        this.practiceRendering = this.PracticeRendering()
        uiSceneRoot += this.practiceRendering.uiElement
        
        this.perfectRendering = this.PerfectRendering()
        uiSceneRoot += this.perfectRendering.uiElement
        
        this.songCardRendering = this.SongCardRendering()
        uiSceneRoot += this.songCardRendering.uiElement
        
        this.skillStarRendering = this.SkillStarRendering()
        uiSceneRoot += this.skillStarRendering.uiElement
        
        // "Game Over" pane must be on top of other UI elements, but below the text box
        uiSceneRoot += this.endlessModeRendering.endlessModeGameOverPane

        this.textboxRendering = this.TextBoxRendering()
        uiSceneRoot += this.textboxRendering.uiElement
        
        
        this.allInnerRenderers = listOf(textboxRendering, perfectRendering, practiceRendering, endlessModeRendering, skillStarRendering)
    }

    override fun onWorldReset(world: World) {
        super.onWorldReset(world)
        
        this.allInnerRenderers.forEach { it.onWorldReset(world) }
        
        hudRedFlash = 0f
    }

    fun fireSkillStar() {
        this.skillStarRendering.fireSkillStar()
    }

    override fun render(batch: SpriteBatch) {
        super.render(batch)
        
        // tmpMatrix is still available at this point, will be used to reset camera at the end
        
        batch.projectionMatrix = uiCamera.combined
        batch.begin()

        if (renderUI.get()) {
            renderUI(batch)
        }
        
        batch.end()
        batch.projectionMatrix = tmpMatrix
    }

    private fun renderUI(batch: SpriteBatch) {
        val engine = this.engine
        val modifiers = engine.modifiers
        val uiCam = this.uiCamera
        
        skillStarRendering.renderUI(batch)

        textboxRendering.renderUI(batch)

        perfectRendering.renderUI(batch)

        songCardRendering.renderUI(batch)
        
        practiceRendering.renderUI(batch)

        endlessModeRendering.renderUI(batch)

        uiSceneRoot.renderAsRoot(batch)

        // HUD red flash when endless mode life lost
        if (hudRedFlash > 0f) {
            if (modifiers.endlessScore.flashHudRedWhenLifeLost) {
                batch.setColor(1f, 0f, 0f, hudRedFlash)
                batch.draw(AssetRegistry.get<Texture>("hud_vignette"), 0f, 0f, uiCam.viewportWidth, uiCam.viewportHeight)
                batch.setColor(1f, 1f, 1f, 1f)
            }

            hudRedFlash = (hudRedFlash - (Gdx.graphics.deltaTime / 0.75f)).coerceAtLeast(0f)
        }
    }

    private fun renderSongInfoCard(batch: SpriteBatch, font: BitmapFont,
                                   card: SongInfoCard, bottomRight: Boolean, currentSeconds: Float) {
        if (!card.isVisible(currentSeconds)) return
        val lastPackedColor = batch.packedColor
        val texture: Texture = AssetRegistry["hud_song_card"]
        val height = 42f
        val width = (height / texture.height.coerceAtLeast(1)) * texture.width
        val baselineY = height * 2f

        var progress = Interpolation.circle.apply(((currentSeconds - card.secondsStart) / SongInfoCard.TRANSITION_TIME).coerceIn(0f, 1f))
        if (!card.deployed) {
            progress = 1f - progress
        }
        val x = if (!bottomRight) (width * MathUtils.lerp(-1f, 0f, progress)) else (1280f - width * MathUtils.lerp(0f, 1f, progress))
        val y = baselineY + (if (bottomRight) (-height * 1.1f) else 0f)

        batch.setColor(0f, 0f, 0f, 1f)
        batch.draw(texture, x, y, width, height, 0, 0, texture.width, texture.height, bottomRight, bottomRight)
        batch.setColor(1f, 1f, 1f, 1f)
        val textPadding = 12f
        val textWidth = width * 0.75f - (height + textPadding) /* Triangle part */
        font.setColor(1f, 1f, 1f, 1f)
        font.drawCompressed(batch, card.text,
                if (!bottomRight) (x + width - (height + textPadding) - textWidth) else (x + (height + textPadding)),
                y + (font.capHeight) / 2f + height / 2f,
                textWidth, if (bottomRight) Align.left else Align.right)

        batch.packedColor = lastPackedColor
    }
    
    
    abstract inner class InnerRendering : World.WorldResetListener {
        abstract val uiElement: UIElement
        
        abstract fun renderUI(batch: SpriteBatch)
        
        override fun onWorldReset(world: World) {
        }
    }
    
    inner class TextBoxRendering : InnerRendering() {

        private val textBoxSuperpane: Pane
        private val textBoxDialoguePane: TextboxPane = TextboxPane()
        private val textBoxBlackPane: RectElement = RectElement(Color(0f, 0f, 0f, 0.5f))
        private val textBoxLabel: TextLabel = TextLabel("")
        private val textBoxInputLabel: TextLabel = TextLabel(RodinSpecialChars.BORDERED_A, font = PRManiaGame.instance.fontGameTextbox)
        
        override val uiElement: UIElement get() = textBoxSuperpane
        
        init {
            textBoxSuperpane = Pane().apply {
                Anchor.TopCentre.configure(this, offsetY = 64f)
                this.bounds.height.set(150f)
            }
            textBoxSuperpane += textBoxBlackPane

            textBoxSuperpane += Pane().apply {
                Anchor.TopCentre.configure(this)
                this.bounds.width.set(1000f)

                this += textBoxDialoguePane
                this += textBoxLabel.apply {
                    Anchor.TopCentre.configure(this)
                    this.markup.set(baseMarkup)
                    this.renderAlign.set(Align.center)
                    this.textAlign.set(TextAlign.LEFT)
                }
                this += textBoxInputLabel.apply {
                    this.renderAlign.set(Align.right)
                    this.bounds.width.set(48f)
                    this.bounds.height.set(48f)
                    Anchor.BottomRight.configure(this, offsetX = -10f, offsetY = -6f)
                }
            }
        }

        override fun renderUI(batch: SpriteBatch) {
            val engine = this@WorldRendererWithUI.engine

            val textBox = engine.activeTextBox
            textBoxSuperpane.visible.set(textBox != null)
            if (textBox != null) {
                val style = textBox.textBox.style
                textBoxBlackPane.visible.set(style == TextBoxStyle.BANNER)
                textBoxDialoguePane.visible.set(style == TextBoxStyle.DIALOGUE)

                textBoxLabel.text.set(textBox.textBox.text)
                textBoxLabel.textAlign.set(textBox.textBox.align)
                val textColor = if (style == TextBoxStyle.BANNER) Color.WHITE else Color.BLACK
                textBoxLabel.textColor.set(textColor)
                textBoxInputLabel.text.set(if (textBox.secondsTimer > 0f) "" else {
                    if (textBox.isADown || MathHelper.getSawtoothWave(1.25f) < 0.25f)
                        RodinSpecialChars.FILLED_A else RodinSpecialChars.BORDERED_A
                })
                textBoxInputLabel.textColor.set(textColor)
            }
        }
    }
    
    inner class PerfectRendering : InnerRendering() {

        private val perfectPane: Pane
        private val perfectIcon: ImageNode
        private val perfectIconFlash: ImageNode
        private val perfectIconFailed: ImageNode
        
        override val uiElement: UIElement get() = perfectPane
        
        init {
            perfectIcon = ImageNode(AssetRegistry.get<PackedSheet>("tileset_ui")["perfect"])
            perfectIconFailed = ImageNode(AssetRegistry.get<PackedSheet>("tileset_ui")["perfect_failed"]).apply {
                this.visible.set(false)
            }
            perfectIconFlash = ImageNode(AssetRegistry.get<PackedSheet>("tileset_ui")["perfect_hit"]).apply {
                this.opacity.set(0f)
            }
            perfectPane = Pane().apply {
                Anchor.TopLeft.configure(this, offsetX = 32f, offsetY = 32f)
                this.bounds.width.set(600f)
                this.bounds.height.set(64f)
                this += Pane().apply {
                    this.bindWidthToSelfHeight()
                    this.padding.set(Insets(4f))
                    this += perfectIcon
                    this += perfectIconFlash
                    this += perfectIconFailed
                }
                this += TextLabel(binding = { Localization.getVar("play.perfect").use() },
                        font = PRManiaGame.instance.fontGameGoForPerfect).apply {
                    Anchor.TopRight.configure(this)
                    this.textColor.set(Color.WHITE)
                    this.padding.set(Insets(0f, 0f, 5f, 0f))
                    this.bindWidthToParent(adjust = -64f)
                    this.renderAlign.set(Align.left)
                }
            }
        }

        override fun renderUI(batch: SpriteBatch) {
            val modifiers = this@WorldRendererWithUI.engine.modifiers
            val perfectCh = modifiers.perfectChallenge
            if (perfectCh.enabled.get()) {
                perfectPane.visible.set(true)
                perfectCh.hit = (perfectCh.hit - Gdx.graphics.deltaTime / (if (perfectCh.failed) 0.5f else 0.125f)).coerceIn(0f, 1f)

                perfectIconFlash.opacity.set(if (perfectCh.failed) 0f else perfectCh.hit)
                perfectIcon.visible.set(!perfectCh.failed)
                perfectIconFailed.visible.set(perfectCh.failed)

                if (perfectCh.failed && perfectCh.hit > 0f) {
                    val maxShake = 3
                    val x = MathUtils.randomSign() * MathUtils.random(0, maxShake).toFloat()
                    val y = MathUtils.randomSign() * MathUtils.random(0, maxShake).toFloat()
                    perfectIconFailed.bounds.x.set(x)
                    perfectIconFailed.bounds.y.set(y)
                } else {
                    perfectIconFailed.bounds.x.set(0f)
                    perfectIconFailed.bounds.y.set(0f)
                }
            } else {
                perfectPane.visible.set(false)
            }
        }
    }
    
    inner class PracticeRendering : InnerRendering() {

        private val moreTimesLabel: TextLabel = TextLabel("")
        private val moreTimesVar: IntVar = IntVar(0)

        override val uiElement: UIElement get() = moreTimesLabel
        
        init {
            moreTimesLabel.apply {
                Anchor.BottomRight.configure(this)
                val locVar = Localization.getVar("practice.moreTimes.times", Var { listOf(moreTimesVar.use()) })
                this.text.bind { locVar.use() }
                this.renderAlign.set(Align.right)
                this.margin.set(Insets(0f, 16f, 0f, 16f))
                this.markup.set(Markup(emptyMap(), TextRun(PRManiaGame.instance.fontGameMoreTimes, "")))
                this.bounds.width.set(510f)
                this.bounds.height.set(86f)
                this.textColor.set(Color.WHITE)
                this.visible.bind { moreTimesVar.use() > 0 }
            }
        }

        override fun renderUI(batch: SpriteBatch) {
            val inputter = this@WorldRendererWithUI.engine.inputter
            moreTimesVar.set(inputter.practice.moreTimes.get())
            
            renderClearText(batch, inputter)
        }
        
        private fun renderClearText(batch: SpriteBatch, inputter: EngineInputter) {
            val clearText = inputter.practice.clearText
            val uiCam = this@WorldRendererWithUI.uiCamera
            if (clearText > 0f) {
                val normalScale = 1f
                val transitionEnd = 0.15f
                val transitionStart = 0.2f
                val scale: Float = when (val progress = 1f - clearText) {
                    in 0f..transitionStart -> {
                        Interpolation.exp10Out.apply(normalScale * 2f, normalScale, progress / transitionStart)
                    }
                    in (1f - transitionEnd)..1f -> {
                        Interpolation.exp10Out.apply(normalScale, normalScale * 1.5f, (progress - (1f - transitionEnd)) / transitionEnd)
                    }
                    else -> normalScale
                }
                val alpha: Float = when (val progress = 1f - clearText) {
                    in 0f..transitionStart -> {
                        Interpolation.exp10Out.apply(0f, 1f, progress / transitionStart)
                    }
                    in (1f - transitionEnd)..1f -> {
                        Interpolation.exp10Out.apply(1f, 0f, (progress - (1f - transitionEnd)) / transitionEnd)
                    }
                    else -> 1f
                }
                val white: Float = when (val progress = 1f - clearText) {
                    in 0f..transitionStart * 0.75f -> {
                        Interpolation.linear.apply(1f, 0f, progress / (transitionStart * 0.75f))
                    }
                    else -> 0f
                }

                val paintboxFont = PRManiaGame.instance.fontGamePracticeClear
                paintboxFont.useFont { font ->
                    font.scaleMul(scale)
                    font.setColor(1f, 1f, MathUtils.lerp(0.125f, 1f, white), alpha)
                    font.drawCompressed(batch, Localization.getValue("practice.clear"),
                            0f, uiCam.viewportHeight / 2f + font.capHeight / 2, uiCam.viewportWidth, Align.center)
                    font.scaleMul(1f / scale)
                }

                val newValue = (clearText - Gdx.graphics.deltaTime / 1.5f).coerceAtLeast(0f)
                inputter.practice.clearText = newValue
            }
        }
    }
    
    inner class EndlessModeRendering : InnerRendering() {
        
        val showEndlessModeScore: ReadOnlyBooleanVar = BooleanVar { engine.modifiers.endlessScore.enabled.use() }
        val prevHighScore: IntVar = IntVar(-1)
        val dailyChallengeDate: Var<LocalDate?> = Var(null)
        val endlessModeSeed: Var<String?> = Var(null)
        private val currentEndlessScore: IntVar = IntVar(0)
        private val currentEndlessLives: IntVar = IntVar(0)

        private val endlessModeScorePane: Pane
        private val endlessModeScoreLabelScaleXY: FloatVar = FloatVar(1f)
        private val endlessModeScoreLabel: TextLabel
        val endlessModeGameOverPane: Pane
        private val endlessModeGameOverLabel: TextLabel
        private val endlessModeHighScoreLabel: TextLabel

        override val uiElement: UIElement get() = endlessModeScorePane
        
        init {
            endlessModeScorePane = Pane().apply {
                this.visible.bind { showEndlessModeScore.use() }
                Anchor.TopLeft.configure(this, offsetX = 32f, offsetY = 32f)
//            this.bounds.width.set(400f)
                this.bindWidthToParent(adjust = -64f)
                this.bounds.height.set(200f)

                val vbox = VBox().apply {
                    this += Pane().apply {
                        this.bounds.height.set(40f)

                        val prevTextVar: ReadOnlyVar<String> = Var.bind {
                            val date = dailyChallengeDate.use()
                            val seed = endlessModeSeed.use()
                            if (date != null) {
                                Localization.getVar("play.endless.dailyChallenge", Var { listOf(date.format(DateTimeFormatter.ISO_DATE)) }).use()
                            } else if (seed != null) {
                                Localization.getVar("play.endless.seed", Var { listOf(seed) }).use()
                            } else {
                                Localization.getVar("play.endless.prevHighScore", Var { listOf(prevHighScore.use()) }).use()
                            }
                        }
                        endlessModeHighScoreLabel = TextLabel(binding = { prevTextVar.use() },
                                font = PRManiaGame.instance.fontGameUIText).apply {
                            this.bindWidthToParent(multiplier = 0.4f)
                            this.doXCompression.set(false)
                            this.renderAlign.set(Align.topLeft)
                            val defaultTextColor = Color().grey(229f / 255f)
                            this.textColor.set(defaultTextColor)
                            this.setScaleXY(0.6f)
                            this.textColor.bind {
                                val maxLives = engine.modifiers.endlessScore.maxLives.use()
                                if (endlessModeSeed.use() != null && maxLives == 1) {
                                    Color(1f, 0.35f, 0.35f, 1f)
                                } else defaultTextColor
                            }
                        }
                        this += endlessModeHighScoreLabel
                    }

                    val currentScoreVar = Localization.getVar("play.endless.score", Var { listOf(currentEndlessScore.use()) })
                    endlessModeScoreLabel = TextLabel(binding = { currentScoreVar.use() },
                            font = PRManiaGame.instance.fontPauseMenuTitle).apply {
                        this.bounds.height.set(100f)
                        this.renderAlign.set(Align.topLeft)
                        this.textColor.set(Color(1f, 1f, 1f, 1f))
                        val scaleMul = 1f / 1.25f
                        this.scaleX.bind { endlessModeScoreLabelScaleXY.use() * scaleMul }
                        this.scaleY.bind { endlessModeScoreLabelScaleXY.use() * scaleMul }
                    }
                    this += endlessModeScoreLabel

                    val endlessModeLivesLabel = TextLabel(binding = {
                        val l = currentEndlessLives.use()
                        /* space at start is necessary -> */ " [font=prmania_icons scale=6 offsety=-0.125]${"R".repeat(l)}[]"
                    }).apply {
                        this.bounds.height.set(40f)
                        Anchor.TopRight.configure(this)
                        this.markup.set(baseMarkup)
                        this.renderAlign.set(Align.left)
                        this.textColor.set(Color(1f, 1f, 1f, 1f))
                        this.setScaleXY(0.333f)
                    }
                    this += endlessModeLivesLabel

                }
                this += vbox
            }

            endlessModeGameOverPane = Pane().apply {
                this.visible.bind {
                    engine.modifiers.endlessScore.gameOverUIShown.use()
                }
                this += RectElement(Color(0f, 0f, 0f, 0.5f))
            }

            endlessModeGameOverLabel = TextLabel(binding = { Localization.getVar("play.endless.gameOver").use() },
                    font = PRManiaGame.instance.fontPauseMenuTitle).apply {
                Anchor.Centre.configure(this)
                this.bounds.height.set(350f)
                this.textColor.set(Color(81f / 255, 107f / 255, 1f, 1f))
                this.renderAlign.set(Align.center)
            }
            endlessModeGameOverPane += endlessModeGameOverLabel
        }

        override fun renderUI(batch: SpriteBatch) {
            val modifiers = this@WorldRendererWithUI.engine.modifiers
            if (showEndlessModeScore.get()) {
                val endlessScore = modifiers.endlessScore
                val oldLives = currentEndlessLives.get()
                val newLives = endlessScore.lives.get()
                currentEndlessLives.set(newLives)
                if (newLives < oldLives) {
                    hudRedFlash = 1f
                }
                val oldScore = currentEndlessScore.get()
                val newScore = endlessScore.score.get()
                if (oldScore != newScore) {
                    currentEndlessScore.set(newScore)
                    val scaleVar = endlessModeScoreLabelScaleXY
                    if (newScore > oldScore) {
                        val newScale = 1.25f
                        scaleVar.set(newScale)
                        uiSceneRoot.animations.enqueueAnimation(Animation(Interpolation.pow5In, 0.25f, newScale, 1f, delay = 0.15f), scaleVar)
                    } else {
                        uiSceneRoot.animations.cancelAnimationFor(scaleVar)
                        scaleVar.set(1f)
                    }
                }
                endlessModeHighScoreLabel.visible.set(!endlessScore.hideHighScoreText)
            }
        }
    }
    
    inner class SkillStarRendering : InnerRendering() {
        override val uiElement: UIElement = Pane().apply { 
            this.bounds.width.set(0f)
            this.bounds.height.set(0f)
        }
        
        var showSkillStarSetting: Boolean = PRManiaGame.instance.settings.showSkillStar.getOrCompute()

        private var skillStarSpinAnimation: Float = 0f
        private var skillStarPulseAnimation: Float = 0f

        override fun renderUI(batch: SpriteBatch) {
            val engine = this@WorldRendererWithUI.engine
            val inputter = engine.inputter
            val uiSheet: PackedSheet = AssetRegistry["tileset_ui"]
            
            val skillStarInput = inputter.skillStarBeat
            if (skillStarInput.isFinite() && showSkillStarSetting) {
                if (skillStarSpinAnimation > 0) {
                    skillStarSpinAnimation -= Gdx.graphics.deltaTime / 1f
                    if (skillStarSpinAnimation < 0)
                        skillStarSpinAnimation = 0f
                }
                if (skillStarPulseAnimation > 0) {
                    skillStarPulseAnimation -= Gdx.graphics.deltaTime / 0.5f
                    if (skillStarPulseAnimation < 0)
                        skillStarPulseAnimation = 0f
                } else {
                    // Pulse before skill star input
                    val threshold = 0.1f
                    for (i in 0 until 4) {
                        val beatPoint = engine.tempos.beatsToSeconds(skillStarInput - i)
                        if (engine.seconds in beatPoint..beatPoint + threshold) {
                            skillStarPulseAnimation = 0.5f
                            break
                        }
                    }
                }

                val texColoured = uiSheet["skill_star"]
                val texGrey = uiSheet["skill_star_grey"]

                val scale = Interpolation.exp10.apply(1f, 2f, (skillStarPulseAnimation).coerceAtMost(1f))
                val rotation = Interpolation.exp10Out.apply(0f, 360f, 1f - skillStarSpinAnimation)
                batch.draw(if (inputter.skillStarGotten.get()) texColoured else texGrey,
                        1184f, 32f, 32f, 32f, 64f, 64f, scale, scale, rotation)
            }
        }

        override fun onWorldReset(world: World) {
            super.onWorldReset(world)
            skillStarSpinAnimation = 0f
            skillStarPulseAnimation = 0f
        }

        fun fireSkillStar() {
            skillStarSpinAnimation = 1f
            skillStarPulseAnimation = 2f
        }
    }
    
    inner class SongCardRendering : InnerRendering() {
        
        override val uiElement: UIElement = Pane().apply {
            this.bounds.width.set(0f)
            this.bounds.height.set(0f)
        }
        
        val songTitleCard: SongInfoCard = SongInfoCard()
        val songArtistCard: SongInfoCard = SongInfoCard()

        override fun renderUI(batch: SpriteBatch) {
            val textboxFont = PRManiaGame.instance.fontGameTextbox
            textboxFont.useFont { font ->
                font.scaleMul(0.75f)
                val sec = engine.seconds
                renderSongInfoCard(batch, font, songTitleCard, false, sec)
                renderSongInfoCard(batch, font, songArtistCard, true, sec)
            }
        }

        override fun onWorldReset(world: World) {
            super.onWorldReset(world)
            songTitleCard.reset()
            songArtistCard.reset()
        }
    }
    
}
