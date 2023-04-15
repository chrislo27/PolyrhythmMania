package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.CheckBox
import paintbox.ui.control.ComboBox
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.UpdateNotesL10N
import polyrhythmmania.ui.PRManiaSkins


class UpdateNotesMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    companion object {
        private val TEXT_COLOR: Color = LongButtonSkin.TEXT_COLOR
        
        val updates: List<String> = listOf("v1.1", "v1.2", "v2.0")
        val latestUpdate: String = updates.last()
    }
    
    private val settings: Settings get() = main.settings
    private val viewingUpdate: Var<String> = Var(latestUpdate)
    
    private val descLabel: TextLabel
    private val scrollPane: ScrollPane

    init {
        this.setSize(0.64f)
        val titleVar = Localization.getVar("mainMenu.updateNotes.title", Var { listOf(viewingUpdate.use()) })
        this.titleText.bind { titleVar.use() }
        this.contentPane.bounds.height.set(520f)
        this.showLogo.set(false)

        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(4f, 0f, 2f, 2f))
            this.bounds.height.set(40f)
        }
        contentPane.addChild(hbox)
        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.close").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            hbox += TextLabel(binding = { Localization.getVar("mainMenu.updateNotes.selectVersion").use() }, font = font).apply {
                this.bounds.width.set(130f)
                this.renderAlign.set(Align.right)
                this.padding.set(Insets(0f, 0f, 4f, 0f))
                this.setScaleXY(0.75f)
            }
            hbox += ComboBox(updates.asReversed(), latestUpdate, font = font).apply { 
                this.bounds.width.set(100f)
                this.setScaleXY(0.85f)
                this.onItemSelected = { newItem ->
                    viewingUpdate.set(newItem)
                }
            }
            hbox += CheckBox(binding = { Localization.getVar("mainMenu.updateNotes.dontShowAgain").use() }, font = font).apply {
                this.bounds.width.set(160f)
                this.textLabel.setScaleXY(0.5f)
                this.textLabel.doLineWrapping.set(true)
                this.imageNode.padding.set(Insets(4f, 4f, 4f, 0f))
                this.checkedState.set(settings.lastUpdateNotes.getOrCompute() == latestUpdate)
                this.onCheckChanged = {
                    settings.lastUpdateNotes.set(if (it) latestUpdate else "")
                    settings.persist()
                }
            }
        }

        scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)
            
            this.vBar.unitIncrement.set(10f)
            this.vBar.blockIncrement.set(40f)
        }
        contentPane.addChild(scrollPane)

//        val vbox = VBox().apply {
//            Anchor.TopLeft.configure(this)
//            this.bounds.height.set(300f)
//            this.spacing.set(0f)
//        }
//
//        vbox.temporarilyDisableLayouts {
//        }
//        vbox.sizeHeightToChildren(100f)

        val descVar: ReadOnlyVar<String> = Var {
            val u = viewingUpdate.use()
            UpdateNotesL10N.getVar("updateNotes.${u}.desc").use()
        }
        descLabel = TextLabel(text = "" /* Bound later */, font = font).apply {
            this.autosizeBehavior.set(TextLabel.AutosizeBehavior.Active(TextLabel.AutosizeBehavior.Dimensions.HEIGHT_ONLY))
            Anchor.TopLeft.configure(this)
            this.bindWidthToParent(adjust = 0f, multiplier = 1f)
            this.bounds.x.set(0f)
            this.padding.set(Insets(12f, 12f, 6f, 6f))
            this.markup.set(this@UpdateNotesMenu.markup)
            this.textColor.set(TEXT_COLOR)
            this.renderAlign.set(Align.topLeft)
            this.textAlign.set(TextAlign.LEFT)
            this.doLineWrapping.set(true)
            this.setScaleXY(0.75f)
            this.text.bind {
                descVar.use() + "\n"
            }
        }
        scrollPane.setContent(descLabel)
    }

    fun prepareShow(): UpdateNotesMenu {
        descLabel.resizeBoundsToContent(affectWidth = false)
        scrollPane.setContent(descLabel)
        return this
    }
}