package polyrhythmmania.engine.tempo


data class TempoChange(val beat: Float, val newTempo: Float,
                       val newSwing: Swing = Swing.STRAIGHT)
