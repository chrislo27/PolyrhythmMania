package paintbox.binding


fun interface VarChangedListener<T> {
    fun onChange(v: ReadOnlyVar<T>)
}
