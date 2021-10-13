package polyrhythmmania.library.menu

import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.ui.control.Button
import paintbox.ui.control.Toggle
import paintbox.ui.control.ToggleGroup
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.SkinFactory
import polyrhythmmania.PRManiaGame

class LibraryEntryButton(val libraryMenu: LibraryMenu)
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
    override val toggleGroup: Var<ToggleGroup?> = Var(null)
}
