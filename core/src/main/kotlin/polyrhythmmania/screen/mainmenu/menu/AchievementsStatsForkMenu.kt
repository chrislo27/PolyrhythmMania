package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import paintbox.ui.Anchor
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization


class AchievementsStatsForkMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    init {
        this.setSize(0.45f)
        this.titleText.bind { Localization.getVar("mainMenu.achievementsStatsFork.title").use() }
        this.contentPane.bounds.height.set(250f)

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
            fun separator(): UIElement {
                return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                    this.bounds.height.set(10f)
                    this.margin.set(Insets(4f, 4f, 0f, 0f))
                }
            }
            
            vbox += createLongButton { Localization.getVar("mainMenu.achievements.title").use() }.apply {
                this.setOnAction {
                    
                }
            }
            vbox += createLongButton { Localization.getVar("mainMenu.statistics.title").use() }.apply {
                this.setOnAction {
                    
                }
                this.disabled.set(true)
            }
            
            vbox += separator()
            
            vbox += createLongButton { Localization.getVar("mainMenu.achievementsStatsFork.delete").use() }.apply {
                this.setOnAction {
                    menuCol.pushNextMenu(ConfirmResettingAchievementsMenu(menuCol).also { menuCol.addMenu(it) })
                }
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