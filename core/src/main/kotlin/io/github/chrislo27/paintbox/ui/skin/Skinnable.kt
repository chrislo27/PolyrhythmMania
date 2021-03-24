package io.github.chrislo27.paintbox.ui.skin

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.chrislo27.paintbox.ui.UIElement
import io.github.chrislo27.paintbox.util.ReadOnlyVar
import io.github.chrislo27.paintbox.util.Var


abstract class Skinnable<SELF : Skinnable<SELF>> : UIElement() {

    open val skinFactory: SkinFactory<Skin<SELF>, SELF> by lazy {
        val skinID = getSkinID()
        val skinFactory = DefaultSkins[skinID] ?: error("No registered default skin for for skin ID $skinID")
        skinFactory as SkinFactory<Skin<SELF>, SELF>
    }
    val skin: Skin<SELF> by lazy { 
        skinFactory.createSkin(this as SELF)
    }

    abstract fun getSkinID(): String
    
    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        skin.renderSelf(originX, originY, batch)
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        skin.renderSelfAfterChildren(originX, originY, batch)
    }
}
