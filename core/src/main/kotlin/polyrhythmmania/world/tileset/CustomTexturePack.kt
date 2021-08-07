package polyrhythmmania.world.tileset

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.WriterConfig
import com.twelvemonkeys.imageio.plugins.tga.TGAImageWriteParam
import net.lingala.zip4j.ZipFile
import paintbox.util.Version
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRMania
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam


class CustomTexturePack(id: String, var fallbackID: String)
    : TexturePack(id, emptySet()), Disposable {
    
    companion object {
        const val TEXTURE_PACK_VERSION: Int = 0
        
        // If this list is updated, update the categorical list in TexturePackEditDialog
        val ALLOWED_LIST: List<String> = listOf(
                        "cube_border",
                        "cube_border_platform",
                        "cube_border_z",
                        "cube_face_x",
                        "cube_face_y",
                        "cube_face_z",
                        "explosion_0",
                        "explosion_1",
                        "explosion_2",
                        "explosion_3",
                        "indicator_a",
                        "indicator_dpad",
                        "input_feedback_0",
                        "input_feedback_1",
                        "input_feedback_2",
                        "piston_a",
                        "piston_a_extended",
                        "piston_a_extended_face_x",
                        "piston_a_extended_face_z",
                        "piston_a_partial",
                        "piston_a_partial_face_x",
                        "piston_a_partial_face_z",
                        "piston_dpad",
                        "piston_dpad_extended",
                        "piston_dpad_extended_face_x",
                        "piston_dpad_extended_face_z",
                        "piston_dpad_partial",
                        "piston_dpad_partial_face_x",
                        "piston_dpad_partial_face_z",
                        "platform",
                        "platform_with_line",
                        "red_line",
                        "rods_borders",
                        "rods_fill",
                        "sign_a",
                        "sign_a_shadow",
                        "sign_bo",
                        "sign_bo_shadow",
                        "sign_dpad",
                        "sign_dpad_shadow",
                        "sign_n",
                        "sign_n_shadow",
                        "sign_ta",
                        "sign_ta_shadow",
        )
        
        fun textureFilterToID(textureFilter: Texture.TextureFilter): String {
            return when (textureFilter) {
                Texture.TextureFilter.Nearest -> "nearest"
                Texture.TextureFilter.Linear -> "linear"
                Texture.TextureFilter.MipMapNearestNearest -> "nearest"
                Texture.TextureFilter.MipMapLinearNearest -> "linear"
                Texture.TextureFilter.MipMapNearestLinear -> "nearest"
                Texture.TextureFilter.MipMap, Texture.TextureFilter.MipMapLinearLinear -> "linear"
            }
        }
        
        fun getTextureFilterFromID(id: String): Texture.TextureFilter {
            return when (id) {
                "linear" -> Texture.TextureFilter.Linear
                "nearest" -> Texture.TextureFilter.Nearest
                else -> Texture.TextureFilter.Linear
            }
        }

        fun readFromStream(zipFile: ZipFile): ReadResult {
            val json: JsonObject
            zipFile.getInputStream(zipFile.getFileHeader("texture_pack.json")).use { zipInputStream ->
                val reader = zipInputStream.reader()
                json = Json.parse(reader).asObject()
            }
            
            val id: String = json.get("id").asString()
            val fallbackID: String = json.get("fallbackID").asString()
            val formatVersion: Int = json.getInt("formatVersion", 0)
            val programVersion: Version? = Version.parse(json.getString("programVersion", null))
            
            val texturesObj: JsonObject = json.get("textures").asObject()
            val textureMetadata: MutableList<ReadResult.TextureMetadata> = mutableListOf()
            texturesObj.forEach { member ->
                val o = member.value.asObject()
                val texID = o.get("id").asString()
                val tgaStream = zipFile.getInputStream(zipFile.getFileHeader("textures/${texID}.tga"))
                val tgaBytes = tgaStream.readBytes()
                tgaStream.close()
                val pixmap = Pixmap(tgaBytes, 0, tgaBytes.size)
                textureMetadata += ReadResult.TextureMetadata(texID, pixmap,
                        getTextureFilterFromID(o.getString("magFilter", "linear")),
                        getTextureFilterFromID(o.getString("minFilter", "linear")))
            }
            
            val regionsObj: JsonObject = json.get("regions").asObject()
            val regionMetadata: MutableList<ReadResult.RegionMetadata> = mutableListOf()
            regionsObj.forEach { member ->
                val o = member.value.asObject()
                val regID = o.get("id").asString()
                val spacingObj = o.get("spacing")?.asObject()
                regionMetadata += ReadResult.RegionMetadata(regID, o.getString("tex", "???"),
                        o.getInt("x", 0), o.getInt("y", 0), o.getInt("w", 0), o.getInt("h", 0),
                        if (spacingObj != null) RegionSpacing.readJson(spacingObj) else RegionSpacing.ZERO)
            }
            
            return ReadResult(id, fallbackID, formatVersion, programVersion, textureMetadata, regionMetadata)
        }
    }

    class ReadResult(val id: String, val fallbackID: String, val formatVersion: Int, val programVersion: Version?,
                     val textures: List<TextureMetadata>, val regions: List<RegionMetadata>) {
        
        data class TextureMetadata(val id: String, val pixmap: Pixmap,
                                   val magFilter: Texture.TextureFilter, val minFilter: Texture.TextureFilter)
        
        data class RegionMetadata(val id: String, val textureID: String, val regionX: Int, val regionY: Int,
                                  val regionW: Int, val regionH: Int, val spacing: RegionSpacing)
        
        /**
         * Must be called on the GL thread.
         */
        fun createAndLoadTextures(): CustomTexturePack {
            return CustomTexturePack(id, fallbackID).also { ctp ->
                val allTextures = textures.associate { tmd ->
                    tmd.id to Texture(tmd.pixmap).also { tex ->
                        tex.setFilter(tmd.minFilter, tmd.magFilter)
                    }
                }
                regions.forEach { rmd ->
                    val texRegion = TextureRegion(allTextures.getValue(rmd.textureID)).also { tr ->
                        if (rmd.regionH != 0 && rmd.regionW != 0) {
                            tr.setRegion(rmd.regionX, rmd.regionY, rmd.regionW, rmd.regionH)
                        }
                    }
                    ctp.add(TilesetRegion(rmd.id, texRegion, rmd.spacing))
                }
            }
        }
    }

    override fun dispose() {
        getAllUniqueTextures().forEach { it.disposeQuietly() }
    }

    fun writeToOutputStream(zipOutputStream: ZipOutputStream) {
        // Create texture pack manifest
        val allRegions = this.allRegions.toList()
        val textures: List<Texture> = getAllUniqueTextures()
        val regionIDs: Map<String, List<TilesetRegion>> = allRegions.groupBy({ it.id }, { it })
        val texturesGroupedByRegion: Map<Texture, List<TilesetRegion>> = allRegions.groupBy({ it.texture }, { it })
        val texturesMap: Map<Texture, String> = textures.associateWith {
            val regionList = texturesGroupedByRegion[it] ?: emptyList()
            if (regionList.size == 1 && regionIDs[regionList.first().id]?.size == 1) {
                regionList.first().id
            } else {
                UUID.randomUUID().toString()
            }
        }

        val jsonObj: JsonObject = Json.`object`()
        
        jsonObj.add("formatVersion", TEXTURE_PACK_VERSION)
        jsonObj.add("programVersion", PRMania.VERSION.toString())
        jsonObj.add("id", id)
        jsonObj.add("fallbackID", fallbackID)
        
        val texturesObj = Json.`object`()
        textures.forEach { tex ->
            val texID = texturesMap.getValue(tex)
            texturesObj.add(texID, Json.`object`().also { o ->
                o.add("id", texID)
                o.add("magFilter", textureFilterToID(tex.magFilter))
                o.add("minFilter", textureFilterToID(tex.minFilter))
            })
        }
        jsonObj.add("textures", texturesObj)
        
        val regionsObj = Json.`object`()
        allRegions.forEach { region ->
            regionsObj.add(region.id, Json.`object`().also { o ->
                o.add("id", region.id)
                o.add("tex", texturesMap.getValue(region.texture))
                if (region.regionX != 0 || region.regionY != 0 || region.regionWidth != region.texture.width || region.regionHeight != region.texture.height) {
                    o.add("x", region.regionX)
                    o.add("y", region.regionY)
                    o.add("w", region.regionWidth)
                    o.add("h", region.regionHeight)
                }
                if (region.spacing != RegionSpacing.ZERO) {
                    o.add("spacing", region.spacing.toJson(Json.`object`()))
                }
            })
        }
        jsonObj.add("regions", regionsObj)
        
        zipOutputStream.use { zip ->
            zip.setComment("Polyrhythm Mania texture pack - $TEXTURE_PACK_VERSION - ${PRMania.VERSION}")

            zip.putNextEntry(ZipEntry("texture_pack.json"))
            val jsonWriter = zip.bufferedWriter()
            jsonObj.writeTo(jsonWriter, WriterConfig.PRETTY_PRINT)
            jsonWriter.flush()
            zip.closeEntry()
            
            val resDir = "textures/"
            zip.putNextEntry(ZipEntry(resDir))
            zip.closeEntry()
            
            val tgaWriter = ImageIO.getImageWritersByFormatName("TGA").next()
            try {
                texturesMap.forEach { (tex, uuid) ->
                    zip.putNextEntry(ZipEntry("${resDir}${uuid}.tga"))

                    // Write as TGA, compressed where possible
                    ImageIO.createImageOutputStream(zip)?.use { imageOutputStream ->
                        tgaWriter.output = imageOutputStream
                        
                        val param: TGAImageWriteParam = (tgaWriter.defaultWriteParam as TGAImageWriteParam).apply { 
                            this.compressionMode = ImageWriteParam.MODE_EXPLICIT
                            this.compressionType = "RLE"
                        }
                        
                        val textureData = tex.textureData
                        if (!textureData.isPrepared) {
                            textureData.prepare()
                        }
                        
                        val texturePixmap = textureData.consumePixmap()
                        val pixmap = Pixmap(texturePixmap.width, texturePixmap.height, Pixmap.Format.RGBA8888)
                        pixmap.blending = Pixmap.Blending.None
                        pixmap.setColor(1f, 1f, 1f, 0f)
                        pixmap.fill()
                        pixmap.drawPixmap(texturePixmap, 0, 0) // Force conversion to RGBA8888
                        if (textureData.disposePixmap()) {
                            pixmap.disposeQuietly()
                        }

                        val bufImg = BufferedImage(pixmap.width, pixmap.height, BufferedImage.TYPE_4BYTE_ABGR)
                        val array: ByteArray = (bufImg.raster.dataBuffer as DataBufferByte).data
                        
                        val pixels: ByteBuffer = pixmap.pixels
                        val originalOrder = pixels.order()
                        pixels.order(ByteOrder.LITTLE_ENDIAN)
                        var arrayIndex = 0
                        while (pixels.remaining() >= 4 && arrayIndex < array.size) {
                            val r = pixels.get()
                            val g = pixels.get()
                            val b = pixels.get()
                            val a = pixels.get()
                            
                            array[arrayIndex++] = a
                            array[arrayIndex++] = b
                            array[arrayIndex++] = g
                            array[arrayIndex++] = r
                        }
                        pixels.order(originalOrder)
                        
                        pixmap.disposeQuietly()
                        tgaWriter.write(null, IIOImage(bufImg, null, null), param)
                    } ?: error("Failed to create ImageOutputStream for a texture")
                    
                    zip.closeEntry()
                }
            } finally {
                tgaWriter.dispose()
            }
        }
    }
    
}