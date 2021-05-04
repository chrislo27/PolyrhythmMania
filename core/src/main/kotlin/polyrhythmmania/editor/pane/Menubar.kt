package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.ImageNode
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.ButtonSkin
import polyrhythmmania.editor.Editor


class Menubar(val editorPane: EditorPane) : Pane() {
    
    val editor: Editor = editorPane.editor
    
    init {
        this += Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f + 4f)
            this.margin.set(Insets(0f, 0f, 0f, 4f))
            Anchor.TopLeft.configure(this, offsetX = 0f, offsetY = 0f)
            this.skinID.set(EditorSkins.BUTTON)
//            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_icon_button_new"))).apply {
//                this.tint.bind { editorPane.palette.menubarIconTint.use() }
//            }
            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_icon_buttons_editor"), 32, 0, 16, 16)).apply {
//                this.tint.bind { editorPane.palette.menubarIconTint.use() }
            }
        }
        this += Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f + 4f)
            this.margin.set(Insets(0f, 0f, 0f, 4f))
            Anchor.TopLeft.configure(this, offsetX = 32f * 1 + 4f * 1, offsetY = 0f)
            this.skinID.set(EditorSkins.BUTTON)
//            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_icon_button_open"))).apply {
//                this.tint.bind { editorPane.palette.menubarIconTint.use() }
//            }
            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_icon_buttons_editor"), 16, 0, 16, 16)).apply {
//                this.tint.bind { editorPane.palette.menubarIconTint.use() }
            }
        }
        this += Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f + 4f)
            this.margin.set(Insets(0f, 0f, 0f, 4f))
            Anchor.TopLeft.configure(this, offsetX = 32f * 2 + 4f * 2, offsetY = 0f)
            this.skinID.set(EditorSkins.BUTTON)
//            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_icon_button_save"))).apply {
//                this.tint.bind { editorPane.palette.menubarIconTint.use() }
//            }
            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_icon_buttons_editor"), 0, 0, 16, 16)).apply {
//                this.tint.bind { editorPane.palette.menubarIconTint.use() }
            }
        }
    }
}