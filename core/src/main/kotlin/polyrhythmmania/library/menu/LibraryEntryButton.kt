package polyrhythmmania.library.menu

import com.badlogic.gdx.graphics.Color
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.font.TextBlock
import paintbox.font.TextRun
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.Button
import paintbox.ui.control.Toggle
import paintbox.ui.control.ToggleGroup
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.SkinFactory
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.library.LevelEntry

class LibraryEntryButton(val libraryMenu: LibraryMenu, val levelEntry: LevelEntry, toggleGroup: ToggleGroup)
    : Button("", PRManiaGame.instance.fontMainMenuMain), Toggle {

    companion object {
        val LIBRARY_BUTTON_SKIN_ID: String = "LibraryEntryButton"

        init {
            DefaultSkins.register(LIBRARY_BUTTON_SKIN_ID, SkinFactory { element: LibraryEntryButton ->
                LibraryEntryButtonSkin(element)
            })
        }
    }

    override val selectedState: BooleanVar = BooleanVar(false)
    override val toggleGroup: Var<ToggleGroup?> = Var(toggleGroup)
    
    init {
        val main = libraryMenu.main
        val title = levelEntry.getTitle()
        val subtitle = levelEntry.getSubtitle()
        if (levelEntry is LevelEntry.Legacy) {
            this.internalTextBlock.set(TextBlock(listOf(
                    TextRun(main.fontMainMenuMain, Localization.getValue("mainMenu.library.legacyIndicator") + " ", scaleX = 0.9f, scaleY = 0.9f),
                    TextRun(main.fontMainMenuThin, title, scaleX = 0.9f, scaleY = 0.9f),
            )))
        } else if (levelEntry is LevelEntry.Modern) {
            this.internalTextBlock.set(TextBlock(listOf(
                    TextRun(main.fontMainMenuRodin, title, scaleX = 0.9f, scaleY = 0.9f),
                    TextRun(main.fontMainMenuRodin, if (subtitle.isBlank()) "" else "\n$subtitle", scaleX = 0.75f, scaleY = 0.75f),
            )))
        }
        
        this.borderStyle.set(SolidBorder(Color.DARK_GRAY))
    }

    override fun getDefaultSkinID(): String {
        return LIBRARY_BUTTON_SKIN_ID
    }
}
