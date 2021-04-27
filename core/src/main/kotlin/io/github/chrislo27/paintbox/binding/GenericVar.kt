package io.github.chrislo27.paintbox.binding

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

    private val listeners: MutableSet<VarChangedListener<T>> = mutableSetOf()

    private val invalidationListener: VarChangedListener<Any> = InvalListener(this as GenericVar<Any>)

    constructor(item: T) {
        binding = GenericBinding.Const(item)
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
            listeners.removeIf { it is InvalListener && it.disposeMe }
        }
    }

    override fun set(item: T) {
        val existingBinding = binding
        if (existingBinding is GenericBinding.Const && existingBinding.item === item) {
            return
        }
        reset()
        currentValue = item
        binding = GenericBinding.Const(item)
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
            is GenericBinding.Const -> binding.item
            is GenericBinding.Compute -> {
                if (!invalidated) {
                    currentValue as T
                } else {
                    val ctx = Var.Context()
                    val result = binding.computation(ctx)
                    val oldDependencies = dependencies
                    oldDependencies.forEach { it.removeListener(invalidationListener) }
                    dependencies = ctx.dependencies
                    dependencies.forEach { it.addListener(invalidationListener) }
                    currentValue = result
                    invalidated = false
                    notifyListeners()
                    result
                }
            }
            is GenericBinding.SideEffecting -> {
                if (invalidated) {
                    val ctx = Var.Context()
                    val result = binding.sideEffectingComputation(ctx, binding.item)
                    val oldDependencies = dependencies
                    oldDependencies.forEach { it.removeListener(invalidationListener) }
                    dependencies = ctx.dependencies
                    dependencies.forEach { it.addListener(invalidationListener) }
                    currentValue = result
                    invalidated = false
                    notifyListeners()
                    binding.item = result
                }
                binding.item
            }
        }
    }

    override fun addListener(listener: VarChangedListener<T>) {
        if (listener !in listeners) {
            listeners.add(listener)
        }
    }

    override fun removeListener(listener: VarChangedListener<T>) {
        if (listener in listeners) {
            listeners.remove(listener)
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
                parent.invalidated = true
                parent.notifyListeners()
            } else {
                disposeMe = true
            }
        }
    }

    private sealed class GenericBinding<T> {
        class Const<T>(val item: T) : GenericBinding<T>() {
            override fun getValue(): T = item
        }

        class Compute<T>(val computation: Var.Context.() -> T) : GenericBinding<T>() {
            override fun getValue(): T = computation.invoke(Var.Context())
        }

        class SideEffecting<T>(var item: T, val sideEffectingComputation: Var.Context.(existing: T) -> T) : GenericBinding<T>() {
            override fun getValue(): T = item
        }

        abstract fun getValue(): T
    }
}