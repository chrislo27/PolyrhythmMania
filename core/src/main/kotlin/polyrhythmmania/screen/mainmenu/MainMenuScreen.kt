package polyrhythmmania.screen.mainmenu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.util.WindowSize
import io.github.chrislo27.paintbox.util.gdxutils.disposeQuietly
import io.github.chrislo27.paintbox.util.gdxutils.drawQuad
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.container.Container
import polyrhythmmania.screen.mainmenu.menu.MenuCollection
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.world.EntityCube
import polyrhythmmania.world.EntityPlatform
import kotlin.math.ceil


class MainMenuScreen(main: PRManiaGame) : PRManiaScreen(main) {

    companion object {
        const val FLIP_SEC_PER_TILE: Float = 1f / 15f / 2
    }

    private class Tile(var x: Int, var y: Int, var flipAmt: Float = 0f) {
        fun reset() {
            flipAmt = 0f
        }
    }

    data class TileFlip(val startX: Int, val startY: Int, val width: Int, val height: Int,
                                 var cornerStart: Corner = Corner.TOP_LEFT,
                                 var flipWidth: Float = 3f,) {
        var diagonalProgress: Float = 0f

        var isDone: Boolean = false
            private set

        fun update(delta: Float, mainMenu: MainMenuScreen) {
            if (isDone) return

            val progressDelta = delta / FLIP_SEC_PER_TILE

            diagonalProgress += progressDelta

            var anyNotDone = false
            for (ix in startX until (startX + width)) {
                if (ix !in 0 until mainMenu.tilesWidth) continue
                for (iy in startY until (startY + height)) {
                    if (iy !in 0 until mainMenu.tilesHeight) continue
                    val tile = mainMenu.tiles[ix][iy]
                    if (tile.flipAmt >= 1f) continue
                    val thisDiag = computeDiagonalIndex(ix, iy)
                    val newAmt = ((diagonalProgress - thisDiag) / flipWidth).coerceIn(0f, 1f)
                    tile.flipAmt = newAmt
                    if (!anyNotDone && newAmt < 1f)
                        anyNotDone = true
                }
            }

            if (!anyNotDone) {
                isDone = true
            }
        }
        
        fun computeDiagonalIndex(ix: Int, iy: Int): Int {
            return when (cornerStart) {
                Corner.TOP_LEFT -> (ix - startX) + (iy - startY)
                Corner.TOP_RIGHT -> ((width - 1) - (ix - startX)) + (iy - startY)
                Corner.BOTTOM_LEFT -> (ix - startX) + ((height - 1) - (iy - startY))
                Corner.BOTTOM_RIGHT -> ((width - 1) - (ix - startX)) + ((height - 1) - (iy - startY))
            }
        }
    }

    private val lastProjMatrix: Matrix4 = Matrix4()
    private val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    private val fullCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        this.update()
    }

    private val batch: SpriteBatch = main.batch
    private val sceneRoot: SceneRoot = SceneRoot(uiCamera)
    private val processor: InputProcessor = sceneRoot.inputSystem

    private val container: Container = Container(null, SimpleTimingProvider { throw it })
    
    private val menuPane: Pane = Pane()
    private val menuCollection: MenuCollection = MenuCollection(this, sceneRoot, menuPane)

    private val gradientStart: Color = Color(0f, 32f / 255f, 55f / 255f, 1f)
    private val gradientEnd: Color = Color.BLACK.cpy()

    // Related to tile flip effect --------------------------------------------------------

    val tileSize: Int = 48 //32
    val tilesWidth: Int = ceil(1280f / tileSize).toInt()
    val tilesHeight: Int = ceil(720f / tileSize).toInt()
    private val tiles: Array<Array<Tile>> = Array(tilesWidth) { x -> Array(tilesHeight) { y -> Tile(x, y) } }
    var flipAnimation: TileFlip? = null
        set(value) {
            field = value
            resetTiles()
            swapFramebuffers()
        }
    private var framebufferSize: WindowSize = WindowSize(0, 0)

    /**
     * The old framebuffer should be the last rendered frame.
     */
    private lateinit var framebufferOld: FrameBuffer

    /**
     * The current framebuffer is what's drawn for this frame.
     */
    private lateinit var framebufferCurrent: FrameBuffer


    // FIXME remove this later
    private var colorIndex: Int = 0
    private val colors: List<Color> = listOf(Color(1f, 0.5f, 0.5f, 1f), Color(0.5f, 1f, 0.5f, 1f), Color(0.5f, 0.5f, 1f, 1f))

    init {
        createFramebuffers(Gdx.graphics.width, Gdx.graphics.height)

        val world = container.world
        val renderer = container.renderer
        world.entities.toList().forEach { world.removeEntity(it) }
        renderer.camera.position.x = -2f
        renderer.camera.position.y = 0.5f

        // TODO move this out
        for (x in 0 until 7) {
            for (z in -2..0) {
                world.addEntity(EntityCube(world, false).apply {
                    this.position.set(x.toFloat(), 0f, z.toFloat())
                })
            }
            if (x == 0) {
                world.addEntity(EntityRowBlockDecor(world).apply {
                    this.type = EntityRowBlockDecor.Type.PISTON_A
                    this.position.set(x.toFloat(), 1f + MathUtils.FLOAT_ROUNDING_ERROR, -1f)
                })
            } else {
                world.addEntity(EntityPlatform(world).apply {
                    this.position.set(x.toFloat(), 1f + MathUtils.FLOAT_ROUNDING_ERROR, -1f)
                })
            }
        }
        run {
            world.addEntity(EntityCubeHovering(world).apply {
                this.position.set(-2f, 2f, -3f)
            })
            world.addEntity(EntityCubeHovering(world, withLine = true).apply {
                this.position.set(2f, 2f, -4f)
            })
            world.addEntity(EntityCubeHovering(world).apply {
                this.position.set(6f, 0f, -4f)
            })
            world.addEntity(EntityCubeHovering(world).apply {
                this.position.set(-3.5f, 1f, 0f)
            })
            world.addEntity(EntityCubeHovering(world).apply {
                this.position.set(0f, -2f, 1f)
            })
        }
    }

    init {
        val leftPane = Pane().apply {
            this.margin.set(Insets(64f))
        }
        val logoImage = ImageNode(TextureRegion(AssetRegistry.get<Texture>("logo_2lines_en"))).apply {
            this.bounds.height.set(192f)
//            this.margin.set(Insets(0f, 0f, 32f, 32f))
            this.bounds.y.set(24f)
            this.renderAlign.set(Align.topLeft)
        }
        leftPane.addChild(logoImage)
        menuPane.apply { 
            Anchor.BottomLeft.configure(this)
            this.bindHeightToParent(-(logoImage.bounds.height.getOrCompute() + logoImage.bounds.y.getOrCompute() + 32f))
        }
        leftPane.addChild(menuPane)

        sceneRoot += leftPane
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render(delta)

        // Draw active scene
        val boundFB: FrameBuffer = framebufferCurrent
        val camera = uiCamera
        boundFB.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Render background
        batch.projectionMatrix = camera.combined
        batch.begin()

        batch.drawQuad(-400f, 0f, gradientEnd, camera.viewportWidth, 0f, gradientEnd, camera.viewportWidth,
                camera.viewportHeight, gradientStart, -400f, camera.viewportHeight + 400f, gradientStart)

        batch.end()

        // Render world
        container.renderer.render(batch, container.engine)

        // Render UI
        batch.projectionMatrix = camera.combined
        batch.begin()

        sceneRoot.renderAsRoot(batch)

        batch.end()
        boundFB.end()

        // Tile flip effect
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(framebufferCurrent.colorBufferTexture, 0f, 0f, camera.viewportWidth, camera.viewportHeight, 0f, 0f, 1f, 1f)

        val currentFlip = this.flipAnimation
        if (currentFlip != null) {
            currentFlip.update(delta, this)
            if (currentFlip.isDone) {
                this.flipAnimation = null
                resetTiles()
            } else {
                val tileSizeF = tileSize.toFloat()
                val tileSizeU = tileSizeF / camera.viewportWidth
                val tileSizeV = tileSizeF / camera.viewportHeight

                for (tx in currentFlip.startX until (currentFlip.startX + currentFlip.width)) {
                    if (tx !in 0 until tilesWidth) continue
                    for (ty in currentFlip.startY until (currentFlip.startY + currentFlip.height)) {
                        if (ty !in 0 until tilesHeight) continue
                        val rx: Float = tx * tileSizeF
                        val ry: Float = camera.viewportHeight - ((ty + 1) * tileSizeF)
                        val tile = tiles[tx][ty]
                        val flipAmt = tile.flipAmt

                        val oldAmt = (flipAmt / 0.5f).coerceIn(0f, 1f)
                        val oldAmtInv = 1f - oldAmt
                        val newAmt = (flipAmt / 0.5f - 1f).coerceIn(0f, 1f)

                        batch.setColor(0f, 0f, 0f, 1f)
                        batch.fillRect(rx, ry, tileSizeF, tileSizeF)
                        batch.setColor(1f, 1f, 1f, 1f)

                        if (flipAmt <= 0.5f) {
                            batch.draw(framebufferOld.colorBufferTexture,
                                    rx + (tileSizeF - (tileSizeF * oldAmtInv)) * 0.5f, ry,
                                    tileSizeF * oldAmtInv, tileSizeF,
                                    tx * tileSizeU, 1f - (ty + 1) * tileSizeV, (tx + 1) * tileSizeU, 1f - (ty) * tileSizeV)
                        } else {
                            batch.draw(framebufferCurrent.colorBufferTexture,
                                    rx + (tileSizeF - (tileSizeF * newAmt)) * 0.5f, ry,
                                    tileSizeF * newAmt, tileSizeF,
                                    tx * tileSizeU, 1f - (ty + 1) * tileSizeV, (tx + 1) * tileSizeU, 1f - (ty) * tileSizeV)
                        }

                        batch.setColor(1f, 1f, 1f, 1f)
                    }
                }
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }

        batch.end()

        // Swap the buffers around so that the "current" one is now old.
        if (this.flipAnimation == null) {
            swapFramebuffers()
        }
    }

    override fun renderUpdate() {
        super.renderUpdate()
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            colorIndex = (colorIndex + 1) % colors.size
            flipAnimation = TileFlip(5, 5, 16, 9, cornerStart = Corner.TOP_LEFT)
        }
        // FIXME DEBUG remove later 
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            main.screen = MainMenuScreen(main)
        }
    }

    fun prepareShow(): MainMenuScreen {

        return this
    }

    private fun updateFramebuffers() {
        val cachedFramebufferSize = this.framebufferSize
        val width = Gdx.graphics.width
        val height = Gdx.graphics.height
        if (cachedFramebufferSize.width != width || cachedFramebufferSize.height != height) {
            framebufferOld.disposeQuietly()
            framebufferCurrent.disposeQuietly()
            createFramebuffers(width, height)
        }
    }

    private fun swapFramebuffers() {
        val tmpBuffer = framebufferOld
        framebufferOld = framebufferCurrent
        framebufferCurrent = tmpBuffer
    }

    private fun createFramebuffers(width: Int, height: Int) {
        this.framebufferOld = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)
        this.framebufferCurrent = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)
        this.framebufferSize = WindowSize(width, height)
    }

    private fun resetTiles() {
        tiles.forEach { it.forEach { t -> t.reset() } }
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

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        fullCamera.setToOrtho(false, width.toFloat(), height.toFloat())
        fullCamera.update()
        updateFramebuffers()
    }

    override fun dispose() {
        framebufferOld.disposeQuietly()
        framebufferCurrent.disposeQuietly()
    }

    override fun getDebugString(): String {
        return "" +
"""path: ${sceneRoot.mainLayer.lastHoveredElementPath.map { "${it::class.java.simpleName}" }}

"""
    }
}