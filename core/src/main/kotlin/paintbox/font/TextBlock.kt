package paintbox.font

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import org.lwjgl.system.MathUtil
import paintbox.util.ColorStack
import paintbox.util.gdxutils.scaleMul


/**
 * A cached list of [TextRun]s, with various metrics pre-computed.
 */
data class TextBlock(val runs: List<TextRun>) {

    /*
    TextBlock internal workings:
    TextBlock allows for combining multiple fonts and styles into a single text block.
    It also allows for render-time alignment changes (left, centre, right).
    
    In order to do this, TextBlock needs to be aware of each gdx GlyphRun and its position,
    as well as where each line is (for horizontal positioning).
    
    Each TextRun is mapped to a gdx GlyphLayout. Each GlyphLayout has a series of
    GlyphRuns, which has their own x/y positions.
    
    TextBlock will take all these "flattened" GlyphRuns and treat them as if they are strung together.
    It recomputes the x/y for all of them based on each TextRun's style. It will also be
    aware of the individual lines since that is used for horizontal alignment.
    
    For rendering purposes, each GlyphRun is then mapped back to its own GlyphLayout, which can be passed
    into the gdx BitmapFont draw function.
     */

    data class TextRunInfo(val run: TextRun, val currentFontNumber: Long, val glyphLayout: GlyphLayout) {
        val font: PaintboxFont = run.font
        val width: Float = glyphLayout.width
        val height: Float = glyphLayout.height
        val glyphRunInfo: List<GlyphRunInfo> = glyphLayout.runs.map { glyphRun ->
            GlyphRunInfo(this, glyphRun)
        }
    }

    data class GlyphRunInfo(val textRunInfo: TextRunInfo, val glyphRun: GlyphLayout.GlyphRun) {
        val glyphRunAsLayout: GlyphLayout = GlyphLayout().also { l ->
            l.runs.clear()
            l.runs.setSize(1)
            l.runs[0] = glyphRun
        }
        var lineIndex: Int = 0
        var posX: Float = 0f
        var posY: Float = 0f
    }

    data class LineInfo(
            val index: Int, val width: Float, val posY: Float,
            /*// Pair in order of (TextRun index, GlyphRunInfo index) 
                        val glyphIndexStart: Pair<Int, Int>, val glyphIndexEndEx: Pair<Int, Int>*/
    )

    var lineWrapping: Float? = null

    private var runInfo: List<TextRunInfo> = listOf()
    var lineInfo: List<LineInfo> = listOf()
        private set

    var width: Float = 0f
        private set
    var height: Float = 0f
        private set

    var firstCapHeight: Float = 0f
        private set
    var lastDescent: Float = 0f
        private set

    constructor(runs: List<TextRun>, wrapWidth: Float) : this(runs) {
        this.lineWrapping = wrapWidth
    }


    private fun adjustFontForTextRun(font: BitmapFont, textRun: TextRun) {
        font.scaleMul(textRun.scaleX, textRun.scaleY)
    }

    private fun resetFontForTextRun(font: BitmapFont, textRun: TextRun) {
        font.scaleMul(1f / textRun.scaleX, 1f / textRun.scaleY)
    }

    fun computeLayouts() {
        var maxPosX: Float = 0f
        var posX: Float = 0f
        var posY: Float = 0f
        var lastCapHeight = 0f
        this.firstCapHeight = 0f

        val lineInfo: MutableList<LineInfo> = mutableListOf()
        var currentLineWidth = 0f
        var currentLineIndex = 0
        var currentLineStartY = 0f

        val doLineWrapping: Boolean = this.lineWrapping != null
        val lineWrapWidth: Float = this.lineWrapping ?: 0f

        val runInfo: List<TextRunInfo> = runs.mapIndexed { textRunIndex, textRun ->
            // Set font scales and metrics
            val paintboxFont = textRun.font
            val font = paintboxFont.begin()
            adjustFontForTextRun(font, textRun)

            val color = Color(1f, 1f, 1f, 1f)
            Color.argb8888ToColor(color, textRun.color)
            val textRunInfo = TextRunInfo(textRun, paintboxFont.currentFontNumber,
                    if (doLineWrapping) {
                        val continuationLineWidth = (lineWrapWidth - currentLineWidth).coerceAtLeast(0f)
                        // Find the trailing line's wrap point since it may not start at x=0
                        var text = textRun.text
                        val gl = GlyphLayout()
                        // Don't wrap text here, we need to find the first line of runs
                        gl.setText(font, text, color, (lineWrapWidth).coerceAtLeast(0f), Align.left, false)
                        if (continuationLineWidth < lineWrapWidth && gl.runs.size > 0) {
                            // The continuation line width is smaller, so find the wrap point there.
                            // But we need to verify that the line break is where it ought to be, and NOT
                            // a consequence of the (temp) smaller max width.   
                            
                            // Only the first run matters for wrapping.    
                            val firstRunWidth = gl.runs.first().width
                            if (firstRunWidth > continuationLineWidth) {
                                // The contiguous block does NOT fit! Find the wrap point and inject a newline.
                                gl.setText(font, textRun.text, color, continuationLineWidth, Align.left, true)
                                if (gl.runs.size >= 2) {
                                    // Inject the newline where the new run was added. The new run will always be
                                    // the second one, since the first original run will have been split.    
                                    val first = gl.runs[0]
                                    val wrapIndex = first.glyphs.size + 1
                                    if (wrapIndex in 0 until text.length) {
                                        text = text.substring(0, wrapIndex) + "\n" + text.substring(wrapIndex).trimStart()
                                    }
                                }
                            }
                        }
                        gl.setText(font, text, color, (lineWrapWidth).coerceAtLeast(0f), Align.left, true)
                        gl
                    } else {
                        GlyphLayout(font, textRun.text, color, 0f, Align.left, false)
                    })

            val offX = textRun.offsetXEm * font.spaceXadvance
            val offY = textRun.offsetYEm * font.xHeight
            val xAdvanceEm = textRun.xAdvanceEm * font.spaceXadvance
            val capHeight = font.data.capHeight
            if (this.firstCapHeight == 0f) {
                this.firstCapHeight = capHeight
            }

            // Each GlyphRun inside the GlyphLayout inside TextRunInfo is relative to 0, 0
            // Adjust x/y of GlyphRunInfo inside TextRunInfo
            val yBeforeGlyphRuns = posY

            // Offset by the TextRun offset
            posX += offX
            posY += offY

            if (currentLineStartY == 0f) {
                // First line needs to have its y position set
                currentLineStartY = posY
            }

            var lastGlyphRunRightEdge = 0f
            var lastGlyphRunY = 0f
            var updateCurrentLineStartY = false
            textRunInfo.glyphRunInfo.forEachIndexed { glyphRunInfoIndex, glyphRunInfo ->
                val glyphRun = glyphRunInfo.glyphRun

                glyphRunInfo.lineIndex = currentLineIndex

                // Update X/Y based on GlyphRun x/y
                if (glyphRun.x < lastGlyphRunRightEdge
                        || (glyphRunInfoIndex == 0 && textRun.text.isNotEmpty() && textRun.text[0] == '\n')) {
                    // The glyph went back to the left. Consider this a new line
                    // It is also a new line if the text run begins with a newline (otherwise it would get appended to the last one)
                    currentLineWidth = posX
                    posX = 0f

//                    val firstInfoIndex = lineInfo.lastOrNull()?.glyphIndexEndEx ?: Pair(0, 0)
                    lineInfo += LineInfo(
                            currentLineIndex, currentLineWidth, currentLineStartY,
                            /*firstInfoIndex, Pair(textRunIndex, glyphRunInfoIndex)*/
                    )
                    currentLineIndex++
                    currentLineWidth = 0f
                    updateCurrentLineStartY = true
                    glyphRunInfo.lineIndex = currentLineIndex
                } else {
                    posX += glyphRun.x
                    posY += glyphRun.y
                }

                glyphRunInfo.posX = posX
                // Adding capHeight makes the drawing position at the baseline
                glyphRunInfo.posY = posY + capHeight

                // Bump x forward by width of GlyphRun
                posX += glyphRun.width
                if (posX > maxPosX)
                    maxPosX = posX

                lastGlyphRunRightEdge = glyphRun.x + glyphRun.width
                lastGlyphRunY = glyphRun.y
            }

            // Reverse any x offsets if they are not to be carried over
            if (!textRun.carryOverOffsetX) {
                posX -= offX
            }

            // Move y down in case there are whitespace lines
            posX += xAdvanceEm
            posY = yBeforeGlyphRuns + -(textRunInfo.glyphLayout.height - capHeight)
            lastCapHeight = font.data.capHeight
            lastDescent = font.data.descent

            // New line check
            if (posY < yBeforeGlyphRuns) {
                // The posX should be where the last GlyphRun left off
                // However, if the run ends in newlines with optional whitespace, then posX should be 0
                val runText = textRunInfo.run.text
                val runEndsInNewlines = runText.trimEnd { it == ' ' }.endsWith('\n') //textRunInfo.glyphLayout.height - capHeight > abs(lastGlyphRunY)
                if (runEndsInNewlines) {
                    currentLineWidth = posX
                    posX = 0f
                    // This is a new line
//                    val firstInfoIndex = lineInfo.lastOrNull()?.glyphIndexEndEx ?: Pair(0, 0)
                    lineInfo += LineInfo(
                            currentLineIndex, currentLineWidth, currentLineStartY,
                            /*firstInfoIndex, Pair(textRunIndex, textRunInfo.glyphRunInfo.size)*/
                    )
                    currentLineIndex++
                    currentLineWidth = 0f
                    updateCurrentLineStartY = true
                } else {
                    // Not a new line.
                    posX = lastGlyphRunRightEdge
                }
            }
            if (updateCurrentLineStartY)
                currentLineStartY = posY

            // Carry over the y offset if necessary
            if (textRun.carryOverOffsetY) {
                posY += offY
            }

            currentLineWidth = posX

            // Reset font scale
            resetFontForTextRun(font, textRun)
            paintboxFont.end()
            textRunInfo
        }

//        val firstInfoIndex = lineInfo.lastOrNull()?.glyphIndexEndEx ?: Pair(0, 0)
        lineInfo += LineInfo(
                currentLineIndex, currentLineWidth, currentLineStartY,
                /*firstInfoIndex, runInfo.size to runInfo.last().glyphRunInfo.size*/
        )

        this.runInfo = runInfo
        this.lineInfo = lineInfo
        this.width = maxPosX
        this.height = -posY + firstCapHeight
    }

    fun isRunInfoInvalid(): Boolean {
        return (runInfo.isEmpty() && runs.isNotEmpty()) || runInfo.any { l ->
            l.run.font.currentFontNumber != l.currentFontNumber
        }
    }

    /**
     * Draws this text block. The y value is the baseline value. Same as calling [drawCompressed] with compressText = false.
     */
    fun draw(batch: SpriteBatch, x: Float, y: Float, align: TextAlign = TextAlign.LEFT,
             scaleX: Float = 1f, scaleY: Float = 1f, alignAffectsRender: Boolean = false,) {
        drawCompressed(batch, x, y, 0f, align, scaleX, scaleY, alignAffectsRender = alignAffectsRender,
                compressText = false)
    }

    /**
     * Draws this text block, constraining to the [maxWidth] if necessary.
     * The y value is the baseline value of the first line.
     * The [align] determines how each *line* of text is aligned horizontally.
     * The [batch]'s color is used to tint.
     * 
     * @param alignAffectsRender If true, the [align] param will also change the render alignment according to [maxWidth].
     * For example, if [align] is [TextAlign.RIGHT], then the ENTIRE text block will be right-aligned according to 
     * the [maxWidth]. If [alignAffectsRender] was false, then the entire block is rendered left-aligned.
     */
    fun drawCompressed(batch: SpriteBatch, x: Float, y: Float, maxWidth: Float,
                       align: TextAlign = TextAlign.LEFT, scaleX: Float = 1f, scaleY: Float = 1f,
                       alignAffectsRender: Boolean = false, compressText: Boolean = true) {
        if (isRunInfoInvalid()) {
            computeLayouts()
        }
        val runInfo = this.runInfo
        if (runInfo.isEmpty())
            return
        if (compressText && maxWidth <= 0f)
            return

        val batchColor: Float = batch.packedColor

        val tint = ColorStack.getAndPush().set(batch.color)
        val requiresTinting = tint.r != 1f || tint.g != 1f || tint.b != 1f || tint.a != 1f

        val globalScaleX: Float = scaleX * (if (maxWidth <= 0f || this.width <= 0f || this.width * scaleX < maxWidth) (1f) else (maxWidth / (this.width * scaleX)))
        val globalScaleY: Float = scaleY

        if (globalScaleX <= MathUtils.FLOAT_ROUNDING_ERROR || globalScaleY <= MathUtils.FLOAT_ROUNDING_ERROR || globalScaleX.isNaN() || globalScaleY.isNaN()) {
            ColorStack.pop()
            return
        }

        val shouldScaleX = globalScaleX != 1f
        val shouldScaleY = globalScaleY != 1f
        val scaleAnything = shouldScaleX || shouldScaleY
        val alignXWidth: Float = if (alignAffectsRender) {
            maxWidth
        } else if (shouldScaleX) (this.width * globalScaleX) else (this.width)

        runInfo.forEach { textRunInfo ->
            val paintboxFont = textRunInfo.font
            val font = paintboxFont.begin()
            adjustFontForTextRun(font, textRunInfo.run)

            if (scaleAnything) {
                font.scaleMul(globalScaleX, globalScaleY)
            }

            textRunInfo.glyphRunInfo.forEach { glyphRunInfo ->
                val layout = glyphRunInfo.glyphRunAsLayout
                val alignXOffset = when (align) {
                    TextAlign.LEFT -> 0f
                    TextAlign.CENTRE -> (alignXWidth - lineInfo[glyphRunInfo.lineIndex].width * globalScaleX) / 2f
                    TextAlign.RIGHT -> (alignXWidth - lineInfo[glyphRunInfo.lineIndex].width * globalScaleX)
                }

                if (shouldScaleX) {
                    layout.runs.forEach { run ->
                        for (i in 0 until run.xAdvances.size) {
                            run.xAdvances[i] *= globalScaleX
                        }
                    }
                }
                if (shouldScaleY) {
                    layout.runs.forEach { run ->
                        run.y *= globalScaleY
                    }
                }

                val runCount = layout.runs.size
                if (requiresTinting) {
                    for (i in 0 until runCount) {
                        val run = layout.runs[i]
                        ColorStack.getAndPush().set(run.color)
                        if (run.color.r == 1f && run.color.g == 1f && run.color.b == 1f) {
                            run.color.mul(tint)
                        } else {
                            run.color.a *= tint.a // Ignore RGB
                        }
                    }
                }

                font.draw(batch, layout,
                        x + (glyphRunInfo.posX) * globalScaleX + alignXOffset,
                        y + (glyphRunInfo.posY) * globalScaleY)

                if (requiresTinting) {
                    for (index in runCount - 1 downTo 0) {
                        val popped = ColorStack.pop() ?: continue
                        val run = layout.runs[index]
                        run.color.set(popped)
                    }
                }

                if (shouldScaleX) {
                    layout.runs.forEach { run ->
                        for (i in 0 until run.xAdvances.size) {
                            run.xAdvances[i] /= globalScaleX
                        }
                    }
                }
                if (shouldScaleY) {
                    layout.runs.forEach { run ->
                        run.y /= globalScaleY
                    }
                }
            }

            if (scaleAnything) {
                font.scaleMul(1f / globalScaleX, 1f / globalScaleY)
            }

            resetFontForTextRun(font, textRunInfo.run)
            paintboxFont.end()
        }

        ColorStack.pop()

        batch.packedColor = batchColor
    }

    fun recolorAll(newColor: Color): TextBlock {
        return this.copy(runs = runs.map { it.copy(color = Color.argb8888(newColor)) })
    }

}