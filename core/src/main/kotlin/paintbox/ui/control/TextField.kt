package paintbox.ui.control

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import paintbox.PaintboxGame
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.PaintboxFont
import paintbox.ui.*
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.Skin
import paintbox.ui.skin.SkinFactory
import paintbox.util.ColorStack
import paintbox.util.Vector2Stack
import paintbox.util.gdxutils.*

/**
 * A single-line text field.
 */
open class TextField(font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
    : Control<TextField>(), Focusable {
    companion object {
        const val TEXT_FIELD_SKIN_ID: String = "TextField"

        const val BACKSPACE: Char = 8.toChar()
        const val ENTER_DESKTOP = '\r'
        const val ENTER_ANDROID = '\n'
        const val TAB = '\t'
        const val DELETE: Char = 127.toChar()
        const val BULLET: Char = 149.toChar()

        const val KEY_REPEAT_INITIAL_TIME: Float = 0.4f
        const val KEY_REPEAT_INTERVAL: Float = 0.05f
        const val CARET_BLINK_RATE: Float = 0.5f
        const val DEFAULT_NEWLINE_WRAP: Char = '\uE056'
        const val DEFAULT_CARET_WIDTH: Float = 2f
        val DEFAULT_INPUT_FILTER: (Char) -> Boolean = { true }

        init {
            DefaultSkins.register(TEXT_FIELD_SKIN_ID, SkinFactory { element: TextField ->
                TextFieldSkin(element)
            })
        }
    }
    
    protected enum class KeyMode {
        NONE, MOVE_LEFT, MOVE_RIGHT, //BACKSPACE, DELETE
    }

    override val hasFocus: ReadOnlyVar<Boolean> = Var(false)
    private var caretBlinkTimer: Float = 0f
    private var keyRepeatTimer: Float = 0f
    private var keymode: KeyMode = KeyMode.NONE
    private val caretPos: Var<Int> = Var(0)

    val font: Var<PaintboxFont> = Var(font)
    val text: Var<String> = Var("")
    val emptyHintText: Var<String> = Var("")

    var inputFilter: Var<(Char) -> Boolean> = Var(DEFAULT_INPUT_FILTER)
    var enterPressedAction: () -> Unit = { requestUnfocus() }
    val characterLimit: Var<Int> = Var(Int.MAX_VALUE)
    val newlineWrapChar: Var<Char> = Var(DEFAULT_NEWLINE_WRAP)
    val isPassword: Var<Boolean> = Var(false)
    val canPasteText: Var<Boolean> = Var(true)
    val canInputNewlines: Var<Boolean> = Var(false)
    val textScale: FloatVar = FloatVar(1f)
    val textColor: Var<Color> = Var(Color(0f, 0f, 0f, 1f))
    val caretWidth: FloatVar = FloatVar(DEFAULT_CARET_WIDTH)

    private val glyphLayout: ReadOnlyVar<GlyphLayout> = Var.sideEffecting(GlyphLayout()) { layout ->
        val paintboxFont = this@TextField.font.use()
        paintboxFont.currentFontNumberVar.use()
        paintboxFont.useFont { bitmapFont ->
            bitmapFont.scaleMul(textScale.useF())
            val originalText = text.use()
            val translated = translateTextToRenderable(originalText, isPassword.use(), newlineWrapChar.use())
            layout.setText(bitmapFont, translated)
        }
        layout
    }

    /**
     * text.length + 1 positions, representing the cumulative x position from each character.
     */
    private val characterPositions: ReadOnlyVar<MutableList<Float>> = Var.sideEffecting(mutableListOf(0f)) { list ->
        list.clear()
        val gl = glyphLayout.use()
        val advances = gl.runs.firstOrNull()?.xAdvances // We only care about the first line (first GlyphRun).
        if (advances != null) {
            var cumulative = 0f
            for (i in 0 until advances.size) {
                val toAdd = cumulative + advances[i]
                list.add(toAdd)
                cumulative = toAdd
            }
        } else {
            list.add(0f)
        }
        list
    }

    /**
     * Represents the x offset for rendering the text. Represents the number of pixels to move the text left.
     * Used for rendering text and calculating where the cursor is to set the caret.
     */
    private val xOffset: FloatVar = FloatVar(0f)

    init {
        this.doClipping.set(true)
        
        this.text.addListener {
            val newLength = it.getOrCompute().length
            if (caretPos.getOrCompute() > newLength) setCaret(newLength)
        }

        this.addInputEventListener { event ->
            var consumed = false
            when (event) {
                is ClickPressed -> {
                    if (event.button == Input.Buttons.LEFT) {
                        onClickPressed(event)
                        consumed = true
                    }
                }
                is TouchDragged -> {
                    if (pressedState.getOrCompute().pressed) {
                        moveCaretFromMouse(event)
                        consumed = true
                    }
                }
                is KeyTyped -> {
                    if (hasFocus.getOrCompute()) {
                        onKeyTyped(event.character)
                        consumed = true
                    }
                }
                is KeyDown -> {
                    if (hasFocus.getOrCompute()) {
                        onKeyDown(event.keycode)
                        consumed = true
                    }
                }
                is KeyUp -> {
                    if (hasFocus.getOrCompute()) {
                        onKeyUp(event.keycode)
                        consumed = true
                    }
                }
            }
            consumed
        }
    }

    fun setCaret(position: Int): Int {
        val clamped = position.coerceIn(0, text.getOrCompute().length)
        caretPos.set(clamped)
        // Update xOffset
        val currentXOffset = xOffset.get()
        val characterPos = characterPositions.getOrCompute()
        val newCaretOffset = characterPos[clamped.coerceIn(0, characterPos.size - 1)].coerceAtLeast(0f)
        if (newCaretOffset < currentXOffset) {
            val contentZoneWidth = contentZone.width.get()
            xOffset.set(newCaretOffset.coerceAtMost(characterPos.last() - contentZoneWidth).coerceAtLeast(0f))
        } else {
            val contentZoneWidth = contentZone.width.get()
            if (newCaretOffset > currentXOffset + contentZoneWidth) {
                xOffset.set((newCaretOffset - contentZoneWidth).coerceAtLeast(0f))
            }
        }

        return clamped
    }

    fun resetCaretBlinkTimer() {
        caretBlinkTimer = 0f
    }

    protected open fun translateTextToRenderable(text: String, isPassword: Boolean, newlineWrapChar: Char): String {
        return (if (isPassword) BULLET.toString().repeat(text.length) else text)
                .replace("\r\n", "$newlineWrapChar")
                .replace('\r', newlineWrapChar)
                .replace('\n', newlineWrapChar)
    }

    protected open fun onKeyTyped(character: Char) {
        // Focus is assumed.
        val control = Gdx.input.isControlDown()
        val alt = Gdx.input.isAltDown()
        val shift = Gdx.input.isShiftDown()
        val currentText = text.getOrCompute()
        val caret = caretPos.getOrCompute()
        
        when (character) {
            TAB -> {}
            BACKSPACE -> {
                if (currentText.isNotEmpty() && caret > 0) {
                    val newCaretPos = if (control && !alt && !shift) {
                        getNextWordPosFromCaret(-1)
                    } else {
                        caret - 1
                    }
                    if (newCaretPos != caret) {
                        val newText = currentText.substring(0, newCaretPos) + currentText.substring(caret)
                        text.set(newText)
                        setCaret(newCaretPos)
                    }
                }
            }
            DELETE -> {
                if (currentText.isNotEmpty() && caret < currentText.length) {
                    val cutIndex = if (control && !alt && !shift) {
                        getNextWordPosFromCaret(+1)
                    } else {
                        caret + 1
                    }
                    if (cutIndex != caret) {
                        val newText = currentText.substring(0, caret) + currentText.substring(cutIndex)
                        text.set(newText)
                        // Don't update caret
                    }
                }
            }
            ENTER_ANDROID, ENTER_DESKTOP -> {
                if (canInputNewlines.getOrCompute() && shift && !alt && !control) {
                    text.set(currentText.substring(0, caret) + "\n" + currentText.substring(caret))
                    setCaret(caret + 1)
                } else {
                    enterPressedAction.invoke()
                }
            }
            else -> {
                if (character < 32.toChar()) return
                val charLimit = characterLimit.getOrCompute()
                if (!inputFilter.getOrCompute().invoke(character) || (charLimit > 0 && currentText.length >= charLimit))
                    return

                val newText = currentText.substring(0, caret) + character + currentText.substring(caret)
                text.set(newText)
                setCaret(caret + 1)

                resetCaretBlinkTimer()
            }
        }
    }

    protected open fun onKeyDown(keycode: Int) {
        // Focus is assumed.
        val control = Gdx.input.isControlDown()
        val alt = Gdx.input.isAltDown()
        val shift = Gdx.input.isShiftDown()
        when (keycode) {
            Input.Keys.LEFT -> {
                keymode = KeyMode.MOVE_LEFT
                keyRepeatTimer = KEY_REPEAT_INITIAL_TIME
                moveCaretFromKeypress()
            }
            Input.Keys.RIGHT -> {
                keymode = KeyMode.MOVE_RIGHT
                keyRepeatTimer = KEY_REPEAT_INITIAL_TIME
                moveCaretFromKeypress()
            }
            Input.Keys.HOME -> {
                keymode = KeyMode.NONE
                setCaret(0)
            }
            Input.Keys.END -> {
                keymode = KeyMode.NONE
                setCaret(Int.MAX_VALUE)
            }
            Input.Keys.ESCAPE -> {
                requestUnfocus()
            }
            Input.Keys.V -> {
                if (control && !shift && !alt) {
                    attemptPaste()
                }
            }
        }
    }
    
    fun attemptPaste() {
        val charLimit = characterLimit.getOrCompute()
        val currentText = text.getOrCompute()
        val caret = caretPos.getOrCompute()
        try {
            var data: String = Gdx.app.clipboard.contents?.replace("\r", "") ?: return
            if (!canInputNewlines.getOrCompute()) {
                data = data.replace("\n", "")
            }

            if (data.all(inputFilter.getOrCompute()) && canPasteText.getOrCompute()) {
                var pasteText = data
                val totalSize = pasteText.length + currentText.length
                if (charLimit > 0 && totalSize > charLimit) {
                    pasteText = pasteText.substring(0, charLimit - currentText.length)
                }

                val newText = currentText.substring(0, caret) + pasteText + currentText.substring(caret)
                text.set(newText)
                setCaret(caret + pasteText.length)
            }
        } catch (ignored: Exception) {
        }
        
    }

    protected open fun onKeyUp(keycode: Int) {
        // Focus is assumed.
        when (keycode) {
            Input.Keys.LEFT -> {
                if (keymode == KeyMode.MOVE_LEFT) keymode = KeyMode.NONE
            }
            Input.Keys.RIGHT -> {
                if (keymode == KeyMode.MOVE_RIGHT) keymode = KeyMode.NONE
            }
        }
    }

    protected open fun onClickPressed(event: ClickPressed) {
        requestFocus()
        resetCaretBlinkTimer()
        moveCaretFromMouse(event)
    }

    protected fun moveCaretFromMouse(event: MouseInputEvent) {
        val lastMouseInside: Vector2 = this.getPosRelativeToRoot(Vector2Stack.getAndPush())
        lastMouseInside.x = event.x - lastMouseInside.x
        lastMouseInside.y = event.y - lastMouseInside.y

        val xInContent = (lastMouseInside.x - contentZone.x.get())//.coerceIn(0f, contentZone.width.getOrCompute().coerceAtLeast(0f))
        val cursorX = xInContent + xOffset.get()
        // Calculate where the caret should go.
        val pos = characterPositions.getOrCompute().toList()
        if (pos.isEmpty()) {
            setCaret(0)
        } else {
            var index = 0
            for ((i, item) in pos.withIndex()) {
                if (item > cursorX) {
                    break
                }
                index = i
            }
            val targetCharWidth = pos[index]
            if (index + 1 < pos.size) {
                val nextWidth = pos[index + 1]
                val midWidth = MathUtils.lerp(targetCharWidth, nextWidth, 0.7f) // Not "true" middle, adjusted for UX
                val targetCaretIndex = if (cursorX < midWidth) index else (index + 1)
                setCaret(targetCaretIndex)
            } else {
                setCaret(index)
            }
        }

        Vector2Stack.pop()
        resetCaretBlinkTimer()
    }
    
    protected fun doKeyRepeatAction() {
        val keymode = this.keymode
        when (keymode) {
            KeyMode.MOVE_LEFT, KeyMode.MOVE_RIGHT -> {
                moveCaretFromKeypress()
            }
        }
    }

    protected fun moveCaretFromKeypress() {
        val keymode = this.keymode
        val wordJump = Gdx.input.isControlDown()
        val currentCaret = caretPos.getOrCompute()
        if (keymode == KeyMode.MOVE_LEFT) {
            if (wordJump) {
                setCaret(getNextWordPosFromCaret(-1))
            } else {
                setCaret(currentCaret - 1)
            }
            resetCaretBlinkTimer()
        } else if (keymode == KeyMode.MOVE_RIGHT) {
            if (wordJump) {
                setCaret(getNextWordPosFromCaret(+1))
            } else {
                setCaret(currentCaret + 1)
            }
            resetCaretBlinkTimer()
        }
    }
    
    protected fun getNextWordPosFromCaret(dir: Int): Int {
        val currentCaret = caretPos.getOrCompute()
        val currentText = text.getOrCompute()
        if (dir == 0) return currentCaret
        return (if (dir < 0) {
            currentText.substring(0, currentCaret).trimEnd().lastIndexOf(' ') + 1
        } else {
            val res = currentText.substring(currentCaret).indexOf(' ')
            if (res == -1) currentText.length else (currentCaret + res + 1)
        }).coerceIn(0, currentText.length)
    }

    override fun onFocusGained() {
        super.onFocusGained()
        (hasFocus as Var).set(true)
    }

    override fun onFocusLost() {
        super.onFocusLost()
        (hasFocus as Var).set(false)
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val delta = Gdx.graphics.deltaTime
        caretBlinkTimer += delta
        if (keymode != KeyMode.NONE && keyRepeatTimer > 0f) {
            keyRepeatTimer -= delta
            if (keyRepeatTimer <= 0f) {
                keyRepeatTimer = KEY_REPEAT_INTERVAL
                doKeyRepeatAction()
            }
        }

        super.renderSelf(originX, originY, batch)
    }

    override fun getDefaultSkinID(): String = TextField.TEXT_FIELD_SKIN_ID

    open class TextFieldSkin(element: TextField) : Skin<TextField>(element) {
        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val renderBounds = element.contentZone
            val rectX = renderBounds.x.get() + originX
            val rectY = originY - renderBounds.y.get()
//            val rectW = renderBounds.width.getOrCompute()
            val rectH = renderBounds.height.get()
            val lastPackedColor = batch.packedColor
            val opacity = element.apparentOpacity.get()

            val tmpColor = ColorStack.getAndPush()
            val layout: GlyphLayout = element.glyphLayout.getOrCompute()
            val paintboxFont = element.font.getOrCompute()
            var caretHeight = 1f
            val overallOffsetX = element.xOffset.get()
            val hasFocusNow = element.hasFocus.getOrCompute()
            val textColor = element.textColor.getOrCompute()
            paintboxFont.useFont { bitmapFont ->
                bitmapFont.scaleMul(element.textScale.get())
                tmpColor.set(textColor)
                tmpColor.a *= opacity
                batch.color = tmpColor
                bitmapFont.draw(batch, layout, rectX - overallOffsetX, rectY - (rectH - layout.height) / 2f)

                if (!hasFocusNow && element.text.getOrCompute().isEmpty()) {
                    tmpColor.set(textColor)
                    tmpColor.a *= opacity * 0.5f
                    bitmapFont.color = tmpColor
                    bitmapFont.draw(batch, element.emptyHintText.getOrCompute(), rectX /* no offset */, rectY - (rectH - layout.height) / 2f)
                    bitmapFont.setColor(1f, 1f, 1f, 1f)
                }

                caretHeight = bitmapFont.data.lineHeight
            }
            // Draw caret
            if (hasFocusNow) {
                if ((element.caretBlinkTimer % (CARET_BLINK_RATE * 2)) <= 0.5f) {
                    tmpColor.set(textColor)
                    tmpColor.a *= opacity
                    batch.color = tmpColor
                    val caretPos = element.caretPos.getOrCompute()
                    val charPos = element.characterPositions.getOrCompute()
                    val posX = if (caretPos in 0 until charPos.size) charPos[caretPos] else 0f
                    val caretWidth = element.caretWidth.get()
                    batch.fillRect(rectX - overallOffsetX + posX, rectY - (rectH + caretHeight) / 2f, caretWidth, caretHeight)
                }
            }

            ColorStack.pop()
            batch.packedColor = lastPackedColor
        }

        override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        }
    }
}
