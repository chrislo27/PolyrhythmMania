package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.Markup
import paintbox.font.TextRun
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.container.LevelMetadata
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.ui.PRManiaSkins
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


class LevelMetadataDialog(editorPane: EditorPane) 
    : EditorDialog(editorPane) {
    
    data class Genre(val genreName: String, val customToString: ((Genre) -> String)? = null) {
        companion object {
            val NO_GENRE: Genre = Genre("") { Localization.getValue("editor.dialog.levelMetadata.noPresetGenre") }
            val DEFAULT_GENRES: List<Genre> = listOf(NO_GENRE) + listOf(
                    "Blues",
                    "Rock",
                    "Country",
                    "Dance",
                    "Disco",
                    "Funk",
                    "Jazz",
                    "Metal",
                    "Pop",
                    "Rap",
                    "Reggae",
                    "Techno",
                    "Ska",
                    "Eurobeat",
                    "Classical",
                    "Soul",
                    "Ethnic",
                    "Electronic",
                    "EDM",
                    "Rock 'n' Roll",
                    "Retro",
                    "Lo-Fi",
                    "J-Pop",
                    "K-Pop",
            ).sortedBy { it.lowercase(Locale.ROOT) }.map { Genre(it) }
        }

        override fun toString(): String {
            return customToString?.invoke(this) ?: genreName
        }
    }

    private val levelMetadata: Var<LevelMetadata> = Var(editor.container.levelMetadata)

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.levelMetadata.title").use() }

        bottomPane.addChild(Button("").apply {
            Anchor.BottomRight.configure(this)
            this.bindWidthToSelfHeight()
            this.applyDialogStyleBottom()
            this.setOnAction {
                attemptClose()
            }
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_linear")["x"])).apply {
                this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("common.close")))
        })

        val scrollPane: ScrollPane = ScrollPane().apply {
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(0f, 0f, 0f, 0f))
            this.vBar.unitIncrement.set(64f)
            this.vBar.blockIncrement.set(100f)
            this.vBar.skinID.set(PRManiaSkins.SCROLLBAR_SKIN)
        }
        contentPane.addChild(scrollPane)
        
        val vbox = VBox().apply {
            this.spacing.set(8f)
            this.margin.set(Insets(0f, 0f, 0f, 8f))
        }


        vbox.temporarilyDisableLayouts { 
            fun separator(): UIElement {
                return RectElement(Color(1f, 1f, 1f, 0.5f)).apply {
                    this.margin.set(Insets(4f, 4f, 0f, 0f))
                    this.bounds.height.set(10f)    
                }
            }
           
            vbox += TextLabel(binding = { Localization.getVar("editor.dialog.levelMetadata.information").use() }).apply {
                this.markup.set(editorPane.palette.markupInstantiatorDesc)
                this.bounds.height.set(100f)
                this.renderAlign.set(Align.topLeft)
                this.textColor.set(Color.WHITE)
                this.margin.set(Insets(4f))
                this.doLineWrapping.set(true)
            }
            vbox += separator()
            
            val textLabelWidth = 250f
            val labelHeight = 32f
            val focusGroup = FocusGroup()

            fun addInfoField(labelText: String, getter: (LevelMetadata) -> String): HBox {
                return HBox().apply {
                    this.bounds.height.set(labelHeight)
                    this.spacing.set(4f)
                    this += TextLabel(binding = { Localization.getVar(labelText).use() }, font = editorPane.main.mainFontBold).apply {
                        this.bounds.width.set(textLabelWidth)
                        this.renderAlign.set(Align.right)
                        this.textColor.set(Color.WHITE)
                        this.padding.set(Insets(0f, 0f, 0f, 4f))
                        this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.${labelText}.tooltip")))
                    }
                    this += TextLabel(binding = { getter(levelMetadata.use()) }).apply {
                        this.markup.set(editorPane.palette.markup)
                        this.bindWidthToParent(adjust = -textLabelWidth)
                        this.renderAlign.set(Align.left)
                        this.textColor.set(Color.WHITE)
                        this.padding.set(Insets(1f, 1f, 2f, 2f))
                    }
                }
            }
            
            val fieldLabelMarkup = Markup(mapOf(), TextRun(editorPane.main.mainFontBold, ""), Markup.FontStyles.ALL_USING_DEFAULT_FONT)
            fun addTextField(labelText: String, charLimit: Int, 
                             getter: (LevelMetadata) -> String,
                             allowNewlines: Boolean = false,
                             textFieldSizeAdjust: Float = 0f, textFieldSizeMultiplier: Float = 1f,
                             requiredField: Boolean = false,
                             copyFunc: (LevelMetadata, newText: String) -> LevelMetadata, ): Pair<HBox, TextField> {
                val textField = TextField(editorPane.palette.rodinDialogFont).apply {
                    focusGroup.addFocusable(this)
                    this.textColor.set(Color(1f, 1f, 1f, 1f))
                    this.canInputNewlines.set(allowNewlines)
                    this.characterLimit.set(charLimit)
                    this.text.set(getter(levelMetadata.getOrCompute()))
                    this.text.addListener { t ->
                        val newText = t.getOrCompute()
                        if (this.hasFocus.get()) {
                            levelMetadata.set(copyFunc(levelMetadata.getOrCompute(), newText))
                        }
                    }
                    this.setOnRightClick {
                        requestFocus()
                        text.set("")
                    }
                }
                val hbox = HBox().apply {
                    this.bounds.height.set(labelHeight)
                    this.spacing.set(4f)
                    this += TextLabel(binding = { "${if (requiredField) "[color=prmania_negative]* []" else ""}${Localization.getVar(labelText).use()}" }).apply {
                        this.bounds.width.set(textLabelWidth)
                        this.markup.set(fieldLabelMarkup)
                        this.renderAlign.set(Align.right)
                        this.textColor.set(Color.WHITE)
                        this.padding.set(Insets(0f, 0f, 0f, 4f))
                        this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.${labelText}.tooltip", Var { listOf(charLimit) })))
                    }
                    this += RectElement(Color.BLACK).apply {
                        this.bindWidthToParent(adjust = -textLabelWidth + textFieldSizeAdjust, multiplier = textFieldSizeMultiplier)
                        this.padding.set(Insets(1f, 1f, 2f, 2f))
                        this.border.set(Insets(1f))
                        this.borderStyle.set(SolidBorder(Color.WHITE))
                        this += textField
                    }
                }
                
                return hbox to textField
            }
            fun <T> addComboBox(labelText: String, list: List<T>,
                                getter: (LevelMetadata) -> T,
                                requiredField: Boolean = false,
                                copyFunc: (LevelMetadata, newValue: T) -> LevelMetadata, ): Pair<HBox, ComboBox<T>> {
                val combobox = ComboBox(list, getter(levelMetadata.getOrCompute()), editorPane.palette.musicDialogFont).apply {
                    this.bounds.width.set(250f)
                    this.selectedItem.addListener { item ->
                        levelMetadata.set(copyFunc(levelMetadata.getOrCompute(), item.getOrCompute()))
                    }
                }
                val hbox = HBox().apply {
                    this.bounds.height.set(labelHeight)
                    this.spacing.set(4f)
                    this += TextLabel(binding = { "${if (requiredField) "[color=prmania_negative]* []" else ""}${Localization.getVar(labelText).use()}" }).apply {
                        this.bounds.width.set(textLabelWidth)
                        this.markup.set(fieldLabelMarkup)
                        this.renderAlign.set(Align.right)
                        this.textColor.set(Color.WHITE)
                        this.padding.set(Insets(0f, 0f, 0f, 4f))
                        this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.${labelText}.tooltip")))
                    }
                    this += combobox
                }
                
                return hbox to combobox
            }

            fun addYearField(hbox: HBox, labelText: String, getter: (LevelMetadata) -> Int) {
                hbox += TextLabel(binding = { Localization.getVar(labelText).use() }, font = editorPane.main.mainFontBold).apply {
                    this.padding.set(Insets(0f, 0f, 4f, 4f))
                    this.margin.set(Insets(0f, 0f, 10f, 4f))
                    this.resizeBoundsToContent(affectWidth = true, affectHeight = false)
                    this.renderAlign.set(Align.right)
                    this.textColor.set(Color.WHITE)
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.${labelText}.tooltip")))
                }
                hbox += RectElement(Color.BLACK).apply {
                    this.bounds.width.set(75f)
                    this.padding.set(Insets(1f, 1f, 2f, 2f))
                    this.border.set(Insets(1f))
                    this.borderStyle.set(SolidBorder(Color.WHITE))
                    this += TextField(editorPane.palette.rodinDialogFont).apply {
                        focusGroup.addFocusable(this)
                        this.textColor.set(Color(1f, 1f, 1f, 1f))
//                            this.characterLimit.set(LevelMetadata.LIMIT_YEAR.last.toString().length)
                        this.characterLimit.set(4) // XXXX
                        this.inputFilter.set {
                            it in '0'..'9'
                        }
                        this.text.set(getter(levelMetadata.getOrCompute()).takeUnless { it == 0 }?.toString() ?: "")
                        this.text.addListener { t ->
                            val newText = t.getOrCompute()
                            if (this.hasFocus.get()) {
                                val newYear: Int = newText.toIntOrNull()?.takeIf { it in LevelMetadata.LIMIT_YEAR }
                                        ?: 0
                                levelMetadata.set(levelMetadata.getOrCompute().copy(albumYear = newYear))
                                this.text.set(newYear.takeUnless { it == 0 }?.toString() ?: "")
                            }
                        }
                        this.setOnRightClick {
                            requestFocus()
                            text.set("")
                        }
                    }
                }
            }

            vbox += addInfoField("levelMetadata.initialCreationDate") { 
                val datetime = it.initialCreationDate.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault())
                DateTimeFormatter.RFC_1123_DATE_TIME.format(datetime)
            }
            vbox += addTextField("levelMetadata.levelCreator", LevelMetadata.LIMIT_LEVEL_CREATOR,
                    LevelMetadata::levelCreator, textFieldSizeMultiplier = 0.7f, requiredField = true) { t, newText ->
                t.copy(levelCreator = newText)
            }.first
            vbox += addTextField("levelMetadata.description", LevelMetadata.LIMIT_DESCRIPTION,
                    LevelMetadata::description, allowNewlines = true) { t, newText ->
                t.copy(description = newText)
            }.first
            vbox += addTextField("levelMetadata.songName", LevelMetadata.LIMIT_SONG_NAME,
                    LevelMetadata::songName, textFieldSizeMultiplier = 0.6f, requiredField = true) { t, newText ->
                t.copy(songName = newText)
            }.first
            vbox += addTextField("levelMetadata.songArtist", LevelMetadata.LIMIT_ARTIST_NAME,
                    LevelMetadata::songArtist, textFieldSizeMultiplier = 0.6f, requiredField = true) { t, newText ->
                t.copy(songArtist = newText)
            }.first
            vbox += addTextField("levelMetadata.albumName", LevelMetadata.LIMIT_ALBUM_NAME,
                    LevelMetadata::albumName, textFieldSizeMultiplier = 0.6f) { t, newText ->
                t.copy(albumName = newText)
            }.first.also { hbox ->
                addYearField(hbox, "levelMetadata.albumYear", LevelMetadata::albumYear)
            }
            vbox += addTextField("levelMetadata.genre", LevelMetadata.LIMIT_GENRE, LevelMetadata::genre,
                    textFieldSizeAdjust = -600f) { t, newText ->
                t.copy(genre = newText)
            }.also { (hbox, textField) ->
                hbox += Pane().apply { 
                    this.bounds.width.set(16f)
                }
                hbox += TextLabel(binding = { Localization.getVar("editor.dialog.levelMetadata.genrePreset").use() }, font = editorPane.palette.musicDialogFont).apply {
                    this.bounds.width.set(200f)
                    this.renderAlign.set(Align.right)
                    this.textColor.set(Color.WHITE)
                    this.padding.set(Insets(0f, 0f, 0f, 4f))
                }
                val identicalGenre: Genre? = Genre.DEFAULT_GENRES.find { it.genreName == textField.text.getOrCompute() }
                hbox += ComboBox<Genre>(Genre.DEFAULT_GENRES, identicalGenre ?: Genre.DEFAULT_GENRES.first(),
                        font = editorPane.palette.musicDialogFont).apply { 
                    this.bounds.width.set(250f)
                    this.selectedItem.addListener {
                        val genreName = it.getOrCompute().genreName
                        if (genreName.isNotEmpty()) {
                            textField.requestFocus()
                            textField.text.set(genreName)
                            textField.requestUnfocus()
                        }
                    }
                }
            }.first
            vbox += addComboBox("levelMetadata.difficulty", LevelMetadata.LIMIT_DIFFICULTY.toList(),
                    LevelMetadata::difficulty) { lm, newValue ->
                lm.copy(difficulty = newValue)
            }.also { (hbox, combobox) ->
                combobox.itemStringConverter.set { 
                    if (it <= 0) Localization.getValue("editor.dialog.levelMetadata.noDifficulty") else "$it"
                }
                combobox.bounds.width.set(150f)
                hbox += TextLabel(text = " / 10", font = editorPane.palette.musicDialogFont).apply {
                    this.bounds.width.set(100f)
                    this.renderAlign.set(Align.left)
                    this.textColor.set(Color.WHITE)
                }
            }.first
            
        }


        vbox.sizeHeightToChildren(300f)
        scrollPane.setContent(vbox)
    }
    
    init {
        this.levelMetadata.addListener {
            editor.container.levelMetadata = it.getOrCompute()
        }
    }

    override fun canCloseDialog(): Boolean {
        return true
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
    }
}