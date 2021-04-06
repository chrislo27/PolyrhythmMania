package polyrhythmmania.util

object WaveUtils {

	/**
	 * At 0.5, the peak (1.0) will be reached. At 0.0 and 1.0, the bottom will be reached (0.0).
	 */
	fun getBounceWave(alpha: Float): Float {
		// -(2x - 1)^2 + 1
		return -((alpha * 2f - 1) * (alpha * 2f - 1)) + 1
	}
}