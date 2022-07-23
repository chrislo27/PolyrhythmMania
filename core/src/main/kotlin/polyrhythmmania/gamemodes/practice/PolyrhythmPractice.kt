package polyrhythmmania.gamemodes.practice

import com.eclipsesource.json.Json
import net.beadsproject.beads.ugens.SamplePlayer
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.block.Block
import polyrhythmmania.editor.block.Instantiator
import polyrhythmmania.editor.block.Instantiators
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.music.MusicVolume
import polyrhythmmania.engine.tempo.TempoChange
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.gamemodes.SidemodeAssets
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.LoopParams
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.world.WorldSettings
import polyrhythmmania.world.tileset.TilesetPalette
import java.util.*


abstract class AbstractPolyrhythmPractice(main: PRManiaGame, val flagBit: Int)
    : GameMode(main, PlayTimeType.REGULAR) {
    
    companion object {
        fun parseBlocksJson(json: String, engine: Engine): List<Block> {
            val blocksObj = Json.parse(json).asArray()
            val instantiators = Instantiators.instantiatorMap
            val blocks: MutableList<Block> = mutableListOf()
            for (value in blocksObj) {
                val obj = value.asObject()
                @Suppress("UNCHECKED_CAST")
                val inst = (instantiators[obj.getString("inst", null)] as? Instantiator<Block>?) ?: continue
                val block: Block = inst.factory.invoke(inst, engine)
                block.readFromJson(obj, EnumSet.noneOf(EditorSpecialFlags::class.java))
                blocks.add(block)
            }
            blocks.sortWith(Block.getComparator())
            return blocks
        }
    }
}

class Polyrhythm1Practice(main: PRManiaGame)
    : AbstractPolyrhythmPractice(main, 0b0001) {
    
    companion object {
        // Container version 6
        const val BLOCKS_JSON: String = """[{"inst":"spawnPattern","b":8,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,0,0,1,0,0,0,2,2],"dpad":[0,0,0,0,0,0,0,0,0,0]}},{"inst":"deployRod","b":12,"w":1,"t":0,"rowSetting":0},{"inst":"deployRod","b":16,"w":1,"t":0,"rowSetting":0},{"inst":"retractPistons","b":15.5,"w":1,"t":1,"rowSetting":2},{"inst":"retractPistons","b":22,"w":1,"t":0,"rowSetting":2},{"inst":"despawnPattern","b":23,"w":1,"t":0,"rowSetting":2},{"inst":"condApplause","b":20.25,"w":1,"t":2,"rowSetting":0},{"inst":"despawnPattern","b":39,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":38,"w":1,"t":0,"rowSetting":2},{"inst":"deployRod","b":32,"w":1,"t":0,"rowSetting":0},{"inst":"retractPistons","b":31.5,"w":1,"t":1,"rowSetting":2},{"inst":"deployRod","b":28,"w":1,"t":0,"rowSetting":0},{"inst":"condApplause","b":36.25,"w":1,"t":2,"rowSetting":0},{"inst":"spawnPattern","b":24,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,0,0,1,0,0,0,2,2],"dpad":[0,0,0,0,0,0,0,0,0,0]}},{"inst":"retractPistons","b":54,"w":1,"t":0,"rowSetting":2},{"inst":"condApplause","b":52.25,"w":1,"t":2,"rowSetting":0},{"inst":"deployRod","b":44,"w":1,"t":0,"rowSetting":0},{"inst":"spawnPattern","b":40,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[0,0,0,0,0,0,0,0,0,0]}},{"inst":"retractPistons","b":47.5,"w":1,"t":1,"rowSetting":2},{"inst":"deployRod","b":48,"w":1,"t":0,"rowSetting":0},{"inst":"despawnPattern","b":55,"w":1,"t":0,"rowSetting":2},{"inst":"condApplause","b":84.25,"w":1,"t":2,"rowSetting":1},{"inst":"despawnPattern","b":87,"w":1,"t":0,"rowSetting":2},{"inst":"deployRod","b":80,"w":1,"t":0,"rowSetting":1},{"inst":"retractPistons","b":86,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":72,"w":4,"t":0,"patternData":{"rowCount":10,"a":[0,0,0,0,0,0,0,0,0,0],"dpad":[1,0,1,0,1,0,1,0,2,2]}},{"inst":"retractPistons","b":79.5,"w":1,"t":1,"rowSetting":2},{"inst":"deployRod","b":76,"w":1,"t":0,"rowSetting":1},{"inst":"condApplause","b":100.25,"w":1,"t":2,"rowSetting":1},{"inst":"deployRod","b":92,"w":1,"t":0,"rowSetting":1},{"inst":"retractPistons","b":102,"w":1,"t":0,"rowSetting":2},{"inst":"despawnPattern","b":103,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":88,"w":4,"t":0,"patternData":{"rowCount":10,"a":[0,0,0,0,0,0,0,0,0,0],"dpad":[1,0,1,0,1,0,1,0,2,2]}},{"inst":"deployRod","b":96,"w":1,"t":0,"rowSetting":1},{"inst":"retractPistons","b":95.5,"w":1,"t":1,"rowSetting":2},{"inst":"retractPistons","b":118,"w":1,"t":0,"rowSetting":2},{"inst":"deployRod","b":108,"w":1,"t":0,"rowSetting":2},{"inst":"condApplause","b":116.25,"w":1,"t":2,"rowSetting":2},{"inst":"spawnPattern","b":104,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,0,0,1,0,0,0,2,2],"dpad":[2,2,1,0,0,0,1,0,0,0]}},{"inst":"deployRod","b":112,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":111.5,"w":1,"t":1,"rowSetting":0},{"inst":"despawnPattern","b":119,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":112,"w":1,"t":2,"rowSetting":1},{"inst":"retractPistons","b":128,"w":1,"t":2,"rowSetting":1},{"inst":"deployRod","b":124,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":120,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,0,0,1,0,0,0,2,2],"dpad":[2,2,1,0,0,0,1,0,0,0]}},{"inst":"deployRod","b":128,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":134,"w":1,"t":0,"rowSetting":2},{"inst":"despawnPattern","b":135,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":127.5,"w":1,"t":1,"rowSetting":0},{"inst":"condApplause","b":132.25,"w":1,"t":2,"rowSetting":2},{"inst":"deployRod","b":140,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":136,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[1,0,1,0,1,0,1,0,2,2]}},{"inst":"retractPistons","b":150,"w":1,"t":0,"rowSetting":2},{"inst":"despawnPattern","b":151,"w":1,"t":0,"rowSetting":2},{"inst":"condApplause","b":148.25,"w":1,"t":2,"rowSetting":2},{"inst":"deployRod","b":144,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":143.5,"w":1,"t":1,"rowSetting":2},{"inst":"despawnPattern","b":167,"w":1,"t":0,"rowSetting":2},{"inst":"deployRod","b":156,"w":1,"t":0,"rowSetting":2},{"inst":"condApplause","b":164.25,"w":1,"t":2,"rowSetting":2},{"inst":"retractPistons","b":166,"w":1,"t":0,"rowSetting":2},{"inst":"deployRod","b":160,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":152,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[1,0,1,0,1,0,1,0,2,2]}},{"inst":"retractPistons","b":182,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":168,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[2,2,1,0,0,0,1,0,0,0]}},{"inst":"deployRod","b":176,"w":1,"t":0,"rowSetting":2},{"inst":"deployRod","b":172,"w":1,"t":0,"rowSetting":2},{"inst":"condApplause","b":180.25,"w":1,"t":2,"rowSetting":2},{"inst":"retractPistons","b":175.5,"w":1,"t":1,"rowSetting":0},{"inst":"despawnPattern","b":183,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":159.5,"w":1,"t":1,"rowSetting":2},{"inst":"retractPistons","b":176,"w":1,"t":2,"rowSetting":1},{"inst":"deployRod","b":64,"w":1,"t":0,"rowSetting":0},{"inst":"retractPistons","b":70,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":63.5,"w":1,"t":1,"rowSetting":2},{"inst":"spawnPattern","b":56,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[0,0,0,0,0,0,0,0,0,0]}},{"inst":"condApplause","b":68.25,"w":1,"t":2,"rowSetting":0},{"inst":"despawnPattern","b":71,"w":1,"t":0,"rowSetting":2},{"inst":"deployRod","b":60,"w":1,"t":0,"rowSetting":0},{"inst":"retractPistons","b":191.5,"w":1,"t":1,"rowSetting":0},{"inst":"condApplause","b":196.25,"w":1,"t":2,"rowSetting":2},{"inst":"despawnPattern","b":199,"w":1,"t":0,"rowSetting":2},{"inst":"deployRod","b":192,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":184,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[2,2,1,0,0,0,1,0,0,0]}},{"inst":"deployRod","b":188,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":192,"w":1,"t":2,"rowSetting":1},{"inst":"retractPistons","b":198,"w":1,"t":0,"rowSetting":2},{"inst":"endState","b":205,"w":2,"t":0}]"""
    }
    
    init {
        TilesetPalette.createGBA1TilesetPalette().applyTo(container.renderer.tileset)
        container.world.tilesetPalette.copyFrom(container.renderer.tileset)
        
        container.world.worldSettings = WorldSettings(showInputIndicators = true)
    }
    
    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, 129f))

        val music: BeadsMusic = SidemodeAssets.polyrhythmTheme
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f
        musicData.loopParams = LoopParams(SamplePlayer.LoopType.LOOP_FORWARDS, 0.0, music.musicSample.lengthMs)
        musicData.rate = 1f
        musicData.firstBeatSec = 0f
        musicData.beadsMusic = music
        musicData.volumeMap.addMusicVolume(MusicVolume(0f, 0f, 100))
        musicData.volumeMap.addMusicVolume(MusicVolume(200f, 4f, 0))
        musicData.update()
        
        container.addBlocks(parseBlocksJson(BLOCKS_JSON, engine))
    }

}

class Polyrhythm2Practice(main: PRManiaGame)
    : AbstractPolyrhythmPractice(main, 0b0010) {

    companion object {
        // Container version 6
        const val BLOCKS_JSON: String = """[{"inst":"spawnPattern","b":8,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[0,0,0,0,0,0,0,0,0,0]}},{"inst":"deployRod","b":12,"w":1,"t":0,"rowSetting":0},{"inst":"retractPistons","b":15.5,"w":1,"t":1,"rowSetting":2},{"inst":"deployRod","b":16,"w":1,"t":0,"rowSetting":0},{"inst":"retractPistons","b":22,"w":1,"t":0,"rowSetting":2},{"inst":"despawnPattern","b":23,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":24,"w":4,"t":0,"patternData":{"rowCount":10,"a":[2,1,0,1,0,1,0,1,0,2],"dpad":[0,0,0,0,0,0,0,0,0,0]}},{"inst":"deployRod","b":28,"w":1,"t":0,"rowSetting":0},{"inst":"retractPistons","b":32,"w":1,"t":1,"rowSetting":2},{"inst":"deployRod","b":32,"w":1,"t":0,"rowSetting":0},{"inst":"retractPistons","b":38,"w":1,"t":0,"rowSetting":2},{"inst":"despawnPattern","b":39,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":72,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[2,2,2,2,2,2,1,0,2,2]}},{"inst":"deployRod","b":76,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":79.5,"w":1,"t":1,"rowSetting":2},{"inst":"deployRod","b":80,"w":1,"t":0,"rowSetting":2},{"inst":"condApplause","b":84.25,"w":1,"t":2,"rowSetting":1},{"inst":"retractPistons","b":86,"w":1,"t":0,"rowSetting":2},{"inst":"despawnPattern","b":87,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":104,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[2,2,2,1,0,0,1,0,0,2]}},{"inst":"deployRod","b":108,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":111.5,"w":1,"t":1,"rowSetting":2},{"inst":"deployRod","b":112,"w":1,"t":0,"rowSetting":2},{"inst":"condApplause","b":116.25,"w":1,"t":2,"rowSetting":2},{"inst":"retractPistons","b":118,"w":1,"t":0,"rowSetting":2},{"inst":"despawnPattern","b":119,"w":1,"t":0,"rowSetting":2},{"inst":"endState","b":173,"w":2,"t":0},{"inst":"retractPistons","b":70,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":40,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[0,0,0,0,0,0,0,0,0,0]}},{"inst":"spawnPattern","b":56,"w":4,"t":0,"patternData":{"rowCount":10,"a":[2,1,0,1,0,1,0,1,0,2],"dpad":[0,0,0,0,0,0,0,0,0,0]}},{"inst":"retractPistons","b":54,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":47.5,"w":1,"t":1,"rowSetting":2},{"inst":"deployRod","b":44,"w":1,"t":0,"rowSetting":0},{"inst":"despawnPattern","b":55,"w":1,"t":0,"rowSetting":2},{"inst":"despawnPattern","b":71,"w":1,"t":0,"rowSetting":2},{"inst":"deployRod","b":60,"w":1,"t":0,"rowSetting":0},{"inst":"deployRod","b":64,"w":1,"t":0,"rowSetting":0},{"inst":"retractPistons","b":64,"w":1,"t":1,"rowSetting":2},{"inst":"deployRod","b":48,"w":1,"t":0,"rowSetting":0},{"inst":"deployRod","b":96,"w":1,"t":0,"rowSetting":2},{"inst":"despawnPattern","b":103,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":95.5,"w":1,"t":1,"rowSetting":2},{"inst":"deployRod","b":92,"w":1,"t":0,"rowSetting":2},{"inst":"condApplause","b":100.25,"w":1,"t":2,"rowSetting":1},{"inst":"spawnPattern","b":88,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[2,2,2,2,2,2,1,0,2,2]}},{"inst":"retractPistons","b":102,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":120,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[2,2,2,1,0,0,1,0,0,2]}},{"inst":"deployRod","b":128,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":127.5,"w":1,"t":1,"rowSetting":2},{"inst":"despawnPattern","b":135,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":134,"w":1,"t":0,"rowSetting":2},{"inst":"deployRod","b":124,"w":1,"t":0,"rowSetting":2},{"inst":"condApplause","b":132.25,"w":1,"t":2,"rowSetting":2},{"inst":"retractPistons","b":159.5,"w":1,"t":1,"rowSetting":2},{"inst":"condApplause","b":164.25,"w":1,"t":2,"rowSetting":2},{"inst":"retractPistons","b":166,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":136,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[1,0,0,1,0,0,1,0,0,2]}},{"inst":"deployRod","b":144,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":143.5,"w":1,"t":1,"rowSetting":2},{"inst":"deployRod","b":160,"w":1,"t":0,"rowSetting":2},{"inst":"despawnPattern","b":151,"w":1,"t":0,"rowSetting":2},{"inst":"retractPistons","b":150,"w":1,"t":0,"rowSetting":2},{"inst":"deployRod","b":140,"w":1,"t":0,"rowSetting":2},{"inst":"despawnPattern","b":167,"w":1,"t":0,"rowSetting":2},{"inst":"condApplause","b":148.25,"w":1,"t":2,"rowSetting":2},{"inst":"deployRod","b":156,"w":1,"t":0,"rowSetting":2},{"inst":"spawnPattern","b":152,"w":4,"t":0,"patternData":{"rowCount":10,"a":[1,0,1,0,1,0,1,0,2,2],"dpad":[1,0,0,1,0,0,1,0,0,2]}}]"""
    }

    init {
        TilesetPalette.createGBA2TilesetPalette().applyTo(container.renderer.tileset)
        container.world.tilesetPalette.copyFrom(container.renderer.tileset)
    }

    override fun initialize() {
        engine.tempos.addTempoChange(TempoChange(0f, 148.5f))

        val music: BeadsMusic = SidemodeAssets.polyrhythmTheme
        val musicData = engine.musicData
        musicData.musicSyncPointBeat = 0f
        musicData.loopParams = LoopParams(SamplePlayer.LoopType.LOOP_FORWARDS, 0.0, music.musicSample.lengthMs)
        musicData.rate = 148.5f / 129f
        musicData.firstBeatSec = 0f
        musicData.beadsMusic = music
        musicData.volumeMap.addMusicVolume(MusicVolume(0f, 0f, 100))
        musicData.volumeMap.addMusicVolume(MusicVolume(168f, 4f, 0))
        musicData.update()

        container.addBlocks(parseBlocksJson(BLOCKS_JSON, engine))
    }

}
