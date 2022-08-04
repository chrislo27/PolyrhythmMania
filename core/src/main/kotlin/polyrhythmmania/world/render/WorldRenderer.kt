package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Scaling
import paintbox.Paintbox
import paintbox.registry.AssetRegistry
import paintbox.util.*
import paintbox.util.gdxutils.NestedFrameBuffer
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.intersects
import polyrhythmmania.world.World
import polyrhythmmania.world.WorldType
import polyrhythmmania.world.entity.Entity
import polyrhythmmania.world.render.bg.WorldBackground
import polyrhythmmania.world.render.bg.WorldBackgroundFromWorldType
import polyrhythmmania.world.tileset.Tileset
import kotlin.math.roundToInt


open class WorldRenderer(val world: World, val tileset: Tileset) : Disposable, World.WorldResetListener {

    companion object {
        val comparatorRenderOrder: Comparator<Entity> = Comparator { o1, o2 ->
            // Explicitly choose rows on the x-axis first, ordered by z value
            val o1Z = o1.position.z + o1.renderSortOffsetZ
            val o2Z = o2.position.z + o2.renderSortOffsetZ
            if (o1Z < o2Z) {
                -1
            } else if (o1Z > o2Z) {
                1
            } else {
                val o1X = o1.position.x + o1.renderSortOffsetX
                val o1Y = o1.position.y + o1.renderSortOffsetY
                val o2X = o2.position.x + o2.renderSortOffsetX
                val o2Y = o2.position.y + o2.renderSortOffsetY
                val xyz1 = o1X - o1Z - o1Y
                val xyz2 = o2X - o2Z - o2Y
                -xyz1.compareTo(xyz2)
            }
        }

        fun convertWorldToScreen(vec3: Vector3): Vector3 {
            return vec3.apply {
                val oldX = this.x
                val oldY = this.y // + MathHelper.getSineWave((System.currentTimeMillis() * 3).toLong() + (x * -500 - z * 500).toLong(), 2f) * 0.4f
                val oldZ = this.z
                this.x = oldX / 2f + oldZ / 2f
                this.y = oldX * (8f / 32f) + (oldY - 3f) * 0.5f - oldZ * (8f / 32f)
                this.z = 0f
            }
        }

        // For doing entity render culling
        private val tmpVec: Vector3 = Vector3(0f, 0f, 0f)
        private val tmpRect: Rectangle = Rectangle(0f, 0f, 0f, 0f)
        private val tmpRect2: Rectangle = Rectangle(0f, 0f, 0f, 0f)
    }

    protected var isDisposed: Boolean = false
        private set
    
    val camera: OrthographicCamera = OrthographicCamera().apply {
        zoom = 1f
        setToOrtho(false, 5 * (16f / 9f), 5f)
        update()
    }
    protected val tmpMatrix: Matrix4 = Matrix4()

    protected val fbRenderCamera: OrthographicCamera = OrthographicCamera().apply {
        setToOrtho(false, 1280f, 720f)
        update()
    }
    /**
     * Used to determine when to re-create the framebuffers
     */
    private var lastKnownWindowSize: WindowSize = WindowSize(-1, -1)
    private var framebufferSize: WindowSize = WindowSize(0, 0)
    private val framebuffers: Array<NestedFrameBuffer?> = Array(2) { null }
    /**
     * Represents the rendered world as a framebuffer.
     */
    var mainFrameBuffer: NestedFrameBuffer?
        get() = framebuffers[0]
        private set(value) { framebuffers[0] = value }
    /**
     * Represents just the light portion as a framebuffer.
     */
    var lightFrameBuffer: NestedFrameBuffer?
        get() = framebuffers[1]
        private set(value) { framebuffers[1] = value }

    
    var entitiesRenderedLastCall: Int = 0
        private set
    var entityRenderTimeNano: Long = 0L
        private set
    
    var worldBackground: WorldBackground = WorldBackgroundFromWorldType
    
    init {
        @Suppress("LeakingThis")
        this.world.worldResetListeners += this as World.WorldResetListener
    }

    override fun onWorldReset(world: World) {
        val cam = camera
        cam.position.set(cam.viewportWidth / 2f, cam.viewportHeight / 2f, 0f) // Ignore zoom value
        cam.zoom = 1f
        cam.update()
    }
    
    protected open fun shouldUseMainFb(): Boolean {
        return false
    }

    open fun render(batch: SpriteBatch) {
        // Re-create framebuffers if needed
        checkForResize()
        
        val camera = this.camera
        // TODO better camera controls and refactoring
        if (world.worldMode.worldType == WorldType.Dunk) {
            camera.position.x = camera.zoom * camera.viewportWidth / 2f
            camera.position.y = camera.zoom * camera.viewportHeight / 2f
            camera.position.x -= 2f
            camera.position.y += 0.125f
        }
        camera.update()
        
        val mainFb: NestedFrameBuffer? = if (shouldUseMainFb()) this.mainFrameBuffer else null
        if (mainFb != null) {
            mainFb.begin()
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }

        tmpMatrix.set(batch.projectionMatrix)
        batch.projectionMatrix = camera.combined
        batch.begin()

        // Blending for framebuffers w/ transparency in format. Assumes premultiplied
        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        
        // Background
        worldBackground.render(batch, world, camera)

        // Entities
        val entityRenderTime = System.nanoTime()
        var entitiesRendered = 0
        val camWidth = camera.viewportWidth * camera.zoom
        val camHeight = camera.viewportHeight * camera.zoom
        val leftEdge = camera.position.x - camWidth / 2f
        val bottomEdge = camera.position.y - camHeight / 2f
        val currentTileset = this.tileset
        
        tmpRect2.set(leftEdge, bottomEdge, camWidth, camHeight)
        this.entitiesRenderedLastCall = 0
        world.sortEntitiesByRenderOrder()
        world.entities.forEach { entity ->
            val convertedVec = convertWorldToScreen(tmpVec.set(entity.position))
            tmpRect.set(convertedVec.x, convertedVec.y, entity.renderWidth, entity.renderHeight)
            // Only render entities that are in scene
            if (tmpRect.intersects(tmpRect2)) {
                entitiesRendered++
                entity.render(this, batch, currentTileset)
            }
        }
        this.entitiesRenderedLastCall = entitiesRendered
        this.entityRenderTimeNano = System.nanoTime() - entityRenderTime

        // Blending for framebuffers w/ transparency in format. Assumes premultiplied
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        
        batch.end()
        batch.projectionMatrix = tmpMatrix

        
        // Build and render light buffer
        val lightFb = lightFrameBuffer
        if (lightFb != null && !world.spotlights.isAmbientLightingFull()) {
            val spotlights = world.spotlights
            
            val oldSrcFunc = batch.blendSrcFunc
            val oldDstFunc = batch.blendDstFunc

            // Render lights
            lightFb.begin()
            tmpMatrix.set(batch.projectionMatrix)
            batch.projectionMatrix = this.camera.combined
            batch.begin()

            // Clear with ambient light colour
            ColorStack.use { tmpColor ->
                spotlights.ambientLight.computeFinalForAmbientLight(tmpColor)
                Gdx.gl.glClearColor(tmpColor.r, tmpColor.g, tmpColor.b, 1f)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            }
            
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)

            // Render each light
            val tmpVec = Vector3Stack.getAndPush()
            val lightTex = AssetRegistry.get<Texture>("world_spotlight")
            val lightTexAspectRatio = lightTex.height.toFloat() / lightTex.width
            val width = 1.25f
            for (spotlight in spotlights.allSpotlights) {
                if (spotlight.lightColor.strength <= 0f) {
                    continue
                }
                convertWorldToScreen(tmpVec.set(spotlight.position))
                ColorStack.use { tmp ->
                    batch.color = spotlight.lightColor.computeFinalForSpotlight(tmp)
                }
                batch.draw(lightTex, tmpVec.x - width / 2f, tmpVec.y, width, width * lightTexAspectRatio)
            }
            Vector3Stack.pop()

            batch.end()
            batch.setColor(1f, 1f, 1f, 1f)
            lightFb.end()


            // Render light fb
            batch.projectionMatrix = fbRenderCamera.combined
            batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ZERO)
            batch.begin()

            batch.setColor(1f, 1f, 1f, 1f)
            val fbTex = lightFb.colorBufferTexture
            batch.draw(fbTex, 0f, 0f, fbRenderCamera.viewportWidth, fbRenderCamera.viewportHeight, 0, 0, fbTex.width, fbTex.height, false, true)

            batch.end()
            batch.setColor(1f, 1f, 1f, 1f)
            batch.setBlendFunction(oldSrcFunc, oldDstFunc)
            batch.projectionMatrix = tmpMatrix
        }
        
        
        if (mainFb != null) {
            mainFb.end()
            
            // Render main fb
            batch.projectionMatrix = fbRenderCamera.combined
            batch.begin()

            batch.setColor(1f, 1f, 1f, 1f)
            val fbTex = mainFb.colorBufferTexture
            batch.draw(fbTex, 0f, 0f, fbRenderCamera.viewportWidth, fbRenderCamera.viewportHeight, 0, 0, fbTex.width, fbTex.height, false, true)

            batch.end()
            batch.projectionMatrix = tmpMatrix
        }
    }

    private fun checkForResize() { // Must be called on the GL thread.
        val cachedWindowSize = this.lastKnownWindowSize
        val nullWindowSize = cachedWindowSize.width == -1 || cachedWindowSize.height == -1
        val windowWidth = Gdx.graphics.width
        val windowHeight = Gdx.graphics.height
        if (((windowWidth > 0 && windowHeight > 0) && (windowWidth != cachedWindowSize.width || windowHeight != cachedWindowSize.height)) || nullWindowSize) {
            // Width/height MAY be 0.
            this.lastKnownWindowSize = WindowSize(windowWidth, windowHeight)

            val scaling: Scaling = Scaling.fit
            val worldSize = Vector2Stack.getAndPush().set(
                    scaling.apply(fbRenderCamera.viewportWidth, fbRenderCamera.viewportHeight, windowWidth.toFloat(), windowHeight.toFloat())
            )
            val vpWidth = worldSize.x.roundToInt()
            val vpHeight = worldSize.y.roundToInt()
            Vector2Stack.pop()
            
            val cachedFramebufferSize = this.framebufferSize
            if (vpWidth > 0 && vpHeight > 0 && (cachedFramebufferSize.width != vpWidth || cachedFramebufferSize.height != vpHeight)) {
                createFramebuffers(vpWidth, vpHeight)
            } else if (nullWindowSize) {
                Paintbox.LOGGER.debug("World renderer light framebuffer: nullWindowSize")
                createFramebuffers(1280, 720)
            }
        }
    }
    
    protected fun disposeFramebuffers() {
        val framebuffersArray = this.framebuffers
        val oldFbsList = framebuffersArray.toList()
        framebuffersArray.fill(null)
        oldFbsList.forEach { it?.disposeQuietly() }
        Paintbox.LOGGER.debug("Disposed WorldRenderer framebuffers")
    }
    
    protected fun createFramebuffers(width: Int, height: Int) {
        val framebuffersArray = this.framebuffers
        disposeFramebuffers()
        
        if (!this.isDisposed) {
            val newFbWidth = HdpiUtils.toBackBufferX(width)
            val newFbHeight = HdpiUtils.toBackBufferY(height)
            framebuffersArray.indices.forEach { i ->
                val format: Pixmap.Format = when (i) {
                    0 -> Pixmap.Format.RGBA8888 // Main framebuffer needs transparency
                    else -> Pixmap.Format.RGB888
                }
                framebuffersArray[i] = NestedFrameBuffer(format, newFbWidth, newFbHeight, false).apply {
                    this.colorBufferTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                }
            }
            this.framebufferSize = WindowSize(newFbWidth, newFbHeight)
            Paintbox.LOGGER.debug("Updated world renderer light framebuffer to be backbuffer ${newFbWidth}x${newFbHeight} (logical ${width}x${height})")
        }
    }

    override fun dispose() {
        removeWorldHooks()
        disposeFramebuffers()
        isDisposed = true
    }

    @Suppress("RemoveCurlyBracesFromTemplate")
    open fun getDebugString(): String {
        return """e: ${world.entities.size}  r: ${entitiesRenderedLastCall} (${DecimalFormats["0.0000"].format((entityRenderTimeNano) / 1_000_000.0)} ms)
"""
    }

    /**
     * Removes any world listeners, like the world reset listener.
     */
    fun removeWorldHooks() {
        this.world.worldResetListeners.remove(this)
    }
}