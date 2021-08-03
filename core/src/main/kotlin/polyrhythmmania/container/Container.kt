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
import paintbox.packing.CascadingRegionMap
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.util.Version
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRMania
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
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.StockTexturePack
import polyrhythmmania.world.tileset.StockTexturePacks
import polyrhythmmania.world.tileset.TexturePack
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
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
        const val FILE_EXTENSION: String = "prmania"
        const val CONTAINER_VERSION: Int = 7

        const val RES_KEY_COMPRESSED_MUSIC: String = "compressed_music"
        
        val DEFAULT_TRACKS_BEFORE_V7: List<String> = listOf("input_0", "input_1", "input_2", "fx_0", "fx_1") // Default tracks indexes for container version 6 and below
    }

    val world: World = World()
    @Suppress("CanBePrimaryConstructorProperty")
    val soundSystem: SoundSystem? = soundSystem
    val timing: TimingProvider = timingProvider // Could also be the SoundSystem in theory
    val engine: Engine = Engine(timing, world, soundSystem, this)
    val renderer: WorldRenderer by lazy {
        WorldRenderer(world, Tileset(StockTexturePacks.gba).apply { 
            world.tilesetPalette.applyTo(this)
        })
    }
    val _blocks: MutableList<Block> = CopyOnWriteArrayList()
    val blocks: List<Block> get() = _blocks
    
    var resultsText: ResultsText = ResultsText.DEFAULT
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
        soundSystem?.dispose()
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
    fun writeToFile(file: File) {
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
        jsonObj.add("tilesetConfig", this.world.tilesetPalette.toJson())
        val resultsText = this.resultsText
        if (resultsText != ResultsText.DEFAULT) {
            jsonObj.add("resultsText", resultsText.toJson())
        }
        val worldSettings = this.world.worldSettings
        if (worldSettings != WorldSettings.DEFAULT) {
            jsonObj.add("worldSettings", worldSettings.toJson())
        }


        // Pack
        file.outputStream().use { fos ->
            ZipOutputStream(fos).use { zip ->
                zip.setComment("Polyrhythm Mania save file - ${PRMania.VERSION}")

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

        data class Res(val key: String, val uuid: String, val ext: String)
        
        val resourcesMap: Map<String, Res> = json.get("resources").asObject().get("list").asArray().associate { value ->
            value as JsonObject
            val res = Res(value.getString("key", null), value.getString("uuid", null)!!, value.getString("ext", "tmp"))
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
        if (containerVersion >= 3) {
            // Legacy name: tilesetConfig is the TilesetPalette
            val tilesetObj = json.get("tilesetConfig")?.asObject()
            if (tilesetObj != null) {
                this.world.tilesetPalette.fromJson(tilesetObj)
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
                    Paintbox.LOGGER.info("Missing instantiator ID '$instID'")
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
                val tempFile = TempFileUtils.createTempFile("extres", true, ".${res.ext}")
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

        return LoadMetadata(containerVersion, programVersion)
    }

    data class LoadMetadata(val containerVersion: Int, val programVersion: Version?) {
        val isFutureVersion: Boolean = (programVersion != null && programVersion > PRMania.VERSION) || (containerVersion > CONTAINER_VERSION)
    }
}