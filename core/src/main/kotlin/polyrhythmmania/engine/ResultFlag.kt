package polyrhythmmania.engine


/**
 * The result flag is used as a suggestion of how the player did when the end signal is fired in [Engine].
 * 
 * It is reset to the [NONE] value in [Engine.resetMutableState].
 */
enum class ResultFlag {
    NONE,
    
    PASS,
    FAIL,
}
