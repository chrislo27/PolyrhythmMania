package polyrhythmmania.engine.input

/**
 * A simple enum bucketing the score [weight] to the type of input.
 */
enum class InputScore(val weight: Float) {
    ACE(1.0f), GOOD(0.9f), BARELY(0.6f), MISS(0f)
}