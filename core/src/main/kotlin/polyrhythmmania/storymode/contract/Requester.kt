package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.StoryL10N


class Requester(val id: String, val isNameLong: Boolean = false) {

    companion object {
        val DEBUG: Requester = Requester("debug", false)
        
        val POLYRHYTHM_INC: Requester = Requester("polyrhythm_inc", false)
        val BUILDROID: Requester = Requester("buildroid", false)
        val POLYBUILD: Requester = Requester("polybuild", false)
        
        val ALIENS: Requester = Requester("aliens", true)
        val ANIMAL_ACROBATICS: Requester = Requester("animal_acrobatics", false)
        val CUBE_ROOT: Requester = Requester("cube_root", false)
        val DIYRE: Requester = Requester("diyre", true)
        val DOUGH: Requester = Requester("dough", false)
        val GAME_DEV: Requester = Requester("game_dev", false)
        val GOOD_SPORTS: Requester = Requester("good_sports", false)
        val KRIQ: Requester = Requester("kriq", true)
        val LOCKSTEP_MARTIAN: Requester = Requester("lockstep_martian", true)
        val MOON_BUNNY: Requester = Requester("moon_bunny", true)
        val PEAS: Requester = Requester("peas", false)
        val SHIPSTEERING: Requester = Requester("shipsteering", true)
        val STOMP_CHOMP_AGRI: Requester = Requester("stomp_chomp_agri", true)
        val TOSS_BOYS: Requester = Requester("toss_boys", false)
    }
    
    val localizedName: ReadOnlyVar<String> by lazy { StoryL10N.getVar("inboxItem.contract.requester.$id") }
    
}
