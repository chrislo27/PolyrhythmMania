package polyrhythmmania.world.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Scaling
import paintbox.Paintbox
import paintbox.registry.AssetRegistry
import paintbox.util.*
import paintbox.util.gdxutils.NestedFrameBuffer
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.intersects
import polyrhythmmania.util.FrameBufferManager
import polyrhythmmania.util.FrameBufferMgrSettings
import polyrhythmmania.world.World
import polyrhythmmania.world.WorldType
import polyrhythmmania.world.entity.Entity
import polyrhythmmania.world.entity.HasLightingRender
import polyrhythmmania.world.render.bg.WorldBackground
import polyrhythmmania.world.render.bg.WorldBackgroundFromWorldType
import polyrhythmmania.world.tileset.Tileset
import kotlin.math.floor
import kotlin.system.measureNanoTime


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
        val comparatorRenderOrderLighting: Comparator<Entity> = Comparator { o1, o2 ->
            val o1Z = o1.position.z + o1.renderSortOffsetZ
            val o2Z = o2.position.z + o2.renderSortOffsetZ
            val o1X = o1.position.x + o1.renderSortOffsetX
            val o1Y = o1.position.y + o1.renderSortOffsetY
            val o2X = o2.position.x + o2.renderSortOffsetX
            val o2Y = o2.position.y + o2.renderSortOffsetY
            
            // Find the "depth value", which is z-x
            val depth1 = floor(o1Z - o1X)
            val depth2 = floor(o2Z - o2X)
            
            // Lower depths are rendered first
            if (depth1 < depth2) {
                -1
            } else if (depth1 > depth2) {
                1
            } else {
                // Everything is on the same depth. We go from lower Y to upper Y, left to right (increasing Z)
                if (o1Y < o2Y) {
                    -1
                } else if (o1Y > o2Y) {
                    1
                } else {
                    o1Z.compareTo(o2Z)
                }
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
    protected val frameBufferManager: FrameBufferManager = FrameBufferManager(
            listOf(FrameBufferMgrSettings(Pixmap.Format.RGBA8888), FrameBufferMgrSettings(Pixmap.Format.RGB888), FrameBufferMgrSettings(Pixmap.Format.RGBA8888)),
            tag = "WorldRenderer", scaling = Scaling.fit, referenceWindowSize = WindowSize(1280, 720)
    )
    /**
     * Represents the rendered world as a framebuffer. Format has alpha.
     */
    val mainFrameBuffer: NestedFrameBuffer?
        get() = frameBufferManager.getFramebuffer(0)
    /**
     * Represents the total light portion (ambient light + lighting) as a framebuffer. Format will have no alpha.
     */
    val totalLightFrameBuffer: NestedFrameBuffer?
        get() = frameBufferManager.getFramebuffer(1)
    /**
     * Represents just the lighting portion for entities as a framebuffer. Used to handle "depth clipping". Format has alpha.
     */
    private val entityLightFrameBuffer: NestedFrameBuffer?
        get() = frameBufferManager.getFramebuffer(2)

    
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
        frameBufferManager.frameUpdate()
        
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
        val camWidth = camera.viewportWidth * camera.zoom
        val camHeight = camera.viewportHeight * camera.zoom
        val leftEdge = camera.position.x - camWidth / 2f
        val bottomEdge = camera.position.y - camHeight / 2f
        val currentTileset = this.tileset

        val visibleAreaRect = RectangleStack.getAndPush().set(leftEdge, bottomEdge, camWidth, camHeight)
        val entityRenderTime = measureNanoTime {
            var entitiesRendered = 0
            val tmpEntityVec = Vector3Stack.getAndPush()
            val tmpEntityRect = RectangleStack.getAndPush()
            
            world.sortEntitiesByRenderOrder()
            world.entities.forEach { entity ->
                try {
                    val cullingInEffect = entity.shouldApplyRenderCulling()
                    if (cullingInEffect) {
                        entity.setCullingRect(tmpEntityRect, tmpEntityVec)
                    }
                    // Only render entities that are in scene
                    if (!cullingInEffect || tmpEntityRect.intersects(visibleAreaRect)) {
                        entitiesRendered++
                        entity.render(this, batch, currentTileset)
                    }
                } catch (e: Exception) {
                    Paintbox.LOGGER.error("Exception while normally rendering entity ${e.javaClass.name} $e", "WorldRenderer", throwable = e)
                    throw e
                }
            }
            
            RectangleStack.pop()
            Vector3Stack.pop()
            
            this.entitiesRenderedLastCall = entitiesRendered
        }
        this.entityRenderTimeNano = entityRenderTime

        // Blending for framebuffers w/ transparency in format. Assumes premultiplied
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        
        batch.end()
        batch.projectionMatrix = tmpMatrix

        
        // Build and render light buffer
        val entityLightFb = entityLightFrameBuffer
        val totalLightFb = totalLightFrameBuffer
        if (entityLightFb != null && totalLightFb != null && !world.spotlights.isAmbientLightingFull()) { // Full ambient lighting means no lights are visible anyway
            val oldSrcFunc = batch.blendSrcFunc
            val oldDstFunc = batch.blendDstFunc
            tmpMatrix.set(batch.projectionMatrix)
            
            // Use world camera
            batch.projectionMatrix = this.camera.combined
            
            // Render entity light buffer
            entityLightFb.begin()
            batch.begin()
            
            Gdx.gl.glClearColor(1f, 1f, 1f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            
            // Entities either block or emit light
            batch.setColor(1f, 1f, 1f, 1f)
            this.entityRenderTimeNano += measureNanoTime {
                val tmpEntityVec = Vector3Stack.getAndPush()
                val tmpEntityRect = RectangleStack.getAndPush()
                
                if (world.entities.any { it is HasLightingRender }) {
//                    world.sortEntitiesByRenderOrder(WorldRenderer.comparatorRenderOrderLighting)
                    world.entities.forEach { entity ->
                        try {
                            if (entity is HasLightingRender) {
                                batch.setBlendFunction(GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_ALPHA)
                                entity.renderBlockingEffectBeforeLighting(this, batch, currentTileset)

                                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
                                entity.renderLightingEffect(this, batch, currentTileset)

                                batch.setBlendFunction(GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_ALPHA)
                                entity.renderBlockingEffectAfterLighting(this, batch, currentTileset)
                            } else {
                                val cullingInEffect = entity.shouldApplyRenderCulling()
                                if (cullingInEffect) {
                                    entity.setCullingRect(tmpEntityRect, tmpEntityVec)
                                }
                                if (!cullingInEffect || tmpEntityRect.intersects(visibleAreaRect)) {
                                    // Not a light emitter, so just render the blocking effect using Entity.render
                                    batch.setBlendFunction(GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_ALPHA)
                                    entity.render(this, batch, currentTileset)
                                }
                            }
                        } catch (e: Exception) {
                            Paintbox.LOGGER.error("Exception while lighting buffer rendering entity ${e.javaClass.name} $e", "WorldRenderer", throwable = e)
                            throw e
                        }
                    }
                }
                
                RectangleStack.pop()
                Vector3Stack.pop()
            }
            
            batch.end()
            batch.setColor(1f, 1f, 1f, 1f)
            entityLightFb.end()
            
            
            // Render total light buffer
            totalLightFb.begin()
            batch.begin()
            
            val spotlights = world.spotlights

            // Clear with ambient light colour
            ColorStack.use { tmpColor ->
                spotlights.ambientLight.computeFinalForAmbientLight(tmpColor)
                Gdx.gl.glClearColor(tmpColor.r, tmpColor.g, tmpColor.b, 1f)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            }
            
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
            batch.setColor(1f, 1f, 1f, 1f)
            
            // Switch to fb space
            batch.projectionMatrix = fbRenderCamera.combined
            // Draw the entity light buffer on top
            val entityFbTex = entityLightFb.colorBufferTexture
            batch.draw(entityFbTex, 0f, 0f, fbRenderCamera.viewportWidth, fbRenderCamera.viewportHeight, 0, 0, entityFbTex.width, entityFbTex.height, false, true)

            
            // Back to world space
            batch.projectionMatrix = this.camera.combined
            
            // Render each spotlight, this IGNORES entity blocking
            Vector3Stack.use { tmpVec ->
                val lightTex = AssetRegistry.get<Texture>("world_light_spotlight")
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
            }

            batch.end()
            batch.setColor(1f, 1f, 1f, 1f)
            totalLightFb.end()


            // Render total light fb onto world scene
            batch.projectionMatrix = fbRenderCamera.combined
            batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ZERO)
            batch.begin()

            batch.setColor(1f, 1f, 1f, 1f)
            val fbTex = totalLightFb.colorBufferTexture
            batch.draw(fbTex, 0f, 0f, fbRenderCamera.viewportWidth, fbRenderCamera.viewportHeight, 0, 0, fbTex.width, fbTex.height, false, true)

            batch.end()
            batch.setColor(1f, 1f, 1f, 1f)
            batch.setBlendFunction(oldSrcFunc, oldDstFunc)
            batch.projectionMatrix = tmpMatrix
        }
        
        RectangleStack.pop() // visibleAreaRect
        
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

    override fun dispose() {
        removeWorldHooks()
        frameBufferManager.disposeQuietly()
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