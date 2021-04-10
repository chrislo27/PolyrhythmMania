package polyrhythmmania.engine.input

enum class InputScore(val weight: Float) {
    ACE(1.0f), GOOD(0.9f), BARELY(0.6f), MISS(0f)
}