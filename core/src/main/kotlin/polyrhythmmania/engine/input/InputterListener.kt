package polyrhythmmania.engine.input

import polyrhythmmania.world.EntityRodPR


interface InputterListener {
    
    fun onMissed(inputter: EngineInputter, firstMiss: Boolean)
    
    fun onInputResultHit(inputter: EngineInputter, result: InputResult, countsAsMiss: Boolean)
    
    fun onSkillStarHit(beat: Float)
    
    fun onRodPRExploded(rod: EntityRodPR, inputter: EngineInputter, countedAsMiss: Boolean)
    
}