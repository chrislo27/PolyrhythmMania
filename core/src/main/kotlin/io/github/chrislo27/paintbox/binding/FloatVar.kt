package io.github.chrislo27.paintbox.binding

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap


/**
 * The [Float] specialization of [Var].
 */
class FloatVar : Var<Float> {
    
    private var binding: FloatBinding
    private var invalidated: Boolean = true // Used for Compute and SideEffecting bindings
    private var currentValue: Float = 0f
    private var dependencies: Set<ReadOnlyVar<Any>> = emptySet()

    private val listeners: MutableSet<VarChangedListener<Float>> = mutableSetOf()

    private val invalidationListener: VarChangedListener<Any> = InvalListener(this) as VarChangedListener<Any>

    constructor(item: Float) {
        binding = FloatBinding.Const(item)
    }

    constructor(computation: Var.Context.() -> Float) {
        binding = FloatBinding.Compute(computation)
    }

    constructor(item: Float, sideEffecting: Var.Context.(existing: Float) -> Float) {
        binding = FloatBinding.SideEffecting(item, sideEffecting)
    }

    private fun reset() {
        dependencies = emptySet()
        invalidated = true
        currentValue = 0f
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

    override fun set(item: Float) {
        val existingBinding = binding
        if (existingBinding is FloatBinding.Const && existingBinding.item == item) {
            return
        }
        reset()
        currentValue = item
        binding = FloatBinding.Const(item)
        notifyListeners()
    }

    override fun bind(computation: Var.Context.() -> Float) {
        reset()
        binding = FloatBinding.Compute(computation)
        notifyListeners()
    }

    override fun sideEffecting(item: Float, sideEffecting: Var.Context.(existing: Float) -> Float) {
        reset()
        binding = FloatBinding.SideEffecting(item, sideEffecting)
        notifyListeners()
    }

    override fun sideEffecting(sideEffecting: Var.Context.(existing: Float) -> Float) {
        sideEffecting(getOrCompute(), sideEffecting)
    }

    override fun getOrCompute(): Float {
        val result: Float = when (val binding = this.binding) {
            is FloatBinding.Const -> binding.item
            is FloatBinding.Compute -> {
                if (!invalidated) {
                    currentValue
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
            is FloatBinding.SideEffecting -> {
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
        return result
    }

    override fun addListener(listener: VarChangedListener<Float>) {
        if (listener !in listeners) {
            listeners.add(listener)
        }
    }

    override fun removeListener(listener: VarChangedListener<Float>) {
        if (listener in listeners) {
            listeners.remove(listener)
        }
    }

    override fun toString(): String {
        return getOrCompute().toString()
    }

    private class InvalListener(v: FloatVar) : VarChangedListener<Float> {
        val weakRef: WeakReference<FloatVar> = WeakReference(v)
        var disposeMe: Boolean = false
        override fun onChange(v: ReadOnlyVar<Float>) {
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

    private sealed class FloatBinding {
        class Const(val item: Float) : FloatBinding() {
            override fun getValue(): Float = item
        }

        class Compute(val computation: Var.Context.() -> Float) : FloatBinding() {
            override fun getValue(): Float = computation.invoke(Var.Context())
        }

        class SideEffecting(var item: Float, val sideEffectingComputation: Var.Context.(existing: Float) -> Float) : FloatBinding() {
            override fun getValue(): Float = item
        }

        abstract fun getValue(): Float
    }
}