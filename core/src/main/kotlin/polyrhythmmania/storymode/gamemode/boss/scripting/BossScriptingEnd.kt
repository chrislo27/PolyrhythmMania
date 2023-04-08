package polyrhythmmania.storymode.gamemode.boss.scripting

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.gamemode.boss.*
import polyrhythmmania.world.EventEndState
import polyrhythmmania.world.EventMoveCameraRelative
import polyrhythmmania.world.EventZoomCamera
import polyrhythmmania.world.tileset.PaletteTransition
import polyrhythmmania.world.tileset.TransitionCurve


class BossScriptEnd(
    gamemode: StoryBossGameMode,
    script: Script,
) : BossScriptFunction(gamemode, script) {
    
    private fun getBossEntities(): List<AbstractEntityBossRobot> = world.entities.filterIsInstance<AbstractEntityBossRobot>()
    private fun getFaceEntities(): List<EntityBossRobotFace> = world.entities.filterIsInstance<EntityBossRobotFace>()
    private fun getMiddleEntities(): List<EntityBossRobotMiddle> = world.entities.filterIsInstance<EntityBossRobotMiddle>()

    private fun MutableList<Event>.moveCamera(duration: Float): MutableList<Event> {
        val zoomTransition = PaletteTransition.DEFAULT.copy(duration = duration, transitionCurve = TransitionCurve.SMOOTHER)
        val startZoom = 1.55f
        val endZoom = 0.85f
        this += EventZoomCamera(engine, 0f, zoomTransition, startZoom, endZoom)
        this += EventMoveCameraRelative(engine, 0f, zoomTransition, Vector3(2f, 2f, 0f), useOriginalCameraPositionToStart = true)

        return this
    }
    
    private fun MutableList<Event>.changeFaceTexture(face: EntityBossRobotFace.Face): MutableList<Event> {
        this += object : Event(engine) {
            override fun onStart(currentBeat: Float) {
                Gdx.app.postRunnable {
                    getFaceEntities().forEach { it.currentFace = face }
                }
            }
        }

        return this
    }
    
    private fun MutableList<Event>.setBodyToExploded(): MutableList<Event> {
        this += object : Event(engine) {
            override fun onStart(currentBeat: Float) {
                Gdx.app.postRunnable {
                    getMiddleEntities().forEach { it.currentTextureID = "boss_robot_middle_exploded" }
                }
            }
        }

        return this
    }
    
    private fun MutableList<Event>.addExplosionEntity(): MutableList<Event> {
        this += object : Event(engine) {
            override fun onStart(currentBeat: Float) {
                Gdx.app.postRunnable {
                    val pos = StoryBossGameMode.BOSS_POSITION.cpy()
                    pos.x += 0.5f
                    pos.y += 0.5f
                    world.addEntity(
                        EntityBossExplosion(
                            world,
                            engine.tempos.beatsToSeconds(currentBeat),
                            pos
                        )
                    )
                }
            }
        }

        return this
    }
    
    override fun getEvents(): List<Event> {
        val events = mutableListOf<Event>()
        
        events
            .despawnPattern()
            .changeLightStrength(LightStrength.NORMAL, 1.0f)
            .rest(1.0f)
            
            .rest(4.0f)

            .targetLights(SIDE_DOWNSIDE, emptySet())
            .targetLights(SIDE_UPSIDE, emptySet())
            .changeLightStrength(LightStrength(0.25f, 1.0f), 4.0f)
            .moveCamera(4.0f)
            .addEvent("boss_health_bar_hide", object : Event(engine) {
                override fun onStart(currentBeat: Float) {
                    Gdx.app.postRunnable {
                        modifierModule.triggerUIHide()
                    }
                }
            })
            
            .playSfx(StoryAssets["sfx_boss_whirring"])
            .addEvent("start_jittering", object : Event(engine) {
                init {
                    this.width = 7f
                }

                override fun onUpdate(currentBeat: Float) {
                    val progress = getBeatPercentage(currentBeat)
                    
                    Gdx.app.postRunnable {
                        getBossEntities().forEach {
                            it.jitterAmplitude = Interpolation.pow3.apply(0f, 4f, progress)
                        }
                    }
                }
            })
            .rest(7.0f)
        
            .playSfx(StoryAssets["sfx_boss_error"])
            .changeFaceTexture(EntityBossRobotFace.Face.BLUE_SCREEN)
            .addEvent("stop_movement_bobbing_and_jitter", object : Event(engine) {
                override fun onStart(currentBeat: Float) {
                    Gdx.app.postRunnable {
                        getBossEntities().forEach {
                            it.jitterAmplitude = 0f    
                            it.stopBobbing = true
                        }
                    }
                }
            })
            .rest(9.0f)
            
            .changeFaceTexture(EntityBossRobotFace.Face.NONE)
            .setBodyToExploded()
            .playSfx(StoryAssets["sfx_boss_explosion"])
            .addExplosionEntity()
            .rest(14.0f)
        
            .todo("complete level, no drum beat")
            .addEvent(EventEndState(engine, 0f))
        
        return events
    }
}
