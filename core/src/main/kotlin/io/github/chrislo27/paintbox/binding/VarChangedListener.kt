package io.github.chrislo27.paintbox.binding


fun interface VarChangedListener<T> {
    fun onChange(v: ReadOnlyVar<T>)
}
