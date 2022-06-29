package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import net.lingala.zip4j.ZipFile
import paintbox.binding.IntVar
import paintbox.binding.Var
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.Button
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.Localization
import polyrhythmmania.container.Container
import polyrhythmmania.container.TexturePackSource
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.util.TempFileUtils
import polyrhythmmania.world.tileset.CustomTexturePack
import polyrhythmmania.world.tileset.StockTexturePacks
import java.util.zip.ZipOutputStream


class ManageCustomTexPackDialog(
        editorPane: EditorPane, 
) : EditorDialog(editorPane) {
    
    private sealed class State {
        open class None : State()
        class TmpMsg(val msg: String, var timerSec: Float) : None()
        
        class SwapWith(val fromID: Int) : State()
        class CopyTo(val fromID: Int) : State()
    }
    
    private val currentState: Var<State> = Var(State.None())
    private val container: Container get() = editor.container

    init {
        this.titleLabel.text.bind {
            Localization.getVar("editor.dialog.manageTexPacks.title").use()
        }

        bottomPane.addChild(Button("").apply {
            Anchor.BottomRight.configure(this)
            this.bindWidthToSelfHeight()
            this.applyDialogStyleBottom()
            this.disabled.bind { currentState.use() !is State.None }
            this.setOnAction {
                attemptClose()
            }
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_linear")["x"])).apply {
                this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("common.close")))
        })
        
        contentPane += VBox().apply { 
            this.spacing.set(12f)
            this.temporarilyDisableLayouts { 
                val descLabel = TextLabel(binding = { 
                    when (val s = currentState.use()) {
                        is State.TmpMsg -> s.msg
                        is State.None -> Localization.getVar("editor.dialog.manageTexPacks.desc.selectPack").use()
                        is State.SwapWith -> Localization.getVar("editor.dialog.manageTexPacks.desc.swapWith", listOf(s.fromID)).use()
                        is State.CopyTo -> Localization.getVar("editor.dialog.manageTexPacks.desc.copyTo", listOf(s.fromID)).use()
                    }
                }).apply {
                    this.markup.set(editorPane.palette.markup)
                    this.textColor.set(Color.WHITE.cpy())
                    this.renderAlign.set(Align.center)
                    this.bounds.height.set(48f)
                }
                this += descLabel
                
                TexturePackSource.CUSTOM_RANGE.forEach { id ->
                    this += HBox().apply {
                        Anchor.TopCentre.configure(this)    
                        this.spacing.set(6f)
                        this.bounds.height.set(44f)
                        this.align.set(HBox.Align.CENTRE)
                        this.temporarilyDisableLayouts { 
                            this += TextLabel("").apply {
                                this.markup.set(editorPane.palette.markup)
                                this.textColor.set(Color.WHITE.cpy())
                                this.renderAlign.set(Align.center)
                                
                                this.bounds.width.set(450f)
                                
                                this.borderStyle.set(SolidBorder(Color.LIGHT_GRAY).apply { 
                                    this.roundedCorners.set(true)
                                })
                                this.border.set(Insets(2f))
                                
                                val thisPackVar: Var<CustomTexturePack?> = container.customTexturePacks[id - 1]
                                val regionCount = IntVar { thisPackVar.use()?.getAllTilesetRegions()?.size ?: 0 }
                                this.text.bind {
                                    var str = Localization.getVar("editor.dialog.manageTexPacks.packNumber", Var {
                                        val fallbackID: String = thisPackVar.use()?.fallbackID?.use() ?: StockTexturePacks.gba.id
                                        listOf(id, regionCount.use(), Localization.getVar("editor.dialog.texturePack.stock.${fallbackID}").use())
                                    }).use()

                                    if (thisPackVar.use()?.isEmpty() == false) {
                                        str = "[font=rodin color=CYAN]★[] $str [font=rodin color=CLEAR]★[]" // Clear star is so the centering looks good
                                    }

                                    str
                                }
                            }
                            val mainActionButton = Button(binding = {
                                when (val s = currentState.use()) {
                                    is State.None -> Localization.getVar("editor.dialog.manageTexPacks.action.edit")
                                    is State.SwapWith -> {
                                        if (s.fromID == id)
                                            Localization.getVar("common.cancel")
                                        else Localization.getVar("editor.dialog.manageTexPacks.action.swap")
                                    }
                                    is State.CopyTo -> {
                                        if (s.fromID == id) 
                                            Localization.getVar("common.cancel")
                                        else Localization.getVar("editor.dialog.manageTexPacks.action.copy")
                                    }
                                }.use()
                            }, font = editorPane.palette.musicDialogFontBold).apply {
                                this.bounds.width.set(200f)
                                this.applyDialogStyleContent()
                                this.font.bind {
                                    val s = currentState.use()
                                    if ((s is State.SwapWith && s.fromID == id) || (s is State.CopyTo && s.fromID == id)) {
                                        main.mainFontItalic
                                    } else editorPane.palette.musicDialogFontBold
                                }
                                
                                this.setOnAction { 
                                    val defaultTimerSec = 5f
                                    
                                    when (val s = currentState.getOrCompute()) {
                                        is State.None -> editor.attemptOpenTexturePackEditDialog(id - 1)
                                        is State.SwapWith -> {
                                            if (s.fromID == id) {
                                                // Cancel since this is the same "from"
                                                currentState.set(State.None())
                                            } else {
                                                val ctr = container
                                                val thisTp = ctr.customTexturePacks[s.fromID - 1]
                                                val targetTp = ctr.customTexturePacks[id - 1]
                                                val tmp: CustomTexturePack? = thisTp.getOrCompute()
                                                thisTp.set(targetTp.getOrCompute())
                                                targetTp.set(tmp)
                                                currentState.set(State.TmpMsg(Localization.getValue("editor.dialog.manageTexPacks.action.swap.complete", s.fromID, id), defaultTimerSec))
                                                syncThisCustomPackWithContainer()
                                            }
                                        }
                                        is State.CopyTo -> {
                                            if (s.fromID == id) {
                                                // Cancel since this is the same "from"
                                                currentState.set(State.None())
                                            } else {
                                                val ctr = container
                                                val fromTp = ctr.customTexturePacks[s.fromID - 1]
                                                val targetTp = ctr.customTexturePacks[id - 1]

                                                val finalState = State.TmpMsg(Localization.getValue("editor.dialog.manageTexPacks.action.copy.complete", s.fromID, id), defaultTimerSec)
                                                val fromPack = fromTp.getOrCompute()
                                                val oldPack = targetTp.getOrCompute()
                                                val oldTextures = oldPack?.getAllUniqueTextures() ?: emptyList()
                                                if (fromPack == null) {
                                                    Gdx.app.postRunnable {
                                                        targetTp.set(null)
                                                        syncThisCustomPackWithContainer()
                                                        oldTextures.forEach { it.disposeQuietly() }

                                                        currentState.set(finalState)
                                                    }
                                                } else {
                                                    // Copy the pack by writing it and reading it back. 
                                                    // This ensures the textures are copied and loaded separately and are not shared
                                                    val tmpFile = TempFileUtils.createTempFile("texturepack-copy").apply { 
                                                        deleteOnExit()
                                                    }
                                                    
                                                    tmpFile.outputStream().use { fos ->
                                                        ZipOutputStream(fos).use { zip ->
                                                            fromPack.writeToOutputStream(zip)
                                                        }
                                                    }
                                                    
                                                    val readResult: CustomTexturePack.ReadResult = ZipFile(tmpFile).use { zf ->
                                                        CustomTexturePack.readFromStream(zf)
                                                    }
                                                    
                                                    Gdx.app.postRunnable {
                                                        // Load textures on GL thread
                                                        val newPack = readResult.createAndLoadTextures()

                                                        targetTp.set(newPack)
                                                        syncThisCustomPackWithContainer()
                                                        oldTextures.forEach { it.disposeQuietly() }
                                                        
                                                        currentState.set(finalState)
                                                        tmpFile.delete()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            this += mainActionButton
                            this += Button("").apply {
                                this.bindWidthToSelfHeight()
                                this.applyDialogStyleContent()
                                this.disabled.bind { currentState.use() !is State.None }
                                
                                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["refresh"])).apply { 
                                    this.rotation.set(90f)
                                }
                                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.manageTexPacks.action.swap.tooltip")))
                                
                                this.setOnAction { 
                                    currentState.set(State.SwapWith(id))
                                }
                            }
                            this += Button("").apply {
                                this.bindWidthToSelfHeight()
                                this.applyDialogStyleContent()
                                this.disabled.bind { currentState.use() !is State.None }
                                
                                this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_colour_picker_copy")))
                                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.manageTexPacks.action.copy.tooltip")))
                                
                                this.setOnAction {
                                    currentState.set(State.CopyTo(id))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelfAfterChildren(originX, originY, batch)
        
        val s = currentState.getOrCompute()
        if (s is State.TmpMsg) {
            s.timerSec -= Gdx.graphics.deltaTime
            if (s.timerSec <= 0f) {
                currentState.set(State.None())
            }
        }
    }

    fun prepareShow(): ManageCustomTexPackDialog {
        currentState.set(State.None())
        return this
    }

    override fun canCloseDialog(): Boolean {
        return currentState.getOrCompute() is State.None
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
        syncThisCustomPackWithContainer()
    }
    
    fun syncThisCustomPackWithContainer() {
        editor.container.setTexturePackFromSource()
    }
    
}
