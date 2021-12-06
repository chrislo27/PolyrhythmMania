package paintbox.binding

import java.lang.ref.WeakReference

/**
 * The [Long] specialization of [ReadOnlyVar].
 *
 * Provides the [get] method which is a primitive-type long.
 */
sealed interface ReadOnlyLongVar : ReadOnlyVar<Long> {

    /**
     * Gets (and computes if necessary) the value represented by this [ReadOnlyLongVar].
     * Unlike the [ReadOnlyVar.getOrCompute] function, this will always return a primitive `long` value.
     *
     * If using this [ReadOnlyLongVar] in a binding, use [Var.Context] to do dependency tracking,
     * and use the `long` specialization specific functions ([Var.Context.use]).
     */
    fun get(): Long

    @Deprecated("Use ReadOnlyLongVar.get() instead to avoid explicit boxing",
            replaceWith = ReplaceWith("this.get()"),
            level = DeprecationLevel.ERROR)
    override fun getOrCompute(): Long {
        return get() // WILL BE BOXED!
    }
}

/**
 * The [Long] specialization of [Var].
 *
 * Provides the [get] method which is a primitive-type long.
 */
class LongVar : ReadOnlyLongVar, Var<Long> {

    private var binding: LongBinding
    private var invalidated: Boolean = true // Used for Compute and SideEffecting bindings
    private var currentValue: Long = 0L
    private var dependencies: Set<ReadOnlyVar<Any?>> = emptySet() // Cannot be generic since it can depend on any other Var

    private var listeners: Set<VarChangedListener<Long>> = emptySet()

    /**
     * This is intentionally generic type Any? so further unchecked casts are avoided when it is used
     */
    private val invalidationListener: VarChangedListener<Any?> = InvalListener(this)

    constructor(item: Long) {
        binding = LongBinding.Const
        currentValue = item
    }

    constructor(computation: Var.Context.() -> Long) {
        binding = LongBinding.Compute(computation)
    }

    constructor(item: Long, sideEffecting: Var.Context.(existing: Long) -> Long) {
        binding = LongBinding.SideEffecting(item, sideEffecting)
    }

    private fun reset() {
        dependencies = emptySet()
        invalidated = true
        currentValue = 0L
    }

    private fun notifyListeners() {
        var anyNeedToBeDisposed = false
        listeners.forEach {
            it.onChange(this)
            if (it is InvalListener && it.disposeMe) {
                anyNeedToBeDisposed = true
            }
        }
        if (anyNeedToBeDisposed) {
            listeners = listeners.filter { it is InvalListener && it.disposeMe }.toSet()
        }
    }

    override fun set(item: Long) {
        val existingBinding = binding
        if (existingBinding is LongBinding.Const && currentValue == item) {
            return
        }
        reset()
        currentValue = item
        binding = LongBinding.Const
        notifyListeners()
    }

    override fun bind(computation: Var.Context.() -> Long) {
        reset()
        binding = LongBinding.Compute(computation)
        notifyListeners()
    }

    override fun sideEffecting(item: Long, sideEffecting: Var.Context.(existing: Long) -> Long) {
        reset()
        binding = LongBinding.SideEffecting(item, sideEffecting)
        notifyListeners()
    }

    override fun sideEffecting(sideEffecting: Var.Context.(existing: Long) -> Long) {
        sideEffecting(get(), sideEffecting)
    }

    /**
     * The implementation of [getOrCompute] but returns a long primitive.
     */
    override fun get(): Long {
        val result: Long = when (val binding = this.binding) {
            is LongBinding.Const -> this.currentValue
            is LongBinding.Compute -> {
                if (!invalidated) {
                    currentValue
                } else {
                    val oldCurrentValue = currentValue
                    val ctx = Var.Context()
                    val result = binding.computation(ctx)
                    val oldDependencies = dependencies
                    oldDependencies.forEach { it.removeListener(invalidationListener) }
                    dependencies = ctx.dependencies
                    dependencies.forEach { it.addListener(invalidationListener) }
                    currentValue = result
                    invalidated = false
                    if (oldCurrentValue != currentValue) {
                        notifyListeners()
                    }
                    result
                }
            }
            is LongBinding.SideEffecting -> {
                if (invalidated) {
                    val oldCurrentValue = currentValue
                    val ctx = Var.Context()
                    val result = binding.sideEffectingComputation(ctx, binding.item)
                    val oldDependencies = dependencies
                    oldDependencies.forEach { it.removeListener(invalidationListener) }
                    dependencies = ctx.dependencies
                    dependencies.forEach { it.addListener(invalidationListener) }
                    currentValue = result
                    invalidated = false
                    if (oldCurrentValue != currentValue) {
                        notifyListeners()
                    }
                    binding.item = result
                }
                binding.item
            }
        }
        return result
    }


    @Deprecated("Use LongVar.get() instead to avoid explicit boxing",
            replaceWith = ReplaceWith("this.get()"),
            level = DeprecationLevel.ERROR)
    override fun getOrCompute(): Long {
        return get() // WILL BE BOXED!
    }

    override fun addListener(listener: VarChangedListener<Long>) {
        if (listener !in listeners) {
            listeners = listeners + listener
        }
    }

    override fun removeListener(listener: VarChangedListener<Long>) {
        if (listener in listeners) {
            listeners = listeners - listener
        }
    }

    override fun toString(): String {
        return get().toString()
    }

    /**
     * [Sets][set] this [IntVar] to be the negation of its value from [IntVar.get].
     *
     * Returns the new state.
     */
    fun negate(): Long {
        val newState = -this.get()
        this.set(newState)
        return newState
    }

    /**
     * Cannot be inner for garbage collection reasons! We are avoiding an explicit strong reference to the parent Var
     */
    private class InvalListener(v: LongVar) : VarChangedListener<Any?> {
        val weakRef: WeakReference<LongVar> = WeakReference(v)
        var disposeMe: Boolean = false

        override fun onChange(v: ReadOnlyVar<Any?>) {
            val parent = weakRef.get()
            if (!disposeMe && parent != null) {
                if (!parent.invalidated) {
                    parent.invalidated = true
                    parent.notifyListeners()
                }
            } else {
                disposeMe = true
            }
        }
    }

    private sealed class LongBinding {
        /**
         * Represents a constant value. The value is actually stored in [LongVar.currentValue].
         */
        object Const : LongBinding()

        class Compute(val computation: Var.Context.() -> Long) : LongBinding()

        class SideEffecting(var item: Long, val sideEffectingComputation: Var.Context.(existing: Long) -> Long) : LongBinding()
    }
}

/**
 * [Sets][Var.set] this [Long] [Var] to be the negation of its value from [Var.getOrCompute].
 *
 * Returns the new state.
 */
fun Var<Long>.negate(): Long {
    val newState = -this.getOrCompute()
    this.set(newState)
    return newState
}
