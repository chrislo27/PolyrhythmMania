package io.github.chrislo27.paintbox.font

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.Disposable
import io.github.chrislo27.paintbox.util.WindowSize
import io.github.chrislo27.paintbox.util.gdxutils.copy
import kotlin.math.min


abstract class PaintboxFont(val file: FileHandle, val windowSize: WindowSize,
                            val fontSize: Int, val borderSize: Float,
                            val loadPriority: LoadPriority) : Disposable {

    enum class LoadPriority {
        ALWAYS, LAZY
    }

    protected abstract var currentFont: BitmapFont?
    
    protected var lastWidth: Float = 1f
    protected var lastHeight: Float = 1f

    val font: BitmapFont
        get() {
            return if (currentFont == null && loadPriority == LoadPriority.LAZY) {
                load()
                currentFont!!
            } else {
                currentFont!!
            }
        }

    open fun resize(width: Float, height: Float) {
        lastWidth = width
        lastHeight = height
    }
    
    abstract fun load()

}

class PaintboxFontFreeType(file: FileHandle, windowSize: WindowSize, fontSize: Int, borderSize: Float,
                           val ftfParameter: FreeTypeFontGenerator.FreeTypeFontParameter, loadPriority: LoadPriority)
    : PaintboxFont(file, windowSize, fontSize, borderSize, loadPriority) {

    companion object {
        init {
            FreeTypeFontGenerator.setMaxTextureSize(2048)
        }
    }

    constructor(file: FileHandle, defaultWindowSize: WindowSize,
                parameter: FreeTypeFontGenerator.FreeTypeFontParameter, loadPriority: LoadPriority) :
            this(file, defaultWindowSize, parameter.size, parameter.borderWidth, parameter, loadPriority)

    private var generator: FreeTypeFontGenerator? = null
    override var currentFont: BitmapFont? = null
    private var afterLoad: PaintboxFontFreeType.(BitmapFont) -> Unit = {}
    private var oldScale: Float = -1f

    fun setAfterLoad(func: PaintboxFontFreeType.(BitmapFont) -> Unit): PaintboxFontFreeType {
        afterLoad = func
        return this
    }

    override fun resize(width: Float, height: Float) {
        super.resize(width, height)
        this.dispose()
        if (loadPriority == LoadPriority.ALWAYS) {
            load()
        }
    }

    override fun load() {
        val scale: Float = min(lastWidth / windowSize.width, lastHeight / windowSize.height)
        if (scale != oldScale) {
            oldScale = scale
            this.dispose()

            val newParam = ftfParameter.copy()
            newParam.size = (fontSize * scale).toInt()
            newParam.borderWidth = borderSize * scale

            val generator = FreeTypeFontGenerator(file)
            this.generator = generator
            val generatedFont = generator.generateFont(newParam)
            currentFont = generatedFont
            this.afterLoad(generatedFont)
        }
    }

    @Synchronized
    override fun dispose() {
        val font = this.currentFont
        if (font != null) {
            font.dispose()
            (font.data as? Disposable)?.dispose()
        }
        generator?.dispose()

        this.currentFont = null
        generator = null
    }
}

class PaintboxFontBitmap(file: FileHandle, windowSize: WindowSize, fontSize: Int, borderSize: Float,
                         font: BitmapFont)
    : PaintboxFont(file, windowSize, fontSize, borderSize, LoadPriority.ALWAYS) {

    override var currentFont: BitmapFont? = font

    override fun load() {
    }

    override fun dispose() {
        currentFont?.dispose()
        currentFont = null
    }
}
