package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import com.eclipsesource.json.Json
import com.eclipsesource.json.WriterConfig
import paintbox.binding.FloatVar
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
import paintbox.util.Matrix4Stack
import polyrhythmmania.Localization
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.screen.mainmenu.EntityRowBlockDecor
import polyrhythmmania.ui.ColourPicker
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.world.*
import polyrhythmmania.world.render.Tileset
import polyrhythmmania.world.render.TilesetConfig
import polyrhythmmania.world.render.WorldRenderer


class TilesetEditDialog(editorPane: EditorPane, val tilesetConfig: TilesetConfig,
                        val titleLocalization: String = "editor.dialog.tileset.title") 
    : EditorDialog(editorPane) {

    enum class ResetDefault(val baseConfig: TilesetConfig) {
        PR1(TilesetConfig.createGBA1TilesetConfig()),
        PR2(TilesetConfig.createGBA2TilesetConfig())
    }

    private var resetDefault: ResetDefault = ResetDefault.PR1

    val currentMapping: Var<TilesetConfig.ColorMapping> = Var(tilesetConfig.allMappings[0])
    
    val objPreview: ObjectPreview = ObjectPreview()
    val colourPicker: ColourPicker = ColourPicker(false, font = editorPane.palette.musicDialogFont).apply { 
        this.setColor(currentMapping.getOrCompute().color.getOrCompute())
    }
    
    private val rodRotation: FloatVar = FloatVar(0f)

    init {
        this.titleLabel.text.bind { Localization.getVar(titleLocalization).use() }

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
            this.bindWidthToParent(multiplier = 0.4f)
            this.vBar.blockIncrement.set(64f)
            this.vBar.skinID.set(PRManiaSkins.SCROLLBAR_SKIN)
        }
        contentPane.addChild(scrollPane)

        val listVbox = VBox().apply {
            this.spacing.set(1f)
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
                        updateColourPickerToMapping(mapping)
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
                                    applyCurrentMappingToPreview(defaultColor)
                                    updateColourPickerToMapping()
                                }
                            }
                            v += HBox().apply {
                                this.spacing.set(8f)
                                this.bounds.height.set(64f)
                                this.temporarilyDisableLayouts { 
                                    this += ImageNode(AssetRegistry.get<PackedSheet>("tileset_parts")["xyz"]).apply { 
                                        this.bounds.width.set(64f)
                                    }
                                    this += TextLabel("[b][color=#FF0000]X-[] [color=#00D815]Y+[] [color=#0000FF]Z+[][]").apply { 
                                        this.markup.set(editorPane.palette.markup)
                                        this.bindWidthToParent(adjust = -64f)
                                        this.textColor.set(Color.WHITE)
                                        this.renderBackground.set(true)
                                        this.bgPadding.set(Insets(8f))
                                        (this.skin.getOrCompute() as TextLabelSkin).defaultBgColor.set(Color(1f, 1f, 1f, 01f))
                                    }
                                }
                            }
                            v += HBox().apply { 
                                this.bounds.height.set(32f)
                                this.spacing.set(8f)
                                this += TextLabel(binding = { Localization.getVar("editor.dialog.tileset.rotateRod").use() }).apply {
                                    this.markup.set(editorPane.palette.markup)
                                    this.textColor.set(Color.WHITE)
                                    this.bounds.width.set(100f)
                                    this.renderAlign.set(Align.right)
                                }
                                this += Pane().apply {
                                    this.padding.set(Insets(4f))
                                    this += Slider().apply slider@{
                                        this.bindWidthToParent(adjust = -100f)
                                        this.minimum.set(0f)
                                        this.maximum.set(1f)
                                        this.tickUnit.set(0f)
                                        this.setValue(0f)
                                        rodRotation.bind { this@slider.value.useF() * 2f }
                                    }
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
            this.bindWidthToParent(adjustBinding = { -(bounds.height.useF() + 4f) })
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
                    }
                    tilesetConfig.applyTo(objPreview.worldRenderer.tileset)
                    updateColourPickerToMapping()
                }
            }
            bottomHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_colour_picker_copy")))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.tileset.copyAll")))
                this.setOnAction { 
                    Gdx.app.clipboard.contents = tilesetConfig.toJson().toString(WriterConfig.MINIMAL)
                }
            }
            bottomHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_colour_picker_paste")))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.tileset.pasteAll")))
                this.setOnAction { 
                    val clipboard = Gdx.app.clipboard
                    if (clipboard.hasContents()) {
                        try {
                            val jsonValue = Json.parse(clipboard.contents)
                            if (jsonValue.isObject) {
                                tilesetConfig.fromJson(jsonValue.asObject())
                                applyCurrentMappingToPreview(currentMapping.getOrCompute().color.getOrCompute())
                                tilesetConfig.applyTo(objPreview.worldRenderer.tileset)
                                updateColourPickerToMapping()
                            }
                        } catch (ignored: Exception) {}
                    }
                }
            }
        }
        bottomPane.addChild(bottomHbox)
    }
    
    init {
        colourPicker.currentColor.addListener { c ->
            applyCurrentMappingToPreview(c.getOrCompute().cpy())
        }
    }
    
    private fun applyCurrentMappingToPreview(newColor: Color) {
        val m = currentMapping.getOrCompute()
        m.color.set(newColor.cpy())
        m.applyTo(objPreview.worldRenderer.tileset)
    }
    
    private fun updateColourPickerToMapping(mapping: TilesetConfig.ColorMapping = currentMapping.getOrCompute()) {
        colourPicker.setColor(mapping.color.getOrCompute())
    }

    override fun canCloseDialog(): Boolean {
        return true
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
        editor.updateTilesetChangesState()
    }
    
    inner class ObjectPreview : UIElement() {

        val world: World = World()
        val worldRenderer: WorldRenderer = WorldRenderer(world, Tileset(AssetRegistry.get<PackedSheet>("tileset_parts")).apply { 
            tilesetConfig.applyTo(this)
        })
        
        val rodEntity: EntityRodDecor
        
        init {
            this += ImageNode(editor.previewTextureRegion)
            rodEntity = object : EntityRodDecor(world) {
                override fun getAnimationAlpha(): Float {
                    return (rodRotation.get() % 1f).coerceIn(0f, 1f)
                }
            }
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
            world.addEntity(rodEntity.apply {
                this.position.set(4f, 1f, 0f)
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
            val prevMatrix = Matrix4Stack.getAndPush().set(batch.projectionMatrix)
            batch.projectionMatrix = cam.combined
            val frameBuffer = editor.previewFrameBuffer
            frameBuffer.begin()
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            worldRenderer.render(batch, editor.engine)
            frameBuffer.end()
            batch.projectionMatrix = prevMatrix
            batch.begin()

            Matrix4Stack.pop()
            
            batch.packedColor = lastPackedColor
        }
    }
}