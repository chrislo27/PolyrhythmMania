package paintbox.binding

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap


/**
 * The default implementation of [Var].
 */
class GenericVar<T> : Var<T> {

    private var binding: GenericBinding<T>
    private var invalidated: Boolean = true // Used for Compute and SideEffecting bindings
    private var currentValue: T? = null
    private var dependencies: Set<ReadOnlyVar<Any?>> = emptySet() // Cannot be generic since it can depend on any other Var

    private var listeners: Set<VarChangedListener<T>> = emptySet()

    /**
     * This is intentionally generic type Any? so further unchecked casts are avoided when it is used
     */
    @Suppress("UNCHECKED_CAST")
    private val invalidationListener: VarChangedListener<Any?> = (InvalListener(this) as VarChangedListener<Any?>)

    @Suppress("UNCHECKED_CAST")
    constructor(item: T) {
        currentValue = item
        binding = GenericBinding.Const
    }

    constructor(computation: Var.Context.() -> T) {
        binding = GenericBinding.Compute(computation)
    }

    constructor(item: T, sideEffecting: Var.Context.(existing: T) -> T) {
        binding = GenericBinding.SideEffecting(item, sideEffecting)
    }

    private fun reset() {
        dependencies = emptySet()
        invalidated = true
        currentValue = null
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

    @Suppress("UNCHECKED_CAST")
    override fun set(item: T) {
        val existingBinding = binding
        if (existingBinding is GenericBinding.Const && currentValue == item) {
            return
        }
        reset()
        currentValue = item
        binding = GenericBinding.Const
        notifyListeners()
    }

    override fun bind(computation: Var.Context.() -> T) {
        reset()
        binding = GenericBinding.Compute(computation)
        notifyListeners()
    }

    override fun sideEffecting(item: T, sideEffecting: Var.Context.(existing: T) -> T) {
        reset()
        binding = GenericBinding.SideEffecting(item, sideEffecting)
        notifyListeners()
    }

    override fun sideEffecting(sideEffecting: Var.Context.(existing: T) -> T) {
        sideEffecting(getOrCompute(), sideEffecting)
    }

    override fun getOrCompute(): T {
        return when (val binding = this.binding) {
            is GenericBinding.Const ->
                @Suppress("UNCHECKED_CAST") (currentValue as T) // Cannot be currentValue!! since the actual type of T may be nullable
            is GenericBinding.Compute -> {
                if (!invalidated) {
                    @Suppress("UNCHECKED_CAST") (currentValue as T)
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
                    if (currentValue != oldCurrentValue) {
                        notifyListeners()
                    }
                    result
                }
            }
            is GenericBinding.SideEffecting -> {
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
                    if (currentValue != oldCurrentValue) {
                        notifyListeners()
                    }
                    binding.item = result
                }
                binding.item
            }
        }
    }

    override fun addListener(listener: VarChangedListener<T>) {
        if (listener !in listeners) {
            listeners = listeners + listener
        }
    }

    override fun removeListener(listener: VarChangedListener<T>) {
        if (listener in listeners) {
            listeners = listeners - listener
        }
    }

    override fun toString(): String {
        return getOrCompute().toString()
    }

    /**
     * Cannot be inner for garbage collection reasons! We are avoiding an explicit strong reference to the parent Var
     */
    private class InvalListener<T>(v: GenericVar<T>) : VarChangedListener<T> {
        val weakRef: WeakReference<GenericVar<T>> = WeakReference(v)
        var disposeMe: Boolean = false
        
        override fun onChange(v: ReadOnlyVar<T>) {
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

    private sealed class GenericBinding<out T> {
        object Const : GenericBinding<Nothing>()

        class Compute<T>(val computation: Var.Context.() -> T) : GenericBinding<T>()

        class SideEffecting<T>(var item: T, val sideEffectingComputation: Var.Context.(existing: T) -> T)
            : GenericBinding<T>()
    }
}