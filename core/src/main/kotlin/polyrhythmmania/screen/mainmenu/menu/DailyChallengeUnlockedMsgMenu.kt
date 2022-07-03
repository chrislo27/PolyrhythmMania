package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.utils.Align
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import polyrhythmmania.Localization


class DailyChallengeUnlockedMsgMenu(
        menuCol: MenuCollection,
) : StandardMenu(menuCol) {
    
    init {
        this.setSize(MMMenu.WIDTH_MEDIUM)
        this.titleText.bind { Localization.getVar("mainMenu.dailyChallengeUnlocked.title").use() }
        this.contentPane.bounds.height.set(250f)
        this.deleteWhenPopped.set(true)

        contentPane.addChild(TextLabel(Localization.getVar("mainMenu.dailyChallengeUnlocked.desc")).apply {
            this.bindHeightToParent(adjust = -40f)
            this.markup.set(this@DailyChallengeUnlockedMsgMenu.markup)
            this.textColor.set(LongButtonSkin.TEXT_COLOR)
            this.renderAlign.set(Align.topLeft)
            this.doLineWrapping.set(true)
        })

        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(4f, 0f, 2f, 2f))
            this.bounds.height.set(40f)
        }
        contentPane.addChild(hbox)
        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.ok").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
        }
    }
}