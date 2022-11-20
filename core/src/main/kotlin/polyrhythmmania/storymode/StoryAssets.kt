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
        
        StoryAssets.loadAsset<Texture>("desk_ui_scrollbar_back", "story/textures/desk/ui/scrollbar_back.png")
        StoryAssets.loadAsset<Texture>("desk_ui_scrollbar_thumb_active", "story/textures/desk/ui/scrollbar_thumb_active.png")
        StoryAssets.loadAsset<Texture>("desk_ui_scrollbar_thumb_inactive", "story/textures/desk/ui/scrollbar_thumb_inactive.png")
        StoryAssets.loadAsset<Texture>("desk_ui_pane", "story/textures/desk/ui/pane.9patch.png")
        StoryAssets.loadAsset<Texture>("desk_bg_temp", "story/textures/desk/bg_temp.png")
        StoryAssets.loadAsset<Texture>("desk_bg", "story/textures/desk/bg/bg.png")
        StoryAssets.loadAsset<Texture>("desk_bg_pipes_lower", "story/textures/desk/bg/pipes_lower.png")
        StoryAssets.loadAsset<Texture>("desk_bg_pipes_upper", "story/textures/desk/bg/pipes_upper.png")
        StoryAssets.loadAsset<Texture>("desk_bg_pistons", "story/textures/desk/bg/pistons.png")
        StoryAssets.loadAsset<Texture>("desk_contract_envelope", "story/textures/desk/contract_envelope.png")
        StoryAssets.loadAsset<Texture>("desk_contract_paper", "story/textures/desk/contract_paper.png")
        StoryAssets.loadAsset<Texture>("desk_contract_full", "story/textures/desk/contract_full.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_frame", "story/textures/desk/inboxitem/frame.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_frame_cap_top", "story/textures/desk/inboxitem/frame_cap_top.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_frame_cap_bottom", "story/textures/desk/inboxitem/frame_cap_bottom.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_selected", "story/textures/desk/inboxitem/selected.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_available", "story/textures/desk/inboxitem/available.png")
        (0..4).forEach { i ->
            StoryAssets.loadAsset<Texture>("desk_inboxitem_available_blink_$i", "story/textures/desk/inboxitem/available_blink/blink_$i.png")
        }
        StoryAssets.loadAsset<Texture>("desk_inboxitem_unavailable", "story/textures/desk/inboxitem/unavailable.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_cleared", "story/textures/desk/inboxitem/cleared.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_skipped", "story/textures/desk/inboxitem/skipped.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_off", "story/textures/desk/inboxitem/led_off.png")
        
        StoryAssets.loadAsset<Texture>("dunk_background_fruit_basket_1", "story/textures/world/dunk/fruit_basket_1.jpg", linearTexture())
        StoryAssets.loadAsset<Texture>("dunk_background_fruit_basket_2", "story/textures/world/dunk/fruit_basket_2.jpg", linearTexture())
        StoryAssets.loadAsset<Texture>("dunk_background_hole_in_one_1", "story/textures/world/dunk/hole_in_one_1.jpg", linearTexture())
        StoryAssets.loadAsset<Texture>("dunk_background_hole_in_one_2", "story/textures/world/dunk/hole_in_one_2.jpg", linearTexture())
        
        StoryAssets.loadAsset<Sound>("jingle_gba", "story/sounds/intro/jingle_gba.ogg")
        StoryAssets.loadAsset<Sound>("jingle_arcade", "story/sounds/intro/jingle_arcade.ogg")
        StoryAssets.loadAsset<Sound>("jingle_modern", "story/sounds/intro/jingle_modern.ogg")
        StoryAssets.loadAsset<Sound>("sfx_desk_unlocked", "story/sounds/item_unlocked.ogg")
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
