package polyrhythmmania.world.tileset

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry


/**
 * Should not be instantiated until assets have been loaded.
 */
object StockTexturePacks {

    val missingTilesetRegion: TilesetRegion by lazy { TilesetRegion("MISSING", TextureRegion(AssetRegistry.get<Texture>("tileset_missing_tex"))) }

    val gba: TexturePack by lazy { StockTexturePack("gba", AssetRegistry.get<PackedSheet>("tileset_gba")) }
    private val hdNoFallback: TexturePack by lazy { StockTexturePack("hdNoFallback", AssetRegistry.get<PackedSheet>("tileset_hd")) }
    val hd: TexturePack by lazy { CascadingTexturePack("hd", listOf(hdNoFallback, gba)) }

}