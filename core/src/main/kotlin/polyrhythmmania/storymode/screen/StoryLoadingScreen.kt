package polyrhythmmania.storymode.screen

import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import polyrhythmmania.PRManiaGame
import polyrhythmmania.screen.SimpleLoadingScreen
import polyrhythmmania.ui.LoadingIconRod


open class StoryLoadingScreen<T>(main: PRManiaGame, val loadFunc: LoadFunction<T>, val doAfterLoad: (T) -> Unit)
    : SimpleLoadingScreen(main) {
    
    class LoadResult<T>(val result: T) {
        companion object {
            val SIGNAL: LoadResult<Nothing?> = LoadResult(null)
        }
    }
    
    fun interface LoadFunction<T> {
        /**
         * Called each frame to load. When a non-null [LoadResult] is returned, the loading is finished. Otherwise,
         * a null result will continue the loading.
         */
        fun load(delta: Float): LoadResult<T>?
    }
    

    var minimumShowTime: Float = 0.25f
    var minWaitTimeBeforeLoadStart: Float = 0f
    var minWaitTimeAfterLoadFinish: Float = 0f
    
    private var isReadyToContinue: Boolean = false
    private var alreadyRanAfterLoad: Boolean = false
    private var discardFrame: Boolean = true
    private lateinit var loadResult: LoadResult<T>
    
    init {
        this.textLabelLoading.text.set("")
        
        val pane = Pane().apply {
            this.margin.set(Insets(32f))

            this += LoadingIconRod().apply {
                this.bounds.height.set(64f)
                Anchor.BottomRight.configure(this)
                this.bindWidthToSelfHeight(multiplier = 192f / 128f)
            }
        }
        this.sceneRoot += pane
    }
    
    override fun render(delta: Float) {
        super.render(delta)
        
        if (main.screen === this) { // Only load when this is the current screen -- don't load while in a transition
            minimumShowTime -= delta
            
            if (isReadyToContinue) {
                if (discardFrame) {
                    discardFrame = false
                } else {
                    minWaitTimeAfterLoadFinish -= delta
                    if (minimumShowTime <= 0f && minWaitTimeAfterLoadFinish <= 0f) {
                        if (!alreadyRanAfterLoad) {
                            alreadyRanAfterLoad = true
                            doAfterLoad(loadResult.result)
                        }
                    }
                }
            } else {
                minWaitTimeBeforeLoadStart -= delta
                if (minWaitTimeBeforeLoadStart <= 0f) {
                    val res = loadFunc.load(delta)
                    if (res != null) {
                        loadResult = res
                        isReadyToContinue = true
                    }
                }
            }
        }
    }
    
}
