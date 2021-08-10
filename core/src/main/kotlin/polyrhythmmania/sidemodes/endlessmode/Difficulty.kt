package polyrhythmmania.sidemodes.endlessmode

enum class Difficulty(val value: Int) {
    VERY_EASY(0),
    EASY(1),
    MEDIUM(2),
    HARD(3),
    ;
    
    companion object {
        val VALUES: List<Difficulty> = values().toList()
    }
}
