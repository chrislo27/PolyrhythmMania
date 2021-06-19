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
    private var dependencies: Set<ReadOnlyVar<Any>> = emptySet()

    private var listeners: Set<VarChangedListener<T>> = emptySet()

    private val invalidationListener: VarChangedListener<Any> = InvalListener(this as GenericVar<Any>)

    @Suppress("UNCHECKED_CAST")
    constructor(item: T) {
        currentValue = item
        binding = GenericBinding.Const as GenericBinding<T>
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
        binding = GenericBinding.Const as GenericBinding<T>
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
                @Suppress("UNCHECKED_CAST") (currentValue as T)
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

    private class InvalListener(v: GenericVar<Any>) : VarChangedListener<Any> {
        val weakRef: WeakReference<GenericVar<Any>> = WeakReference(v)
        var disposeMe: Boolean = false
        override fun onChange(v: ReadOnlyVar<Any>) {
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

    private sealed class GenericBinding<T> {
        object Const : GenericBinding<Any>()

        class Compute<T>(val computation: Var.Context.() -> T) : GenericBinding<T>()

        class SideEffecting<T>(var item: T, val sideEffectingComputation: Var.Context.(existing: T) -> T)
            : GenericBinding<T>()
    }
}