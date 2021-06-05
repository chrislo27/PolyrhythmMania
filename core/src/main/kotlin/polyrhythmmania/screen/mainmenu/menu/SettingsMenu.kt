package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.layout.HBox
import io.github.chrislo27.paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import kotlin.concurrent.thread
import kotlin.system.exitProcess


class SettingsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    init {
        this.setSize(MMMenu.WIDTH_EXTRA_SMALL)
        this.titleText.bind { Localization.getVar("mainMenu.settings.title").use() }
        this.contentPane.bounds.height.set(300f)
        
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
            vbox += createLongButton { Localization.getVar("mainMenu.settings.audio").use() }.apply {
                this.disabled.set(true)
            }
            vbox += createLongButton { Localization.getVar("mainMenu.settings.video").use() }.apply {
                this.disabled.set(true)
            }
            vbox += createLongButton { Localization.getVar("mainMenu.settings.input").use() }.apply { 
                this.disabled.set(true)
            }
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
        }
    }
}