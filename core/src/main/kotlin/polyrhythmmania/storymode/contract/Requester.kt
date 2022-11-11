package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.StoryL10N


class Requester(val id: String) {

    companion object {
        val DEBUG: Requester = Requester("debug")
        val POLYRHYTHM_INC: Requester = Requester("polyrhythm_inc")
        
        val STOMP_CHOMP_AGRI: Requester = Requester("stomp_chomp_agri")
    }
    
    val localizedName: ReadOnlyVar<String> by lazy { StoryL10N.getVar("inboxItem.contract.requester.$id") }
    
}
