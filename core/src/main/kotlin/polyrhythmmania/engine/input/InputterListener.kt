package polyrhythmmania.engine.input


interface InputterListener {
    
    fun onMissed(inputter: EngineInputter, firstMiss: Boolean)
    
    fun onInputResultHit(inputter: EngineInputter, result: InputResult, countsAsMiss: Boolean)
    
    fun onSkillStarHit(beat: Float)
    
}