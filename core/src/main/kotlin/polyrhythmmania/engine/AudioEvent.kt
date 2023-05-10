package polyrhythmmania.engine


/**
 * An [AudioEvent] is an [Event] where the [onAudioStart], [onAudioUpdate], and [onAudioEnd] functions may be called
 * with early or late by the [Engine] to match input calibration settings.
 */
open class AudioEvent(engine: Engine) : Event(engine) {
    
    var audioUpdateCompletion: UpdateCompletion = UpdateCompletion.PENDING

    open fun onAudioStart(atBeat: Float, actualBeat: Float, inputCalibration: InputCalibration) {
    }

    open fun onAudioUpdate(atBeat: Float, actualBeat: Float) {
    }

    open fun onAudioEnd(atBeat: Float, actualBeat: Float) {
    }

    override fun readyToDelete(): Boolean {
        return super.readyToDelete() && audioUpdateCompletion == UpdateCompletion.COMPLETED
    }
}
