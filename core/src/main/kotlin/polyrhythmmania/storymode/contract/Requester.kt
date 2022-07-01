package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.StoryL10N


enum class Requester(val id: String) {
    
    DEBUG("debug"),
    HR("hr"),
    ;
    
    val localizedName: ReadOnlyVar<String> by lazy { StoryL10N.getVar("contract.requester.$id") }
    
}