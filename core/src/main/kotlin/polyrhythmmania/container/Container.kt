package polyrhythmmania.container

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import com.eclipsesource.json.WriterConfig
import paintbox.registry.AssetRegistry
import paintbox.util.Version
import paintbox.util.gdxutils.disposeQuietly
import net.beadsproject.beads.ugens.SamplePlayer
import net.lingala.zip4j.ZipFile
import polyrhythmmania.PRMania
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.Instantiator
import polyrhythmmania.editor.block.Instantiators
import polyrhythmmania.editor.pane.dialog.MusicDialog
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.Swing
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.soundsystem.sample.GdxAudioReader
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.util.TempFileUtils
import polyrhythmmania.world.World
import polyrhythmmania.world.render.GBATileset
import polyrhythmmania.world.render.WorldRenderer
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
        const val CONTAINER_VERSION: Int = 1

        const val KEY_COMPRESSED_MUSIC: String = "compressed_music"
    }

    val world: World = World()
    val soundSystem: SoundSystem? = soundSystem
    val timing: TimingProvider = timingProvider // Could also be the SoundSystem in theory
    val engine: Engine = Engine(timing, world, soundSystem)
    val renderer: WorldRenderer by lazy {
        WorldRenderer(world, GBATileset(AssetRegistry["tileset_gba"]))
    }
    val _blocks: MutableList<Block> = CopyOnWriteArrayList()
    val blocks: List<Block> get() = _blocks

    private val _resources: MutableMap<String, ExternalResource> = ConcurrentHashMap()
    val resources: Map<String, ExternalResource> get() = _resources
    var compressedMusic: ExternalResource? = null
        private set

    fun setCompressedMusic(res: ExternalResource?) {
        removeResource(KEY_COMPRESSED_MUSIC)
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

    fun addBlock(block: Block) {
        val blocks = this._blocks
        if (block !in blocks) {
            blocks.add(block)
        }
    }

    fun addBlocks(blocksToAdd: List<Block>) {
        val blocks = this._blocks
        blocksToAdd.forEach { block ->
            if (block !in blocks) {
                blocks.add(block)
            }
        }
    }

    fun removeBlock(block: Block) {
        val blocks = this._blocks
        blocks.remove(block)
    }

    fun removeBlocks(blocksToAdd: List<Block>) {
        val blocks = this._blocks
        blocks.removeAll(blocksToAdd)
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
                musicObj.add("musicFirstBeat", musicData.musicFirstBeat)
                val loopParams = musicData.loopParams
                musicObj.add("looping", loopParams.loopType == SamplePlayer.LoopType.LOOP_FORWARDS)
                musicObj.add("loopStartMs", loopParams.startPointMs)
                musicObj.add("loopEndMs", loopParams.endPointMs)
            })
        })
        jsonObj.add("blocks", Json.array().also { blocksArray ->
            val instantiators = Instantiators.list
            val classMapping: Map<Class<*>, Instantiator<*>> = instantiators.associateBy { it.blockClass }
            for (block in blocks.toList()) {
                val o = Json.`object`()
                val javaClass = block.javaClass
                val inst = classMapping[javaClass] ?: continue
                o.add("inst", inst.id)
                block.writeToJson(o)
                blocksArray.add(o)
            }
        })


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
            musicData.musicFirstBeat = musicObj.getFloat("musicFirstBeat", 0f)
            musicData.loopParams = LoopParams(
                    if (musicObj.getBoolean("looping", false)) SamplePlayer.LoopType.LOOP_FORWARDS else SamplePlayer.LoopType.NO_LOOP_FORWARDS,
                    musicObj.getDouble("loopStartMs", 0.0),
                    musicObj.getDouble("loopEndMs", 0.0)
            )
        }

        val blocksObj = json.get("blocks").asArray()
        val instantiators = Instantiators.map
        val blocks: MutableList<Block> = mutableListOf()
        for (value in blocksObj) {
            val obj = value.asObject()
            @Suppress("UNCHECKED_CAST")
            val inst = (instantiators[obj.getString("inst", null)] as? Instantiator<Block>?) ?: continue
            val block: Block = inst.factory.invoke(inst, engine)
            block.readFromJson(obj)
            blocks.add(block)
        }
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

        val compressedMusicRes = resources[KEY_COMPRESSED_MUSIC]
        this.compressedMusic = compressedMusicRes

        // Set up music and other resources
        if (compressedMusicRes != null) {
            val newMusic: BeadsMusic = GdxAudioReader.newMusic(FileHandle(compressedMusicRes.file), null)
            engine.musicData.beadsMusic = newMusic
            engine.musicData.update()
        }

        return LoadMetadata(containerVersion, programVersion)
    }

    data class LoadMetadata(val containerVersion: Int, val programVersion: Version?)
}