package polyrhythmmania.storymode.gamemode

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.block.GenericBlock
import polyrhythmmania.engine.EventChangePlaybackSpeed
import polyrhythmmania.util.TempFileUtils
import java.util.*


class StoryGameModeFromFile(main: PRManiaGame, val file: FileHandle, val percentSpeed: Int = 100) :
    AbstractStoryGameMode(main) {

    init {
        val tmpFile = TempFileUtils.createTempFile("storylevel")
        file.read().buffered().use { input ->
            tmpFile.outputStream().buffered().use { output ->
                input.copyTo(output)
            }
        }
        val loadMetadata = this.container.readFromFile(tmpFile, EnumSet.of(EditorSpecialFlags.STORY_MODE))
        Gdx.app.postRunnable { loadMetadata.loadOnGLThread() }
        tmpFile.deleteOnExit()
    }

    override fun initialize() {
        if (percentSpeed != 100) {
            container.addBlock(
                GenericBlock(engine, true) {
                    listOf(
                        EventChangePlaybackSpeed(this.engine, percentSpeed / 100f)
                    )
                }
            )
        }
    }
}