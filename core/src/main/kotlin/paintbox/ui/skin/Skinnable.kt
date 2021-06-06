package paintbox.ui.skin

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.ui.UIElement


abstract class Skinnable<SELF : Skinnable<SELF>> : UIElement() {

    val skinID: Var<String> by lazy { Var(getDefaultSkinID()) }
    val skinFactory: Var<SkinFactory<Skin<SELF>, SELF>> by lazy {
        Var.bind {
            val skinID = skinID.use()
            val skinFactory = DefaultSkins[skinID] ?: error("No registered default skin for for skin ID $skinID")
            skinFactory as SkinFactory<Skin<SELF>, SELF>
        }
    }
    val skin: ReadOnlyVar<Skin<SELF>> by lazy { 
        Var.bind { skinFactory.use().createSkin(this@Skinnable as SELF) }
    }

    abstract fun getDefaultSkinID(): String
    
    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        skin.getOrCompute().renderSelf(originX, originY, batch)
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        skin.getOrCompute().renderSelfAfterChildren(originX, originY, batch)
    }
}
