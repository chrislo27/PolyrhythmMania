package polyrhythmmania.engine.modifiers

class PerfectChallengeData : ModifierModule {
    
    // Settings
    var goingForPerfect: Boolean = false
    
    // Data
    var hit: Float = 0f
    var failed: Boolean = false
    
    override fun resetState() {
        hit = 0f
        failed = false
    }
}
