package polyrhythmmania.editor.block

import com.badlogic.gdx.graphics.Color
import com.eclipsesource.json.JsonObject
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.font.Markup
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.CustomMenuItem
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import paintbox.ui.control.DecimalTextField
import paintbox.ui.control.FocusGroup
import paintbox.ui.control.TextField
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.util.DecimalFormats
import polyrhythmmania.Localization
import polyrhythmmania.container.Container
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.render.WorldRendererWithUI
import java.text.DecimalFormat
import java.util.*


class BlockSubtitle(engine: Engine) : Block(engine, BlockSubtitle.BLOCK_TYPES) {

    companion object {

        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)

        private const val DEFAULT_DURATION: Float = 4f
        private const val TEXT_PREVIEW_CHAR_LIMIT: Int = 20
        private const val TEXT_PREVIEW_ELLIPSIS: Char = '…'

        private val beatDecimalFormat: DecimalFormat = DecimalFormats["0.0##"]
    }

    val duration: FloatVar = FloatVar(DEFAULT_DURATION)
    val text: Var<String> = Var("")

    init {
        this.width = 2f

        val blockName = Localization.getVar("block.subtitle.name")
        this.defaultText.bind { blockName.use() }
        this.defaultTextSecondLine.bind {
            val trimmed = text.use().trim()
            val truncated = if (trimmed.length <= TEXT_PREVIEW_CHAR_LIMIT)
                trimmed
            else "${trimmed.take(TEXT_PREVIEW_CHAR_LIMIT)}${TEXT_PREVIEW_ELLIPSIS}"
            "[font=rodin scalex=0.75]${Markup.escape(truncated)}[]"
        }
    }

    override fun compileIntoEvents(): List<Event> {
        return listOf(
            EventSubtitle(engine, this.duration.get(), this.text.getOrCompute()).also {
                it.beat = this.beat
            }
        )
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(450f)

            val focusGroup = FocusGroup()

            ctxmenu.addMenuItem(
                LabelMenuItem.create(
                    Localization.getValue("blockContextMenu.subtitle.duration"),
                    editor.editorPane.palette.markup
                )
            )
            ctxmenu.addMenuItem(CustomMenuItem(
                HBox().apply {
                    this.bounds.height.set(32f)
                    this.spacing.set(4f)

                    fun createTextField(): Pair<UIElement, TextField> {
                        val textField = DecimalTextField(
                            startingValue = duration.get(), decimalFormat = beatDecimalFormat,
                            font = editor.editorPane.palette.musicDialogFont
                        ).apply {
                            this.minimumValue.set(0f)
                            this.textColor.set(Color(1f, 1f, 1f, 1f))

                            this.value.addListener {
                                duration.set(it.getOrCompute())
                            }
                            focusGroup.addFocusable(this)
                        }

                        return RectElement(Color(0f, 0f, 0f, 1f)).apply {
                            this.bindWidthToParent(adjust = -4f, multiplier = 0.333f)
                            this.border.set(Insets(1f))
                            this.borderStyle.set(SolidBorder(Color.WHITE))
                            this.padding.set(Insets(2f))
                            this += textField
                        } to textField
                    }

                    this += HBox().apply {
                        this.spacing.set(4f)
                        this += createTextField().first
                    }
                }
            ))
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(
                LabelMenuItem.create(
                    Localization.getValue("blockContextMenu.subtitle.text"),
                    editor.editorPane.palette.markup
                )
            )
            ctxmenu.addMenuItem(CustomMenuItem(
                HBox().apply {
                    this.bounds.height.set(32f)
                    this.spacing.set(4f)
                    this += RectElement(Color(0f, 0f, 0f, 1f)).apply {
                        this.border.set(Insets(1f))
                        this.borderStyle.set(SolidBorder(Color.WHITE))
                        this.padding.set(Insets(2f))
                        this += TextField(font = editor.main.fontEditorRodin).apply {
                            this.textColor.set(Color(1f, 1f, 1f, 1f))
                            this.text.set(this@BlockSubtitle.text.getOrCompute())
                            this.canInputNewlines.set(true)
                            this.text.addListener { t ->
                                if (hasFocus.get()) {
                                    this@BlockSubtitle.text.set(t.getOrCompute())
                                }
                            }
                            hasFocus.addListener { f ->
                                if (!f.getOrCompute()) {
                                    this.text.set(this@BlockSubtitle.text.getOrCompute())
                                }
                            }
                            this.setOnRightClick {
                                requestFocus()
                                text.set("")
                            }
                            focusGroup.addFocusable(this)
                        }
                    }
                }
            ))
        }
    }

    override fun copy(): BlockSubtitle {
        return BlockSubtitle(engine).also {
            this.copyBaseInfoTo(it)
            it.duration.set(this.duration.get())
            it.text.set(this.text.getOrCompute())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("duration", duration.get())
        obj.add("text", text.getOrCompute())
    }

    override fun readFromJson(obj: JsonObject, editorFlags: EnumSet<EditorSpecialFlags>) {
        super.readFromJson(obj, editorFlags)
        duration.set(obj.getFloat("duration", DEFAULT_DURATION).coerceAtLeast(0f))
        text.set(obj.getString("text", ""))
    }
}

class EventSubtitle(
    engine: Engine,
    duration: Float,
    val text: String,
) : Event(engine) {

    init {
        this.width = duration
    }

    override fun onStartContainer(container: Container, currentBeat: Float) {
        val secondsEnd = engine.tempos.beatsToSeconds(this.beat + this.width, disregardSwing = true)
        val subtitlesRendering = container.renderer.subtitlesRendering

        subtitlesRendering.setCurrentSubtitle(WorldRendererWithUI.CurrentSubtitle(text, secondsEnd))
    }
}
