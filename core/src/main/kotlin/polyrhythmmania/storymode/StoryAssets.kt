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
        
        StoryAssets.loadAsset<Sound>("jingle_gba", "story/sounds/intro/jingle_gba.ogg")
        StoryAssets.loadAsset<Sound>("jingle_arcade", "story/sounds/intro/jingle_arcade.ogg")
        StoryAssets.loadAsset<Sound>("jingle_modern", "story/sounds/intro/jingle_modern.ogg")
        StoryAssets.loadAsset<Sound>("score_filling", "sounds/results/score_filling.ogg")
        StoryAssets.loadAsset<Sound>("score_finish", "sounds/results/score_finish.ogg")
        StoryAssets.loadAsset<Sound>("score_finish_nhs", "sounds/results/score_finish_nhs.ogg")
        StoryAssets.loadAsset<Sound>("score_jingle_tryagain", "story/sounds/results/jingle_tryagain.ogg")
        StoryAssets.loadAsset<Sound>("score_jingle_pass", "story/sounds/results/jingle_ok2.ogg")
        StoryAssets.loadAsset<Sound>("score_jingle_pass_hard", "story/sounds/results/jingle_superb.ogg")
    }

    override fun addUnmanagedAssets(assets: MutableMap<String, Any>) {
    }
}
