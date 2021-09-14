package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
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


class LevelMetadataDialog(editorPane: EditorPane) 
    : EditorDialog(editorPane) {

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

            fun addInfoField(labelText: String, getter: (LevelMetadata) -> String): HBox {
                return HBox().apply {
                    this.bounds.height.set(labelHeight)
                    this.spacing.set(0f)
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
            
            fun addTextField(labelText: String, charLimit: Int, 
                             getter: (LevelMetadata) -> String,
                             allowNewlines: Boolean = false,
                             copyFunc: (LevelMetadata, newText: String) -> LevelMetadata, ): HBox {
                return HBox().apply {
                    this.bounds.height.set(labelHeight)
                    this.spacing.set(0f)
                    this += TextLabel(binding = { Localization.getVar(labelText).use() }, font = editorPane.main.mainFontBold).apply {
                        this.bounds.width.set(textLabelWidth)
                        this.renderAlign.set(Align.right)
                        this.textColor.set(Color.WHITE)
                        this.padding.set(Insets(0f, 0f, 0f, 4f))
                        this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.${labelText}.tooltip", Var { listOf(charLimit) })))
                    }
                    this += RectElement(Color.BLACK).apply {
                        this.bindWidthToParent(adjust = -textLabelWidth)
                        this.padding.set(Insets(1f, 1f, 2f, 2f))
                        this.border.set(Insets(1f))
                        this.borderStyle.set(SolidBorder(Color.WHITE))
                        this += TextField(editorPane.palette.rodinDialogFont).apply {
                            this.textColor.set(Color.WHITE)
                            this.canInputNewlines.set(allowNewlines)
                            this.characterLimit.set(charLimit)
                            this.text.set(getter(levelMetadata.getOrCompute()))
                            this.text.addListener { t ->
                                val newText = t.getOrCompute()
                                if (this.hasFocus.getOrCompute()) {
                                    levelMetadata.set(copyFunc(levelMetadata.getOrCompute(), newText))
                                }
                            }
                            this.setOnRightClick {
                                text.set("")
                                requestFocus()
                            }
                        }
                    }
                }
            }
            fun addYearField(labelText: String, 
                             getter: (LevelMetadata) -> Int,
                             copyFunc: (LevelMetadata, newText: String) -> LevelMetadata): HBox {
                return HBox().apply {
                    this.bounds.height.set(labelHeight)
                    this.spacing.set(0f)
                    this += TextLabel(binding = { Localization.getVar(labelText).use() }, font = editorPane.main.mainFontBold).apply {
                        this.bounds.width.set(textLabelWidth)
                        this.renderAlign.set(Align.right)
                        this.textColor.set(Color.WHITE)
                        this.padding.set(Insets(0f, 0f, 0f, 4f))
                        this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.${labelText}.tooltip")))
                    }
                    this += RectElement(Color.BLACK).apply {
                        this.bounds.width.set(75f)
                        this.padding.set(Insets(1f, 1f, 2f, 2f))
                        this.border.set(Insets(1f))
                        this.borderStyle.set(SolidBorder(Color.WHITE))
                        this += TextField(editorPane.palette.rodinDialogFont).apply {
                            this.textColor.set(Color.WHITE)
//                            this.characterLimit.set(LevelMetadata.LIMIT_YEAR.last.toString().length)
                            this.characterLimit.set(4) // XXXX
                            this.inputFilter.set {
                                it in '0'..'9'
                            }
                            this.text.set(getter(levelMetadata.getOrCompute()).takeUnless { it == 0 }?.toString() ?: "")
                            this.text.addListener { t ->
                                val newText = t.getOrCompute()
                                if (this.hasFocus.getOrCompute()) {
                                    val newYear: Int = newText.toIntOrNull()?.takeIf { it in LevelMetadata.LIMIT_YEAR } ?: 0
                                    levelMetadata.set(levelMetadata.getOrCompute().copy(albumYear = newYear))
                                    this.text.set(newYear.takeUnless { it == 0 }?.toString() ?: "")
                                }
                            }
                            this.setOnRightClick {
                                text.set("")
                                requestFocus()
                            }
                        }
                    }
                }
            }

            vbox += addInfoField("levelMetadata.initialCreationDate") { 
                val datetime = it.initialCreationDate.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault())
                DateTimeFormatter.RFC_1123_DATE_TIME.format(datetime)
            }
            vbox += addTextField("levelMetadata.levelCreator", LevelMetadata.LIMIT_LEVEL_CREATOR, { it.levelCreator }) { t, newText ->
                t.copy(levelCreator = newText)
            }
            vbox += addTextField("levelMetadata.description", LevelMetadata.LIMIT_DESCRIPTION, { it.description },
                    allowNewlines = true) { t, newText ->
                t.copy(description = newText)
            }
            vbox += addTextField("levelMetadata.songName", LevelMetadata.LIMIT_SONG_NAME, { it.songName }) { t, newText ->
                t.copy(songName = newText)
            }
            vbox += addTextField("levelMetadata.songArtist", LevelMetadata.LIMIT_ARTIST_NAME, { it.songArtist }) { t, newText ->
                t.copy(songArtist = newText)
            }
            vbox += addTextField("levelMetadata.albumName", LevelMetadata.LIMIT_ALBUM_NAME, { it.albumName }) { t, newText ->
                t.copy(albumName = newText)
            }
            vbox += addYearField("levelMetadata.albumYear", { it.albumYear }) { t, newText ->
                val newYear: Int? = newText.toIntOrNull()?.takeIf { it in LevelMetadata.LIMIT_YEAR }
                t.copy(albumYear = newYear ?: 0) 
            }
            // TODO add combo box dropdown for common genres
            vbox += addTextField("levelMetadata.genre", LevelMetadata.LIMIT_GENRE, { it.genre }) { t, newText ->
                t.copy(genre = newText)
            }
            
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