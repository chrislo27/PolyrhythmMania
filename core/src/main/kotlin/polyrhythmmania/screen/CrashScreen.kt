package polyrhythmmania.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Align
import paintbox.font.Markup
import paintbox.font.PaintboxFont
import paintbox.font.TextAlign
import paintbox.font.TextRun
import paintbox.logging.SysOutPiper
import paintbox.ui.Pane
import paintbox.ui.SceneRoot
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.Button
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.drawQuad
import paintbox.util.gdxutils.fillRect
import polyrhythmmania.PRMania
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.container.Container
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import kotlin.system.exitProcess


class CrashScreen(main: PRManiaGame, val throwable: Throwable, val lastScreen: Screen?)
    : PRManiaScreen(main) {

    private val batch: SpriteBatch = main.batch
    private val font: PaintboxFont = main.fontMainMenuMain
    private val markup: Markup = Markup(mapOf(
            "prmania_icons" to main.fontIcons,
            "rodin" to main.fontMainMenuRodin,
            "thin" to main.fontMainMenuThin,
    ), TextRun(font, ""), Markup.FontStyles("bold", "italic", "bolditalic"))
    private val markupThin: Markup = Markup(mapOf(
            "prmania_icons" to main.fontIcons,
            "rodin" to main.fontMainMenuRodin,
            "bold" to font,
    ), TextRun(main.fontMainMenuThin, ""), Markup.FontStyles("bold", "italic", "bolditalic"))
    private val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    private val sceneRoot: SceneRoot = SceneRoot(uiCamera)
    private val processor: InputProcessor = sceneRoot.inputSystem
    
    init {
        val pane = Pane().apply {
            this.margin.set(Insets(10f, 10f, 20f, 20f))
        }
        sceneRoot.addChild(pane)
        
        val vbox = VBox().apply { 
            this.spacing.set(0f)
        }
        pane.addChild(vbox)
        
        vbox.temporarilyDisableLayouts { 
            vbox += TextLabel("${PRMania.TITLE} ${PRMania.VERSION} has crashed", font = main.fontMainMenuHeading).apply { 
                this.bounds.height.set(64f)
                this.textColor.set(Color.WHITE)
                this.textAlign.set(TextAlign.CENTRE)
                this.renderAlign.set(Align.center)
            }
            vbox += TextLabel("The program has crashed, but we are able to display this crash info screen.\nIf you were in the Editor, a recovery file has been saved. Access it with Load Level > Open Recovery folder.\nIf you can, take a screenshot of this screen as it contains useful info for the developer." /*+ "\nConsider submitting a bug report at\n[color=#8CCFFF]${PRMania.GITHUB}/issues/new/choose[]."*/).apply {
                this.bounds.height.set(100f)
                this.textColor.set(Color.WHITE)
                this.markup.set(this@CrashScreen.markup)
                this.textAlign.set(TextAlign.CENTRE)
                this.renderAlign.set(Align.top)
            }
            vbox += TextLabel("[b]Last screen:[] ${lastScreen?.javaClass?.canonicalName}\n[b]Log file:[] <user.home>/.polyrhythmmania/logs/${SysOutPiper.logFile.name}\n[b]Exception:[] [color=#FF6B68]${throwable.stackTraceToString().replace("\t", "   ")}[]").apply {
                this.bounds.height.set(500f)
                this.textColor.set(Color.WHITE)
                this.markup.set(this@CrashScreen.markupThin)
                this.textAlign.set(TextAlign.LEFT)
                this.renderAlign.set(Align.topLeft)
                this.doLineWrapping.set(true)
                this.doClipping.set(true)
            }
            
            val hbox = HBox().apply { 
                this.spacing.set(8f)
                this.bounds.height.set(32f)
                this.border.set(Insets(1f, 0f, 0f, 0f))
                this.borderStyle.set(SolidBorder(Color.CYAN))
                this.padding.set(Insets(2f, 0f, 0f, 0f))
            }
            hbox.temporarilyDisableLayouts { 
                hbox += TextLabel("Actions: ", font = font).apply { 
                    this.bounds.width.set(150f)
                    this.renderAlign.set(Align.right)
                    this.textColor.set(Color.WHITE)
                }
                hbox += Button("Open log file location", font = font).apply { 
                    this.bounds.width.set(300f)
                    this.setOnAction { 
                        val uri = SysOutPiper.logFile.parentFile.toURI()
                        Gdx.net.openURI(uri.toString())
                    }
                }
                hbox += Button("Close program", font = font).apply {
                    this.bounds.width.set(200f)
                    this.setOnAction {
                        exitProcess(1)
                    }
                }
            }
            vbox += hbox
        }
    }
    
    init {
        Gdx.input.isCursorCatched = false
        if (Gdx.graphics.isFullscreen) {
            Gdx.graphics.setWindowedMode(PRMania.DEFAULT_SIZE.width, PRMania.DEFAULT_SIZE.height)
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render(delta)

        val camera = uiCamera
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch.projectionMatrix = camera.combined
        batch.begin()

        sceneRoot.renderAsRoot(batch)

        batch.end()
        
        
//        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
//            main.screen = main.mainMenuScreen.prepareShow(false)
//        }
    }
    
    override fun show() {
        super.show()
        main.inputMultiplexer.removeProcessor(processor)
        main.inputMultiplexer.addProcessor(processor)
    }

    override fun hide() {
        super.hide()
        main.inputMultiplexer.removeProcessor(processor)
    }

    override fun dispose() {
    }
}