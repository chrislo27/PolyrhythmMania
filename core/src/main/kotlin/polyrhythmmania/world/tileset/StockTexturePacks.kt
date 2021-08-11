package polyrhythmmania.world.tileset

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry


/**
 * Should not be instantiated until assets have been loaded.
 */
object StockTexturePacks {

    val missingTilesetRegion: TilesetRegion by lazy {
        TilesetRegion("MISSING", TextureRegion(AssetRegistry.get<Texture>("tileset_missing_tex")), RegionSpacing.ZERO)
    }

    val gba: TexturePack by lazy { StockTexturePack("gba", emptySet(), AssetRegistry.get<PackedSheet>("tileset_gba")) }
    private val hdNoFallback: TexturePack by lazy { StockTexturePack("hdNoFallback", emptySet(), AssetRegistry.get<PackedSheet>("tileset_hd")) }
    val hd: TexturePack by lazy { CascadingTexturePack("hd", emptySet(), listOf(hdNoFallback, gba)) }
    
    val allPacks: List<TexturePack> by lazy {
        listOf(gba, hd)
    }
    val allPacksByID: Map<String, TexturePack> by lazy {
        allPacks.associateBy { it.id }
    }
    val allPacksByIDWithDeprecations: Map<String, TexturePack> by lazy {
        allPacks.flatMap { p -> p.deprecatedIDs.map { did -> did to p } }.associate { it.first to it.second } + allPacksByID
    }

}