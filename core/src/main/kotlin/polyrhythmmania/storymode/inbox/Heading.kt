package polyrhythmmania.storymode.inbox

import com.badlogic.gdx.graphics.Texture
import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StoryL10N


enum class Heading(val id: String) {

    TEST_HEADING("test_heading"),
    ;

    val text: ReadOnlyVar<String> by lazy { StoryL10N.getVar("inboxItem.heading.testHeading") }
    val textureID: String get() = "desk_heading_$id"
    
    fun getTexture(): Texture = StoryAssets[this.textureID]

}
