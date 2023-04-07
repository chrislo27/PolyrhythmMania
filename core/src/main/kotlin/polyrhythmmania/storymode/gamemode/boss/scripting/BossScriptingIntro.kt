package polyrhythmmania.storymode.gamemode.boss.scripting

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.gamemode.boss.StoryBossGameMode
import polyrhythmmania.storymode.music.StoryMusicAssets
import polyrhythmmania.world.EventMoveCameraRelative
import polyrhythmmania.world.EventZoomCamera
import polyrhythmmania.world.tileset.PaletteTransition
import polyrhythmmania.world.tileset.TransitionCurve


class BossScriptIntro(
    gamemode: StoryBossGameMode,
    script: Script,
    val shouldShowIntroLights: Boolean,
    val phase1Factory: ((BossScriptIntro) -> BossScriptFunction)?
) : BossScriptFunction(gamemode, script) {

    private fun MutableList<Event>.moveCamera(): MutableList<Event> {
        val zoomTransition = PaletteTransition.DEFAULT.copy(duration = 4f, transitionCurve = TransitionCurve.SMOOTHER)
        val startZoom = 1f
        val endZoom = 1.55f
        this += EventZoomCamera(engine, 0f, zoomTransition, startZoom, endZoom)
        this += EventMoveCameraRelative(engine, 0f, zoomTransition, Vector3(1f, 1f, 0f))

        return this
    }

    private fun MutableList<Event>.playSpotlightSfx(): MutableList<Event> =
        this.playSfx(StoryAssets["sfx_boss_spotlight"])

    private fun MutableList<Event>.playSpotlightSfxShort(): MutableList<Event> =
        this.playSfx(StoryAssets["sfx_boss_spotlight_short"])

    private fun showLightsIntro(events: MutableList<Event>) {
        events
            .changeLightStrength(LightStrength.DARK_BOSS_INTRO, 2.0f)
            .rest(2.0f)


        fun doLightsAnimation(side: Boolean) {
            events
                .targetLights(side, setOf(0))
                .changeLightStrength(LightStrength.DARK_BOSS_INTRO, 1.0f, transitionCurve = TransitionCurve.BOUNCE_OUT)
                .playSpotlightSfx()
                .rest(2.0f)
                .targetLights(side, setOf(0, 3))
                .changeLightStrength(LightStrength.DARK_BOSS_INTRO, 0.0f)
                .playSpotlightSfxShort()
                .rest(0.5f)
                .targetLights(side, setOf(0, 3, 6))
                .changeLightStrength(LightStrength.DARK_BOSS_INTRO, 0.0f)
                .playSpotlightSfxShort()
                .rest(0.5f)
                .targetLights(side, setOf(0, 3, 6, 9))
                .changeLightStrength(LightStrength.DARK_BOSS_INTRO, 0.0f)
                .playSpotlightSfxShort()
                .rest(0.5f)
                .targetLights(side, setOf(0, 3, 6, 9, 11))
                .changeLightStrength(LightStrength.DARK_BOSS_INTRO, 0.0f)
                .playSpotlightSfxShort()
                .rest(3.0f)
        }
        doLightsAnimation(SIDE_UPSIDE)
        doLightsAnimation(SIDE_DOWNSIDE)
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

        val events = mutableListOf<Event>()
        
        if (shouldShowIntroLights) {
            showLightsIntro(events)
        }
        
        events
            .changeLightStrength(LightStrength.NORMAL, 1.0f)
        
        events
            .music(StoryMusicAssets.STEM_ID_BOSS_1_INTRO, 6, extraBeatDuration = EXTRA_BEATS_SPACING)
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

            .spawnPattern(patternPools.introPattern, flipChance = NO_FLIP_CHANCE)
            .rest(4.0f)
            .spawnRods()
            .rest(4.0f)

            .addFunctionAsEvent(phase1Factory?.invoke(this) ?: BossScriptPhase1(gamemode, script))
        
        return events
    }
}
