package polyrhythmmania.world

import com.badlogic.gdx.graphics.Color
import polyrhythmmania.world.entity.SpriteEntity
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion


open class EntityAsmCube(world: World)
    : SpriteEntity(world) {
    
    constructor(world: World, tint: Color) : this(world) {
        this.tint = tint
    }
    
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        return tileset.asmCube
    }
}

open class EntityAsmLane(world: World)
    : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        return tileset.asmLane
    }
}

open class EntityAsmPerp(world: World, val isTarget: Boolean = false)
    : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        return if (isTarget) tileset.asmCentrePerpTarget else tileset.asmCentrePerp
    }
}
