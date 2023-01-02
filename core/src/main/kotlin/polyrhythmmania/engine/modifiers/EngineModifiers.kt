package polyrhythmmania.engine.modifiers

import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.EngineInputter
import polyrhythmmania.engine.input.InputResult
import polyrhythmmania.engine.input.InputterListener
import polyrhythmmania.world.EntityRodPR


class EngineModifiers(val engine: Engine) : InputterListener {

    private val inputter: EngineInputter = engine.inputter

    val perfectChallenge: PerfectChallengeData = PerfectChallengeData(this)
    val endlessScore: EndlessScore = EndlessScore(this)
    val livesMode: LivesMode = LivesMode(this)
    val defectiveRodsMode: DefectiveRodsMode = DefectiveRodsMode(this)
    val monsterGoal: MonsterGoalData = MonsterGoalData(this)

    private val allModules: MutableList<ModifierModule> = mutableListOf(perfectChallenge, endlessScore, livesMode, defectiveRodsMode, monsterGoal)

    init {
        inputter.inputterListeners += this
    }

    fun addModifierModule(module: ModifierModule) {
        allModules += module
    }

    fun removeModifierModule(module: ModifierModule) {
        allModules -= module
    }
    
    fun getAllModifierModules(): List<ModifierModule> = allModules
    
    inline fun <reified M : ModifierModule> getModifierModuleByType(): M? =
            getAllModifierModules().find { it is M } as? M

    fun resetState() {
        allModules.forEach(ModifierModule::resetState)
    }

    fun engineUpdate(beat: Float, seconds: Float, deltaSec: Float) {
        allModules.forEach { module ->
            module.engineUpdate(beat, seconds, deltaSec)
        }
    }


    //region InputterListener overrides

    override fun onMissed(inputter: EngineInputter, firstMiss: Boolean) {
        allModules.forEach { it.onMissed(inputter, firstMiss) }
    }

    override fun onInputResultHit(inputter: EngineInputter, result: InputResult, countsAsMiss: Boolean) {
        allModules.forEach { it.onInputResultHit(inputter, result, countsAsMiss) }
    }

    override fun onSkillStarHit(beat: Float) {
        allModules.forEach { it.onSkillStarHit(beat) }
    }

    override fun onRodPRExploded(rod: EntityRodPR, inputter: EngineInputter, countedAsMiss: Boolean) {
        allModules.forEach { it.onRodPRExploded(rod, inputter, countedAsMiss) }
    }

    //endregion
}
