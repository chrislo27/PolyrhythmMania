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
//            this.skinID.set(PRManiaSkins.SCROLLBAR_SKIN)
            val isDragging: Var<Boolean> = Var {
                thumbPressedState.use().pressed
            }
            this.userChangedValueListener = { newValue ->
                val min = this.minimum.getOrCompute()
                val percentage = ((newValue - min) / (this.maximum.getOrCompute() - min)).coerceIn(0f, 1f)
                val lastPos = editor.container.lastBlockPosition.getOrCompute()
                val pxPerBeat = editor.trackView.pxPerBeat.getOrCompute()
                val beatWidth = contentPane.bounds.width.getOrCompute() / pxPerBeat
                val newBeat = percentage * (if (lastPos < beatWidth) (beatWidth) else (lastPos))
                editor.trackView.beat.set(newBeat)
            }
            editor.trackView.beat.addListener {
                if (!isDragging.getOrCompute()) {
                    val newValue = it.getOrCompute()
                    val lastPos = editor.container.lastBlockPosition.getOrCompute()
                    val pxPerBeat = editor.trackView.pxPerBeat.getOrCompute()
                    val beatWidth = contentPane.bounds.width.getOrCompute() / pxPerBeat
                    val maxBeat = (if (lastPos < beatWidth) (beatWidth) else (lastPos))
                    val min = this.minimum.getOrCompute()
                    setValue(min + (newValue / maxBeat) * (this.maximum.getOrCompute() - min))
                }
            }
        }
        contentPane += scrollBar
        this.contentSection += contentPane
    }


}