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

        
        // Warnings for specialized versions of plain invoke -----------------------------------------------------------
        
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
        
        @Deprecated("Prefer using the LongVar constructor to avoid confusion with generic versions",
                replaceWith = ReplaceWith("LongVar"),
                level = DeprecationLevel.ERROR)
        operator fun invoke(item: Long): LongVar = LongVar(item)
        
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


        /**
         * Adds the [varr] as a dependency and returns the var's value.
         * 
         * This function can be used on any [ReadOnlyVar], even specialized ones.
         */
        @JvmName("use")
        fun <R> use(varr: ReadOnlyVar<R>): R { // DON'T add specialized deprecations for this particular use function for generic compatibility
            dependencies += varr
            return varr.getOrCompute()
        }

        /**
         * Adds the receiver [var][R] as a dependency and returns the var's value.
         * 
         * Note that if there are specializations available, the specialized `use` function should be used instead.
         */
        @JvmName("useAndGet")
        fun <R> ReadOnlyVar<R>.use(): R { // Specialized deprecations may be added for this use function
            return use(this)
        }
        
        
        // Specialization methods below --------------------------------------------------------------------------------

        @Deprecated("Don't use ReadOnlyVar<Float>, use ReadOnlyFloatVar.use() instead to avoid explicit boxing",
                replaceWith = ReplaceWith("(this as ReadOnlyFloatVar).use()"),
                level = DeprecationLevel.ERROR)
        fun ReadOnlyVar<Float>.use(): Float {
            return use(this)
        }

        /**
         * The float specialization method. Adds the receiver as a dependency and returns a primitive float.
         */
        fun ReadOnlyFloatVar.use(): Float {
            dependencies += this
            return this.get()
        }
        

        @Deprecated("Don't use ReadOnlyVar<Boolean>, use ReadOnlyBooleanVar.use() instead to avoid explicit boxing",
                replaceWith = ReplaceWith("(this as ReadOnlyBooleanVar).use()"),
                level = DeprecationLevel.ERROR)
        fun ReadOnlyVar<Boolean>.use(): Boolean {
            return use(this)
        }

        /**
         * The boolean specialization method. Adds the receiver as a dependency and returns a primitive boolean.
         */
        fun ReadOnlyBooleanVar.use(): Boolean {
            dependencies += this
            return this.get()
        }
        

        @Deprecated("Don't use ReadOnlyVar<Int>, use ReadOnlyIntVar.use() instead to avoid explicit boxing",
                replaceWith = ReplaceWith("(this as ReadOnlyIntVar).use()"),
                level = DeprecationLevel.ERROR)
        fun ReadOnlyVar<Int>.use(): Int {
            return use(this)
        }

        /**
         * The int specialization method. Adds the receiver as a dependency and returns a primitive int.
         */
        fun ReadOnlyIntVar.use(): Int {
            dependencies += this
            return this.get()
        }
        

        @Deprecated("Don't use ReadOnlyVar<Long>, use ReadOnlyLongVar.use() instead to avoid explicit boxing",
                replaceWith = ReplaceWith("(this as ReadOnlyLongVar).use()"),
                level = DeprecationLevel.ERROR)
        fun ReadOnlyVar<Long>.use(): Long {
            return use(this)
        }

        /**
         * The long specialization method. Adds the receiver as a dependency and returns a primitive long.
         */
        fun ReadOnlyLongVar.use(): Long {
            dependencies += this
            return this.get()
        }
        
    }
}
