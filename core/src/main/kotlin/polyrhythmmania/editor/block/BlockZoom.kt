package polyrhythmmania.editor.block

import com.badlogic.gdx.graphics.Color
import com.eclipsesource.json.JsonObject
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.RenderAlign
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.CustomMenuItem
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import paintbox.ui.control.Button
import paintbox.ui.control.Slider
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.block.contextmenu.AbstractPatternMenuPane
import polyrhythmmania.editor.block.data.PaletteTransitionData
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventZoomCamera
import polyrhythmmania.world.tileset.PaletteTransition
import java.util.*
import kotlin.math.roundToInt


class BlockZoom(engine: Engine)
    : Block(engine, BlockPaletteChange.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
        const val DEFAULT_ZOOM: Float = 1f
        const val MIN_ZOOM: Float = 0.15f
        const val MAX_ZOOM: Float = 1.2f
    }

    val startZoom: FloatVar = FloatVar(DEFAULT_ZOOM)
    val endZoom: FloatVar = FloatVar(DEFAULT_ZOOM)
    val transitionData: PaletteTransitionData = PaletteTransitionData(PaletteTransition.DEFAULT.copy(duration = 1.0f))

    init {
        this.width = 1f
        val text = Localization.getVar("block.zoom.name", Var { listOf("${(endZoom.use() * 100).roundToInt()}%") })
        this.defaultText.bind { text.use() }
    }

    override fun compileIntoEvents(): List<Event> {
        return listOf(EventZoomCamera(engine, this.beat, transitionData.paletteTransition.getOrCompute(), startZoom.get(), endZoom.get()))
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(520f + 16f)
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.zoomBlock"), editor.editorPane.palette.markup))
            
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(CustomMenuItem(
                    Pane().apply { 
                        this.bounds.height.set(64f + 8f)

                        fun makeSlider(label: String, varr: FloatVar): Pair<HBox, Slider> {
                            val slider: Slider
                            val hbox = HBox().apply {
                                this.bounds.height.set(32f)
                                this.spacing.set(8f)
                                this += TextLabel(label).apply {
                                    this.markup.set(editor.editorPane.palette.markup)
                                    this.renderAlign.set(RenderAlign.right)
                                    this.bounds.width.set(60f)
                                    this.tooltipElement.set(editor.editorPane.createDefaultTooltip(Localization.getValue("blockContextMenu.zoomBlock.valueTooltip")))
                                }
                                slider = Slider().apply {
                                    this.bounds.width.set(250f)
                                    this.minimum.set(MIN_ZOOM)
                                    this.maximum.set(MAX_ZOOM)
                                    this.tickUnit.set(0.05f)
                                    this.setValue(varr.get())
                                    (this.skin.getOrCompute() as? Slider.SliderSkin)?.bgColor?.set(Color(0f, 0f, 0f, 1f))
                                    this.value.addListener {
                                        varr.set(it.getOrCompute())
                                    }
                                    this.setOnRightClick {
                                        setValue(DEFAULT_ZOOM)
                                    }
                                }
                                this += slider
                                this += TextLabel(binding = { "${(varr.use() * 100).roundToInt()}%" }, editor.editorPane.palette.musicDialogFontBold).apply {
                                    this.renderAlign.set(RenderAlign.center)
                                    this.bounds.width.set(50f)
                                    this.tooltipElement.set(editor.editorPane.createDefaultTooltip(Localization.getValue("blockContextMenu.zoomBlock.valueTooltip")))
                                }
                                this += Button(Localization.getVar("common.reset"), font = editor.editorPane.main.mainFont).apply {
                                    this.bounds.width.set(64f)
                                    this.setOnAction {
                                        varr.set(DEFAULT_ZOOM)
                                        slider.setValue(varr.get())
                                    }
                                    AbstractPatternMenuPane.invertButtonSkin(this)
                                }
                            }
                            return hbox to slider
                        }
                        
                        val start = makeSlider(Localization.getValue("blockContextMenu.zoomBlock.startValue"), startZoom)
                        this += start.first
                        val end = makeSlider(Localization.getValue("blockContextMenu.zoomBlock.endValue"), endZoom).apply { 
                            this.first.bounds.y.set(32f + 8f)
                        }
                        this += end.first


                        this += Button(Localization.getVar("blockContextMenu.spawnPattern.swap"), font = editor.editorPane.main.mainFont).apply {
                            Anchor.CentreRight.configure(this)
                            this.bounds.width.set(64f)
                            this.bounds.height.set(32f)
                            this.setOnAction {
                                val tmp = startZoom.get()
                                startZoom.set(endZoom.get())
                                endZoom.set(tmp)
                                
                                start.second.setValue(startZoom.get())
                                end.second.setValue(endZoom.get())
                            }
                            AbstractPatternMenuPane.invertButtonSkin(this)
                        }
                    }
            ))
            
            ctxmenu.addMenuItem(SeparatorMenuItem())
            transitionData.createMenuItems(editor).forEach { ctxmenu.addMenuItem(it) }
        }
    }

    override fun copy(): BlockZoom {
        return BlockZoom(engine).also {
            this.copyBaseInfoTo(it)
            it.transitionData.paletteTransition.set(this.transitionData.paletteTransition.getOrCompute().copy())
            it.startZoom.set(this.startZoom.get())
            it.endZoom.set(this.endZoom.get())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("startZoom", this.startZoom.get())
        obj.add("endZoom", this.endZoom.get())
        transitionData.writeToJson(obj)
    }

    override fun readFromJson(obj: JsonObject, editorFlags: EnumSet<EditorSpecialFlags>) {
        super.readFromJson(obj, editorFlags)
        transitionData.readFromJson(obj)
        this.startZoom.set(obj.getFloat("startZoom", DEFAULT_ZOOM).coerceIn(MIN_ZOOM, MAX_ZOOM))
        this.endZoom.set(obj.getFloat("endZoom", DEFAULT_ZOOM).coerceIn(MIN_ZOOM, MAX_ZOOM))
    }
}