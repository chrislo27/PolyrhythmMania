package polyrhythmmania.storymode.gamemode.boss.scripting

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.gamemode.boss.StoryBossGameMode
import polyrhythmmania.storymode.music.StoryMusicAssets
import polyrhythmmania.world.EventMoveCameraRelative
import polyrhythmmania.world.EventZoomCamera
import polyrhythmmania.world.tileset.PaletteTransition
import polyrhythmmania.world.tileset.TransitionCurve


class BossScriptIntro(gamemode: StoryBossGameMode, script: Script) : BossScriptFunction(gamemode, script) {

    private fun MutableList<Event>.moveCamera(): MutableList<Event> {
        val zoomTransition = PaletteTransition.DEFAULT.copy(duration = 4f, transitionCurve = TransitionCurve.SMOOTHER)
        val startZoom = 1f
        val endZoom = 1.55f
        this += EventZoomCamera(engine, 0f, zoomTransition, startZoom, endZoom)
        this += EventMoveCameraRelative(engine, 0f, zoomTransition, Vector3(1f, 1f, 0f))

        return this
    }

    override fun getEvents(): List<Event> {
        /*
    set_music(mus_story_boss1_intro);
    boss_title_appear();
    boss_appear();
    rest 8.0;
    boss_health_bar_appear();
    bg_scroll_start();
    rest 8.0;

    spawn_pattern("P---P---##|##P---P---");
    rest 4.0;
    spawn_rods();
    rest 4.0;

    retract_pistons();
    
    run boss1_main;
         */

        val pattern = patternPools.introPattern

        return mutableListOf<Event>()
            .music(StoryMusicAssets.STEM_ID_BOSS_1_INTRO, 6)
            .moveCamera()
            .note("boss_title_appear") // Handled by title card separately
            .todo("boss_appear")
            .rest(8.0f)
            .addEvent("boss_health_bar_appear", object : Event(engine) {
                override fun onStart(currentBeat: Float) {
                    Gdx.app.postRunnable {
                        modifierModule.triggerUIShow()
                    }
                }
            })
            .todo("bg_scroll_start")
            .rest(8.0f)

            .spawnPattern(pattern)
            .rest(4.0f)
            .spawnRods(pattern)
            .rest(4.0f)
            .retractPistons()

            .addFunctionAsEvent(BossScriptPhase1(gamemode, script))
    }
}
