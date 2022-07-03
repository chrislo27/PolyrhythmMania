package polyrhythmmania.solitaire

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.binding.invert
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.CheckBox
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.util.gdxutils.set
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.discord.DefaultPresences
import polyrhythmmania.discord.DiscordRichPresence
import polyrhythmmania.screen.mainmenu.menu.MMMenu
import polyrhythmmania.screen.mainmenu.menu.MenuCollection
import polyrhythmmania.screen.mainmenu.menu.StandardMenu
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.statistics.PlayTimeType


class SolitaireMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    private val settings: Settings = main.settings
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

        hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(4f, 0f, 2f, 2f))
            this.bounds.height.set(40f)
        }
        contentPane.addChild(hbox)
        
        val tableau = ImageNode(TextureRegion(AssetRegistry.get<Texture>("solitaire_tableau"))).apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)
            this.doClipping.set(true)
        }
        contentPane.addChild(tableau)
        
        val innerPane = Pane().apply {
            this.padding.set(Insets(24f))
        }
        tableau += innerPane
        gameParent = innerPane
        
        loadingLabel = TextLabel(binding = { Localization.getVar("solitaire.loading").use() }).apply { 
            this.markup.set(this@SolitaireMenu.markup)
            this.renderAlign.set(Align.center)
            this.textColor.set(Color.WHITE.cpy())
        }
        gameParent.addChild(loadingLabel)
    }
    
    private fun createGame(): SolitaireGame {
        GlobalStats.solitaireGamesPlayed.increment()
        return SolitaireGame().apply {
            this.doClipping.set(false)
        }
    }
    
    private fun addMenubarButtons() {
        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.close").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    mainMenu.soundSys.fadeToNormal()
                    menuCol.popLastMenu()
                }
            }
            hbox += TextLabel(binding = {
                Localization.getVar("solitaire.gamesWon", Var {
                    listOf(GlobalStats.solitaireGamesWon.value.use())
                }).use()
            }, font = font).apply {
                this.bounds.width.set(170f)
                this.renderAlign.set(Align.center)
                this.padding.set(Insets(0f, 0f, 4f, 4f))
                this.setScaleXY(0.75f)
                this.tooltipElement.set(createTooltip(Localization.getVar("solitaire.gamesWon.tooltip", Var {
                    listOf(GlobalStats.solitaireGamesPlayed.value.use(),
                        GlobalStats.solitaireGamesWon.value.use(),
                        GlobalStats.solitaireMovesMade.value.use())
                })))
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
            hbox += createSmallButton(binding = { Localization.getVar("solitaire.instructions").use() }).apply {
                this.bounds.width.set(160f)
                this.setOnAction {
                    menuCol.pushNextMenu(menuCol.solitaireHelpMenu, instant = true)
                }
            }
            hbox += CheckBox(binding = { Localization.getVar("solitaire.gameSFX").use() }, font = font).apply {
                this.bounds.width.set(120f)
                this.textLabel.setScaleXY(0.75f)
                this.imageNode.padding.set(Insets(4f, 4f, 4f, 0f))
                this.checkedState.set(settings.solitaireSFX.getOrCompute())
                this.onCheckChanged = {
                    settings.solitaireSFX.invert()
                    settings.persist()
                }
            }
            hbox += CheckBox(binding = { Localization.getVar("solitaire.musicSetting").use() }, font = font).apply {
                this.bounds.width.set(100f)
                this.textLabel.setScaleXY(0.75f)
                this.imageNode.padding.set(Insets(4f, 4f, 4f, 0f))
                this.checkedState.set(settings.solitaireMusic.getOrCompute())
                this.onCheckChanged = {
                    settings.solitaireMusic.invert()
                    settings.persist()
                }
            }
        }
    }

    override fun onMenuEntered() {
        mainMenu.soundSys.fadeToBandpass()
        DiscordRichPresence.updateActivity(DefaultPresences.playingSolitaire())
    }

    override fun onMenuExited() {
        // Fade to normal handled by close button due to being able to view help menu
        mainMenu.setMainMenuRichPresence()
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
        
        GlobalStats.updateModePlayTime(PlayTimeType.SOLITAIRE)
    }
}