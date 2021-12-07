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
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.Matrix4Stack
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.ui.ColourPicker
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.world.*
import polyrhythmmania.world.entity.*
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.*
import kotlin.math.sign


class PaletteEditDialog(editorPane: EditorPane, val tilesetPalette: TilesetPalette,
                        val baseTileset: TilesetPalette?, val canChangeEnabledState: Boolean,
                        val titleLocalization: String = "editor.dialog.tilesetPalette.title",
) : EditorDialog(editorPane) {

    companion object {
        private val PR1_CONFIG: TilesetPalette = TilesetPalette.createGBA1TilesetPalette()
        private val PR2_CONFIG: TilesetPalette = TilesetPalette.createGBA2TilesetPalette()
        private val COLOURLESS_CONFIG: TilesetPalette = TilesetPalette.createColourlessTilesetPalette()
    }
    
    data class ResetDefault(val baseConfig: TilesetPalette)
    
    private val availableResetDefaults: List<ResetDefault> = listOfNotNull(
            ResetDefault(PR1_CONFIG),
            ResetDefault(PR2_CONFIG),
            ResetDefault(COLOURLESS_CONFIG),
            baseTileset?.let { ResetDefault(it) }
    )
    private var resetDefault: ResetDefault = availableResetDefaults.first()
    private val tempTileset: Tileset = Tileset(StockTexturePacks.gba /* This isn't used for rendering so any stock texture pack is fine */).apply {
        tilesetPalette.applyTo(this)
    }

    val groupFaceYMapping: ColorMappingGroupedCubeFaceY = ColorMappingGroupedCubeFaceY("groupCubeFaceYMapping")
    val groupPistonFaceZMapping: ColorMappingGroupedPistonFaceZ = ColorMappingGroupedPistonFaceZ("groupPistonFaceZMapping")
    private val groupMappings: List<ColorMapping> = listOf(groupFaceYMapping, groupPistonFaceZMapping)
    val allMappings: List<ColorMapping> = groupMappings + tilesetPalette.allMappings
    val allMappingsByID: Map<String, ColorMapping> = allMappings.associateBy { it.id }
    val currentMapping: Var<ColorMapping> = Var(allMappings[0])
    
    val objPreview: ObjectPreview = ObjectPreview()
    val colourPicker: ColourPicker = ColourPicker(false, font = editorPane.palette.musicDialogFont).apply { 
        this.setColor(currentMapping.getOrCompute().color.getOrCompute(), true)
    }
    val enabledCheckbox: CheckBox = CheckBox(binding = { Localization.getVar("editor.dialog.tilesetPalette.enabled").use() })

    /**
     * When false, updating the color in [ColourPicker] will NOT apply that colour to the tileset.
     * Used when switching between colour properties since there's no need for it to be applied
     */
    private var shouldColorPickerUpdateUpdateTileset: Boolean = true
    
    private val rodRotation: FloatVar = FloatVar(0f)

    init {
        resetGroupMappingsToTileset()
        
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
            allMappings.forEachIndexed { index, mapping ->
                listVbox += RadioButton(binding = { Localization.getVar("editor.dialog.tilesetPalette.object.${mapping.id}").use() },
                        font = editorPane.palette.musicDialogFont).apply {
                    this.textLabel.textColor.set(Color.WHITE.cpy())
                    this.textLabel.margin.set(Insets(0f, 0f, 8f, 8f))
                    this.textLabel.markup.set(editorPane.palette.markup)
                    if (canChangeEnabledState) {
                        this.color.bind {
                            val enabled = mapping.enabled.use()
                            if (enabled) Color.WHITE else Color.GRAY
                        }
                    } else {
                        this.color.set(Color.WHITE.cpy())
                    }
                    this.imageNode.padding.set(Insets(4f))
                    toggleGroup.addToggle(this)
                    this.bounds.height.set(48f)
                    this.onSelected = {
                        currentMapping.set(mapping)
                        shouldColorPickerUpdateUpdateTileset = false
                        updateColourPickerToMapping(mapping)
                        shouldColorPickerUpdateUpdateTileset = true
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
                            bounds.height.use() * (16f / 9f)
                        }
                    }
                    this += VBox().also { v -> 
                        v.bindWidthToParent(adjustBinding = { objPreview.bounds.width.use() * -1 + -5f })
                        v.spacing.set(4f)
                        v.temporarilyDisableLayouts { 
                            v += HBox().apply {
                                this.spacing.set(8f)
                                this.bounds.height.set(64f)
                                this.temporarilyDisableLayouts { 
                                    this += RectElement(Color().grey(0.95f)).apply {
                                        this.bounds.width.set(64f)
                                        this.padding.set(Insets(2f))
                                        this += ImageNode(AssetRegistry.get<PackedSheet>("tileset_gba")["platform"]).apply {
                                            Anchor.Centre.configure(this)
                                        }
                                        this += ImageNode(AssetRegistry.get<PackedSheet>("tileset_gba")["xyz"]).apply { 
                                            Anchor.Centre.configure(this)
                                        }
                                    }
                                    this += TextLabel("[b][color=#FF0000]X-[] [color=#00D815]Y+[] [color=#0000FF]Z+[][]").apply { 
                                        this.markup.set(editorPane.palette.markup)
                                        this.bounds.width.set(100f)
                                        this.textColor.set(Color.WHITE)
                                        this.renderBackground.set(true)
                                        this.bgPadding.set(Insets(8f))
                                        (this.skin.getOrCompute() as TextLabelSkin).defaultBgColor.set(Color(1f, 1f, 1f, 01f))
                                    }
                                    this += VBox().apply {
                                        this.padding.set(Insets(1f))
                                        this.border.set(Insets(1f))
                                        this.borderStyle.set(SolidBorder(Color.WHITE))
                                        this.bounds.width.set(150f)
                                        this.spacing.set(0f)
                                        this += TextLabel(binding = { Localization.getVar("editor.dialog.tilesetPalette.rotateRod").use() }).apply {
                                            this.padding.set(Insets(1f, 1f, 2f, 2f))
                                            this.markup.set(editorPane.palette.markup)
                                            this.textColor.set(Color.WHITE)
                                            this.bounds.height.set(28f)
                                            this.renderAlign.set(Align.left)
                                        }
                                        this += Pane().apply {
                                            this.bounds.height.set(28f)
                                            this.padding.set(Insets(2f, 2f, 1f, 1f))
                                            this += Slider().apply slider@{
                                                this.minimum.set(0f)
                                                this.maximum.set(1f)
                                                this.tickUnit.set(0f)
                                                this.setValue(0f)
                                                rodRotation.bind { this@slider.value.use() * 2f }
                                            }
                                        }
                                    }
                                }
                            }
                            v += Button(binding = { Localization.getVar("editor.dialog.tilesetPalette.reset").use() },
                                    font = editorPane.palette.musicDialogFont).apply {
                                this.applyDialogStyleContent()
                                this.bounds.height.set(40f)
                                this.setOnAction {
                                    val currentMapping = currentMapping.getOrCompute()
                                    val affectedMappings: Set<ColorMapping> = if (currentMapping is ColorMappingGroup) {
                                        currentMapping.affectsMappings.toSet()
                                    } else setOf(currentMapping)
                                    val baseConfig = resetDefault.baseConfig
                                    affectedMappings.forEach { cm ->
                                        val id = cm.id
                                        val baseConfigMapping = baseConfig.allMappingsByID.getValue(id)
                                        val tilesetPaletteMapping = tilesetPalette.allMappingsByID.getValue(id)
                                        tilesetPaletteMapping.color.set(baseConfigMapping.color.getOrCompute().cpy())
                                    }
                                    
                                    tilesetPalette.applyTo(tempTileset)
                                    
                                    currentMapping.color.set(currentMapping.tilesetGetter(tempTileset).getOrCompute().cpy())
                                    updateColourPickerToMapping()
                                }
                            }
                            v += HBox().apply {
                                this.visible.set(canChangeEnabledState)
                                this.bounds.height.set(40f)
                                this.spacing.set(4f)
                                this.temporarilyDisableLayouts {
                                    val checkbox = enabledCheckbox.apply {
                                        this.bounds.width.set(150f)
                                        this.textLabel.markup.set(editorPane.palette.markup)
                                        this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.tilesetPalette.enabled.tooltip")))
                                        this.imageNode.padding.set(Insets(4f))
                                        this.color.set(Color.WHITE.cpy())
                                        
                                        this.setOnAction { // This overrides the default behaviour of CheckBox
                                            val newState = checkedState.invert()
                                            val currentMapping = currentMapping.getOrCompute()
                                            if (currentMapping is ColorMappingGroup) {
                                                currentMapping.affectsMappings.forEach { m ->
                                                    m.enabled.set(newState)
                                                }
                                            } else {
                                                currentMapping.enabled.set(newState)
                                            }
                                        }
                                    }
                                    this += checkbox
                                    this += Button(binding = { Localization.getVar("editor.dialog.tilesetPalette.enableAll").use() },
                                            font = editorPane.palette.musicDialogFont).apply {
                                        this.bounds.width.set(90f)
                                        this.setScaleXY(0.8f)
                                        this.setOnAction {
                                            checkbox.checkedState.set(true)
                                            tilesetPalette.allMappings.forEach { m ->
                                                m.enabled.set(true)
                                            }
                                        }
                                    }
                                    this += Button(binding = { Localization.getVar("editor.dialog.tilesetPalette.disableAll").use() },
                                            font = editorPane.palette.musicDialogFont).apply {
                                        this.bounds.width.set(90f)
                                        this.setScaleXY(0.8f)
                                        this.setOnAction {
                                            checkbox.checkedState.set(false)
                                            tilesetPalette.allMappings.forEach { m ->
                                                m.enabled.set(false)
                                            }
                                        }
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
            this.bindWidthToParent(adjustBinding = { -(bounds.height.use() + 4f) })
        }
        bottomHbox.temporarilyDisableLayouts {
            bottomHbox += TextLabel(binding = { Localization.getVar("editor.dialog.tilesetPalette.resetLabel").use() },
                    font = editorPane.palette.musicDialogFont).apply {
                this.markup.set(editorPane.palette.markup)
                this.textColor.set(Color.WHITE.cpy())
                this.renderAlign.set(Align.right)
                this.textAlign.set(TextAlign.RIGHT)
                this.doLineWrapping.set(true)
                this.bounds.width.set(250f)
            }
            val toggleGroup = ToggleGroup()
            bottomHbox += VBox().apply { 
                this.spacing.set(2f)
                this.bounds.width.set(185f)
                this += RadioButton(binding = { Localization.getVar("editor.dialog.tilesetPalette.reset.pr1").use() },
                        font = editorPane.palette.musicDialogFont).apply {
                    this.bindHeightToParent(multiplier = 0.5f, adjust = -1f)
                    this.textLabel.markup.set(editorPane.palette.markup)
                    this.imageNode.padding.set(Insets(1f))
                    this.color.set(Color.WHITE.cpy())
                    toggleGroup.addToggle(this)
                    this.onSelected = {
                        resetDefault = availableResetDefaults[0]
                    }
                    this.selectedState.set(true)
                }
                this += RadioButton(binding = { Localization.getVar("editor.dialog.tilesetPalette.reset.pr2").use() },
                        font = editorPane.palette.musicDialogFont).apply {
                    this.bindHeightToParent(multiplier = 0.5f, adjust = -1f)
                    this.textLabel.markup.set(editorPane.palette.markup)
                    this.color.set(Color.WHITE.cpy())
                    this.imageNode.padding.set(Insets(1f))
                    toggleGroup.addToggle(this)
                    this.onSelected = {
                        resetDefault = availableResetDefaults[1]
                    }
                }
            }
            bottomHbox += VBox().apply {
                this.spacing.set(2f)
                this.bounds.width.set(215f)
                this += RadioButton(binding = { Localization.getVar("editor.dialog.tilesetPalette.reset.colourless").use() },
                        font = editorPane.palette.musicDialogFont).apply {
                    this.bindHeightToParent(multiplier = 0.5f, adjust = -1f)
                    this.textLabel.textColor.set(Color.WHITE.cpy())
                    this.textLabel.markup.set(editorPane.palette.markup)
                    this.color.set(Color.WHITE.cpy())
                    this.imageNode.padding.set(Insets(1f))
                    toggleGroup.addToggle(this)
                    this.onSelected = {
                        resetDefault = availableResetDefaults[2]
                    }
                }
                if (baseTileset != null) {
                    this += RadioButton(binding = { Localization.getVar("editor.dialog.tilesetPalette.reset.base").use() },
                            font = editorPane.palette.musicDialogFont).apply {
                        this.bindHeightToParent(multiplier = 0.5f, adjust = -1f)
                        this.imageNode.padding.set(Insets(1f))
                        this.color.set(Color.WHITE.cpy())
                        toggleGroup.addToggle(this)
                        this.onSelected = {
                            resetDefault = availableResetDefaults[3]
                        }
                    }
                }
            }
            bottomHbox += Button(binding = { Localization.getVar("editor.dialog.tilesetPalette.resetAll").use() },
                    font = editorPane.palette.musicDialogFont).apply {
                this.applyDialogStyleBottom()
                this.bounds.width.set(325f)
                this.setOnAction {
                    val baseConfig = resetDefault.baseConfig
                    baseConfig.allMappings.forEach { baseMapping ->
                        val m = tilesetPalette.allMappingsByID.getValue(baseMapping.id)
                        val baseColor = baseMapping.color.getOrCompute()
                        m.color.set(baseColor.cpy())
                    }
                    tilesetPalette.applyTo(objPreview.worldRenderer.tileset)
                    resetGroupMappingsToTileset()
                    updateColourPickerToMapping()
                }
            }
            bottomHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_colour_picker_copy")))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.tilesetPalette.copyAll")))
                this.setOnAction { 
                    Gdx.app.clipboard.contents = tilesetPalette.toJson().toString(WriterConfig.MINIMAL)
                }
            }
            bottomHbox += Button("").apply {
                this.applyDialogStyleBottom()
                this.bindWidthToSelfHeight()
                this.padding.set(Insets(8f))
                this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_colour_picker_paste")))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.tilesetPalette.pasteAll")))
                this.setOnAction { 
                    val clipboard = Gdx.app.clipboard
                    if (clipboard.hasContents()) {
                        try {
                            val jsonValue = Json.parse(clipboard.contents)
                            if (jsonValue.isObject) {
                                tilesetPalette.fromJson(jsonValue.asObject())
                                tilesetPalette.applyTo(objPreview.worldRenderer.tileset)
//                                applyCurrentMappingToPreview(currentMapping.getOrCompute().color.getOrCompute())
                                resetGroupMappingsToTileset()
                                shouldColorPickerUpdateUpdateTileset = false
                                updateColourPickerToMapping()
                                shouldColorPickerUpdateUpdateTileset = true
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
            if (shouldColorPickerUpdateUpdateTileset) {
                applyCurrentMappingToPreview(c.getOrCompute().cpy())
            }
        }
    }
    
    private fun resetGroupMappingsToTileset() {
        groupMappings.forEach { m ->
            m.color.set(m.tilesetGetter(objPreview.worldRenderer.tileset).getOrCompute().cpy())
        }
    }
    
    private fun applyCurrentMappingToPreview(newColor: Color) {
        val m = currentMapping.getOrCompute()
        m.color.set(newColor.cpy())
        m.applyTo(objPreview.worldRenderer.tileset)
    }
    
    private fun updateColourPickerToMapping(mapping: ColorMapping = currentMapping.getOrCompute()) {
        colourPicker.setColor(mapping.color.getOrCompute(), true)
        enabledCheckbox.checkedState.set(mapping.enabled.get())
    }
    
    fun prepareShow(): PaletteEditDialog {
        tilesetPalette.applyTo(objPreview.worldRenderer.tileset)
        resetGroupMappingsToTileset()
        updateColourPickerToMapping()
        return this
    }

    override fun canCloseDialog(): Boolean {
        return true
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
        editor.updatePaletteAndTexPackChangesState()
    }
    
    inner class ObjectPreview : UIElement() {
        
        val world: World = World()
        val worldRenderer: WorldRenderer = WorldRenderer(world, Tileset(editor.container.renderer.tileset.texturePack).apply { 
            tilesetPalette.applyTo(this)
        }, editor.engine)
        
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
            worldRenderer.render(batch)
            frameBuffer.end()
            batch.projectionMatrix = prevMatrix
            batch.begin()

            Matrix4Stack.pop()
            
            batch.packedColor = lastPackedColor
        }
    }

    open inner class ColorMappingGroup(id: String, val affectsMappings: List<ColorMapping>,
                                       tilesetGetter: (Tileset) -> Var<Color>)
        : ColorMapping(id, tilesetGetter) {
        init {
            this.enabled.bind {
                var anyEnabled = false
                affectsMappings.forEach { m ->
                    // Intentionally iterating through all of them since they are all dependencies.
                    // The order of the anyEnabled assignment is also intentional to prevent bad short-circuiting
                    anyEnabled = m.enabled.use() || anyEnabled
                }
                anyEnabled
            }
        }
    }

    inner class ColorMappingGroupedCubeFaceY(id: String)
        : ColorMappingGroup(id,
            listOf("cubeFaceY", "cubeBorder", "signShadow", "cubeBorderZ", "cubeFaceZ", "cubeFaceX").map { tilesetPalette.allMappingsByID.getValue(it) },
            { it.cubeFaceY.color }) {
        
        private val hsv: FloatArray = FloatArray(3) { 0f }

        override fun applyTo(tileset: Tileset) {
            val varr = tilesetGetter(tileset)
            val thisColor = this.color.getOrCompute()
            varr.set(thisColor.cpy())
            allMappingsByID.getValue("cubeFaceY").color.set(thisColor.cpy())

            thisColor.toHsv(hsv)
            hsv[1] = (hsv[1] + 0.18f * hsv[1].sign)
            hsv[2] = (hsv[2] - 0.17f)
            val borderColor = Color(1f, 1f, 1f, thisColor.a).fromHsv(hsv)
            tileset.cubeBorder.color.set(borderColor.cpy())
            allMappingsByID.getValue("cubeBorder").color.set(borderColor.cpy())
            tileset.signShadowColor.set(borderColor.cpy())
            allMappingsByID.getValue("signShadow").color.set(borderColor.cpy())

            hsv[1] = (hsv[1] + 0.03f * hsv[1].sign)
            hsv[2] = (hsv[2] - 0.13f)
            val cubeBorderZColor = Color(1f, 1f, 1f, thisColor.a).fromHsv(hsv)
            tileset.cubeBorderZ.color.set(cubeBorderZColor)
            allMappingsByID.getValue("cubeBorderZ").color.set(cubeBorderZColor.cpy())
            
            // Face
            thisColor.toHsv(hsv)
            hsv[1] = (hsv[1] + 0.08f * hsv[1].sign)
            hsv[2] = (hsv[2] - 0.10f)
            val faceZColor = Color(1f, 1f, 1f, 1f).fromHsv(hsv)
            tileset.cubeFaceZ.color.set(faceZColor.cpy())
            allMappingsByID.getValue("cubeFaceZ").color.set(faceZColor.cpy())
            thisColor.toHsv(hsv)
            hsv[1] = (hsv[1] + 0.11f * hsv[1].sign)
            hsv[2] = (hsv[2] - 0.13f)
            val faceXColor = Color(1f, 1f, 1f, 1f).fromHsv(hsv)
            tileset.cubeFaceX.color.set(faceXColor.cpy())
            allMappingsByID.getValue("cubeFaceX").color.set(faceXColor.cpy())
        }
    }
    
    inner class ColorMappingGroupedPistonFaceZ(id: String)
        : ColorMappingGroup(id,
            listOf("pistonFaceZ", "pistonFaceX").map { tilesetPalette.allMappingsByID.getValue(it) },
            { it.pistonFaceZColor }) {

        private val hsv: FloatArray = FloatArray(3) { 0f }

        override fun applyTo(tileset: Tileset) {
            val varr = tilesetGetter(tileset)
            val thisColor = this.color.getOrCompute()
            varr.set(thisColor.cpy())
            allMappingsByID.getValue("pistonFaceZ").color.set(thisColor.cpy())

            thisColor.toHsv(hsv)
            hsv[2] = (hsv[2] - 0.20f)
            val pistonFaceX = Color(1f, 1f, 1f, thisColor.a).fromHsv(hsv)
            tileset.pistonFaceXColor.set(pistonFaceX.cpy())
            allMappingsByID.getValue("pistonFaceX").color.set(pistonFaceX.cpy())
        }
    }

}