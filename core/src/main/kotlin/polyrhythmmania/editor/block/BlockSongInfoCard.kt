package polyrhythmmania.editor.block

import com.badlogic.gdx.graphics.Color
import com.eclipsesource.json.JsonObject
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.contextmenu.*
import paintbox.ui.control.RadioButton
import paintbox.ui.control.TextField
import paintbox.ui.control.ToggleGroup
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.container.Container
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.input.EventSkillStar
import polyrhythmmania.ui.DecimalTextField
import polyrhythmmania.util.DecimalFormats
import polyrhythmmania.world.render.WorldRenderer
import java.util.*


class BlockSongInfoCard(engine: Engine) : Block(engine, BlockSongInfoCard.BLOCK_TYPES) {
    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
    }
    
    enum class Field(val jsonId: Int) {
        SONG_TITLE(0), SONG_ARTIST(1);
        companion object {
            val ID_MAPPING: Map<Int, Field> = values().associateBy { it.jsonId }
        }
    }
    
    var duration: Float = 4f
    var field: Field = Field.SONG_TITLE
    
    init {
        this.width = 1f
        val text = Localization.getVar("block.songInfoCard.name")
        this.defaultText.bind { text.use() }
    }

    override fun compileIntoEvents(): List<Event> {
        return listOf(EventSongInfoCard(engine, this.field, this.duration).also {
            it.beat = this.beat
        })
    }
    
    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(400f)
            
            ctxmenu.addMenuItem(SimpleMenuItem.create(Localization.getValue("blockContextMenu.songInfoCard.whichField"),
                    editor.editorPane.palette.markup).also {
                it.onAction = {
                    editor.attemptOpenLevelMetadataDialog()
                }
            })
            
            val fieldPane = Pane().apply { 
                this.bounds.height.set(32f)
            }
            val toggleGroup = ToggleGroup()
            fieldPane += RadioButton(Localization.getValue("levelMetadata.songName")).apply {
                this.textLabel.markup.set(editor.editorPane.palette.markup)
                toggleGroup.addToggle(this)
                this.selectedState.set(field == Field.SONG_TITLE)
                this.onSelected = {
                    field = Field.SONG_TITLE
                }
                
                Anchor.TopLeft.configure(this)
                this.bindWidthToParent(multiplier = 0.5f, adjust = -1f)
            }
            fieldPane += RadioButton(Localization.getValue("levelMetadata.songArtist")).apply {
                this.textLabel.markup.set(editor.editorPane.palette.markup)
                toggleGroup.addToggle(this)
                this.selectedState.set(field == Field.SONG_ARTIST)
                this.onSelected = {
                    field = Field.SONG_ARTIST
                }
                
                Anchor.TopRight.configure(this)
                this.bindWidthToParent(multiplier = 0.5f, adjust = -1f)
            }
            ctxmenu.addMenuItem(CustomMenuItem(fieldPane))
            
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.songInfoCard.duration"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(CustomMenuItem(
                    HBox().apply {
                        this.bounds.height.set(32f)
                        this.spacing.set(4f)

                        fun createTextField(): Pair<UIElement, TextField> {
                            val textField = DecimalTextField(startingValue = duration, decimalFormat = DecimalFormats["0.0##"],
                                    font = editor.editorPane.palette.musicDialogFont).apply {
                                this.allowNegatives.set(false)
                                this.textColor.set(Color(1f, 1f, 1f, 1f))

                                this.value.addListener {
                                    duration = it.getOrCompute()
                                }
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
        }
    }

    override fun copy(): BlockSongInfoCard {
        return BlockSongInfoCard(engine).also {
            this.copyBaseInfoTo(it)
            it.duration = this.duration
            it.field = this.field
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("duration", duration)
        obj.add("field", field.jsonId)
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        duration = obj.getFloat("duration", 4f).coerceAtLeast(0f)
        field = Field.ID_MAPPING[obj.getInt("field", 0)] ?: Field.SONG_TITLE
    }
}

class EventSongInfoCard(engine: Engine, val field: BlockSongInfoCard.Field, duration: Float,
                        val customText: String? = null)
    : Event(engine) {
    
    init {
        this.width = duration
    }
    
    private fun getCard(container: Container): WorldRenderer.SongInfoCard = when (field) {
        BlockSongInfoCard.Field.SONG_TITLE -> container.renderer.songTitleCard
        BlockSongInfoCard.Field.SONG_ARTIST -> container.renderer.songArtistCard
    }

    override fun onStartContainer(container: Container, currentBeat: Float) {
        val card = getCard(container)
        card.text = customText ?: when (field) {
            BlockSongInfoCard.Field.SONG_TITLE -> container.levelMetadata.songName
            BlockSongInfoCard.Field.SONG_ARTIST -> container.levelMetadata.songArtist
        }
        card.secondsStart = container.engine.seconds
        card.deployed = true
    }

    override fun onEndContainer(container: Container, currentBeat: Float) {
        val card = getCard(container)
        card.secondsStart = container.engine.seconds
        card.deployed = false
    }
}
