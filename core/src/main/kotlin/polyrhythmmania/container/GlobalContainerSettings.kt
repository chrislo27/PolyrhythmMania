package polyrhythmmania.container

import polyrhythmmania.world.render.ForceTexturePack
import polyrhythmmania.world.render.ForceTilesetPalette
import polyrhythmmania.world.tileset.TilesetPalette


data class GlobalContainerSettings(
        val forceTexturePack: ForceTexturePack, 
        val forceTilesetPalette: ForceTilesetPalette,
        val reducedMotion: Boolean,
) {

    fun applyForcedTilesetPaletteSettings(container: Container) {
        when (container.globalSettings.forceTilesetPalette) {
            ForceTilesetPalette.NO_FORCE ->
                container.world.tilesetPalette
            ForceTilesetPalette.FORCE_PR1 ->
                TilesetPalette.createGBA1TilesetPalette()
            ForceTilesetPalette.FORCE_PR2 ->
                TilesetPalette.createGBA2TilesetPalette()
            ForceTilesetPalette.ORANGE_BLUE ->
                TilesetPalette.createOrangeBlueTilesetPalette()
        }.applyTo(container.renderer.tileset)
    }
    
}

