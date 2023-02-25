package polyrhythmmania.storymode

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import paintbox.registry.AssetRegistryInstance
import paintbox.registry.IAssetLoader
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.inbox.Heading


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
        StoryAssets.loadAsset<Texture>("logo", "story/textures/logo.png", linearTexture())

        StoryAssets.loadAsset<Texture>("title_checkerboard", "story/textures/title/checkerboard.png", TextureLoader.TextureParameter().apply {
            this.wrapU = Texture.TextureWrap.Repeat
            this.wrapV = Texture.TextureWrap.Repeat
        })
        StoryAssets.loadAsset<Texture>("title_file_blank", "story/textures/title/file/file_blank.png")
        StoryAssets.loadAsset<Texture>("title_icon_dotdotdot", "story/textures/title/file/icon_dotdotdot.png")
        StoryAssets.loadAsset<Texture>("title_icon_copy", "story/textures/title/file/icon_copy.png")
        StoryAssets.loadAsset<Texture>("title_icon_move", "story/textures/title/file/icon_move.png")
        StoryAssets.loadAsset<Texture>("title_icon_delete", "story/textures/title/file/icon_delete.png")

        StoryAssets.loadAsset<Texture>("desk_ui_scrollbar_back", "story/textures/desk/ui/scrollbar_back.png")
        StoryAssets.loadAsset<Texture>("desk_ui_scrollbar_thumb_active", "story/textures/desk/ui/scrollbar_thumb_active.png")
        StoryAssets.loadAsset<Texture>("desk_ui_scrollbar_thumb_inactive", "story/textures/desk/ui/scrollbar_thumb_inactive.png")
        StoryAssets.loadAsset<Texture>("desk_ui_pane", "story/textures/desk/ui/pane.9patch.png")
        StoryAssets.loadAsset<Texture>("desk_ui_pane_dark", "story/textures/desk/ui/pane_dark.9patch.png")
        StoryAssets.loadAsset<Texture>("desk_ui_icon_nomiss", "story/textures/desk/ui/icon/nomiss.png")
        StoryAssets.loadAsset<Texture>("desk_ui_icon_skillstar", "story/textures/desk/ui/icon/skillstar.png")
        StoryAssets.loadAsset<Texture>("desk_bg_background", "story/textures/desk/bg/background.png")
        StoryAssets.loadAsset<Texture>("desk_bg_envelope_1", "story/textures/desk/bg/envelope_1.png")
        StoryAssets.loadAsset<Texture>("desk_bg_envelope_2", "story/textures/desk/bg/envelope_2.png")
        StoryAssets.loadAsset<Texture>("desk_bg_envelope_3", "story/textures/desk/bg/envelope_3.png")
        StoryAssets.loadAsset<Texture>("desk_bg_envelope_4", "story/textures/desk/bg/envelope_4.png")
        StoryAssets.loadAsset<Texture>("desk_bg_envelope_5", "story/textures/desk/bg/envelope_5.png")
        StoryAssets.loadAsset<Texture>("desk_bg_envelope_big", "story/textures/desk/bg/envelope_big.png")
        StoryAssets.loadAsset<Texture>("desk_bg_inbox", "story/textures/desk/bg/inbox.png")
        StoryAssets.loadAsset<Texture>("desk_bg_inbox_available", "story/textures/desk/bg/inbox_available.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_background_0", "story/textures/desk/bg/piston_bg_0.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_background_1", "story/textures/desk/bg/piston_bg_1.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_background_2", "story/textures/desk/bg/piston_bg_2.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_background_3", "story/textures/desk/bg/piston_bg_3.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_background_4", "story/textures/desk/bg/piston_bg_4.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_background_5", "story/textures/desk/bg/piston_bg_5.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_background_6", "story/textures/desk/bg/piston_bg_6.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_background_7", "story/textures/desk/bg/piston_bg_7.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_background_8", "story/textures/desk/bg/piston_bg_8.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_foreground_0", "story/textures/desk/bg/piston_fg_0.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_foreground_1", "story/textures/desk/bg/piston_fg_1.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_foreground_2", "story/textures/desk/bg/piston_fg_2.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_foreground_3", "story/textures/desk/bg/piston_fg_3.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_foreground_4", "story/textures/desk/bg/piston_fg_4.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_foreground_5", "story/textures/desk/bg/piston_fg_5.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_foreground_6", "story/textures/desk/bg/piston_fg_6.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_foreground_7", "story/textures/desk/bg/piston_fg_7.png")
        StoryAssets.loadAsset<Texture>("desk_bg_piston_foreground_8", "story/textures/desk/bg/piston_fg_8.png")
        StoryAssets.loadAsset<Texture>("desk_bg_tube_1", "story/textures/desk/bg/tube_1.png")
        StoryAssets.loadAsset<Texture>("desk_bg_tube_2", "story/textures/desk/bg/tube_2.png")
        StoryAssets.loadAsset<Texture>("desk_bg_tube_3", "story/textures/desk/bg/tube_3.png")
        Heading.values().forEach { heading ->
            StoryAssets.loadAsset<Texture>(heading.textureID, "story/textures/desk/heading/${heading.id}.png")
        }
        StoryAssets.loadAsset<Texture>("desk_contract_envelope", "story/textures/desk/contract_envelope.png")
        StoryAssets.loadAsset<Texture>("desk_contract_paper", "story/textures/desk/contract_paper.png")
        StoryAssets.loadAsset<Texture>("desk_contract_full", "story/textures/desk/contract_full.png")
        StoryAssets.loadAsset<Texture>("desk_contract_employment_blank", "story/textures/desk/employment_contract_blank.png")
        StoryAssets.loadAsset<Texture>("desk_contract_employment_signed", "story/textures/desk/employment_contract_signed.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_frame", "story/textures/desk/inboxitem/frame/frame.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_frame_cap_top", "story/textures/desk/inboxitem/frame/frame_cap_top.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_frame_cap_bottom", "story/textures/desk/inboxitem/frame/frame_cap_bottom.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_selected_outline", "story/textures/desk/inboxitem/selected_outline.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_blank", "story/textures/desk/inboxitem/blank.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_unavailable", "story/textures/desk/inboxitem/unavailable.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_available", "story/textures/desk/inboxitem/available.png")
        (0..4).forEach { i ->
            StoryAssets.loadAsset<Texture>("desk_inboxitem_available_blink_$i", "story/textures/desk/inboxitem/available_blink/blink_$i.png")
        }
        StoryAssets.loadAsset<Texture>("desk_inboxitem_cleared", "story/textures/desk/inboxitem/cleared.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_skipped", "story/textures/desk/inboxitem/skipped.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_off", "story/textures/desk/inboxitem/led_off.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_employment_signed_blue", "story/textures/desk/inboxitem/employment_contract/signed_blue.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_employment_signed_green", "story/textures/desk/inboxitem/employment_contract/signed_green.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_employment_signed_red", "story/textures/desk/inboxitem/employment_contract/signed_red.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_employment_unsigned_blue", "story/textures/desk/inboxitem/employment_contract/unsigned_blue.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_employment_unsigned_green", "story/textures/desk/inboxitem/employment_contract/unsigned_green.png")
        StoryAssets.loadAsset<Texture>("desk_inboxitem_employment_unsigned_red", "story/textures/desk/inboxitem/employment_contract/unsigned_red.png")
        
        StoryAssets.loadAsset<Texture>("dunk_background_fruit_basket_1", "story/textures/world/dunk/fruit_basket_1.jpg", linearTexture())
        StoryAssets.loadAsset<Texture>("dunk_background_fruit_basket_2", "story/textures/world/dunk/fruit_basket_2.jpg", linearTexture())
        StoryAssets.loadAsset<Texture>("dunk_background_hole_in_one_1", "story/textures/world/dunk/hole_in_one_1.jpg", linearTexture())
        StoryAssets.loadAsset<Texture>("dunk_background_hole_in_one_2", "story/textures/world/dunk/hole_in_one_2.jpg", linearTexture())
        
        StoryAssets.loadAsset<Texture>("boss_robot_upside", "story/textures/world/boss/robot_upside.png")
        StoryAssets.loadAsset<Texture>("boss_robot_downside", "story/textures/world/boss/robot_downside.png")
        StoryAssets.loadAsset<Texture>("boss_robot_middle", "story/textures/world/boss/robot_middle.png")
        
        StoryAssets.loadAsset<Sound>("jingle_gba", "story/sounds/intro/jingle_gba.ogg")
        StoryAssets.loadAsset<Sound>("jingle_arcade", "story/sounds/intro/jingle_arcade.ogg")
        StoryAssets.loadAsset<Sound>("jingle_modern", "story/sounds/intro/jingle_modern.ogg")
        StoryAssets.loadAsset<Sound>("sfx_desk_unlocked", "story/sounds/item_unlocked.ogg")
        StoryAssets.loadAsset<Sound>("sfx_desk_signature", "story/sounds/signature.ogg")
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
