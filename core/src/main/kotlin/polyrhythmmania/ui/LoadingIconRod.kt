package polyrhythmmania.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import paintbox.binding.IntVar
import paintbox.registry.AssetRegistry
import paintbox.ui.ImageNode
import paintbox.ui.ImageRenderingMode
import paintbox.util.gdxutils.set
import paintbox.util.wave.WaveUtils


class LoadingIconRod : ImageNode(null, renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO) {
    
    companion object {
        private const val NUM_FRAMES: Int = 6
    }
    
    private val regions: List<TextureRegion> = AssetRegistry.get<Texture>("loading_icon_rod").let { tex ->
        (0..<NUM_FRAMES).map { i ->
            TextureRegion(tex, 0, tex.height / NUM_FRAMES * i, tex.width, tex.height / NUM_FRAMES)
        }
    }
    
    private val animationFrame: IntVar = IntVar(0)
    
    var animationDuration: Float = 60f / 128f
    var animationTimeOffset: Float = MathUtils.random()
    
    init {
        this.tint.set(Color().set(255, 8, 0))
        this.textureRegion.bind { 
            regions[animationFrame.use() % regions.size]
        }
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val offsetMs = (animationDuration * animationTimeOffset * 1000).toLong()
        animationFrame.set((WaveUtils.getSawtoothWave(animationDuration, offsetMs = offsetMs) * regions.size).toInt().coerceIn(0, 5))
        super.renderSelf(originX, originY, batch)
    }
}