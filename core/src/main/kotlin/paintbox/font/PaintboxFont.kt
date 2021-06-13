package paintbox.font

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.utils.Disposable
import paintbox.PaintboxGame
import paintbox.binding.ReadOnlyVar
import paintbox.util.WindowSize


/**
 * A [PaintboxFont] is the controller and provider of a [BitmapFont], which may change throughout
 * the program lifecycle.
 *
 * To use the [BitmapFont], call [useFont] and use the supplied font parameter.
 */
abstract class PaintboxFont(val params: PaintboxFontParams)
    : Disposable {

    /**
     * For caching the information in [BitmapFont.BitmapFontData] to prevent floating point error with repeated
     * multiply-and-divide for scaling.
     */
    protected data class FontDataInfo(var scaleX: Float, var scaleY: Float,
                                      var lineHeight: Float, var spaceXadvance: Float, var xHeight: Float,
                                      var capHeight: Float, var ascent: Float, var descent: Float, var down: Float,
                                      var padLeft: Float, var padRight: Float, var padTop: Float, var padBottom: Float) {
        constructor(data: BitmapFont.BitmapFontData) : 
                this(data.scaleX, data.scaleY, data.lineHeight, data.spaceXadvance, data.xHeight, data.capHeight,
                        data.ascent, data.descent, data.down, data.padLeft, data.padRight, data.padTop, data.padBottom)
        
        constructor() : this(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        
        fun applyToFont(bitmapFont: BitmapFont) {
            val data = bitmapFont.data
            data.scaleX = this.scaleX
            data.scaleY = this.scaleY
            data.lineHeight = this.lineHeight
            data.spaceXadvance = this.spaceXadvance
            data.xHeight = this.xHeight
            data.capHeight = this.capHeight
            data.ascent = this.ascent
            data.descent = this.descent
            data.down = this.down
            data.padLeft = this.padLeft
            data.padRight = this.padRight
            data.padTop = this.padTop
            data.padBottom = this.padBottom
        }
        
        fun copyFromFont(bitmapFont: BitmapFont) {
            val data = bitmapFont.data
            this.scaleX = data.scaleX
            this.scaleY = data.scaleY
            this.lineHeight = data.lineHeight
            this.spaceXadvance = data.spaceXadvance
            this.xHeight = data.xHeight
            this.capHeight = data.capHeight
            this.ascent = data.ascent
            this.descent = data.descent
            this.down = data.down
            this.padLeft = data.padLeft
            this.padRight = data.padRight
            this.padTop = data.padTop
            this.padBottom = data.padBottom
        }
    }
    
    protected val fontDataInfo: FontDataInfo = FontDataInfo()

    /**
     * Returns the current font number. The font number is changed every time the backing [BitmapFont] changes.
     *
     * Used by [TextBlock] to determine when text layouts expire.
     */
    abstract val currentFontNumber: Long
    abstract val currentFontNumberVar: ReadOnlyVar<Long>

    /**
     * Called by [PaintboxGame] whenever the window gets resized.
     */
    abstract fun resize(width: Int, height: Int)

    /**
     * Instructs the font instance to provide a [BitmapFont] for use.
     * The [areaWidth] and [areaHeight] are used with reference scaling based on the settings in
     * [PaintboxFontParams].
     *
     * Call [end] once finished. Implementors must ensure that this function cannot be called if attempting to
     * begin again before [end] was called.
     */
    abstract fun begin(areaWidth: Float, areaHeight: Float): BitmapFont

    /**
     * Uses the passed in [camera]'s width and height as the area width and height. This assumes that the camera
     * area spans the entire window. If the camera does NOT span the window (for example, for a frame buffer),
     * use the normal [begin] function with the width and height of the actual area.
     * @see [begin]
     */
    fun begin(camera: OrthographicCamera): BitmapFont = begin(camera.viewportWidth, camera.viewportHeight)


    /**
     * Uses [Gdx.graphics.getWidth][Graphics.getWidth] and [Gdx.graphics.getHeight][Graphics.getHeight] as
     * the area width and height. If the camera does NOT span the window (for example, for a frame buffer),
     * use the normal [begin] function with the width and height of the actual area.
     * @see [begin]
     */
    fun begin(): BitmapFont = begin(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    /**
     * Instructs the font instance that the usage of the provided [BitmapFont] from calling [begin] is complete
     * and cleanup can occur.
     *
     * Implementors must ensure that this function cannot be called if [begin] was not previously called.
     */
    abstract fun end()

    inline fun useFont(cameraWidth: Float, cameraHeight: Float, scope: (font: BitmapFont) -> Unit) {
        val font = begin(cameraWidth, cameraHeight)
        scope.invoke(font)
        end()
    }

    inline fun useFont(camera: OrthographicCamera, scope: (font: BitmapFont) -> Unit) {
        val font = begin(camera)
        scope.invoke(font)
        end()
    }

    inline fun useFont(scope: (font: BitmapFont) -> Unit) {
        val font = begin()
        scope.invoke(font)
        end()
    }

    abstract override fun dispose()

}

data class PaintboxFontParams(val file: FileHandle,
                              val fontSize: Int, val borderSize: Float,
                              val scaleToReferenceSize: Boolean, val referenceSize: WindowSize,
                              val loadPriority: LoadPriority = LoadPriority.LAZY) {
    enum class LoadPriority {
        /**
         * Suggests that the [PaintboxFont] should try to actively load its resources when updated/resized.
         */
        ALWAYS,

        /**
         * Suggests that the [PaintboxFont] should only load its resources when needed.
         */
        LAZY
    }
}
