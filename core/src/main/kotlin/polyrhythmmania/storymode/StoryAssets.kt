package polyrhythmmania.storymode

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import paintbox.registry.AssetRegistryInstance
import paintbox.registry.IAssetLoader
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame


object StoryAssets : AssetRegistryInstance() {
    init {
        Gdx.app.postRunnable {
            PRManiaGame.instance.addDisposeCall {
                this.disposeQuietly()
            }
        }
    }
}

class StoryAssetLoader : IAssetLoader {
    override fun addManagedAssets(manager: AssetManager) {
        StoryAssets.loadAsset<Texture>("logo", "story/textures/logo_2lines_story.png", linearTexture())
        
        StoryAssets.loadAsset<Texture>("desk_envelope", "story/textures/desk/envelope.png", linearTexture())
        
        StoryAssets.loadAsset<Sound>("jingle_gba", "story/sounds/jingle_gba.ogg")
        StoryAssets.loadAsset<Sound>("jingle_arcade", "story/sounds/jingle_arcade.ogg")
        StoryAssets.loadAsset<Sound>("jingle_modern", "story/sounds/jingle_modern.ogg")
        StoryAssets.loadAsset<Sound>("score_filling", "sounds/results/score_filling.ogg")
        StoryAssets.loadAsset<Sound>("score_finish", "sounds/results/score_finish.ogg")
        StoryAssets.loadAsset<Sound>("score_finish_nhs", "sounds/results/score_finish_nhs.ogg")
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
    }
}
