package polyrhythmmania.editor.pane

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import paintbox.binding.*
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.ButtonSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.util.gdxutils.drawCompressed
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.*
import java.util.*
import kotlin.math.*


class InstantiatorPane(val upperPane: UpperPane) : Pane() {

    val editorPane: EditorPane = upperPane.editorPane
    val editor: Editor = upperPane.editor

    val instantiatorList: InstantiatorList

    init {
        val middleDivider = RectElement(binding = { editorPane.palette.instantiatorPaneBorder.use() }).apply {
            Anchor.TopCentre.configure(this)
            this.margin.set(Insets(6f))
            this.bounds.width.bind {
                val margin = margin.use()
                (2f + margin.left + margin.right).roundToInt().toFloat()
            }
            this.bounds.x.set(300f)
        }
        this += middleDivider

        val scrollSelector = Pane().apply {
            this.bounds.width.bind { middleDivider.bounds.x.useF() }
        }
        this += scrollSelector

        val descPane = Pane().apply {
            Anchor.TopRight.configure(this)
            this.bounds.width.bind {
                (parent.use()?.contentZone?.width?.useF()
                        ?: 0f) - (middleDivider.bounds.x.useF() + middleDivider.bounds.width.useF())
            }
            this.padding.set(Insets(2f))
        }
        this += descPane
        descPane += this.InstantiatorDesc()

        instantiatorList = InstantiatorList(this, Instantiators.categoryList, Instantiators.categoryMap)
        scrollSelector += instantiatorList
    }


    inner class InstantiatorDesc : Pane() {
        val summary: TextLabel
        val desc: TextLabel
        val allowedTracks: ImageIcon

        init {
            val summaryHeight = 64f
            summary = TextLabel(binding = { instantiatorList.currentItem.use().summary.use() }, font = editorPane.palette.instantiatorSummaryFont).apply { 
                this.bindWidthToParent(adjust = -36f)
                this.bounds.height.set(summaryHeight)
                this.textColor.bind { editorPane.palette.instantiatorSummaryText.use() }
                this.textAlign.set(TextAlign.LEFT)
                this.renderAlign.set(Align.left)
                this.markup.set(editorPane.palette.markupInstantiatorSummary)
                this.padding.set(Insets(0f, 0f, 0f, 4f))
            }
            desc = TextLabel(binding = { instantiatorList.currentItem.use().desc.use() }/*, font = editorPane.palette.instantiatorDescFont*/).apply {
                this.bounds.y.set(summaryHeight)
                this.bindHeightToParent(adjust = -summaryHeight)
                this.textColor.bind { editorPane.palette.instantiatorDescText.use() }
                this.textAlign.set(TextAlign.LEFT)
                this.renderAlign.set(Align.topLeft)
//                this.setScaleXY(0.75f)
                this.margin.set(Insets(8f, 8f, 0f, 0f))
                this.markup.set(editorPane.palette.markupInstantiatorDesc)
            }
            allowedTracks = ImageIcon(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["allowed_tracks"])).apply { 
                this.bounds.width.set(32f)
                this.bounds.height.set(32f)
                Anchor.TopRight.configure(this, offsetX = -2f)
                val allowedTracksVar = Var { instantiatorList.currentItem.use().allowedTracks }
                val tooltipVar = Localization.getVar("editor.instantiators.allowedTracks", Var {
                    val allowedTracks = allowedTracksVar.use()
                    if (allowedTracks == null) {
                        listOf("null")
                    } else if (allowedTracks == EnumSet.allOf(BlockType::class.java)) {
                        listOf(Localization.getValue("editor.instantiators.allowedTracks.all"))
                    } else {
                        listOf(allowedTracks.map {
                            Localization.getValue(it.nameLocalization)
                        }.sorted().joinToString(separator = ", "))
                    }
                })
                this.tooltipElement.set(editorPane.createDefaultTooltip(Var {
                    if (allowedTracksVar.use() == null) "" else tooltipVar.use()
                }))
                this.visible.bind { allowedTracksVar.use() != null }
            }
            this += summary
            this += desc
            this += allowedTracks
        }
    }
}

class InstantiatorList(val instantiatorPane: InstantiatorPane,
                       val categories: List<ListCategory>,
                       val perCategory: Map<String, List<Instantiator<*>>>
                       )
    : Pane() {
    
    data class IndexTween(val index: IntVar = IntVar(0), val tween: FloatVar = FloatVar(0f)) {
        fun instantUpdateTween() {
            tween.set(index.get().toFloat())
        }
    }

    val upperPane: UpperPane get() = instantiatorPane.upperPane
    val editorPane: EditorPane = instantiatorPane.editorPane
    val editor: Editor = instantiatorPane.editor

    val buttonPane: Pane
    val listPane: Pane
    val listView: ListView
    
    val inCategories: BooleanVar = BooleanVar(true)
    
    val currentList: Var<List<ObjectListable>> = Var(categories)
    val categoryIndex: IndexTween = IndexTween()
    val perCategoryIndex: Map<String, IndexTween> = perCategory.keys.associateWith { IndexTween() }
    val currentIndex: ReadOnlyVar<IndexTween> = Var {
        if (inCategories.useB()) categoryIndex else perCategoryIndex.getValue(categories[categoryIndex.index.useI()].categoryID)
    }
    
    val currentItem: ReadOnlyVar<ObjectListable> = Var {
        currentList.use()[currentIndex.use().index.useI()]
    }
    val currentCategory: ReadOnlyVar<ListCategory> = Var {
        categories[categoryIndex.index.useI()]
    }

    init {
        val buttonWidth = 32f

        buttonPane = Pane().apply {
            this.bounds.width.set(buttonWidth + 2f)
            this.margin.set(Insets(0f, 0f, 0f, 2f))
        }
        this += buttonPane
        buttonPane += Button("").apply {
            this.padding.set(Insets.ZERO)
            Anchor.TopLeft.configure(this)
            this.bounds.width.set(buttonWidth)
            this.bounds.height.set(buttonWidth)
            this.skinID.set(EditorSkins.BUTTON_NO_SKIN)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["arrow_long"])).apply {
                this.tint.bind { (skin.use() as ButtonSkin).bgColorToUse.use() }
            }
            this.setOnAction {
                scroll(-1)
            }
            this.disabled.bind {
                currentIndex.use().index.useI() <= 0
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.instantiators.up.tooltip")))
        }
        buttonPane += Button("").apply {
            this.padding.set(Insets.ZERO)
            Anchor.BottomLeft.configure(this)
            this.bounds.width.set(buttonWidth)
            this.bounds.height.set(buttonWidth)
            this.skinID.set(EditorSkins.BUTTON_NO_SKIN)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["arrow_long"])).apply {
                this.tint.bind { (skin.use() as ButtonSkin).bgColorToUse.use() }
                rotation.set(180f)
            }
            this.setOnAction {
                scroll(+1)
            }
            this.disabled.bind {
                currentIndex.use().index.useI() >= (currentList.use().size - 1)
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.instantiators.down.tooltip")))
        }
        buttonPane += Button("").apply {
            Anchor.CentreLeft.configure(this)
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(buttonWidth)
            this.bounds.height.set(buttonWidth)
            this.skinID.set(EditorSkins.BUTTON_NO_SKIN)
            val tooltipCurrentBlock = Localization.getVar("editor.instantiators.currentBlock.tooltip")
            val tooltipCurrentCat = Localization.getVar("editor.instantiators.currentCategory.tooltip")
            this.tooltipElement.set(editorPane.createDefaultTooltip(binding = {
                if (inCategories.useB()) tooltipCurrentCat.use() else tooltipCurrentBlock.use()
            }))
            this += ImageIcon(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["arrow_instantiator_right"]))
            this.setOnAction {
                if (inCategories.get()) {
                    changeToSpecific()
                }
            }
        }
        buttonPane += Button("").apply {
            Anchor.CentreLeft.configure(this, offsetY = -buttonWidth)
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(buttonWidth)
            this.bounds.height.set(buttonWidth)
            this.skinID.set(EditorSkins.BUTTON_NO_SKIN)
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.instantiators.back.tooltip")))
            this += ImageIcon(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["arrow_instantiator_right"]).apply { 
                flip(true, false)
            })
            this.visible.bind { !inCategories.useB() }
            this.setOnAction {
                changeToCategories()
            }
        }

        listPane = Pane().apply {
            bounds.x.set(buttonWidth + 2f)
            bindWidthToParent(-(buttonWidth + 2f))
        }
        this += listPane
        listView = this.ListView()
        listPane += listView
    }

    init {
        listView.addInputEventListener { event ->
            when (event) {
                is ClickPressed -> {
                    val item = currentItem.getOrCompute()
                    if (event.button == Input.Buttons.LEFT) {
                        if (inCategories.get()) {
                            changeToSpecific()
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            editor.attemptInstantiatorDrag(item as Instantiator<Block>)
                        }
                        true
                    } else if (event.button == Input.Buttons.RIGHT) {
                        if (!inCategories.get()) {
                            changeToCategories()
                        }
                        true
                    } else false
                }
                else -> false
            }
        }
        addInputEventListener { event ->
            when (event) {
                is Scrolled -> {
                    scroll(event.amountY.roundToInt())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun changeToCategories() {
        currentList.set(categories)
        inCategories.set(true)
    }
    
    private fun changeToSpecific(category: ListCategory = categories[categoryIndex.index.get()]) {
        currentList.set(perCategory.getValue(category.categoryID))
        inCategories.set(false)
    }
    
    fun selectCertainInstantiator(inst: Instantiator<*>) {
        val cat = Instantiators.instantiatorCategories[inst] ?: return
        changeToCategories()
        val catIndex = categories.indexOfFirst { it.categoryID == cat }
        if (catIndex in categories.indices) {
            categoryIndex.index.set(catIndex)
            categoryIndex.instantUpdateTween()
            changeToSpecific()
            val currentInCatIndex = currentIndex.getOrCompute()
            currentInCatIndex.index.set(currentList.getOrCompute().indexOf(inst).coerceAtLeast(0))
//            currentInCatIndex.instantUpdateTween()
        }
    }

    fun scroll(down: Int) {
        if (down == 0) return
        val current = currentList.getOrCompute()
        val index = currentIndex.getOrCompute()
        val future = (index.index.get() + down).coerceIn(current.indices)
        index.index.set(future)
    }

    inner class ListView : Pane() {

        init {
            this.doClipping.set(true)
        }

        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val currentIndexTween = currentIndex.getOrCompute()
            val currentIndex = currentIndexTween.index.get()
            var indexTween = currentIndexTween.tween.get()
            val indexAsFloat = currentIndex.toFloat()
            if (indexTween != indexAsFloat) {
                indexTween = MathUtils.lerp(indexTween, indexAsFloat, (Gdx.graphics.deltaTime / 0.075f).coerceIn(0f, 1f))
                if (MathUtils.isEqual(indexTween, indexAsFloat, 0.005f)) {
                    indexTween = indexAsFloat
                }
                currentIndexTween.tween.set(indexTween)
            }

            val renderBounds = this.contentZone
            val x = renderBounds.x.get() + originX
            val y = originY - renderBounds.y.get()
            val w = renderBounds.width.get()
            val h = renderBounds.height.get()
            val lastPackedColor = batch.packedColor
            val opacity = apparentOpacity.get()

            val paintboxFont = editorPane.palette.instantiatorNameFont
            val objs = currentList.getOrCompute()

            paintboxFont.useFont { font ->
                val currentTween = indexTween
                val capHeight = font.capHeight
                val lineHeight = font.lineHeight * 1.5f
                val yOffset = -(h * 0.5f) + capHeight * 0.5f + (currentTween * lineHeight)
                objs.forEachIndexed { index, instantiator ->
                    val offsetAmount = abs((currentTween - index)).coerceAtLeast(0f)
                    val xOffset = ((1.6f).pow(offsetAmount) - 1) * 15f
                    val specificOpacity = (1f - offsetAmount / 5f).coerceAtLeast(0.3f) * opacity
                    if (index == currentIndex) {
                        font.setColor(0.65f, 1f, 1f, specificOpacity)
                    } else {
                        font.setColor(1f, 1f, 1f, specificOpacity)
                    }
                    font.drawCompressed(batch, instantiator.name.getOrCompute(), x + xOffset, y - index * lineHeight + yOffset, w, Align.left)
                }
            }

            batch.packedColor = lastPackedColor
        }
    }
}