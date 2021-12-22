package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import paintbox.binding.FloatVar
import paintbox.font.TextAlign
import paintbox.lazysound.LazySound
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.world.entity.EntityExplosion
import kotlin.math.ceil
import kotlin.math.roundToInt


class ConfirmResettingAchievementsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    private val unlockButtonIn: FloatVar = FloatVar(5f)
    
    init {
        this.setSize(0.45f)
        this.titleText.bind { Localization.getVar("mainMenu.confirmResettingAchievements.title").use() }
        this.contentPane.bounds.height.set(250f)
        this.deleteWhenPopped.set(true)

        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(0f)
            this.bindHeightToParent(-40f)
        }
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bounds.height.set(40f)
        }

        contentPane.addChild(vbox)
        contentPane.addChild(hbox)

        vbox.temporarilyDisableLayouts {
            vbox += TextLabel(binding = { Localization.getVar("mainMenu.confirmResettingAchievements.desc").use() }).apply {
                this.markup.set(this@ConfirmResettingAchievementsMenu.markup)
                this.bounds.height.set(80f)
                this.padding.set(Insets(4f))
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
                this.renderAlign.set(Align.topLeft)
                this.textAlign.set(TextAlign.LEFT)
                this.doLineWrapping.set(true)
            }
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.cancel").use() }).apply {
                this.bounds.width.set(120f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            hbox += createSmallButton(binding = { 
                val unlockTimer = unlockButtonIn.use()
                if (unlockTimer > 0f) {
                    "(${ceil(unlockTimer).roundToInt()})"
                } else Localization.getVar("mainMenu.confirmResettingAchievements.confirmButton").use()
            }).apply {
                this.bounds.width.set(300f)
                this.setOnAction {
                    // TODO reset achievements
                    GlobalStats.resetToResetValues()
                    GlobalStats.persist()
                    
                    menuCol.mainMenu.resetExplosionEffect = EntityExplosion.EXPLOSION_DURATION
                    menuCol.playMenuSound(AssetRegistry.get<LazySound>("sfx_reset_achievements").sound)
                    menuCol.popLastMenu()
                }
                this.disabled.bind { unlockButtonIn.use() > 0f }
            }
        }

    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelfAfterChildren(originX, originY, batch)
        if (unlockButtonIn.get() > 0f) {
            unlockButtonIn.set(unlockButtonIn.get() - Gdx.graphics.deltaTime)
        }
    }
}