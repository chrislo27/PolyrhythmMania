package polyrhythmmania.storymode.screen.cutscene

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.binding.FloatVar
import paintbox.binding.IntVar
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.ui.*
import paintbox.ui.animation.Animation
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.util.gdxutils.GdxDelayedRunnable
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StorySession
import polyrhythmmania.storymode.screen.desktop.DesktopUI.Companion.UI_SCALE


class PostBossCutsceneScreen(
    main: PRManiaGame,
    val storySession: StorySession,
    val isSkippable: Boolean,
    val onExit: () -> Unit,
) : PRManiaScreen(main) {
    
    companion object {
        private const val SKIP_KEY: Int = Input.Keys.ESCAPE
    }

    val batch: SpriteBatch = main.batch
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val processor: InputProcessor = sceneRoot.inputSystem

    private val imageList: List<Texture?> = listOf(
        null,
        StoryAssets.get<Texture>("cutscene_postboss_1"),
        StoryAssets.get<Texture>("cutscene_postboss_2"),
        StoryAssets.get<Texture>("cutscene_postboss_3"),
        StoryAssets.get<Texture>("cutscene_postboss_4"),
    )
    private val currentImageIndex: IntVar = IntVar(0)

    private val currentImage: Var<Texture?> = Var.bind { imageList.getOrNull(currentImageIndex.use()) }
    private val nextImage: Var<Texture?> = Var.bind { imageList.getOrNull(currentImageIndex.use() + 1) }
    private val scroll: FloatVar = FloatVar(0f)
    
    private val clickToAdvanceStartOpacity: FloatVar = FloatVar(0f)
    private var hasInitialWaitPassed: Boolean = false

    init {
        val parent = RectElement(Color.BLACK)
        sceneRoot += parent

        fun UIElement.setBounds() {
            this.bounds.width.set(132f * UI_SCALE)
            this.bounds.height.set(69f * UI_SCALE)
        }

        val imageWindow = Pane().apply {
            this.bounds.x.set(94f * UI_SCALE)
            this.bounds.y.set(27f * UI_SCALE)
            this.setBounds()

            this += ImageNode(binding = { currentImage.use()?.let { TextureRegion(it) } })
            this += Pane().apply {
                this += ImageNode(binding = { nextImage.use()?.let { TextureRegion(it) } }).apply {
                    this.setBounds()
                    Anchor.BottomLeft.configure(this)
                }

                this.doClipping.set(true)

                Anchor.BottomLeft.configure(this)

                this.bindWidthToParent(
                    multiplierBinding = { if (currentImageIndex.use() != 2) scroll.use() else 1f },
                    adjustBinding = { 0f })
                this.bindHeightToParent(
                    multiplierBinding = { if (currentImageIndex.use() == 2) scroll.use() else 1f },
                    adjustBinding = { 0f })
            }
        }
        parent += imageWindow
        
        parent += TextLabel("${Localization.getValue("cutscene.postboss.clickToAdvance")}\n${if (isSkippable) Localization.getValue("cutscene.postboss.skipPrompt", Input.Keys.toString(SKIP_KEY)) else ""}", font = main.fontRobotoItalic).apply { 
            Anchor.BottomCentre.configure(this)
            this.renderAlign.set(RenderAlign.top)
            this.textAlign.set(TextAlign.CENTRE)
            this.bounds.height.set(130f)
            this.textColor.set(Color.LIGHT_GRAY)
            this.opacity.bind { 
                if (currentImageIndex.use() > 0) 0f else ((1f - scroll.use() * 2) * clickToAdvanceStartOpacity.use()).coerceIn(0f, 1f)
            }
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render(delta)

        val camera = uiCamera
        val batch = this.batch
        batch.projectionMatrix = camera.combined
        batch.begin()

        uiViewport.apply()
        batch.setColor(1f, 1f, 1f, 1f)
        sceneRoot.renderAsRoot(batch)

        batch.end()
    }

    override fun renderUpdate() {
        super.renderUpdate()

        storySession.renderUpdate()

        val aKey = main.settings.inputKeymapKeyboard.getOrCompute().buttonA
        val isAnimationStillPlaying = scroll.get() > 0f
        if (hasInitialWaitPassed && !isAnimationStillPlaying &&
            (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    || Gdx.input.isKeyJustPressed(aKey)
                    || Gdx.input.isButtonJustPressed(Buttons.LEFT))
        ) {
            if (currentImageIndex.get() >= imageList.size - 1) {
                onExit()
            } else {
                triggerNextAnimation()
            }
        }
        
        if (isSkippable && Gdx.input.isKeyJustPressed(SKIP_KEY)) {
            // Note: Skipping is allowed even before the initial wait has passed
            onExit()
        }
    }

    private fun triggerNextAnimation() {
        val nextIndex = currentImageIndex.get() + 1
        
        when (nextIndex) {
            4 -> main.playMenuSfx(StoryAssets["sfx_cutscene_postboss_page_open"])
        }
        
        sceneRoot.animations.enqueueAnimation(
            Animation(
                Interpolation.linear,
                duration = 1f,
                start = 0f, end = 1f
            ).apply {
                this.onComplete = {
                    scroll.set(0f)
                    currentImageIndex.set(nextIndex)
                }
            }, scroll
        )
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        uiViewport.update(width, height)
    }

    override fun show() {
        super.show()

        main.inputMultiplexer.removeProcessor(processor)
        main.inputMultiplexer.addProcessor(processor)

        Gdx.input.isCursorCatched = true

        if (currentImageIndex.get() == 0) {
            Gdx.app.postRunnable(GdxDelayedRunnable(1.75f) {
                main.playMenuSfx(StoryAssets["sfx_cutscene_postboss_knocking"])
                sceneRoot.animations.enqueueAnimation(
                    Animation(Interpolation.linear, 0.5f, 0f, 1f, delay = 0.5f),
                    clickToAdvanceStartOpacity
                )
                hasInitialWaitPassed = true
            })
        }
    }

    override fun hide() {
        super.hide()

        main.inputMultiplexer.removeProcessor(processor)

        Gdx.input.isCursorCatched = false
    }

    override fun dispose() {
    }
}