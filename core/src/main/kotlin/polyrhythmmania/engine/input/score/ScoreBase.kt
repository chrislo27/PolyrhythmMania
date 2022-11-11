package polyrhythmmania.engine.input.score


data class ScoreBase(
        override val scoreRaw: Float, 
        override val inputsHit: Int, override val nInputs: Int,
        override val noMiss: Boolean,
        override val skillStar: Boolean?,
) : IScore {
    override val scoreInt: Int = super.scoreInt
    override val ranking: Ranking = super.ranking
}
