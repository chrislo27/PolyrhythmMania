package polyrhythmmania.storymode.music


data class StemMix(val stemIDs: Set<String>) {
    companion object {
        val NONE: StemMix = StemMix(emptySet())
    }
}
