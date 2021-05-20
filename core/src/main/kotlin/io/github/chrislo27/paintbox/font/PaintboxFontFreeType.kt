package io.github.chrislo27.paintbox.font

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.Disposable
import io.github.chrislo27.paintbox.util.WindowSize
import io.github.chrislo27.paintbox.util.gdxutils.copy
import java.lang.Float.min

/**
 * A wrapper around a [FreeTypeFontGenerator].
 * 
 * The [params] passed in will be copied with the [ftfParameter]'s font and border size.
 */
class PaintboxFontFreeType(params: PaintboxFontParams,
                           val ftfParameter: FreeTypeFontGenerator.FreeTypeFontParameter)
    : PaintboxFont(params.copy(fontSize = ftfParameter.size, borderSize = ftfParameter.borderWidth)) {

    companion object {
        init {
            FreeTypeFontGenerator.setMaxTextureSize(2048)
        }
    }

    private var isInBegin: Boolean = false
    private var isLoaded: Boolean = false
    
    private var currentFontNum: Long = Long.MIN_VALUE + 1
    private var lastWindowSize: WindowSize = WindowSize(1280, 720)
    private var upscaledFactor: Float = -1f // 
    private var generator: FreeTypeFontGenerator? = null
        set(value) {
            field?.dispose()
            field = value
        }
    private var currentFont: BitmapFont? = null
        set(value) {
            if (value !== field) {
                currentFontNum++
                
//                val current = field
//                if (current != null) {
//                    current.dispose()
//                    (current.data as? Disposable)?.dispose()
//                }
            }
            field = value
        }
    
    private var afterLoad: PaintboxFontFreeType.(BitmapFont) -> Unit = {}
    
    override fun begin(areaWidth: Float, areaHeight: Float): BitmapFont {
        if (isInBegin) error("Cannot call begin before end")
        isInBegin = true

        if (!isLoaded || currentFont == null) {
            // If the loadPriority is ALWAYS, this will already have been loaded in resize()
            load()
        }
        
        if (this.params.scaleToReferenceSize) {
            val font = currentFont!!
            val referenceSize = this.params.referenceSize
            val scaleX = (referenceSize.width / areaWidth)
            val scaleY = (referenceSize.height / areaHeight)
            if (scaleX >= 0f && scaleY >= 0f && scaleX.isFinite() && scaleY.isFinite()) {
                font.data.setScale(scaleX, scaleY)
            }
        }
        
        return currentFont!!
    }

    override fun end() {
        if (!isInBegin) error("Cannot call end before begin")
        isInBegin = false
        
        // Sets font back to scaleXY = 1.0
        val currentFont = this.currentFont
        if (currentFont != null) {
            this.fontDataInfo.applyToFont(currentFont)
        }
    }
    
    override fun resize(width: Int, height: Int) {
        this.dispose()
        lastWindowSize = WindowSize(width, height)
        if (params.loadPriority == PaintboxFontParams.LoadPriority.ALWAYS/* || params.scaleToReferenceSize*/) {
            load()
        }
    }
    
    private fun load() {
        val windowSize = this.lastWindowSize
        val referenceSize = params.referenceSize
        val scale: Float = if (!params.scaleToReferenceSize) 1f else min(windowSize.width.toFloat() / referenceSize.width, windowSize.height.toFloat() / referenceSize.height)
        
        if (this.upscaledFactor != scale) {
            this.dispose()
//            println("New upscaled factor: ${this.upscaledFactor} to $scale    $windowSize, ref $referenceSize")
            this.upscaledFactor = scale
            

            val newParam = ftfParameter.copy()
            val params = this.params
            newParam.size = (params.fontSize * scale).toInt()
            newParam.borderWidth = params.borderSize * scale

            val generator = FreeTypeFontGenerator(params.file)
            val generatedFont = generator.generateFont(newParam)
            this.generator = generator
            this.currentFont = generatedFont
            this.fontDataInfo.copyFromFont(generatedFont)

            this.afterLoad(generatedFont)
        }
        
        this.isLoaded = true
    }

    fun setAfterLoad(func: PaintboxFontFreeType.(BitmapFont) -> Unit): PaintboxFontFreeType {
        afterLoad = func
        return this
    }

    override fun getCurrentFontNumber(): Long = currentFontNum

    @Synchronized
    override fun dispose() {
        this.isLoaded = false
    }
}
