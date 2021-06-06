package polyrhythmmania.screen

import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.container.Container


class PlayScreen(main: PRManiaGame, val container: Container)
    : PRManiaScreen(main) {
    
    override fun dispose() {
        container.disposeQuietly()
    }
}