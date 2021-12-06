package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.ImageRenderingMode
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.Button
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.TinyFDWrapper
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.PreferenceKeys
import polyrhythmmania.container.Container
import polyrhythmmania.editor.pane.EditorPane
import java.io.File
import kotlin.concurrent.thread


class BannerDialog(editorPane: EditorPane, val afterDialogClosed: () -> Unit) : EditorDialog(editorPane) {

    enum class Substate {
        SUMMARY,
        FILE_DIALOG_OPEN,
        LOAD_ERROR,
    }

    val substate: Var<Substate> = Var(Substate.SUMMARY)

    val descLabel: TextLabel
    
    private val customTexture: Var<Texture?> = editor.container.bannerTexture

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.banner.title").use() }
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
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("common.back")))
            this.disabled.bind {
                val ss = substate.use()
                ss == Substate.FILE_DIALOG_OPEN
            }
        })
        descLabel = TextLabel("").apply {
            this.markup.set(editorPane.palette.markup)
            this.textColor.set(Color.WHITE.cpy())
            this.renderAlign.set(Align.center)
            this.textAlign.set(TextAlign.CENTRE)
            this.visible.bind { 
                val ss = substate.use()
                ss == Substate.FILE_DIALOG_OPEN || ss == Substate.LOAD_ERROR
            }
        }
        contentPane.addChild(descLabel)

        val vbox = VBox().apply { 
            this.spacing.set(4f)
            this.visible.bind { 
                !descLabel.visible.use()
            }
        }
        contentPane += vbox
        
        vbox += RectElement(Color().grey(0.8f, 1f)).apply {
            Anchor.TopCentre.configure(this)
            val border = Insets(4f)
            this.border.set(border)
            this.borderStyle.set(SolidBorder(Color.WHITE).apply {
                this.roundedCorners.set(true)
            })
            this.bounds.height.set(160f + border.top + border.bottom)
            this.bounds.width.bind { 
                (bounds.height.use() - border.top - border.bottom) * 3.2f + (border.left + border.right)
            }
            this += ImageNode(binding = {
                val tex: Texture = customTexture.use() ?: AssetRegistry["library_default_banner"]
                TextureRegion(tex)
            }, renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO)
        }
        vbox += TextLabel(binding = {
            Localization.getVar("editor.dialog.banner.specs", Var {
                listOf(Container.MIN_BANNER_SIZE.toString(), Container.MAX_BANNER_SIZE.toString())
            }).use()
        }).apply {
            this.markup.set(editorPane.palette.markup)
            this.bounds.height.set(260f)
            this.renderAlign.set(Align.center)
            this.textColor.set(Color.WHITE.cpy())
            
            val tb = this.internalTextBlock.getOrCompute()
            tb.computeLayouts()
        }
        
        val hbox = HBox().apply {
            Anchor.TopCentre.configure(this)
            this.bounds.width.set(900f)
            this.align.set(HBox.Align.CENTRE)
            this.spacing.set(16f)
            this.visible.bind { substate.use() == Substate.SUMMARY }
        }
        bottomPane += hbox
        hbox += Button(binding = { Localization.getVar("editor.dialog.banner.selectImage").use() }, font = editorPane.palette.musicDialogFont).apply { 
            this.bounds.width.set(400f)
            this.applyDialogStyleBottom()
            this.setOnAction { 
                showFileDialog(main.attemptRememberDirectory(PreferenceKeys.FILE_CHOOSER_BANNER_IMAGE) ?: main.getDefaultDirectory())
            }
        }
        hbox += Button(binding = { Localization.getVar("editor.dialog.banner.removeImage").use() }, font = editorPane.palette.musicDialogFont).apply { 
            this.bounds.width.set(400f)
            this.applyDialogStyleBottom()
            this.setOnAction { 
                unloadCustomTexture()
            }
            this.disabled.bind { customTexture.use() == null }
        }
        
        bottomPane += Button(binding = { Localization.getVar("common.ok").use() }, font = editorPane.palette.musicDialogFont).apply { 
            Anchor.TopCentre.configure(this)
            this.bounds.width.set(300f)
            this.applyDialogStyleBottom()
            this.setOnAction {
                substate.set(Substate.SUMMARY)
            }
            this.visible.bind { substate.use() == Substate.LOAD_ERROR }
        }
        
    }
    
    private fun showFileDialog(dir: File) {
        descLabel.text.set(Localization.getValue("common.closeFileChooser"))
        substate.set(Substate.FILE_DIALOG_OPEN)
        editorPane.main.restoreForExternalDialog { completionCallback ->
            thread(isDaemon = true) {
                val title = Localization.getValue("fileChooser.bannerImage.title")
                val filter = TinyFDWrapper.FileExtFilter(Localization.getValue("fileChooser.bannerImage.filter"),
                        listOf("*.png")).copyWithExtensionsInDesc()
                TinyFDWrapper.openFile(title, dir, filter) { file: File? ->
                    completionCallback()
                    if (file != null) {
                        Gdx.app.postRunnable {
                            main.persistDirectory(PreferenceKeys.FILE_CHOOSER_BANNER_IMAGE, file.parentFile
                                    ?: main.getDefaultDirectory())
                            try {
                                val tex = Texture(FileHandle(file))
                                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                                
                                if (!Container.isBannerTextureWithinSize(tex)) {
                                    tex.disposeQuietly()
                                    descLabel.text.set(Localization.getValue("editor.dialog.banner.wrongDimensions", "${tex.width}x${tex.height}", Container.MIN_BANNER_SIZE.toString(), Container.MAX_BANNER_SIZE.toString()))
                                    substate.set(Substate.LOAD_ERROR)
                                } else {
                                    unloadCustomTexture()
                                    customTexture.set(tex)
                                    substate.set(Substate.SUMMARY)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                descLabel.text.set(Localization.getValue("editor.dialog.banner.loadError", e.javaClass.name))
                                substate.set(Substate.LOAD_ERROR)
                            }
                        }
                    } else {
                        Gdx.app.postRunnable {
                            substate.set(Substate.SUMMARY)
                        }
                    }
                }
            }
        }
    }
    
    private fun unloadCustomTexture() {
        val ct = this.customTexture.getOrCompute()
        if (ct != null) {
            this.customTexture.set(null)
            ct.disposeQuietly()
        }
    }

    override fun afterDialogClosed() {
        this.afterDialogClosed.invoke()
    }

    override fun canCloseDialog(): Boolean {
        val substate = substate.getOrCompute()
        if (substate == Substate.FILE_DIALOG_OPEN)
            return false
        
        return true
    }

}
