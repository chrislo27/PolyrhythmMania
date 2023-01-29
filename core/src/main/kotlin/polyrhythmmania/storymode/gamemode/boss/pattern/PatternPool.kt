package polyrhythmmania.storymode.gamemode.boss.pattern

import polyrhythmmania.util.RandomBagIterator
import java.util.*


data class PatternPool(
    val patterns: List<Pattern>,
    val random: Random,
    val bannedFirst: Pattern? = null,
) {

    val iter: RandomBagIterator<Pattern> =
        RandomBagIterator(patterns, random, RandomBagIterator.ExhaustionBehaviour.SHUFFLE_EXCLUDE_LAST)

    fun resetAndShuffle() {
        iter.resetToOriginalOrder()
        if (bannedFirst != null) {
            iter.shuffleAndExclude(bannedFirst)
        } else {
            iter.shuffle()
        }
    }
}
