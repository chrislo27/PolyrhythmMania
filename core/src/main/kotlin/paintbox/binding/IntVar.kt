package paintbox.binding

import java.lang.ref.WeakReference

/**
 * The [Int] specialization of [ReadOnlyVar].
 *
 * Provides the [get] method which is a primitive-type int.
 */
interface ReadOnlyIntVar : ReadOnlyVar<Int> {

    /**
     * Gets (and computes if necessary) the value represented by this [ReadOnlyIntVar].
     * Unlike the [ReadOnlyVar.getOrCompute] function, this will always return an `int` value.
     *
     * If using this [ReadOnlyIntVar] in a binding, use [Var.Context] to do dependency tracking,
     * and use the boolean specialization specific functions ([Var.Context.useI]).
     */
    fun get(): Int

    @Deprecated("Use ReadOnlyBooleanVar.get() instead to avoid explicit boxing",
            replaceWith = ReplaceWith("this.get()"),
            level = DeprecationLevel.ERROR)
    override fun getOrCompute(): Int {
        return get() // WILL BE BOXED!
    }
}

/**
 * The [Int] specialization of [Var].
 *
 * Provides the [get] method which is a primitive-type int.
 */
class IntVar : ReadOnlyIntVar, Var<Int> {

    private var binding: IntBinding
    private var invalidated: Boolean = true // Used for Compute and SideEffecting bindings
    private var currentValue: Int = 0
    private var dependencies: Set<ReadOnlyVar<Any?>> = emptySet() // Cannot be generic since it can depend on any other Var

    private var listeners: Set<VarChangedListener<Int>> = emptySet()

    /**
     * This is intentionally generic type Any? so further unchecked casts are avoided when it is used
     */
    private val invalidationListener: VarChangedListener<Any?> = InvalListener(this)

    constructor(item: Int) {
        binding = IntBinding.Const
        currentValue = item
    }

    constructor(computation: Var.Context.() -> Int) {
        binding = IntBinding.Compute(computation)
    }

    constructor(item: Int, sideEffecting: Var.Context.(existing: Int) -> Int) {
        binding = IntBinding.SideEffecting(item, sideEffecting)
    }

    private fun reset() {
        dependencies = emptySet()
        invalidated = true
        currentValue = 0
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

    override fun set(item: Int) {
        val existingBinding = binding
        if (existingBinding is IntBinding.Const && currentValue == item) {
            return
        }
        reset()
        currentValue = item
        binding = IntBinding.Const
        notifyListeners()
    }

    override fun bind(computation: Var.Context.() -> Int) {
        reset()
        binding = IntBinding.Compute(computation)
        notifyListeners()
    }

    override fun sideEffecting(item: Int, sideEffecting: Var.Context.(existing: Int) -> Int) {
        reset()
        binding = IntBinding.SideEffecting(item, sideEffecting)
        notifyListeners()
    }

    override fun sideEffecting(sideEffecting: Var.Context.(existing: Int) -> Int) {
        sideEffecting(get(), sideEffecting)
    }

    /**
     * The implementation of [getOrCompute] but returns a boolean primitive.
     */
    override fun get(): Int {
        val result: Int = when (val binding = this.binding) {
            is IntBinding.Const -> this.currentValue
            is IntBinding.Compute -> {
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
            is IntBinding.SideEffecting -> {
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


    @Deprecated("Use IntVar.get() instead to avoid explicit boxing",
            replaceWith = ReplaceWith("this.get()"),
            level = DeprecationLevel.ERROR)
    override fun getOrCompute(): Int {
        return get() // WILL BE BOXED!
    }

    override fun addListener(listener: VarChangedListener<Int>) {
        if (listener !in listeners) {
            listeners = listeners + listener
        }
    }

    override fun removeListener(listener: VarChangedListener<Int>) {
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
    fun negate(): Int {
        val newState = -this.get()
        this.set(newState)
        return newState
    }

    /**
     * Cannot be inner for garbage collection reasons! We are avoiding an explicit strong reference to the parent Var
     */
    private class InvalListener(v: IntVar) : VarChangedListener<Any?> {
        val weakRef: WeakReference<IntVar> = WeakReference(v)
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

    private sealed class IntBinding {
        /**
         * Represents a constant value. The value is actually stored in [IntVar.currentValue].
         */
        object Const : IntBinding()

        class Compute(val computation: Var.Context.() -> Int) : IntBinding()

        class SideEffecting(var item: Int, val sideEffectingComputation: Var.Context.(existing: Int) -> Int) : IntBinding()
    }
}

/**
 * [Sets][Var.set] this [Int] [Var] to be the negation of its value from [Var.getOrCompute].
 *
 * Returns the new state.
 */
fun Var<Int>.invert(): Int {
    val newState = -this.getOrCompute()
    this.set(newState)
    return newState
}
