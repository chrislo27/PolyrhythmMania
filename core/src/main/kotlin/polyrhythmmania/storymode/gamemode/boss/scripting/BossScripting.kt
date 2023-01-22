package polyrhythmmania.storymode.gamemode.boss.scripting

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Event
import polyrhythmmania.engine.input.EventClearInputs
import polyrhythmmania.gamemodes.endlessmode.Pattern
import polyrhythmmania.storymode.gamemode.boss.*
import polyrhythmmania.storymode.music.StoryMusicAssets
import polyrhythmmania.world.*
import polyrhythmmania.world.tileset.PaletteTransition
import polyrhythmmania.world.tileset.TransitionCurve


abstract class BossScriptFunction(val gamemode: StoryBossGameMode, script: Script) : ScriptFunction(script) {
    
    protected val world: World get() = engine.world
    protected val modifierModule: BossModifierModule = gamemode.modifierModule

    protected fun MutableList<Event>.music(stemID: String, measures: Int): MutableList<Event> =
            this.addEvent(BossMusicEvent(engine, gamemode.stems, stemID, 0f, (measures * 4).toFloat()))

    
    protected fun MutableList<Event>.spawnPattern(pattern: Pattern): MutableList<Event> {
        val patternStart = 0f // Will be offset by Script, so should be zero

        this.addAll(pattern.toEvents(engine, patternStart))

        return this
    }
    
    protected fun MutableList<Event>.spawnRods(pattern: Pattern): MutableList<Event> {
        val patternStart = -4f
        
        val anyA = pattern.anyA
        val anyDpad = pattern.anyDpad
        val bossDamageMultiplier = if (anyA && anyDpad) 1 else 2
        val damageTakenVar = EntityRodPRStoryBoss.PlayerDamageTaken()

        if (anyA) {
            this.add(EventDeployRodBoss(engine, world.rowA, patternStart, damageTakenVar, bossDamageMultiplier))
        }
        if (anyDpad) {
            this.add(EventDeployRodBoss(engine, world.rowDpad, patternStart, damageTakenVar, bossDamageMultiplier))
        }

        return this
    }

    protected fun MutableList<Event>.retractPistons(): MutableList<Event> {
        val retractStartBeat = 0f

        this.add(EventRowBlockRetract(engine, world.rowA, 0, retractStartBeat, affectThisIndexAndForward = true))
        this.add(EventRowBlockRetract(engine, world.rowDpad, 0, retractStartBeat, affectThisIndexAndForward = true))

        return this
    }

    protected fun MutableList<Event>.despawnPattern(): MutableList<Event> {
        val despawnStartBeat = 0f
        
        this.add(EventRowBlockDespawn(engine, world.rowA, 0, despawnStartBeat, affectThisIndexAndForward = true))
        this.add(EventRowBlockDespawn(engine, world.rowDpad, 0, despawnStartBeat, affectThisIndexAndForward = true))
        this.add(EventClearInputs(engine, despawnStartBeat))

        return this
    }
    
}

class BossScriptIntro(gamemode: StoryBossGameMode, script: Script) : BossScriptFunction(gamemode, script) {

    private fun MutableList<Event>.moveCamera(): MutableList<Event> {
        val zoomTransition = PaletteTransition.DEFAULT.copy(duration = 4f, transitionCurve = TransitionCurve.SMOOTHER)
        val startZoom = 1f
        val endZoom = 1.3f
        
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

        val pattern = BossInputPatterns.firstPattern
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
