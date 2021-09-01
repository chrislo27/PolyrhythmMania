package polyrhythmmania.editor

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.EventPlaySFX
import polyrhythmmania.util.Semitones


class EventEditorMetronome(engine: Engine, startBeat: Float)
    : EventPlaySFX(engine, startBeat, "sfx_cowbell", true, { player ->
    val measurePart = engine.timeSignatures.getMeasurePart(startBeat)
    player.pitch = Semitones.getALPitch(if (measurePart <= -1) 0 else if (measurePart == 0) 8 else 3)
})
