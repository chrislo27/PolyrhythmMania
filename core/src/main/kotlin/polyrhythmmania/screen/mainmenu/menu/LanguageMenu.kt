package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.utils.Align
import paintbox.i18n.ILocalizationWithBundle
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.LocalePicker
import polyrhythmmania.Localization
import polyrhythmmania.credits.Credits
import polyrhythmmania.ui.PRManiaSkins
import java.text.NumberFormat
import kotlin.math.roundToInt


class LanguageMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {

    private val bundledLocalizations: List<ILocalizationWithBundle> =
        main.allLocalizations.filterIsInstance<ILocalizationWithBundle>()
    
    init {
        this.setSize(MMMenu.WIDTH_SMALL)
        this.titleText.bind { Localization.getVar("mainMenu.language.title").use() }
        this.contentPane.bounds.height.set(300f)

        val scrollPane = ScrollPane().apply {
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
        val hbox = HBox().apply {
            Anchor.BottomLeft.configure(this)
            this.spacing.set(8f)
            this.padding.set(Insets(4f, 0f, 2f, 2f))
            this.bounds.height.set(40f)
        }

        contentPane.addChild(scrollPane)
        contentPane.addChild(hbox)

        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(16f)
            this.bindHeightToParent(-40f)
        }
        vbox.temporarilyDisableLayouts {
            vbox += TextLabel(binding = { Localization.getVar("mainMenu.language.inaccuracy").use() }).apply {
                this.markup.set(this@LanguageMenu.markup)
                this.autosizeBehavior.set(TextLabel.AutosizeBehavior.Active(TextLabel.AutosizeBehavior.Dimensions.HEIGHT_ONLY))
                this.renderAlign.set(Align.topLeft)
                this.doLineWrapping.set(true)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
                this.setScaleXY(0.75f)
            }

            val locales = LocalePicker.namedLocales
            val (comboboxPane, combobox) = createComboboxOption(locales, LocalePicker.currentLocale.getOrCompute(), {
                Localization.getVar("mainMenu.language.title").use()
            }, itemToString = { it.name }, percentageContent = 0.6f)
            combobox.onItemSelected = { newItem ->
                LocalePicker.currentLocale.set(newItem)
            }
            vbox += comboboxPane
            
            vbox += TextLabel(binding = { 
                val numBaseKeys = bundledLocalizations.sumOf { locBase ->
                    locBase.bundles.use().find { b -> 
                        b.namedLocale.locale.language == ""
                    }?.allKeys?.size ?: 0
                }
                val currentLocale = LocalePicker.currentLocale.use()
                when (currentLocale.locale.language) {
                    "", "en" -> {
                        // Note: This text is intentionally not localized
                        "This is the default language for the game. Did you know there are ${NumberFormat.getIntegerInstance(currentLocale.locale).format(numBaseKeys)} strings in the game that can be translated? That's a lot!"
                    }
                    else -> {
                        val numKeys = bundledLocalizations.sumOf { locBase ->
                            val bundle = locBase.bundlesMap.use()[currentLocale]?.takeIf { 
                                it.namedLocale.locale.language == it.bundle.locale.language
                            }
                            bundle?.allKeys?.size ?: 0
                        }
                        val percentageDec = numKeys.toFloat() / numBaseKeys
                        val percentageInt = if (percentageDec > 0f && percentageDec < 0.01f) 1 else (percentageDec * 100).roundToInt()
                        Localization.getVar("mainMenu.language.stats", listOf(
                                percentageInt, numKeys, numBaseKeys,
                                (Credits.languageCredits[currentLocale.locale]?.let { 
                                    it.primaryLocalizers + it.secondaryLocalizers
                                } ?: listOf("<missing language credit>")).joinToString(separator = ", ")
                        )).use()
                    }
                }
            }).apply {
                this.markup.set(this@LanguageMenu.markup)
                this.autosizeBehavior.set(TextLabel.AutosizeBehavior.Active(TextLabel.AutosizeBehavior.Dimensions.HEIGHT_ONLY))
                this.renderAlign.set(Align.topLeft)
                this.doLineWrapping.set(true)
                this.textColor.set(LongButtonSkin.TEXT_COLOR)
                this.margin.set(Insets(8f, 0f, 0f, 0f))
                this.setScaleXY(0.75f)
            }
        }
        
        vbox.sizeHeightToChildren(100f)
        scrollPane.setContent(vbox)

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