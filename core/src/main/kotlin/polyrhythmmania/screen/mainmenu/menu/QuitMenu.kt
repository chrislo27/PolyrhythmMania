package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import paintbox.font.TextAlign
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import kotlin.concurrent.thread
import kotlin.system.exitProcess


class QuitMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    init {
        this.setSize(MMMenu.WIDTH_SMALL)
        this.titleText.bind { Localization.getVar("mainMenu.quit.title").use() }
        this.contentPane.bounds.height.set(200f)
        
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
            vbox += TextLabel(binding = { Localization.getVar("mainMenu.quit.desc").use() }).apply {
                this.markup.set(this@QuitMenu.markup)
                this.bounds.height.set(80f)
                this.padding.set(Insets(4f))
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
                this.renderAlign.set(Align.topLeft)
                this.textAlign.set(TextAlign.LEFT)
                this.doLineWrapping.set(true)
            }
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("mainMenu.quit.confirm").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    Gdx.app.exit()
                    thread(isDaemon = true) {
                        Thread.sleep(1000L)
                        exitProcess(0)
                    }
                }
            }
            hbox += createSmallButton(binding = { Localization.getVar("common.cancel").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
        }
    }
}