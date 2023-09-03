package polyrhythmmania.container

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.Texture
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
import paintbox.util.WindowSize
import paintbox.util.closeQuietly
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRMania
import polyrhythmmania.container.manifest.LibraryRelevantData
import polyrhythmmania.container.manifest.ResourceTag
import polyrhythmmania.container.manifest.SaveOptions
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.TrackID
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.BlockEndState
import polyrhythmmania.editor.block.Instantiator
import polyrhythmmania.editor.block.Instantiators
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
import polyrhythmmania.world.entity.EntityInputFeedback
import polyrhythmmania.world.render.ForceTexturePack
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.render.WorldRendererWithUI
import polyrhythmmania.world.texturepack.*
import polyrhythmmania.world.tileset.Tileset
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
import kotlin.math.min


/**
 * A [Container] holds together the pieces for a game: the [World], [WorldRenderer], optional [SoundSystem],
 * [TimingProvider], and [Engine].
 *
 * It also contains the external resources that have to be tracked for loading/unloading.
 *
 * There are also pre-defined external resources as a utility.
 */
class Container(
    val soundSystem: SoundSystem?, timingProvider: TimingProvider,
    val globalSettings: GlobalContainerSettings,
) : Disposable {

    companion object {

        const val LEVEL_FILE_EXTENSION: String = "prmania"
        const val PROJECT_FILE_EXTENSION: String = "prmproj"
        const val CONTAINER_VERSION: Int = 13

        const val RES_KEY_COMPRESSED_MUSIC: String = "compressed_music"

        val DEFAULT_TRACKS_BEFORE_V7: List<String> = listOf(
            "input_0",
            "input_1",
            "input_2",
            "fx_0",
            "fx_1"
        ) // Default tracks indexes for container version 6 and below

        const val VERSION_LEVEL_METADATA_ADDED: Int = 9
        const val VERSION_EXPORT_STATISTICS_ADDED: Int = 10
        const val VERSION_MULTIPLE_TEX_PACK_ADDED: Int = 12

        val MIN_BANNER_SIZE: WindowSize = WindowSize(256, 80)
        val MAX_BANNER_SIZE: WindowSize = WindowSize(512, 160)

        fun isBannerTextureWithinSize(tex: Texture): Boolean {
            return tex.width in MIN_BANNER_SIZE.width..MAX_BANNER_SIZE.width
                    && tex.height in MIN_BANNER_SIZE.height..MAX_BANNER_SIZE.height
        }
    }

    val world: World = World(globalSettings.numberOfSpotlightsOverride)
    val timing: TimingProvider = timingProvider // Could also be the SoundSystem in theory
    val engine: Engine = Engine(timing, world, soundSystem, this)
    val texturePack: Var<TexturePack> by lazy { Var(StockTexturePacks.gba) } // Lazy due to late init in StockTexturePacks
    val customTexturePacks: Array<Var<CustomTexturePack?>> = Array(TexturePackSource.CUSTOM_RANGE.last) { Var(null) }
    val texturePackSource: Var<TexturePackSource> = Var(TexturePackSource.StockGBA)
    val renderer: WorldRendererWithUI by lazy {
        WorldRendererWithUI(world, Tileset(
            when (globalSettings.forceTexturePack) {
                ForceTexturePack.NO_FORCE -> this.texturePack
                ForceTexturePack.FORCE_GBA -> Var(StockTexturePacks.gba)
                ForceTexturePack.FORCE_HD -> Var(StockTexturePacks.hd)
                ForceTexturePack.FORCE_ARCADE -> Var(StockTexturePacks.arcade)
            }
        ).apply {
            world.tilesetPalette.applyTo(this)
        }, engine
        )
    }

    private val _blocks: MutableList<Block> = CopyOnWriteArrayList()
    val blocks: List<Block> get() = _blocks

    var resultsText: ResultsText = ResultsText.DEFAULT
    var levelMetadata: LevelMetadata = LevelMetadata.DEFAULT_METADATA.copy(
        initialCreationDate = LocalDateTime.ofInstant(
            Instant.now(),
            ZoneOffset.UTC
        )
    )
    var wasLevelMetadataLoaded: Boolean = false
        private set
    val trackIDs: List<TrackID> = Editor.DEFAULT_TRACKS.map { it.id } // Intentionally doesn't use editor.tracks

    val storyModeMetadata: Var<StoryModeContainerMetadata> = Var(StoryModeContainerMetadata.BLANK)


    private val _resources: MutableMap<String, ExternalResource> = ConcurrentHashMap()
    val resources: Map<String, ExternalResource> get() = _resources
    var compressedMusic: ExternalResource? = null
        private set
    var bannerTexture: Var<Texture?> = Var(null)

    var lastBlockPosition: FloatVar = FloatVar(0f) // Position of very last block
        private set
    var endBlockPosition: FloatVar = FloatVar(Float.POSITIVE_INFINITY) // Position of first End State block
        private set
    val stopPosition: ReadOnlyFloatVar = FloatVar {
        // endPosition if < Infinity, otherwise lastBlockPosition
        val endBlockPos = endBlockPosition.use()
        if (endBlockPos < Float.POSITIVE_INFINITY) endBlockPos else lastBlockPosition.use()
    }

    init {
        engine.inputter.skillStarGotten.addListener {
            if (it.getOrCompute()) {
                renderer.fireSkillStar()
            }
        }
        storyModeMetadata.addListener {
            val metadata = it.getOrCompute()

            engine.inputter.inputChallenge.restriction = metadata.inputTimingRestriction

            val livesMode = engine.modifiers.livesMode
            val lives = metadata.lives
            livesMode.enabled.set(lives > 0)
            livesMode.maxLives.set(lives)
            livesMode.resetState()

            val defectiveRodsMode = engine.modifiers.defectiveRodsMode
            val defectiveThreshold = metadata.defectiveRodsThreshold
            defectiveRodsMode.enabled.set(defectiveThreshold > 0)
            defectiveRodsMode.maxLives.set(defectiveThreshold)
            defectiveRodsMode.resetState()

            val monsterGoal = engine.modifiers.monsterGoal
            val monsterEnabled = metadata.monsterEnabled
            monsterGoal.enabled.set(monsterEnabled)
            monsterGoal.difficulty.set(metadata.monsterDifficulty)
            monsterGoal.recoveryPenalty.set(metadata.monsterRecoveryPenalty)
            monsterGoal.resetState()

            world.resetWorld()
        }
    }

    /**
     * Resets all mutable state within this [Container].
     */
    fun resetMutableState() {
        engine.resetMutableState()
        world.resetWorld()

        this.globalSettings.applyForcedTilesetPaletteSettings(this)
        this.setTexturePackFromSource()

        val blocks = this.blocks.toList()
        engine.addEvents(blocks.flatMap { it.compileIntoEvents() })

        resetInputFeedbackEntities()
    }

    /**
     * Refreshes the feedback entity's base color if the input timing restriction is different
     */
    fun resetInputFeedbackEntities() {
        world.entities.filterIsInstance<EntityInputFeedback>().forEach { ent ->
            ent.updateCurrentColor(engine)
        }
    }

    fun getTexturePackFromSource(source: TexturePackSource): TexturePack? {
        return when (source) {
            TexturePackSource.StockGBA -> StockTexturePacks.gba
            TexturePackSource.StockHD -> StockTexturePacks.hd
            TexturePackSource.StockArcade -> StockTexturePacks.arcade
            is TexturePackSource.Custom -> getCustomTexturePackAsCascading(source.id - 1)
        }
    }

    fun setTexturePackFromSource(source: TexturePackSource = texturePackSource.getOrCompute()): TexturePack {
        val chosen = getTexturePackFromSource(source) ?: StockTexturePacks.gba
        texturePack.set(chosen)
        return chosen
    }

    fun getCustomTexturePackAsCascading(index: Int): CascadingTexturePack? {
        val ctp = customTexturePacks[index].getOrCompute()
        if (ctp != null) {
            return CascadingTexturePack(
                "cascading_custom", emptySet(),
                listOf(
                    ctp,
                    StockTexturePacks.allPacksByIDWithDeprecations[ctp.fallbackID.getOrCompute()]
                        ?: StockTexturePacks.gba
                )
            )
        }

        return null
    }

    fun setCompressedMusic(res: ExternalResource?) {
        val oldCompressedMusic = this.compressedMusic
        if (oldCompressedMusic != res) {
            if (res != null) {
                addResource(res)
            } else {
                if (oldCompressedMusic != null) {
                    removeResource(oldCompressedMusic.key)
                }
            }
            this.compressedMusic = res
        }
    }

    fun addResource(res: ExternalResource) {
        val key = res.key
        val existing = _resources[key]
        if (existing != res) { // Don't dispose and add if it is the same resource
            existing?.dispose()
            _resources[key] = res
        }
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
        customTexturePacks.forEach { pack ->
            (pack.getOrCompute() as? Disposable)?.disposeQuietly()
        }
        resources.values.toList().forEach { it.disposeQuietly() }
        _resources.clear()
        renderer.disposeQuietly()
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
            if (!file.isFile) throw ContainerException("File input given was not a file: ${file.absolutePath}")
        }

        val extRes: List<ExternalResource> = this.resources.values.toList()
        val extResMap: Map<ExternalResource, String /* UUID */> = extRes.associateWith { UUID.randomUUID().toString() }

        // Create manifest
        val jsonObj: JsonObject = Json.`object`()
        val libraryRelevantData = LibraryRelevantData(
            CONTAINER_VERSION,
            PRMania.VERSION,
            saveOptions.isAutosave,
            saveOptions.exportStatistics,
            if (!saveOptions.isProject) UUID.randomUUID() else null,
            levelMetadata.truncateWithLimits()
        )
        libraryRelevantData.writeToManifestJson(jsonObj)

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
                        if (this.newSwing != Swing.STRAIGHT) {
                            o.add("swing", Json.`object`().also { so ->
                                so.add("ratio", this.newSwing.ratio)
                                so.add("div", this.newSwing.division)
                            })
                        }
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

        jsonObj.add("tilesetConfig", Json.`object`().also { tilesetConfigObj ->
            tilesetConfigObj.add("palette", this.world.tilesetPalette.toJson())

            tilesetConfigObj.add("texturePack", Json.`object`().also { texturePackObj ->
                if (customTexturePacks.any { it.getOrCompute() != null }) {
                    texturePackObj.add("hasCustom", true)

                    // As of container version 12:
                    texturePackObj.add("slotCount", customTexturePacks.size)
                    texturePackObj.add("presentIndices", Json.array().also { arr ->
                        customTexturePacks.forEachIndexed { index, varr ->
                            if (varr.getOrCompute() != null) {
                                arr.add(index)
                            }
                        }
                    })
                }

                val currentTexturePack = texturePack.getOrCompute()
                val src = texturePackSource.getOrCompute()
                if (src is TexturePackSource.Custom) {
                    texturePackObj.add("source", "custom")
                    texturePackObj.add("srcIndex", src.id - 1)
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

        val worldSettings = this.world.worldSettings
        if (worldSettings != WorldSettings.DEFAULT) {
            jsonObj.add("worldSettings", worldSettings.toJson())
        }

        val storyModeMetadata = this.storyModeMetadata.getOrCompute()
        if (storyModeMetadata != StoryModeContainerMetadata.BLANK) {
            jsonObj.add("storyModeMetadata", storyModeMetadata.toJson())
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

                customTexturePacks.map { it.getOrCompute() }.forEachIndexed { index, pack ->
                    if (pack != null) {
                        val tmp = TempFileUtils.createTempFile("savingtexpack")
                        tmp.outputStream().use { tmpOutputStream ->
                            ZipOutputStream(tmpOutputStream).use { texPackZip ->
                                texPackZip.setLevel(Deflater.NO_COMPRESSION)
                                pack.writeToOutputStream(texPackZip)
                            }
                        }
                        zip.putNextEntry(ZipEntry("${resDir}texture_pack_${index}.zip"))
                        tmp.inputStream().use { input ->
                            input.copyTo(zip)
                        }
                        zip.closeEntry()
                        tmp.delete()
                    }
                }
                val bannerTexture = this.bannerTexture.getOrCompute()
                if (bannerTexture != null) {
                    try {
                        val tmp = TempFileUtils.createTempFile("savinglvlbanner")
                        val texData = bannerTexture.textureData
                        if (!texData.isPrepared) {
                            texData.prepare()
                        }
                        val pixmap = bannerTexture.textureData.consumePixmap()
                        PixmapIO.writePNG(FileHandle(tmp), pixmap)
                        if (texData.disposePixmap()) {
                            pixmap.disposeQuietly()
                        }

                        zip.putNextEntry(ZipEntry("banner.png"))
                        tmp.inputStream().use { input ->
                            input.copyTo(zip)
                        }
                        zip.closeEntry()
                        tmp.delete()
                    } catch (e: Exception) {
                        Paintbox.LOGGER.error("Failed to save level banner!")
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    /**
     * Reads container info from a file. This should only be called on a NEW [Container] object!
     */
    fun readFromFile(file: File, editorFlags: EnumSet<EditorSpecialFlags>): LoadMetadata {
        val zipFile = ZipFile(file)
        val json: JsonObject
        zipFile.getInputStream(zipFile.getFileHeader("manifest.json")).use { zipInputStream ->
            val reader = zipInputStream.reader()
            json = Json.parse(reader).asObject()
        }

        val libraryRelevantDataLoad = LibraryRelevantData.fromManifestJson(json, file.lastModified())
        val libraryRelevantData: LibraryRelevantData = libraryRelevantDataLoad.first

        val containerVersion: Int = libraryRelevantData.containerVersion

        this.wasLevelMetadataLoaded = libraryRelevantDataLoad.second.wasLevelMetadataLoaded
        if (libraryRelevantData.levelMetadata != null) {
            this.levelMetadata = libraryRelevantData.levelMetadata
        }

        val resourcesMap: Map<String, ResourceTag> =
            json.get("resources").asObject().get("list").asArray().associate { value ->
                value as JsonObject
                val res = ResourceTag(
                    value.getString("key", null),
                    value.getString("uuid", null)!!,
                    value.getString("ext", "tmp")
                )
                Pair(res.key, res)
            }
        val engineObj = json.get("engine").asObject()

        val tempoObj = engineObj.get("tempo").asObject()

        fun JsonObject.decodeTempoChange(): TempoChange {
            val swingObj = this.get("swing")
            val swing: Swing =
                if (swingObj != null && swingObj.isObject && EditorSpecialFlags.STORY_MODE in editorFlags) {
                    swingObj as JsonObject
                    Swing(
                        swingObj.getInt("ratio", Swing.STRAIGHT.ratio),
                        swingObj.getFloat("div", Swing.STRAIGHT.division)
                    )
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
                if (musicObj.getBoolean(
                        "looping",
                        false
                    )
                ) SamplePlayer.LoopType.LOOP_FORWARDS else SamplePlayer.LoopType.NO_LOOP_FORWARDS,
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
                engine.timeSignatures.add(
                    TimeSignature(
                        obj.getFloat("beat", 0f),
                        obj.getInt("divisions", 4),
                        obj.getInt("beatUnit", 4)
                    )
                )
            }
        }

        val customTexturePacksRead: Array<CustomTexturePack.ReadResult?> = Array(this.customTexturePacks.size) { null }
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
                        when (val source: String = texturePackObj.getString("source", "")) {
                            "stock" -> {
                                val stockID: String = texturePackObj.getString("stockID", "")
                                val pack = StockTexturePacks.allPacksByIDWithDeprecations[stockID]
                                if (pack != null) {
                                    texturePack.set(pack)
                                    val sourceFromPack = StockTexturePacks.getTexturePackSource(pack)
                                    if (sourceFromPack != null) {
                                        texturePackSource.set(sourceFromPack)
                                    } else {
                                        texturePackSource.set(TexturePackSource.StockGBA)
                                        Paintbox.LOGGER.warn("[Container] TexturePackSource was not mapped for stock texture pack ${pack.id}, setting to GBA")
                                    }
                                } else {
                                    Paintbox.LOGGER.warn("[Container] Unknown tilesetConfig.texturePack.stockID '${stockID}', skipping stock texture pack")
                                    texturePack.set(StockTexturePacks.gba)
                                    texturePackSource.set(TexturePackSource.StockGBA)
                                }
                            }

                            "custom" -> {
                                // index is present as of container version 12
                                if (containerVersion >= VERSION_MULTIPLE_TEX_PACK_ADDED) {
                                    texturePackSource.set(
                                        TexturePackSource.Custom(
                                            (texturePackObj.get("srcIndex")
                                                .asInt() + 1).coerceIn(TexturePackSource.CUSTOM_RANGE)
                                        )
                                    )
                                } else {
                                    texturePackSource.set(TexturePackSource.Custom(1)) // Default to first pack for older levels
                                }
                            }

                            else -> {
                                // Ignore texture packs. Just use default GBA
                                Paintbox.LOGGER.warn("[Container] Unknown tilesetConfig.texturePack.source '${source}', skipping")
                                texturePackSource.set(TexturePackSource.StockGBA)
                            }
                        }

                        val hasCustomPack = texturePackObj.get("hasCustom")?.asBoolean() == true
                        if (containerVersion < VERSION_MULTIPLE_TEX_PACK_ADDED && hasCustomPack) {
                            zipFile.getInputStream(zipFile.getFileHeader("res/texture_pack.zip"))
                                .use { zipInputStream ->
                                    val tempFile = TempFileUtils.createTempFile("extres", ".zip")
                                    val out = tempFile.outputStream()
                                    zipInputStream.copyTo(out)
                                    val f = ZipFile(tempFile)
                                    val readResult = CustomTexturePack.readFromStream(f)
                                    customTexturePacksRead[0] = readResult
                                    tempFile.delete()
                                }
                        } else if (containerVersion >= VERSION_MULTIPLE_TEX_PACK_ADDED && hasCustomPack) {
                            val slotCount = texturePackObj.get("slotCount").asInt()
                            val presentIndicesArr = texturePackObj.get("presentIndices").asArray()
                            val presentIndices: Set<Int> =
                                presentIndicesArr.filter { it.isNumber }.map { it.asInt() }.toSet()
                            for (i in 0..<min(slotCount, this.customTexturePacks.size)) {
                                if (i in presentIndices) {
                                    zipFile.getInputStream(zipFile.getFileHeader("res/texture_pack_${i}.zip"))
                                        .use { zipInputStream ->
                                            val tempFile = TempFileUtils.createTempFile("extres", ".zip")
                                            val out = tempFile.outputStream()
                                            zipInputStream.copyTo(out)
                                            val f = ZipFile(tempFile)
                                            val readResult = CustomTexturePack.readFromStream(f)
                                            customTexturePacksRead[i] = readResult
                                            tempFile.delete()
                                        }
                                }
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
        if (containerVersion >= 12 && EditorSpecialFlags.STORY_MODE in editorFlags) {
            val storyModeMetadataObj = json.get("storyModeMetadata")?.asObject()
            if (storyModeMetadataObj != null) {
                this.storyModeMetadata.set(StoryModeContainerMetadata.fromJson(storyModeMetadataObj))
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
            } else if (containerVersion >= 11) {
                if (!inst.canBeShown(editorFlags)) {
                    if (instID != null) {
                        Paintbox.LOGGER.warn("[Container] Instantiator '$instID' is not compatible with current editor flags $editorFlags, skipping")
                    }
                    continue
                }
            }
            val block: Block = inst.factory.invoke(inst, engine)
            block.readFromJson(obj, editorFlags)
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

        var levelBannerFile: File? = null
        val bannerHeader = zipFile.getFileHeader("banner.png")
        if (bannerHeader != null) {
            zipFile.getInputStream(bannerHeader).use { zipInputStream ->
                val tempFile = TempFileUtils.createTempFile("banner", ".png")
                val out = tempFile.outputStream()
                zipInputStream.copyTo(out)
                levelBannerFile = tempFile
            }
        }

        zipFile.closeQuietly()

        return LoadMetadata(this, libraryRelevantData, customTexturePacksRead, levelBannerFile)
    }

    class LoadMetadata(
        val container: Container, val libraryRelevantData: LibraryRelevantData,
        val customTexturePacksRead: Array<CustomTexturePack.ReadResult?>,
        val levelBannerFile: File?,
    ) {

        val containerVersion: Int = libraryRelevantData.containerVersion
        val programVersion: Version = libraryRelevantData.programVersion
        val isFutureVersion: Boolean = (programVersion > PRMania.VERSION) || (containerVersion > CONTAINER_VERSION)

        /**
         * Must be called on the GL thread.
         */
        fun loadOnGLThread() {
            customTexturePacksRead.forEachIndexed { index, readResult ->
                if (readResult != null) {
                    val ctp = readResult.createAndLoadTextures()
                    container.customTexturePacks[index].set(ctp)
                }
            }
            container.setTexturePackFromSource()

            if (levelBannerFile != null) {
                try {
                    val tex = Texture(FileHandle(levelBannerFile))
                    tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

                    if (!isBannerTextureWithinSize(tex)) {
                        Paintbox.LOGGER.warn("Ignoring banner texture because it is not the right size (${tex.width}x${tex.height})")
                        tex.disposeQuietly()
                    } else {
                        val old = container.bannerTexture.getOrCompute()
                        container.bannerTexture.set(null)
                        old?.disposeQuietly()
                        container.bannerTexture.set(tex)
                    }
                } catch (e: Exception) {
                    Paintbox.LOGGER.error("Failed to load banner texture!")
                    e.printStackTrace()
                }
            }
        }
    }
}