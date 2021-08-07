package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.Matrix4Stack
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.world.*
import polyrhythmmania.world.entity.*
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.*


class TexturePackEditDialog(editorPane: EditorPane, 
) : EditorDialog(editorPane) {

    companion object {
        val LIST_ENTRIES: List<ListEntry> = listOf(
                ListEntry.Category("terrain"),
                ListEntry.Region("cube_border"),
                ListEntry.Region("cube_border_platform"),
                ListEntry.Region("cube_border_z"),
                ListEntry.Region("cube_face_x"),
                ListEntry.Region("cube_face_y"),
                ListEntry.Region("cube_face_z"),
                ListEntry.Region("platform"),
                ListEntry.Region("platform_with_line"),
                ListEntry.Region("red_line"),
                
                ListEntry.Category("rods"),
                ListEntry.Region("rods_borders"),
                ListEntry.Region("rods_fill"),
                ListEntry.Region("explosion_0"),
                ListEntry.Region("explosion_1"),
                ListEntry.Region("explosion_2"),
                ListEntry.Region("explosion_3"),
                
                ListEntry.Category("pistons"),
                ListEntry.Region("piston_a"),
                ListEntry.Region("piston_a_extended"),
                ListEntry.Region("piston_a_extended_face_x"),
                ListEntry.Region("piston_a_extended_face_z"),
                ListEntry.Region("piston_a_partial"),
                ListEntry.Region("piston_a_partial_face_x"),
                ListEntry.Region("piston_a_partial_face_z"),
                ListEntry.Region("piston_dpad"),
                ListEntry.Region("piston_dpad_extended"),
                ListEntry.Region("piston_dpad_extended_face_x"),
                ListEntry.Region("piston_dpad_extended_face_z"),
                ListEntry.Region("piston_dpad_partial"),
                ListEntry.Region("piston_dpad_partial_face_x"),
                ListEntry.Region("piston_dpad_partial_face_z"),
                
                ListEntry.Category("signs"),
                ListEntry.Region("sign_a"),
                ListEntry.Region("sign_a_shadow"),
                ListEntry.Region("sign_dpad"),
                ListEntry.Region("sign_dpad_shadow"),
                ListEntry.Region("sign_bo"),
                ListEntry.Region("sign_bo_shadow"),
                ListEntry.Region("sign_ta"),
                ListEntry.Region("sign_ta_shadow"),
                ListEntry.Region("sign_n"),
                ListEntry.Region("sign_n_shadow"),
                ListEntry.Region("indicator_a"),
                ListEntry.Region("indicator_dpad"),
                ListEntry.Region("input_feedback_0"),
                ListEntry.Region("input_feedback_1"),
                ListEntry.Region("input_feedback_2"),
        )
    }
    
    sealed class ListEntry(val localizationKey: String) {
        class Category(val categoryID: String) : ListEntry("editor.dialog.texturePack.objectCategory.${categoryID}")
        class Region(val id: String) : ListEntry("editor.dialog.texturePack.object.${id}")
    }

    
    private val isFileChooserOpen: Var<Boolean> = Var(false)
    
    val objPreview: ObjectPreview = ObjectPreview()
    
    private val rodRotation: FloatVar = FloatVar(0f)

    init {
        resetGroupMappingsToTileset()
        
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.texturePack.title").use() }


        val contentPaneContainer = Pane().apply {
            this.visible.bind { !isFileChooserOpen.use() }
        }
        contentPane += contentPaneContainer
        val bottomPaneContainer = Pane().apply {
            this.visible.bind { !isFileChooserOpen.use() }
        }
        bottomPane += bottomPaneContainer
        
        bottomPaneContainer.addChild(Button("").apply {
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

        val scrollPaneWidthProportion = 0.4f
        val scrollPane: ScrollPane = ScrollPane().apply {
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(0f, 0f, 0f, 0f))
            this.bindWidthToParent(multiplier = scrollPaneWidthProportion)
            this.vBar.blockIncrement.set(64f)
            this.vBar.skinID.set(PRManiaSkins.SCROLLBAR_SKIN)
        }
        contentPaneContainer.addChild(scrollPane)

        val listVbox = VBox().apply {
            this.spacing.set(1f)
        }

        listVbox.temporarilyDisableLayouts {
            val toggleGroup = ToggleGroup()
            var firstRegionEntry = true
            LIST_ENTRIES.forEach { listEntry -> 
                listVbox += when (listEntry) {
                    is ListEntry.Category -> {
                        TextLabel(binding = { Localization.getVar(listEntry.localizationKey).use() },
                                font = main.fontEditorDialogTitle).apply {
                            this.textColor.set(Color.WHITE.cpy())
                            this.margin.set(Insets(0f, 0f, 8f, 8f))
                            this.padding.set(this.padding.getOrCompute().copy(bottom = 4f))
                            this.setScaleXY(0.5f)
                            this.renderAlign.set(Align.bottomLeft)
                            this.bounds.height.set(54f)
                        }
                    }
                    is ListEntry.Region -> {
                        RadioButton(binding = { Localization.getVar(listEntry.localizationKey).use() + "[color=LIGHT_GRAY scale=0.8f]\n${listEntry.id}[]" },
                                font = editorPane.palette.musicDialogFont).apply {
                            this.textLabel.textColor.set(Color.WHITE.cpy())
                            this.textLabel.margin.set(Insets(0f, 0f, 0f, 8f))
                            this.textLabel.markup.set(editorPane.palette.markup)
                            this.imageNode.tint.set(Color.WHITE.cpy())
                            this.imageNode.padding.set(Insets(6f, 6f, 6f, 10f))
                            this.bounds.height.set(48f)
                            toggleGroup.addToggle(this)
                            this.onSelected = {
                                
                            }
                            if (firstRegionEntry) {
                                firstRegionEntry = false
                                selectedState.set(true)
                            }
                        }
                    }
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
            
        }
        
        val bottomLeftHbox = HBox().apply {
            this.spacing.set(8f)
            this.bindWidthToParent(multiplier = scrollPaneWidthProportion, adjust = -8f)
        }
        bottomLeftHbox.temporarilyDisableLayouts {
            bottomLeftHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_new"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.new")))
                this.setOnAction {
                    // TODO
                }
            }
            bottomLeftHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_open"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.loadAll")))
                this.setOnAction {
                    // TODO
                }
            }
            bottomLeftHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_save"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.saveAll")))
                this.setOnAction {
                    // TODO
                }
            }
            bottomLeftHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_export"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.exportAllTextures")))
                this.setOnAction {
                    // TODO
                }
            }
            bottomLeftHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_import"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.importTextures")))
                this.setOnAction {
                    // TODO
                }
            }
            bottomLeftHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_export_base"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.exportAllBaseTextures")))
                this.setOnAction {
                    // TODO
                }
            }
        }
        bottomPaneContainer.addChild(bottomLeftHbox)
        bottomPaneContainer.addChild(RectElement(Color().grey(0.31f)).apply { 
            this.bounds.width.set(2f)
            this.bounds.x.bind { (parent.use()?.bounds?.width?.useF() ?: 0f) * scrollPaneWidthProportion - 8f }
        })


        val bottomRightHbox = HBox().apply {
            this.spacing.set(8f)
            this.bindWidthToParent(multiplierBinding = { 1f - scrollPaneWidthProportion }, adjustBinding = { -8f * 2 - bounds.height.useF() })
            this.bounds.x.bind { (parent.use()?.bounds?.width?.useF() ?: 0f) * 0.4f + 8f }
        }
        bottomRightHbox.temporarilyDisableLayouts {
//            bottomRightHbox += Button("").apply {
//                this.applyDialogStyleBottom()
//                this.bindWidthToSelfHeight()
//                this.padding.set(Insets(8f))
//                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_trash"]))
//                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.removeTexture")))
//                this.setOnAction {
//                    // TODO
//                }
//            }
//            bottomRightHbox += Button("").apply {
//                this.applyDialogStyleBottom()
//                this.bindWidthToSelfHeight()
//                this.padding.set(Insets(8f))
//                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_import"]))
//                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.importTexture")))
//                this.setOnAction {
//                    // TODO
//                }
//            }
//            bottomRightHbox += Button("").apply {
//                this.applyDialogStyleBottom()
//                this.bindWidthToSelfHeight()
//                this.padding.set(Insets(8f))
//                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_export"]))
//                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.exportTexture")))
//                this.setOnAction {
//                    // TODO
//                }
//            }
//            bottomRightHbox += Button("").apply {
//                this.applyDialogStyleBottom()
//                this.bindWidthToSelfHeight()
//                this.padding.set(Insets(8f))
//                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_export_base"]))
//                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.texturePack.button.exportBaseTexture")))
//                this.setOnAction {
//                    // TODO
//                }
//            }
        }
        bottomPaneContainer.addChild(bottomRightHbox)
    }
    
    private fun resetGroupMappingsToTileset() {
//        groupMappings.forEach { m ->
//            m.color.set(m.tilesetGetter(objPreview.worldRenderer.tileset).getOrCompute().cpy())
//        }
    }
    
    fun prepareShow(): TexturePackEditDialog {
//        tilesetPalette.applyTo(objPreview.worldRenderer.tileset)
        resetGroupMappingsToTileset()
//        updateColourPickerToMapping()
        return this
    }

    override fun canCloseDialog(): Boolean {
        return !isFileChooserOpen.getOrCompute()
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
    }
    
    inner class ObjectPreview : UIElement() {
        
        val world: World = World()
        val worldRenderer: WorldRenderer = WorldRenderer(world, Tileset(editor.container.renderer.tileset.texturePack).apply { 
//            tilesetPalette.applyTo(this)
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
            world.clearEntities()
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
            
            world.addEntity(EntityPiston(world).apply { 
                this.position.set(6f, 0f, 0f)
                this.type = EntityPiston.Type.PISTON_A
                this.pistonState = EntityPiston.PistonState.FULLY_EXTENDED
            })
            world.addEntity(EntityPiston(world).apply { 
                this.position.set(9f, 0f, 0f)
                this.type = EntityPiston.Type.PISTON_DPAD
                this.pistonState = EntityPiston.PistonState.FULLY_EXTENDED
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