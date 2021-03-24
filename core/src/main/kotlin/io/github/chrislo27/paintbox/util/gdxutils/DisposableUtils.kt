package io.github.chrislo27.paintbox.util.gdxutils

import com.badlogic.gdx.utils.Disposable


fun Disposable.disposeQuietly() {
    try {
        this.dispose()
    } catch (ignored: Exception) {}
}