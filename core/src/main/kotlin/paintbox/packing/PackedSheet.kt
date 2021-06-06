package paintbox.packing

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import paintbox.Paintbox
import paintbox.util.gdxutils.disposeQuietly


/**
 * Represents a set of textures that are packed at runtime and addressable via an ID.
 */
class PackedSheet(val config: Config, initial: List<Packable> = emptyList()) : Disposable {

    private class PackResult(val atlas: TextureAtlas, val originalPackables: List<Packable>, val timeTaken: Double)
        : Disposable {

        val regions: Map<String, TextureAtlas.AtlasRegion> = atlas.regions.associateBy { it.name }

        init {
            // Consistency check
            val keys = regions.keys
            originalPackables.forEach { p ->
                if (p.id !in keys)
                    error("PackedSheet data inconsistency: pack result was missing ${p.id}. Original: ${originalPackables.map { it.id }}, regions: $keys")
            }
        }

        override fun dispose() {
            atlas.dispose()
        }
    }

    private val packables: MutableMap<String, Packable> = mutableMapOf()
    private var atlas: PackResult? = null

    init {
        initial.forEach { p ->
            addPackable(p)
        }
    }

    fun addPackable(packable: Packable) {
        packables[packable.id] = packable
    }

    fun removePackable(id: String) {
        packables.remove(id)
    }

    fun removePackable(packable: Packable) {
        packables.remove(packable.id, packable)
    }

    fun pack() {
        atlas?.dispose()
        atlas = null

        val nano = System.nanoTime()
        val size = config.maxSize
        val packer = PixmapPacker(size, size, config.format, config.padding, config.duplicateBorder, config.packStrategy)
        val packables = packables.values.toList()
        packables.forEach { p ->
            val tex = p.obtainTexture()
            val td = tex.textureData
            if (!td.isPrepared) td.prepare()
            val pixmap = td.consumePixmap()

            packer.pack(p.id, pixmap)

            if (td.disposePixmap()) {
                pixmap.dispose()
            }
            if (p.shouldDisposeTexture()) {
                tex.dispose()
            }
        }

        val newAtlas = packer.generateTextureAtlas(config.atlasMinFilter, config.atlasMagFilter, config.atlasMipMaps)

        packer.dispose()
        val endNano = System.nanoTime()
        val result = PackResult(newAtlas, packables, (endNano - nano) / 1_000_000.0)
        this.atlas = result
//        println("Took ${result.timeTaken} ms to pack ${packables.size} packables")

        val outputFile = config.debugOutputFile
        if (outputFile != null) {
            val regions = newAtlas.regions
            if (regions.size > 0) {
                if (regions.size > 1) Paintbox.LOGGER.debug("Packed sheet output has ${regions.size} regions, only first one will be outputted to $outputFile")
                val td = regions.first().texture.textureData
                if (!td.isPrepared) {
                    td.prepare()
                }
                val pix = td.consumePixmap()
                PixmapIO.writePNG(outputFile, pix)
                if (td.disposePixmap()) {
                    pix.disposeQuietly()
                }
            }
        }
    }

    operator fun get(id: String): TextureAtlas.AtlasRegion {
        val atlas = this.atlas
        return if (atlas != null) {
            atlas.regions[id] ?: error("No atlas region found with ID $id")
        } else error("Atlas was not loaded. Call pack() first")
    }

    override fun dispose() {
        atlas?.dispose()
        atlas = null
    }

    data class Config(
            val maxSize: Int = 1024, val format: Pixmap.Format = Pixmap.Format.RGBA8888, val padding: Int = 2,
            val duplicateBorder: Boolean = true,
            val packStrategy: PixmapPacker.PackStrategy = PixmapPacker.GuillotineStrategy(),
            val atlasMinFilter: Texture.TextureFilter = Texture.TextureFilter.Nearest,
            val atlasMagFilter: Texture.TextureFilter = Texture.TextureFilter.Nearest,
            val atlasMipMaps: Boolean = false,
            val debugOutputFile: FileHandle? = null,
    )
}

/**
 * Used by [PackedSheet] to accept [Texture]s to pack.
 */
interface Packable {

    companion object {
        operator fun invoke(id: String, fileHandle: FileHandle): Packable {
            return TemporaryPackableTex(id, fileHandle)
        }

        operator fun invoke(id: String, internalPath: String): Packable {
            return TemporaryPackableTex(id, internalPath)
        }
    }

    val id: String

    fun obtainTexture(): Texture

    fun shouldDisposeTexture(): Boolean

}

/**
 * Wraps a [Texture]. It is recommended to use the extension function [Texture.asPackable].
 */
class PackableTextureWrapper(override val id: String, val texture: Texture) : Packable {
    override fun obtainTexture(): Texture = this.texture

    override fun shouldDisposeTexture(): Boolean = false
}

/**
 * Wraps a [Texture] to be [Packable].
 */
fun Texture.asPackable(id: String): Packable = PackableTextureWrapper(id, this)

/**
 * An implementation of [Packable] that loads the [Texture] given by the [fileHandle], then disposes it immediately after.
 * It is recommended to use the [Packable.Companion.invoke] functions to retrieve implementations.
 */
class TemporaryPackableTex(override val id: String, val fileHandle: FileHandle) : Packable {

    constructor(id: String, internalPath: String) : this(id, Gdx.files.internal(internalPath))

    override fun obtainTexture(): Texture {
        return Texture(fileHandle)
    }

    override fun shouldDisposeTexture(): Boolean = true
}

class PackedSheetLoader(resolver: FileHandleResolver)
    : AsynchronousAssetLoader<PackedSheet, PackedSheetLoader.PackedSheetLoaderParam>(resolver) {

    class PackedSheetLoaderParam(
            val packables: List<Packable> = emptyList(),
            val config: PackedSheet.Config = PackedSheet.Config()
    ) : AssetLoaderParameters<PackedSheet>()

    override fun getDependencies(fileName: String?, file: FileHandle?, parameter: PackedSheetLoaderParam?): Array<AssetDescriptor<Any>>? {
        return null
    }

    override fun loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: PackedSheetLoaderParam?) {
        // Nothing to load async.
    }

    override fun loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: PackedSheetLoaderParam?): PackedSheet {
        val param = parameter ?: PackedSheetLoaderParam()
        val packedSheet = PackedSheet(param.config, param.packables)
        packedSheet.pack()
        return packedSheet
    }
}
