package polyrhythmmania.editor.pane.track

import paintbox.binding.Var
import paintbox.ui.Pane
import paintbox.ui.control.ScrollBar
import polyrhythmmania.ui.PRManiaSkins


class ScrubTrack(allTracksPane: AllTracksPane) : LongTrackPane(allTracksPane, true) {

    val scrollBar: ScrollBar
    
    init {
        this.sidePanel.sidebarBgColor.bind { editorPane.palette.trackPaneTimeBg.use() }
        this.contentBgColor.bind { editorPane.palette.trackPaneTimeBg.use() }
        this.bounds.height.set(16f)
        this.showContentBorder.set(true)

        val contentPane = Pane()
        scrollBar = ScrollBar(ScrollBar.Orientation.HORIZONTAL).apply { 
            this.skinID.set(PRManiaSkins.EDITOR_SCROLLBAR_SKIN)
            val isDragging: Var<Boolean> = Var {
                thumbPressedState.use().pressed
            }
            this.userChangedValueListener = { newValue ->
                val min = this.minimum.get()
                val percentage = ((newValue - min) / (this.maximum.get() - min)).coerceIn(0f, 1f)
                val lastPos = editor.container.lastBlockPosition.get()
                val pxPerBeat = editor.trackView.pxPerBeat.get()
                val beatWidth = contentPane.bounds.width.get() / pxPerBeat
                val newBeat = percentage * (if (lastPos < beatWidth) (beatWidth) else (lastPos))
                editor.trackView.beat.set(newBeat)
            }
            fun updateOwnValue(newValue: Float) {
                if (!isDragging.getOrCompute()) {
                    val lastPos = editor.container.lastBlockPosition.get()
                    val pxPerBeat = editor.trackView.pxPerBeat.get()
                    val beatWidth = contentPane.bounds.width.get() / pxPerBeat
                    val maxBeat = (if (lastPos < beatWidth) (beatWidth) else (lastPos))
                    val min = this.minimum.get()
                    setValue(min + (newValue / maxBeat) * (this.maximum.get() - min))
                }
            }
            editor.trackView.beat.addListener {
                updateOwnValue(it.getOrCompute())
            }
            editor.container.lastBlockPosition.addListener {
                updateOwnValue(editor.trackView.beat.get())
            }
        }
        contentPane += scrollBar
        this.contentSection += contentPane
    }


}