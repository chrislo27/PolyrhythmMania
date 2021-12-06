package paintbox.binding


interface ReadOnlyVar<out T> {

    /**
     * Gets (and computes if necessary) the value represented by this [ReadOnlyVar].
     * 
     * If using this [ReadOnlyVar] in a binding, use [Var.Context] to do dependency tracking.
     */
    fun getOrCompute(): T

    fun addListener(listener: VarChangedListener<T>)

    fun removeListener(listener: VarChangedListener<T>)

}

/**
 * Represents a writable var.
 *
 * The default implementation is [GenericVar].
 */
interface Var<T> : ReadOnlyVar<T> {

    /**
     * These are default "constructor functions" that will use [GenericVar] as its implementation.
     */
    companion object {
        
        operator fun <T> invoke(item: T): GenericVar<T> = GenericVar(item)
        fun <T> bind(computation: Context.() -> T): GenericVar<T> = GenericVar(computation)
        operator fun <T> invoke(computation: Context.() -> T): GenericVar<T> = GenericVar(computation)
        fun <T> sideEffecting(item: T, sideEffecting: Context.(existing: T) -> T): GenericVar<T> = GenericVar(item, sideEffecting)

        // Warnings for specialized versions of plain invoke
        @Deprecated("Prefer using the FloatVar constructor to avoid confusion with generic versions",
                replaceWith = ReplaceWith("FloatVar"),
                level = DeprecationLevel.ERROR)
        operator fun invoke(item: Float): FloatVar = FloatVar(item)
        @Deprecated("Prefer using the BooleanVar constructor to avoid confusion with generic versions",
                replaceWith = ReplaceWith("BooleanVar"),
                level = DeprecationLevel.ERROR)
        operator fun invoke(item: Boolean): BooleanVar = BooleanVar(item)
        @Deprecated("Prefer using the IntVar constructor to avoid confusion with generic versions",
                replaceWith = ReplaceWith("IntVar"),
                level = DeprecationLevel.ERROR)
        operator fun invoke(item: Int): IntVar = IntVar(item)
    }

    /**
     * Sets this [Var] to be the value of [item].
     */
    fun set(item: T)

    /**
     * Binds this [Var] to be computed from [computation]. The computation can depend
     * on other [ReadOnlyVar]s by calling [Context.use].
     */
    fun bind(computation: Context.() -> T)

    /**
     * Sets this var to be the value of [item] while being updated/mutated
     * by [sideEffecting].
     */
    fun sideEffecting(item: T, sideEffecting: Context.(existing: T) -> T)

    /**
     * Sets this var to be updated/mutated
     * by [sideEffecting], while retaining the existing value gotten from [getOrCompute].
     */
    fun sideEffecting(sideEffecting: Context.(existing: T) -> T) {
        sideEffecting(getOrCompute(), sideEffecting)
    }

    class Context {
        val dependencies: MutableSet<ReadOnlyVar<Any?>> = LinkedHashSet(2)

        @JvmName("use")
        fun <R> use(varr: ReadOnlyVar<R>): R { // DON'T add specialized deprecations for this particular use function for generic compatibility
            dependencies += varr
            return varr.getOrCompute()
        }

        @JvmName("useAndGet")
        fun <R> ReadOnlyVar<R>.use(): R { // Specialized deprecations may be added for this use function
            return use(this)
        }
        
        
        // Specialization methods below

        @Deprecated("Don't use ReadOnlyVar<Float>, use ReadOnlyFloatVar.useF() instead to avoid explicit boxing",
                replaceWith = ReplaceWith("(this as ReadOnlyFloatVar).useF()"),
                level = DeprecationLevel.ERROR)
        fun ReadOnlyVar<Float>.use(): Float {
            return use(this)
        }

        @Deprecated("Don't use the generic use() function, use ReadOnlyFloatVar.useF() instead to avoid explicit boxing",
                replaceWith = ReplaceWith("this.useF()"),
                level = DeprecationLevel.ERROR)
        fun ReadOnlyFloatVar.use(): Float {
            return this.useF()
        }
        
        // The float specialization method. Returns a primitive float.
        fun ReadOnlyFloatVar.useF(): Float {
            dependencies += this
            return this.get()
        }
        

        @Deprecated("Don't use ReadOnlyVar<Boolean>, use ReadOnlyBooleanVar.useB() instead to avoid explicit boxing",
                replaceWith = ReplaceWith("(this as ReadOnlyBooleanVar).useB()"),
                level = DeprecationLevel.ERROR)
        fun ReadOnlyVar<Boolean>.use(): Boolean {
            return use(this)
        }

        @Deprecated("Don't use the generic use() function, use ReadOnlyBooleanVar.useB() instead to avoid explicit boxing",
                replaceWith = ReplaceWith("this.useB()"),
                level = DeprecationLevel.ERROR)
        fun ReadOnlyBooleanVar.use(): Boolean {
            return this.useB()
        }

        // The boolean specialization method. Returns a primitive boolean.
        fun ReadOnlyBooleanVar.useB(): Boolean {
            dependencies += this
            return this.get()
        }
        

        @Deprecated("Don't use ReadOnlyVar<Int>, use ReadOnlyIntVar.useI() instead to avoid explicit boxing",
                replaceWith = ReplaceWith("(this as ReadOnlyIntVar).useI()"),
                level = DeprecationLevel.ERROR)
        fun ReadOnlyVar<Int>.use(): Int {
            return use(this)
        }

        @Deprecated("Don't use the generic use() function, use ReadOnlyIntVar.useB() instead to avoid explicit boxing",
                replaceWith = ReplaceWith("this.useI()"),
                level = DeprecationLevel.ERROR)
        fun ReadOnlyIntVar.use(): Int {
            return this.useI()
        }

        // The int specialization method. Returns a primitive int.
        fun ReadOnlyIntVar.useI(): Int {
            dependencies += this
            return this.get()
        }
    }
}
