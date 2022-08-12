package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.StoryL10N


class Requester(val id: String) {

    companion object {
        val DEBUG: Requester = Requester("debug")
        val HR: Requester = Requester("hr")
    }
    
    val localizedName: ReadOnlyVar<String> by lazy { StoryL10N.getVar("contract.requester.$id") }
    
}
