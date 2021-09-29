package paintbox.binding


fun interface VarChangedListener<in T> {
    fun onChange(v: ReadOnlyVar<T>)
}
