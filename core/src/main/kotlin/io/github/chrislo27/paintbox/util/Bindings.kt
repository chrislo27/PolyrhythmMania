package io.github.chrislo27.paintbox.util

import io.github.chrislo27.paintbox.util.Var.Context
import java.lang.ref.WeakReference


sealed class Binding<T> {
    class Const<T>(val item: T) : Binding<T>() {
        override fun getValue(): T = item
    }

    class Compute<T>(val computation: Var.Context.() -> T) : Binding<T>() {
        override fun getValue(): T = computation.invoke(Var.Context())
    }
    
    class SideEffecting<T>(var item: T, val sideEffectingComputation: Var.Context.(existing: T) -> T) : Binding<T>() {
        override fun getValue(): T = item
    }

    abstract fun getValue(): T
}

interface ReadOnlyVar<T> {

    /**
     * Likely use one of [Context]'s get methods instead in order to track dependencies.
     */
    fun getOrCompute(): T 

    fun addListener(listener: VarChangedListener<T>)

    fun removeListener(listener: VarChangedListener<T>) 
    
}

class Var<T> : ReadOnlyVar<T> {
    class Context {
        val dependencies: MutableSet<ReadOnlyVar<Any>> = LinkedHashSet(2)

        @Suppress("UNCHECKED_CAST")
        @JvmName("use")
        fun <R> use(varr: ReadOnlyVar<R>): R {
            dependencies += (varr as ReadOnlyVar<Any>)
            return varr.getOrCompute()
        }

        @JvmName("useAndGet")
        fun <R> ReadOnlyVar<R>.use(): R {
            return use(this)
        }
    }

    private var binding: Binding<T>
    private var invalidated: Boolean = true // Used for Compute and SideEffecting bindings
    private var currentValue: T? = null
    private var dependencies: List<ReadOnlyVar<Any>> = emptyList()

    private var listeners: Set<VarChangedListener<T>> = emptySet()

    private val invalidationListener: VarChangedListener<Any> = InvalListener(this as Var<Any>)

    constructor(item: T) {
        binding = Binding.Const(item)
    }

    constructor(computation: Context.() -> T) {
        binding = Binding.Compute(computation)
    }
    
    constructor(item: T, sideEffecting: Context.(existing: T) -> T) {
        binding = Binding.SideEffecting(item, sideEffecting)
    }

    private fun reset() {
        dependencies = emptyList()
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

    fun set(item: T) {
        val existingBinding = binding
        if (existingBinding is Binding.Const && existingBinding.item === item) {
            return
        }
        reset()
        currentValue = item
        binding = Binding.Const(item)
        notifyListeners()
    }

    fun bind(computation: Context.() -> T) {
        reset()
        binding = Binding.Compute(computation)
        notifyListeners()
    }
    
    fun sideEffecting(item: T, sideEffecting: Context.(existing: T) -> T) {
        reset()
        binding = Binding.SideEffecting(item, sideEffecting)
        notifyListeners()
    }

    fun sideEffecting(sideEffecting: Context.(existing: T) -> T) {
        sideEffecting(getOrCompute(), sideEffecting)
    }

    override fun getOrCompute(): T {
        return when (val binding = this.binding) {
            is Binding.Const -> binding.item
            is Binding.Compute -> {
                if (!invalidated) {
                    currentValue as T
                } else {
                    val ctx = Context()
                    val result = binding.computation(ctx)
                    val oldDependencies = dependencies
                    oldDependencies.forEach { it.removeListener(invalidationListener) }
                    dependencies = ctx.dependencies.toList()
                    dependencies.forEach { it.addListener(invalidationListener) }
                    currentValue = result
                    invalidated = false
                    notifyListeners()
                    result
                }
            }
            is Binding.SideEffecting -> {
                if (invalidated) {
                    val ctx = Context()
                    val result = binding.sideEffectingComputation(ctx, binding.item)
                    val oldDependencies = dependencies
                    oldDependencies.forEach { it.removeListener(invalidationListener) }
                    dependencies = ctx.dependencies.toList()
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

    private class InvalListener(v: Var<Any>) : VarChangedListener<Any> {
        val weakRef: WeakReference<Var<Any>> = WeakReference(v)
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

    companion object {
//        @JvmStatic
//        fun main(args: Array<String>) {
//            val v1 = Var(1)
//            val v2 = Var(2)
//            val v3 = Var(20)
//            var i = 0
//            val vIncremental: Var<Int> = Var(0)
//            val vSum: Var<Int> = Var {
//                use(v1) + use(v2)
//            }
//            val vSum2: Var<Int> = Var {
//                use(vSum) + use(vIncremental)
//            }
//
//            println("v1: 1 =? ${v1.get()}")
//            println("v2: 2 =? ${v2.get()}")
//            println("vSum: 3 =? ${vSum.get()}")
//            println()
//            println("vSum2: 3 =? ${vSum2.get()}")
//            vIncremental.set(++i)
//            println("vSum2: 4 =? ${vSum2.get()}")
//            vIncremental.set(++i)
//            println("vSum2: 5 =? ${vSum2.get()}")
//            vIncremental.set(++i)
//            println("vSum2: 6 =? ${vSum2.get()}")
//            println()
//
//            vSum.bind {
//                use(v1) + use(v3)
//            }
//            println("vSum: 21 =? ${vSum.get()}")
//            println("vSum only has v1 and v3 listened to: ${vSum.dependencies == listOf(v1, v3)} [${vSum.dependencies.toSet()}]")
//            println()
//            run {
//                val nestA = Var("A")
//                val nestB = Var {
//                    use(nestA) + "B"
//                }
//                val nestC = Var {
//                    use(nestB) + "C"
//                }
//                val nestD = Var {
//                    use(nestC) + "D"
//                }
//
//                println("nestD: ABCD =? ${nestD.get()}")
//                nestA.set("Alpha")
//                println("nestD: AlphaBCD =? ${nestD.get()}")
//            }
//        }
    }
}

fun interface VarChangedListener<T> {
    fun onChange(v: ReadOnlyVar<T>)
}
