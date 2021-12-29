package polyrhythmmania.solitaire

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import paintbox.ui.Anchor
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.util.gdxutils.set
import polyrhythmmania.Localization
import polyrhythmmania.screen.mainmenu.menu.MMMenu
import polyrhythmmania.screen.mainmenu.menu.MenuCollection
import polyrhythmmania.screen.mainmenu.menu.StandardMenu


class SolitaireMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private var loading: Boolean = true
    private val loadingLabel: TextLabel
    private val hbox: HBox
    private val gameParent: UIElement
    private lateinit var game: SolitaireGame

    init {
        this.setSize(MMMenu.WIDTH_LARGE, adjust = 16f)
        this.titleText.bind { Localization.getVar("solitaire.title").use() }
        this.contentPane.bounds.height.set(520f)
        this.showLogo.set(false)
        this.deleteWhenPopped.set(true)

        hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }
        contentPane.addChild(hbox)

        val rect = RectElement(Color().set(0, 80, 53)).apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)
            this.padding.set(Insets(24f))
            this.doClipping.set(true)
        }
        contentPane.addChild(rect)
        gameParent = rect
        
        loadingLabel = TextLabel(binding = { Localization.getVar("solitaire.loading").use() }).apply { 
            this.markup.set(this@SolitaireMenu.markup)
            this.renderAlign.set(Align.center)
            this.textColor.set(Color.WHITE.cpy())
        }
        gameParent.addChild(loadingLabel)
    }
    
    private fun createGame(): SolitaireGame = SolitaireGame().apply {
        this.doClipping.set(false)
    }
    
    private fun addMenubarButtons() {
        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.close").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("solitaire.newGame").use() }).apply {
                this.bounds.width.set(150f)
                this.setOnAction {
                    val oldGame = game
                    gameParent.removeChild(oldGame)
                    game = createGame()
                    gameParent.addChild(game)
                }
            }

        }
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        if (loading) {
            val progress = SolitaireAssets.load(Gdx.graphics.deltaTime)
            if (progress >= 1f) {
                loading = false

                loadingLabel.visible.set(false)
                gameParent.removeChild(loadingLabel)

                game = createGame()
                gameParent.addChild(game)
                addMenubarButtons()
            }
        }
        
        super.renderSelf(originX, originY, batch)
    }
}