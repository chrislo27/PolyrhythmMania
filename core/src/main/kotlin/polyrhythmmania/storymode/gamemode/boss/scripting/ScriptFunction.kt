package polyrhythmmania.storymode.gamemode.boss.scripting

import paintbox.Paintbox
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event

abstract class ScriptFunction(val script: Script) {

    protected val engine: Engine = script.engine

    abstract fun getEvents(): List<Event>

    fun MutableList<Event>.addEvent(evt: Event): MutableList<Event> {
        this.add(evt)
        return this
    }

    fun MutableList<Event>.addEvent(descriptor: String, evt: Event): MutableList<Event> {
        this.add(evt)
        return this
    }

    fun MutableList<Event>.addFunctionAsEvent(func: ScriptFunction): MutableList<Event> =
        this.addEvent(object : Event(this@ScriptFunction.engine) {
            init {
                this.beat = -script.beatRunahead
            }

            override fun onStart(currentBeat: Float) {
                script.addEventsToQueue(func.getEvents())
            }
        })

    fun MutableList<Event>.rest(duration: Float): MutableList<Event> =
        this.addEvent(Rest(duration, this@ScriptFunction.engine))

    fun MutableList<Event>.todo(desc: String): MutableList<Event> =
        this.addEvent(object : Event(engine) {
            override fun onStart(currentBeat: Float) {
                Paintbox.LOGGER.debug("Unimplemented event: $desc", "BossScriptFunction")
            }
        })

    @Suppress("UNUSED_PARAMETER")
    fun MutableList<Event>.note(note: String): MutableList<Event> = this

}
