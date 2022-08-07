package polyrhythmmania.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Scaling
import paintbox.Paintbox
import paintbox.util.WindowSize
import paintbox.util.gdxutils.NestedFrameBuffer
import paintbox.util.gdxutils.disposeQuietly
import kotlin.math.roundToInt


data class FrameBufferMgrSettings(
        val format: Pixmap.Format = Pixmap.Format.RGBA8888,
        val hasDepth: Boolean = false, val hasStencil: Boolean = false,
        val minFilter: Texture.TextureFilter = Texture.TextureFilter.Linear,
        val magFilter: Texture.TextureFilter = Texture.TextureFilter.Linear,
)

/**
 * A [FrameBufferManager] manages a set of [NestedFrameBuffer]s where they all have a size and ratio dictated by
 * [Scaling]. The buffers get automatically disposed and recreated whenever the window size changes.
 */
class FrameBufferManager(
        val frameBufferSettings: List<FrameBufferMgrSettings>,
        val tag: String = "",
        val scaling: Scaling = Scaling.fit,
        val referenceWindowSize: WindowSize = WindowSize(1280, 720),
) : Disposable {
    
    private val loggerTag: String = if (this.tag == "") "FrameBufferManager" else "FrameBufferManager: ${this.tag}"
    private val numBuffers: Int = frameBufferSettings.size
    private val framebuffers: Array<NestedFrameBuffer?> = Array(numBuffers) { null }
    private var lastKnownWindowSize: WindowSize = WindowSize(-1, -1)
    private var framebufferSize: WindowSize = WindowSize(0, 0)
    private var isDisposed: Boolean = false

    fun getFramebuffer(index: Int): NestedFrameBuffer? = framebuffers.getOrNull(index)
    
    /**
     * Call once per frame. May update the frame buffers inside this manager.
     *
     * Must be called during rendering and not in Gdx.postRunnable.
     * 
     * If any dimension of the current window size is 0 (possible if the window is minimized),
     * then the [referenceWindowSize] is used as the buffer size.
     */
    fun frameUpdate() {
        val cachedWindowSize = this.lastKnownWindowSize
        val nullWindowSize = cachedWindowSize.width == -1 || cachedWindowSize.height == -1
        val windowWidth = Gdx.graphics.width
        val windowHeight = Gdx.graphics.height
        if (nullWindowSize || ((windowWidth > 0 && windowHeight > 0) && (windowWidth != cachedWindowSize.width || windowHeight != cachedWindowSize.height))) {
            // Width and/or height MAY be 0.
            this.lastKnownWindowSize = WindowSize(windowWidth, windowHeight)

            val vpWidth: Int
            val vpHeight: Int
            this.scaling.apply(referenceWindowSize.width.toFloat(), referenceWindowSize.height.toFloat(), windowWidth.toFloat(), windowHeight.toFloat()).let { worldSize ->
                vpWidth = worldSize.x.roundToInt()
                vpHeight = worldSize.y.roundToInt()
            }

            val cachedFramebufferSize = this.framebufferSize
            if (vpWidth > 0 && vpHeight > 0 && (cachedFramebufferSize.width != vpWidth || cachedFramebufferSize.height != vpHeight)) {
                createFramebuffers(vpWidth, vpHeight)
            } else if (nullWindowSize) {
                createFramebuffers(referenceWindowSize.width, referenceWindowSize.height)
            }
        }
    }

    private fun disposeFramebuffers() {
        val framebuffersArray = this.framebuffers
        val oldFbsList = framebuffersArray.toList()
        framebuffersArray.fill(null)
        
        var numDisposed = 0
        oldFbsList.forEach {
            if (it != null) {
                it.disposeQuietly()
                numDisposed++
            }
        }
        if (numDisposed > 0) {
            Paintbox.LOGGER.debug("Disposed of $numDisposed framebuffers", tag = loggerTag)
        }
    }

    private fun createFramebuffers(width: Int, height: Int) {
        val framebuffersArray = this.framebuffers
        disposeFramebuffers()

        if (!this.isDisposed) {
            val newFbWidth = HdpiUtils.toBackBufferX(width)
            val newFbHeight = HdpiUtils.toBackBufferY(height)
            framebuffersArray.indices.forEach { i ->
                val settings = frameBufferSettings[i]
                framebuffersArray[i] = NestedFrameBuffer(settings.format, newFbWidth, newFbHeight, settings.hasDepth, settings.hasStencil).apply {
                    this.colorBufferTexture.setFilter(settings.minFilter, settings.magFilter)
                }
            }
            this.framebufferSize = WindowSize(newFbWidth, newFbHeight)
            Paintbox.LOGGER.debug("Re-created $numBuffers framebuffers to be backbuffer ${newFbWidth}x${newFbHeight} (logical ${width}x${height})", tag = loggerTag)
        }
    }

    override fun dispose() {
        if (!isDisposed) {
            disposeFramebuffers()
            isDisposed = true
        }
    }
}
