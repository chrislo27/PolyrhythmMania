package polyrhythmmania.storymode.gamemode.boss.scripting

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.storymode.gamemode.boss.BossModifierModule
import polyrhythmmania.storymode.gamemode.boss.StoryBossGameMode
import java.util.concurrent.CopyOnWriteArrayList


class Script(
    val startBeat: Float,
    val gamemode: StoryBossGameMode,

    /** The amount of time early to place events.
     * Note that the event start beat will still be accurate,
     * but it will be added to the [Engine] this many beats earlier. */
    val beatRunahead: Float,
) : Event(gamemode.engine) {

    companion object {

        private const val LOGGER_TAG: String = "Script"

    }

    val modifierModule: BossModifierModule = gamemode.modifierModule

    private var isStopped: Boolean = false
    private var internalBeat: Float = 0f // Only advanced by Rest
    private var restUntilEngineBeat: Float = startBeat - beatRunahead

    private val eventQueue: MutableList<Event> = CopyOnWriteArrayList()
    private val tempEventsToRemove: MutableList<Event> = mutableListOf<Event>()

    init {
        this.beat = startBeat - beatRunahead
        this.width = Float.POSITIVE_INFINITY // Used to keep this event running forever
    }

    fun addEventsToQueue(events: List<Event>): Script {
        eventQueue.addAll(events)
        return this
    }

    fun addEventToQueue(event: Event): Script {
        eventQueue.add(event)
        return this
    }

    override fun onUpdate(currentBeat: Float) {
        if (isStopped) return

        if (currentBeat < restUntilEngineBeat) {
            return
        }

        val beatOrigin = this.startBeat

        val eventIterator = eventQueue.iterator()
        val toRemove = this.tempEventsToRemove
        while (eventIterator.hasNext() && currentBeat >= restUntilEngineBeat) {
            val evt = eventIterator.next()

            if (evt is Rest) {
                this.restUntilEngineBeat = beatOrigin + this.internalBeat + evt.restDuration - this.beatRunahead
                this.internalBeat += evt.restDuration
//                Paintbox.LOGGER.debug("Got rest for ${evt.restDuration} beats, resting until engine beat ${this.restUntilEngineBeat} (currently ${currentBeat})", tag = LOGGER_TAG)
            } else {
                evt.beat += beatOrigin + internalBeat
                engine.addEvent(evt)
//                Paintbox.LOGGER.debug("Added event ${evt.javaClass.name} at beat ${evt.beat}, currently beat ${engine.beat}", tag = LOGGER_TAG)
            }

            toRemove += evt // Note: Iterator#remove is not supported for this list impl, so an external list is used
        }

        eventQueue.removeAll(toRemove)
        toRemove.clear()
    }

    fun stop() {
        isStopped = true
    }

    override fun readyToDelete(): Boolean {
        return isStopped
    }
}
