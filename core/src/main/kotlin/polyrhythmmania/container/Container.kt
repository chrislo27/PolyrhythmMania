package polyrhythmmania.container

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import com.eclipsesource.json.WriterConfig
import net.beadsproject.beads.ugens.SamplePlayer
import net.lingala.zip4j.ZipFile
import paintbox.Paintbox
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyFloatVar
import paintbox.binding.Var
import paintbox.util.Version
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRMania
import polyrhythmmania.container.manifest.ResourceTag
import polyrhythmmania.container.manifest.SaveOptions
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.TrackID
import polyrhythmmania.editor.block.*
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.ResultsText
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.Swing
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.engine.timesignature.TimeSignature
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.soundsystem.sample.GdxAudioReader
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.util.TempFileUtils
import polyrhythmmania.world.World
import polyrhythmmania.world.WorldSettings
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.*
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


/**
 * A [Container] holds together the pieces for a game: the [World], [WorldRenderer], optional [SoundSystem],
 * [TimingProvider], and [Engine].
 *
 * It also contains the external resources that have to be tracked for loading/unloading.
 *
 * There are also pre-defined external resources as a utility.
 */
class Container(soundSystem: SoundSystem?, timingProvider: TimingProvider) : Disposable {

    companion object {
        const val LEVEL_FILE_EXTENSION: String = "prmania"
        const val PROJECT_FILE_EXTENSION: String = "prmproj"
        const val CONTAINER_VERSION: Int = 10

        const val RES_KEY_COMPRESSED_MUSIC: String = "compressed_music"
        
        val DEFAULT_TRACKS_BEFORE_V7: List<String> = listOf("input_0", "input_1", "input_2", "fx_0", "fx_1") // Default tracks indexes for container version 6 and below
        
        const val VERSION_LEVEL_METADATA_ADDED: Int = 9
    }

    val world: World = World()
    @Suppress("CanBePrimaryConstructorProperty")
    val soundSystem: SoundSystem? = soundSystem
    val timing: TimingProvider = timingProvider // Could also be the SoundSystem in theory
    val engine: Engine = Engine(timing, world, soundSystem, this)
    val texturePack: Var<TexturePack> = Var(StockTexturePacks.gba)
    val customTexturePack: Var<CustomTexturePack?> = Var(null)
    val texturePackSource: Var<TexturePackSource> = Var(TexturePackSource.STOCK_GBA)
    val renderer: WorldRenderer by lazy {
        WorldRenderer(world, Tileset(texturePack).apply { 
            world.tilesetPalette.applyTo(this)
        }, engine)
    }
    
    val _blocks: MutableList<Block> = CopyOnWriteArrayList()
    val blocks: List<Block> get() = _blocks
    
    var resultsText: ResultsText = ResultsText.DEFAULT
    var levelMetadata: LevelMetadata = LevelMetadata.DEFAULT_METADATA.copy(initialCreationDate = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
    var wasLevelMetadataLoaded: Boolean = false
        private set
    val trackIDs: List<TrackID> = Editor.DEFAULT_TRACKS.map { it.id }

    private val _resources: MutableMap<String, ExternalResource> = ConcurrentHashMap()
    val resources: Map<String, ExternalResource> get() = _resources
    var compressedMusic: ExternalResource? = null
        private set
    
    var lastBlockPosition: FloatVar = FloatVar(0f) // Position of very last block
        private set
    var endBlockPosition: FloatVar = FloatVar(Float.POSITIVE_INFINITY) // Position of first End State block
        private set
    val stopPosition: ReadOnlyFloatVar = FloatVar {
        // endPosition if < Infinity, otherwise lastBlockPosition
        val endBlockPos = endBlockPosition.useF()
        if (endBlockPos < Float.POSITIVE_INFINITY) endBlockPos else lastBlockPosition.useF()
    }
    
    init {
        engine.inputter.skillStarGotten.addListener {
            if (it.getOrCompute()) {
                renderer.fireSkillStar()
            }
        }
    }
    
    fun getTexturePackFromSource(source: TexturePackSource): TexturePack? {
        return when (source) {
            TexturePackSource.STOCK_GBA -> StockTexturePacks.gba
            TexturePackSource.STOCK_HD -> StockTexturePacks.hd
            TexturePackSource.CUSTOM -> getCustomTexturePackAsCascading()
        }
    }
    
    fun setTexturePackFromSource(source: TexturePackSource = texturePackSource.getOrCompute()): TexturePack {
        val chosen = getTexturePackFromSource(source) ?: StockTexturePacks.gba
        texturePack.set(chosen)
        return chosen
    }
    
    fun getCustomTexturePackAsCascading(): CascadingTexturePack? {
        val ctp = customTexturePack.getOrCompute()
        if (ctp != null) {
            return CascadingTexturePack("cascading_custom", emptySet(),
                    listOf(ctp, StockTexturePacks.allPacksByIDWithDeprecations[ctp.fallbackID] ?: StockTexturePacks.gba))
        }
        
        return null
    }

    fun setCompressedMusic(res: ExternalResource?) {
        removeResource(RES_KEY_COMPRESSED_MUSIC)
        if (res != null) {
            addResource(res)
        }
        this.compressedMusic = res
    }

    fun addResource(res: ExternalResource) {
        val key = res.key
        val existing = _resources[key]
        existing?.dispose()
        _resources[key] = res
    }

    fun removeResource(key: String) {
        val removed = _resources.remove(key)
        removed?.dispose()
    }
    
    fun updateLastPoints() {
        val blocks = this.blocks.sortedBy { it.beat }
        val firstEndBlock: BlockEndState? = blocks.firstOrNull { it is BlockEndState } as? BlockEndState?
        lastBlockPosition.set(blocks.lastOrNull()?.beat ?: 0f)
        endBlockPosition.set(firstEndBlock?.beat ?: Float.POSITIVE_INFINITY)
    }

    fun addBlock(block: Block) {
        val blocks = this._blocks
        if (block !in blocks) {
            blocks.add(block)
            updateLastPoints()
        }
    }

    fun addBlocks(blocksToAdd: List<Block>) {
        val blocks = this._blocks
        blocksToAdd.forEach { block ->
            if (block !in blocks) {
                blocks.add(block)
            }
        }
        updateLastPoints()
    }

    fun removeBlock(block: Block) {
        val blocks = this._blocks
        blocks.remove(block)
        updateLastPoints()
    }

    fun removeBlocks(blocksToAdd: List<Block>) {
        val blocks = this._blocks
        blocks.removeAll(blocksToAdd)
        updateLastPoints()
    }

    override fun dispose() {
        if (engine.metricsEnabled) {
            engine.metricsReporter.report()
        }
        
        soundSystem?.dispose()
        (customTexturePack.getOrCompute() as? Disposable?)?.disposeQuietly()
        resources.values.toList().forEach { it.disposeQuietly() }
        _resources.clear()
    }

    /*
    Container file format:
      - Compressed zip archive with file extension .prmania
      - /manifest.json
      - /res/
        - Contains the ExternalResources
     */

    /**
     * Writes the [Container] to a file.
     */
    fun writeToFile(file: File, saveOptions: SaveOptions) {
        if (!file.exists()) {
            file.createNewFile()
        } else {
            if (!file.isFile) error("File given was not a file: ${file.absolutePath}")
        }

        val extRes: List<ExternalResource> = this.resources.values.toList()
        val extResMap: Map<ExternalResource, String /* UUID */> = extRes.associateWith { UUID.randomUUID().toString() }

        // Create manifest
        val jsonObj: JsonObject = Json.`object`()
        jsonObj.add("containerVersion", CONTAINER_VERSION)
        jsonObj.add("programVersion", PRMania.VERSION.toString())
        jsonObj.add("isAutosave", saveOptions.isAutosave)
        jsonObj.add("resources", Json.`object`().also { obj ->
            obj.add("list", Json.array().also { array ->
                extResMap.forEach { (res, uuid) ->
                    array.add(Json.`object`().also { resObj ->
                        resObj.add("key", res.key)
                        resObj.add("uuid", uuid)
                        resObj.add("ext", res.file.extension)
                    })
                }
            })
        })
        jsonObj.add("editor", Json.`object`().also { editorObj ->
            editorObj.add("trackIndexes", Json.`object`().also { trackIndexesObj ->
                val trackIDs = this.trackIDs
                trackIndexesObj.add("count", trackIDs.size)
                trackIndexesObj.add("ids", Json.array(*trackIDs.map { it.id }.toTypedArray()))
            })
        })
        jsonObj.add("engine", Json.`object`().also { engineObj ->
            engineObj.add("tempo", Json.`object`().also { tempoObj ->
                fun TempoChange.encode(): JsonValue {
                    return Json.`object`().also { o ->
                        o.add("beat", this.beat)
                        o.add("tempo", this.newTempo)
                        o.add("swing", Json.`object`().also { so ->
                            so.add("ratio", this.newSwing.ratio)
                            so.add("div", this.newSwing.division)
                        })
                    }
                }

                val globalTempo = engine.tempos.getGlobalTempo()
                tempoObj.add("startingTempo", globalTempo.encode())
                tempoObj.add("changes", Json.array().also { array ->
                    (engine.tempos.getAllTempoChanges().toList() - globalTempo).sortedBy { it.beat }.forEach { tc ->
                        array.add(tc.encode())
                    }
                })
            })
            engineObj.add("music", Json.`object`().also { musicObj ->
                val musicData = engine.musicData
                musicObj.add("volumes", Json.array().also { array ->
                    fun MusicVolume.encode(): JsonValue {
                        return Json.`object`().also { o ->
                            o.add("beat", this.beat)
                            o.add("width", this.width)
                            o.add("vol", this.newVolume)
                        }
                    }
                    (musicData.volumeMap.getAllMusicVolumes().toList()).forEach { mv ->
                        array.add(mv.encode())
                    }
                })
                musicObj.add("firstBeatSec", musicData.firstBeatSec)
                musicObj.add("musicFirstBeat", musicData.musicSyncPointBeat)
                musicObj.add("rate", musicData.rate) // As of container version 6
                val loopParams = musicData.loopParams
                musicObj.add("looping", loopParams.loopType == SamplePlayer.LoopType.LOOP_FORWARDS)
                musicObj.add("loopStartMs", loopParams.startPointMs)
                musicObj.add("loopEndMs", loopParams.endPointMs)
            })
            // As of container version 2:
            engineObj.add("timeSignatures", Json.`object`().also { timeSigObj ->
                timeSigObj.add("list", Json.array().also { array ->
                    engine.timeSignatures.map.values.forEach {
                        val node = Json.`object`()
                        node.set("beat", it.beat)
                        node.set("divisions", it.beatsPerMeasure)
                        node.set("beatUnit", it.beatUnit)
                        node.set("measure", it.measure)
                        array.add(node)
                    }
                })
            })
        })
        jsonObj.add("blocks", Json.array().also { blocksArray ->
            val classMapping: Map<Class<*>, Instantiator<*>> = Instantiators.classMapping
            for (block in blocks.toList()) {
                val o = Json.`object`()
                val javaClass = block.javaClass
                val inst = classMapping[javaClass] ?: continue
                o.add("inst", inst.id)
                block.writeToJson(o)
                blocksArray.add(o)
            }
        })

        val currentCustomTexturePack = customTexturePack.getOrCompute()
        jsonObj.add("tilesetConfig", Json.`object`().also { tilesetConfigObj ->
            tilesetConfigObj.add("palette", this.world.tilesetPalette.toJson())
            tilesetConfigObj.add("texturePack", Json.`object`().also { texturePackObj ->
                if (currentCustomTexturePack != null) {
                    texturePackObj.add("hasCustom", true)
                }
                
                val currentTexturePack = texturePack.getOrCompute()
                val src = texturePackSource.getOrCompute()
                if (src == TexturePackSource.CUSTOM) {
                    texturePackObj.add("source", "custom")
                } else {
                    texturePackObj.add("source", "stock")
                    texturePackObj.add("stockID", currentTexturePack.id)
                }
            })
        })
        
        val resultsText = this.resultsText
        if (resultsText != ResultsText.DEFAULT) {
            jsonObj.add("resultsText", resultsText.toJson())
        }
        
        jsonObj.add("levelMetadata", levelMetadata.truncateWithLimits().toJson())
        
        val worldSettings = this.world.worldSettings
        if (worldSettings != WorldSettings.DEFAULT) {
            jsonObj.add("worldSettings", worldSettings.toJson())
        }


        // Pack
        file.outputStream().use { fos ->
            ZipOutputStream(fos).use { zip ->
                zip.setComment("Polyrhythm Mania level file - ${PRMania.VERSION}")

                zip.putNextEntry(ZipEntry("manifest.json"))
                val jsonWriter = zip.bufferedWriter()
                jsonObj.writeTo(jsonWriter, WriterConfig.PRETTY_PRINT)
                jsonWriter.flush()
                zip.closeEntry()

                // Resources
                val resDir = "res/"
                zip.putNextEntry(ZipEntry(resDir))
                zip.closeEntry()
                extResMap.forEach { (res, uuid) ->
                    zip.putNextEntry(ZipEntry("${resDir}${uuid}"))
                    res.file.inputStream().use { input ->
                        input.copyTo(zip)
                    }
                    zip.closeEntry()
                }
                
                if (currentCustomTexturePack != null) {
                    val tmp = TempFileUtils.createTempFile("savingtexpack")
                    tmp.outputStream().use { tmpOutputStream ->
                        ZipOutputStream(tmpOutputStream).use { texPackZip ->
                            texPackZip.setLevel(Deflater.NO_COMPRESSION)
                            currentCustomTexturePack.writeToOutputStream(texPackZip)
                        }
                    }
                    zip.putNextEntry(ZipEntry("${resDir}texture_pack.zip"))
                    tmp.inputStream().use { input ->
                        input.copyTo(zip)
                    }
                    zip.closeEntry()
                    tmp.delete()
                }
            }
        }
    }


    /**
     * Reads container info from a file. This should only be called on a NEW [Container] object!
     */
    fun readFromFile(file: File): LoadMetadata {
        val zipFile = ZipFile(file)
        val json: JsonObject
        zipFile.getInputStream(zipFile.getFileHeader("manifest.json")).use { zipInputStream ->
            val reader = zipInputStream.reader()
            json = Json.parse(reader).asObject()
        }

        val containerVersion: Int = json.getInt("containerVersion", 0)
        val programVersion: Version? = Version.parse(json.getString("programVersion", null))
        
        val resourcesMap: Map<String, ResourceTag> = json.get("resources").asObject().get("list").asArray().associate { value ->
            value as JsonObject
            val res = ResourceTag(value.getString("key", null), value.getString("uuid", null)!!, value.getString("ext", "tmp"))
            Pair(res.key, res)
        }
        val engineObj = json.get("engine").asObject()

        val tempoObj = engineObj.get("tempo").asObject()

        fun JsonObject.decodeTempoChange(): TempoChange {
            val swingObj = this.get("swing")
            val swing: Swing = if (swingObj != null && swingObj.isObject) {
                swingObj as JsonObject
                Swing(swingObj.getInt("ratio", Swing.STRAIGHT.ratio), swingObj.getFloat("div", Swing.STRAIGHT.division))
            } else Swing.STRAIGHT
            return TempoChange(this.getFloat("beat", 0f), this.getFloat("tempo", 1f), swing)
        }
        engine.tempos.addTempoChange(tempoObj.get("startingTempo").asObject().decodeTempoChange().copy(beat = 0f))
        engine.tempos.addTempoChangesBulk(
                tempoObj.get("changes").asArray().map { value ->
                    value.asObject().decodeTempoChange()
                }
        )

        val musicObj = engineObj.get("music").asObject()
        val volumesObj = musicObj.get("volumes").asArray()
        fun JsonObject.decodeMusicVolume(): MusicVolume {
            return MusicVolume(this.getFloat("beat", 0f), this.getFloat("width", 0f), this.getInt("vol", 100))
        }
        engine.musicData.volumeMap.addMusicVolumesBulk(
                volumesObj.asArray().map { value ->
                    value.asObject().decodeMusicVolume()
                }
        )
        engine.musicData.also { musicData ->
            musicData.firstBeatSec = musicObj.getFloat("firstBeatSec", 0f)
            musicData.musicSyncPointBeat = musicObj.getFloat("musicFirstBeat", 0f)
            musicData.loopParams = LoopParams(
                    if (musicObj.getBoolean("looping", false)) SamplePlayer.LoopType.LOOP_FORWARDS else SamplePlayer.LoopType.NO_LOOP_FORWARDS,
                    musicObj.getDouble("loopStartMs", 0.0),
                    musicObj.getDouble("loopEndMs", 0.0)
            )
            if (containerVersion >= 6) {
                val rateField = musicObj.get("rate")?.asFloat()
                if (rateField != null) {
                    musicData.rate = rateField.coerceAtLeast(0f)
                }
            }
        }
        if (containerVersion >= 2) {
            val timeSigObj = engineObj.get("timeSignatures").asObject()
            val list = timeSigObj.get("list").asArray()
            list.forEach { 
                val obj = it.asObject()
                engine.timeSignatures.add(TimeSignature(obj.getFloat("beat", 0f), obj.getInt("divisions", 4), obj.getInt("beatUnit", 4)))
            }
        }
        var customTexturePackRead: CustomTexturePack.ReadResult? = null
        if (containerVersion >= 3) {
            val tilesetObj = json.get("tilesetConfig")?.asObject()
            if (tilesetObj != null) {
                if (containerVersion <= 7) {
                    // Container version [3, 7]: tilesetConfig is the actual tilesetPalette object.
                    val tilesetPalette = this.world.tilesetPalette
                    tilesetPalette.fromJson(tilesetObj)
                    tilesetPalette.allMappings.forEach { it.enabled.set(true) }
                } else {
                    // Container version [8, ): tilesetConfig is a larger obj. Palette is in own object "palette" now.
                    val paletteObj = tilesetObj.get("palette")?.asObject()
                    if (paletteObj != null) {
                        val tilesetPalette = this.world.tilesetPalette
                        tilesetPalette.fromJson(paletteObj)
                        tilesetPalette.allMappings.forEach { it.enabled.set(true) }
                    }
                    val texturePackObj = tilesetObj.get("texturePack")?.asObject()
                    if (texturePackObj != null) {
                        val hasCustom: Boolean = texturePackObj.get("hasCustom")?.asBoolean() ?: false
                        
                        when (val source: String = texturePackObj.getString("source", "")) {
                            "stock" -> {
                                val stockID: String = texturePackObj.getString("stockID", "")
                                val pack = StockTexturePacks.allPacksByIDWithDeprecations[stockID]
                                if (pack != null) {
                                    texturePack.set(pack)
                                    if (stockID == StockTexturePacks.hd.id) {
                                        texturePackSource.set(TexturePackSource.STOCK_HD)
                                    } else {
                                        texturePackSource.set(TexturePackSource.STOCK_GBA)
                                    }
                                } else {
                                    Paintbox.LOGGER.warn("[Container] Unknown tilesetConfig.texturePack.stockID '${stockID}', skipping stock texture pack")
                                    texturePack.set(StockTexturePacks.gba)
                                    texturePackSource.set(TexturePackSource.STOCK_GBA)
                                }
                            }
                            "custom" -> {
                                texturePackSource.set(TexturePackSource.CUSTOM)
                            }
                            else -> {
                                // Ignore texture packs. Just use default GBA
                                Paintbox.LOGGER.warn("[Container] Unknown tilesetConfig.texturePack.source '${source}', skipping")
                                texturePackSource.set(TexturePackSource.STOCK_GBA)
                            }
                        }
                        
                        if (hasCustom) {
                            zipFile.getInputStream(zipFile.getFileHeader("res/texture_pack.zip")).use { zipInputStream ->
                                val tempFile = TempFileUtils.createTempFile("extres", ".zip")
                                val out = tempFile.outputStream()
                                zipInputStream.copyTo(out)
                                val f = ZipFile(tempFile)
                                val readResult = CustomTexturePack.readFromStream(f)
                                customTexturePackRead = readResult
                                tempFile.delete()
                            }
                        }
                    }
                }
                
                world.tilesetPalette.applyTo(renderer.tileset)
            }
        }
        if (containerVersion >= 4) {
            val resultsObj = json.get("resultsText")?.asObject()
            if (resultsObj != null) {
                this.resultsText = ResultsText.fromJson(resultsObj)
            }
        }
        if (containerVersion >= 5) {
            val worldSettingsObj = json.get("worldSettings")?.asObject()
            if (worldSettingsObj != null) {
                this.world.worldSettings = WorldSettings.fromJson(worldSettingsObj)
            }
        }
        this.wasLevelMetadataLoaded = false
        if (containerVersion >= VERSION_LEVEL_METADATA_ADDED) {
            val metadataObj = json.get("levelMetadata")?.asObject()
            if (metadataObj != null) {
                this.levelMetadata = LevelMetadata.fromJson(metadataObj,
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneOffset.UTC))
                        .truncateWithLimits()
                this.wasLevelMetadataLoaded = true
            }
        }

        val blocksObj = json.get("blocks").asArray()
        val instantiators = Instantiators.instantiatorMap
        val blocks: MutableList<Block> = mutableListOf()
        for (value in blocksObj) {
            val obj = value.asObject()
            val instID = obj.getString("inst", null)

            @Suppress("UNCHECKED_CAST")
            val inst = (instantiators[instID] as? Instantiator<Block>?)
            if (inst == null) {
                if (instID != null) {
                    Paintbox.LOGGER.warn("[Container] Missing instantiator ID '$instID', skipping")
                }
                continue
            }
            val block: Block = inst.factory.invoke(inst, engine)
            block.readFromJson(obj)
            blocks.add(block)
        }
        blocks.sortWith(Block.getComparator())
        this.addBlocks(blocks)
        engine.addEvents(blocks.flatMap { it.compileIntoEvents() })

        resourcesMap.forEach { (key, res) ->
            zipFile.getInputStream(zipFile.getFileHeader("res/${res.uuid}")).use { zipInputStream ->
                val tempFile = TempFileUtils.createTempFile("extres", ".${res.ext}")
                val out = tempFile.outputStream()
                zipInputStream.copyTo(out)
                addResource(ExternalResource(key, tempFile, true))
            }
        }

        val compressedMusicRes = resources[RES_KEY_COMPRESSED_MUSIC]
        this.compressedMusic = compressedMusicRes

        // Set up music and other resources
        if (compressedMusicRes != null) {
            // Music reader decompressed to another file, so the original compressedMusic file is not a dependency after
            val newMusic: BeadsMusic = GdxAudioReader.newMusic(FileHandle(compressedMusicRes.file), null)
            engine.musicData.beadsMusic = newMusic
            engine.musicData.update()
        }

        return LoadMetadata(this, containerVersion, programVersion, customTexturePackRead)
    }

    data class LoadMetadata(val container: Container, val containerVersion: Int, val programVersion: Version?,
                            val customTexturePackRead: CustomTexturePack.ReadResult?) {
        val isFutureVersion: Boolean = (programVersion != null && programVersion > PRMania.VERSION) || (containerVersion > CONTAINER_VERSION)

        /**
         * Must be called on the GL thread.
         */
        fun loadOnGLThread() {
            if (customTexturePackRead != null) {
                val ctp = customTexturePackRead.createAndLoadTextures()
                container.customTexturePack.set(ctp)
            }
            container.setTexturePackFromSource()
        }
    }
}