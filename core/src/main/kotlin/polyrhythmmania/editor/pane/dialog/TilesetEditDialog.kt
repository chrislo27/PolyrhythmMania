package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.ColorStack
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.screen.mainmenu.EntityRowBlockDecor
import polyrhythmmania.ui.ColourPicker
import polyrhythmmania.world.EntityCube
import polyrhythmmania.world.EntityPlatform
import polyrhythmmania.world.EntitySign
import polyrhythmmania.world.World
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.TilesetConfig
import polyrhythmmania.world.render.WorldRenderer


class TilesetEditDialog(editorPane: EditorPane) : EditorDialog(editorPane) {

    enum class ResetDefault(val baseConfig: TilesetConfig) {
        PR1(TilesetConfig.createGBA1TilesetConfig()),
        PR2(TilesetConfig.createGBA2TilesetConfig())
    }

    private var resetDefault: ResetDefault = ResetDefault.PR1

    val tilesetConfig: TilesetConfig = editor.container.tilesetConfig
    val currentMapping: Var<TilesetConfig.ColorMapping> = Var(tilesetConfig.allMappings[0])
    
    val objPreview: ObjectPreview = ObjectPreview()
    val colourPicker: ColourPicker = ColourPicker(false, font = editorPane.palette.musicDialogFont).apply { 
        this.setColor(currentMapping.getOrCompute().color.getOrCompute())
    }

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.tileset.title").use() }

        bottomPane.addChild(Button("").apply {
            Anchor.BottomRight.configure(this)
            this.bounds.width.bind { bounds.height.useF() }
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
            this.bindWidthToParent(multiplier = 0.4f)
            this.vBar.blockIncrement.set(64f)
        }
        contentPane.addChild(scrollPane)

        val listVbox = VBox().apply {
            this.spacing.set(8f)
        }

        listVbox.temporarilyDisableLayouts {
            val toggleGroup = ToggleGroup()
            tilesetConfig.allMappings.forEachIndexed { index, mapping ->
                listVbox += RadioButton(binding = { Localization.getVar("editor.dialog.tileset.object.${mapping.id}").use() },
                        font = editorPane.palette.musicDialogFont).apply {
                    this.textLabel.textColor.set(Color.WHITE.cpy())
                    this.textLabel.margin.set(Insets(0f, 0f, 8f, 8f))
                    this.imageNode.tint.set(Color.WHITE.cpy())
                    this.imageNode.padding.set(Insets(4f))
                    toggleGroup.addToggle(this)
                    this.bounds.height.set(48f)
                    this.onSelected = {
                        currentMapping.set(mapping)
                        colourPicker.setColor(mapping.color.getOrCompute())
                    }
                    if (index == 0) selectedState.set(true)
                }
            }
        }
        listVbox.sizeHeightToChildren(300f)
        scrollPane.setContent(listVbox)

        val previewVbox = VBox().apply {
            Anchor.TopRight.configure(this)
            this.bindWidthToParent(multiplier = 0.6f, adjust = -8f)
            this.spacing.set(12f)
        }
        contentPane.addChild(previewVbox)
        previewVbox.temporarilyDisableLayouts {
            previewVbox += HBox().apply {
                this.spacing.set(8f)
                this.margin.set(Insets(4f))
                this.bounds.height.set(200f)
                this.temporarilyDisableLayouts {
                    this += objPreview.apply {
                        this.bounds.width.bind { 
                            bounds.height.useF() * (16f / 9f)
                        }
                    }
                    this += VBox().also { v -> 
                        v.bindWidthToParent(adjustBinding = { objPreview.bounds.width.useF() * -1 })
                        v.spacing.set(4f)
                        v.temporarilyDisableLayouts { 
                            v += Button(binding = { Localization.getVar("editor.dialog.tileset.reset").use() },
                                    font = editorPane.palette.musicDialogFont).apply {
                                this.applyDialogStyleContent()
                                this.bounds.height.set(40f)
                                this.setOnAction {
                                    val currentMapping = currentMapping.getOrCompute()
                                    val defaultColor = resetDefault.baseConfig.allMappingsByID.getValue(currentMapping.id).color.getOrCompute()
                                    currentMapping.color.set(defaultColor.cpy())
                                    updateCurrentMappingToPreview(defaultColor)
                                }
                            }
                        }
                    }
                }
            }
            previewVbox += colourPicker.apply {
                this.bindWidthToParent()
                this.bounds.height.set(220f)
            }
        }

        
        
        val bottomHbox = HBox().apply {
            this.spacing.set(8f)
            this.bindWidthToParent(multiplier = 0.9f)
        }
        bottomHbox.temporarilyDisableLayouts {
            bottomHbox += TextLabel(binding = { Localization.getVar("editor.dialog.tileset.resetLabel").use() },
                    font = editorPane.palette.musicDialogFont).apply {
                this.markup.set(editorPane.palette.markup)
                this.textColor.set(Color.WHITE.cpy())
                this.renderAlign.set(Align.right)
                this.textAlign.set(TextAlign.RIGHT)
                this.doLineWrapping.set(true)
                this.bounds.width.set(250f)
            }
            val toggleGroup = ToggleGroup()
            bottomHbox += RadioButton(binding = { Localization.getVar("editor.dialog.tileset.reset.pr1").use() },
                    font = editorPane.palette.musicDialogFont).apply {
                this.textLabel.textColor.set(Color.WHITE.cpy())
                this.imageNode.tint.set(Color.WHITE.cpy())
                this.imageNode.padding.set(Insets(4f))
                toggleGroup.addToggle(this)
                this.bounds.width.set(200f)
                this.onSelected = {
                    resetDefault = ResetDefault.PR1
                }
                this.selectedState.set(true)
            }
            bottomHbox += RadioButton(binding = { Localization.getVar("editor.dialog.tileset.reset.pr2").use() },
                    font = editorPane.palette.musicDialogFont).apply {
                this.textLabel.textColor.set(Color.WHITE.cpy())
                this.imageNode.tint.set(Color.WHITE.cpy())
                this.imageNode.padding.set(Insets(4f))
                toggleGroup.addToggle(this)
                this.bounds.width.set(200f)
                this.onSelected = {
                    resetDefault = ResetDefault.PR2
                }
            }
            bottomHbox += Button(binding = { Localization.getVar("editor.dialog.tileset.resetAll").use() },
                    font = editorPane.palette.musicDialogFont).apply {
                this.applyDialogStyleBottom()
                this.bounds.width.set(325f)
                this.setOnAction {
                    val baseConfig = resetDefault.baseConfig
                    baseConfig.allMappings.forEach { baseMapping ->
                        val m = tilesetConfig.allMappingsByID.getValue(baseMapping.id)
                        val baseColor = baseMapping.color.getOrCompute()
                        m.color.set(baseColor.cpy())
                        if (m == currentMapping.getOrCompute()) {
                            updateCurrentMappingToPreview(baseColor)
                        }
                    }
                    tilesetConfig.applyTo(objPreview.worldRenderer.tileset)
                }
            }
        }
        bottomPane.addChild(bottomHbox)
    }
    
    init {
        colourPicker.currentColor.addListener { c ->
            updateCurrentMappingToPreview(c.getOrCompute().cpy())
        }
    }
    
    private fun updateCurrentMappingToPreview(newColor: Color) {
        val m = currentMapping.getOrCompute()
        m.color.set(newColor.cpy())
        m.applyTo(objPreview.worldRenderer.tileset)
    }

    override fun canCloseDialog(): Boolean {
        return true
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
        tilesetConfig.applyTo(editor.container.renderer.tileset)
    }
    
    inner class ObjectPreview : UIElement() {

        val world: World = World()
        val worldRenderer: WorldRenderer = WorldRenderer(world, Tileset(AssetRegistry.get<PackedSheet>("tileset_parts")).apply { 
            tilesetConfig.applyTo(this)
        })
        
        init {
            this += ImageNode(editor.previewTextureRegion)
        }
        
        init {
            world.entities.toList().forEach { world.removeEntity(it) }
            for (x in 2..12) {
                for (z in -5..4) {
                    val ent = if (z == 0) EntityPlatform(world, withLine = x == 4) else EntityCube(world, withLine = x == 4, withBorder = z == 1)
                    world.addEntity(ent.apply { 
                        this.position.set(x.toFloat(), -1f, z.toFloat())
                    })
                    if (z == 0 && x <= 4) {
                        world.addEntity(EntityPlatform(world, withLine = x == 4).apply {
                            this.position.set(x.toFloat(), 0f, z.toFloat())
                        })
                    }
                }
            }
            
            world.addEntity(EntityRowBlockDecor(world).apply { 
                this.position.set(6f, 0f, 0f)
                this.type = EntityRowBlockDecor.Type.PISTON_A
                this.pistonState = EntityRowBlockDecor.PistonState.FULLY_EXTENDED
            })
            world.addEntity(EntityRowBlockDecor(world).apply { 
                this.position.set(9f, 0f, 0f)
                this.type = EntityRowBlockDecor.Type.PISTON_DPAD
                this.pistonState = EntityRowBlockDecor.PistonState.FULLY_EXTENDED
            })
            world.addEntity(EntityCube(world).apply { 
                this.position.set(7f, 0f, 2f)
            })

            // Button signs
            val signs = mutableListOf<EntitySign>()
            signs += EntitySign(world, EntitySign.Type.A).apply {
                this.position.set(5f, 2f, -3f)
            }
            signs += EntitySign(world, EntitySign.Type.DPAD).apply {
                this.position.set(6f, 2f, -3f)
            }
            signs += EntitySign(world, EntitySign.Type.BO).apply {
                this.position.set(4f, 2f, -2f)
            }
            signs += EntitySign(world, EntitySign.Type.TA).apply {
                this.position.set(5f, 2f, -2f)
            }
            signs += EntitySign(world, EntitySign.Type.N).apply {
                this.position.set(6f, 2f, -2f)
            }
            signs.forEach { sign ->
                sign.position.x += (12 / 32f)
                sign.position.z += (8 / 32f)
                world.addEntity(sign)
            }
        }

        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val renderBounds = this.paddingZone
            val x = renderBounds.x.get() + originX
            val y = originY - renderBounds.y.get()
            val w = renderBounds.width.get()
            val h = renderBounds.height.get()
            val lastPackedColor = batch.packedColor


            val cam = worldRenderer.camera
            cam.zoom = 1f / 2f
            cam.position.x = 3.5f
            cam.position.y = 1f
            cam.update()

            batch.end()
            val frameBuffer = editor.previewFrameBuffer
            frameBuffer.begin()
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            worldRenderer.render(batch, editor.engine)
            frameBuffer.end()
            batch.begin()

            
            batch.packedColor = lastPackedColor
        }
    }
}