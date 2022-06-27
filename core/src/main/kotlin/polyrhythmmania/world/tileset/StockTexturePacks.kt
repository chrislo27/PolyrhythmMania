package polyrhythmmania.world.tileset

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import polyrhythmmania.container.TexturePackSource


/**
 * Should not be instantiated until assets have been loaded.
 */
object StockTexturePacks {
    
    private data class TexturePackAndSource(val texturePack: TexturePack, val source: TexturePackSource)

    val missingTilesetRegion: TilesetRegion by lazy {
        TilesetRegion("MISSING", TextureRegion(AssetRegistry.get<Texture>("tileset_missing_tex")), RegionSpacing.ZERO)
    }
    

    private val gbaSrc: TexturePackAndSource by lazy { 
        TexturePackAndSource(StockTexturePack("gba", emptySet(), AssetRegistry.get<PackedSheet>("tileset_gba")), TexturePackSource.StockGBA)
    }
    val gba: TexturePack by lazy { gbaSrc.texturePack }
    
    private val hdNoFallback: TexturePack by lazy { 
        StockTexturePack("hdNoFallback", emptySet(), AssetRegistry.get<PackedSheet>("tileset_hd")) 
    }
    private val hdSrc: TexturePackAndSource by lazy { 
        TexturePackAndSource(CascadingTexturePack("hd", emptySet(), listOf(hdNoFallback, gba)), TexturePackSource.StockHD)
    }
    val hd: TexturePack by lazy { hdSrc.texturePack }
    
    private val arcadeNoFallback: TexturePack by lazy { 
        StockTexturePack("arcadeNoFallback", emptySet(), AssetRegistry.get<PackedSheet>("tileset_arcade"))
    }
    private val arcadeSrc: TexturePackAndSource by lazy { 
        TexturePackAndSource(CascadingTexturePack("arcade", emptySet(), listOf(arcadeNoFallback, gba)), TexturePackSource.StockArcade)
    }
    val arcade: TexturePack by lazy { arcadeSrc.texturePack }
    
    
    private val allPackAndSources: List<TexturePackAndSource> by lazy {
        listOf(gbaSrc, hdSrc, arcadeSrc)
    }
    private val allPacksToSource: Map<TexturePack, TexturePackSource> by lazy {
        allPackAndSources.associate { it.texturePack to it.source }
    }
    private val allPacksBySource: Map<TexturePackSource, TexturePack> by lazy {
        allPackAndSources.associate { it.source to it.texturePack }
    }
    val allPacks: List<TexturePack> by lazy { allPackAndSources.map { it.texturePack } }
    val allPacksByID: Map<String, TexturePack> by lazy { allPacks.associateBy { it.id } }
    val allPacksByIDWithDeprecations: Map<String, TexturePack> by lazy {
        allPacks.flatMap { p -> p.deprecatedIDs.map { did -> did to p } }.associate { it.first to it.second } + allPacksByID
    }
    
    fun getTexturePackSource(stock: TexturePack): TexturePackSource? = allPacksToSource[stock]
    fun getPackFromSource(source: TexturePackSource): TexturePack? = allPacksBySource[source]

}