package polyrhythmmania.util

import java.util.*


/**
 * A container of items that can be permuted (shuffled) and then extracted in that order before being re-permuted.
 * The initial state of the iterator has the same order as the input container, call [shuffle] to initialize.
 *
 * This is similar to Tetris's "Random Generator" but you can force the items between the "seams" to not be the same.
 */
class RandomBagIterator<T>(
    container: List<T>,
    val random: Random,
    val exhaustionBehaviour: ExhaustionBehaviour = ExhaustionBehaviour.SHUFFLE_EXCLUDE_LAST,
) : Iterator<T> {
    
    enum class ExhaustionBehaviour {
        DO_NOTHING,
        SHUFFLE,
        SHUFFLE_EXCLUDE_LAST;
    }

    private val originalOrder: List<T> = container.toList()
    private val items: MutableList<T> = container.toMutableList()
    private var currentIndex: Int = 0
    
    fun resetToOriginalOrder() {
        currentIndex = 0
        items.clear()
        items.addAll(originalOrder)
    }

    /**
     * Shuffles and resets the iterator.
     */
    fun shuffle() {
        currentIndex = 0
        items.shuffle(random)
    }

    /**
     * Shuffles and resets the iterator, but the provided [bannedFirst] cannot be the first item (unless it is the only item).
     */
    fun shuffleAndExclude(bannedFirst: T) {
        currentIndex = 0
        if (items.size > 1) {
            if (items.size == 2 && (items[0] == bannedFirst || items[1] == bannedFirst)) {
                if (items[0] == bannedFirst) {
                    // Swap the first and second items since that is the only valid permutation
                    val oldFirst = items[0]
                    items[0] = items[1]
                    items[1] = oldFirst
                } // else the second item is the banned first item and it is still the only valid permutation
            } else {
                do {
                    items.shuffle(random)
                } while (items[0] == bannedFirst)
            }
        }
    }

    /**
     * Shuffles and resets the iterator in the same manner as [shuffleAndExclude], but the `bannedFirst` parameter is
     * the last item in this current iterator's state.
     * 
     * If there are no items, then calling this function has the same effect as calling [shuffle].
     */
    fun shuffleExcludeFromLastState() {
        if (items.isEmpty()) {
            shuffle()
        } else {
            shuffleAndExclude(items.last())
        }
    }
    
    override fun hasNext(): Boolean {
        return currentIndex < items.size
    }

    override fun next(): T {
        if (!hasNext()) {
            if (items.isNotEmpty() && exhaustionBehaviour != ExhaustionBehaviour.DO_NOTHING) {
                if (exhaustionBehaviour == ExhaustionBehaviour.SHUFFLE_EXCLUDE_LAST) {
                    shuffleExcludeFromLastState()
                } else {
                    shuffle()
                }
            } else {
                throw NoSuchElementException("No more elements in this RandomBagIterator")
            }
        }
        return items[currentIndex++]
    }
}