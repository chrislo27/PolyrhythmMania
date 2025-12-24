package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.Gdx
import paintbox.binding.BooleanVar
import paintbox.util.gdxutils.GdxRunnableTransition
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.sample.MusicSamplePlayer
import polyrhythmmania.storymode.inbox.InboxDB
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.music.StoryMusicAssets
import polyrhythmmania.storymode.music.StoryMusicHandler


class BonusMusic(val desktopUI: DesktopUI) {
    
    companion object {
        private const val FADE_OUT_TIME: Float = 0.2f
    }
    
    val isPlaying: BooleanVar = BooleanVar(false)
    
    private val musicHandler: StoryMusicHandler by lazy { desktopUI.storySession.musicHandler }
    private val soundSystem: SoundSystem by lazy { musicHandler.soundSystem }
    private var currentPlayer: MusicSamplePlayer? = null
    
    init {
        isPlaying.addListener { v -> onPlayingSwitch(v.getOrCompute()) }
        desktopUI.currentInboxItem.addListener { v -> onInboxItemChange(v.getOrCompute()) }
        StoryMusicAssets.initBonusMusicStems()
    }
    
    private fun onPlayingSwitch(newState: Boolean) {
        if (newState) {
            musicHandler.fadeOut(FADE_OUT_TIME)
            createNewPlayer()
        } else {
            musicHandler.transitionToDesktopMix(desktopUI.scenario.inboxState)
            currentPlayer?.let { player ->
                Gdx.app.postRunnable(GdxRunnableTransition(player.gain, 0f, FADE_OUT_TIME) { _, newValue ->
                    player.gain = newValue
                    if (newValue <= 0f) {
                        player.kill()
                    }
                }.toRunnable())
            }
            currentPlayer = null
        }
    }
    
    private fun createNewPlayer() {
        currentPlayer?.kill()
        
        val stem = StoryMusicAssets.bonusMusicStems.getOrLoad(StoryMusicAssets.STEM_ID_BONUS) ?: return
        val audioContext = soundSystem.audioContext
        val player = MusicSamplePlayer(stem.sample, audioContext).apply { 
            this.killOnEnd = true
            this.position = -FADE_OUT_TIME * 2 * 1000.0;
            
            this.killListeners += { player ->
                Gdx.app.postRunnable {
                    if (currentPlayer === player) {
                        isPlaying.set(false)
                    }
                }
            }
        }
        audioContext.out.addInput(player)
        currentPlayer = player
    }
    
    private fun onInboxItemChange(inboxItem: InboxItem?) {
        if (isPlaying.get()) {
            if (inboxItem?.id != InboxDB.ITEM_WITH_END_OF_THE_ASSEMBLY_LINE_MUSIC) {
                isPlaying.set(false)
            }
        }
    }
}