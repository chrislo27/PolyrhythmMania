package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.PaintboxGame
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.PaintboxFont
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.Tooltip
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.editor.CameraPanningSetting
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.screen.mainmenu.menu.StandardMenu
import polyrhythmmania.ui.ColourPicker
import kotlin.math.max


class TilesetEditDialog(editorPane: EditorPane) : EditorDialog(editorPane) {

    val settings: Settings = editorPane.main.settings

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.settings.title").use() }

        bottomPane.addChild(Button("").apply {
            Anchor.BottomRight.configure(this)
            this.bounds.width.bind { bounds.height.useF() }
            this.applyDialogStyleBottom()
            this.setOnAction {
                editorPane.closeDialog()
                settings.persist()
                Paintbox.LOGGER.info("Settings persisted (editor)")
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
        }
        contentPane.addChild(scrollPane)

        val pane = Pane()
        val vbox = VBox().apply {
            this.spacing.set(8f)
        }
        pane += vbox

        vbox.temporarilyDisableLayouts {
            vbox += ColourPicker(true, font = editorPane.palette.musicDialogFont).apply { 
                this.bounds.width.set(300f)
            }
            vbox += ColourPicker(false, font = editorPane.palette.musicDialogFont).apply { 
                this.bounds.width.set(300f)
            }
        }
        vbox.sizeHeightToChildren(300f)

        pane.bounds.height.bind {
            max(vbox.bounds.height.useF(), pane.parent.use()?.bounds?.height?.useF() ?: 300f)
        }
        scrollPane.setContent(pane)

//        val hbox = HBox().apply {
//            Anchor.BottomCentre.configure(this)
//            this.align.set(HBox.Align.CENTRE)
//            this.spacing.set(16f)
//            this.bounds.width.set(700f)
//        }
//        bottomPane.addChild(hbox)
    }

    override fun canCloseDialog(): Boolean {
        return true
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
        settings.persist()
        Paintbox.LOGGER.info("Settings persisted (editor)")
    }
}