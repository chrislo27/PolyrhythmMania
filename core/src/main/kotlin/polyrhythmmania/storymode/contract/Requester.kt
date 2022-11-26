package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.StoryL10N


class Requester(val id: String) {

    companion object {
        val DEBUG: Requester = Requester("debug")
        
        val POLYRHYTHM_INC: Requester = Requester("polyrhythm_inc")
        val BUILDROID: Requester = Requester("buildroid")
        val POLYBUILD: Requester = Requester("polybuild")
        
        val ALIENS: Requester = Requester("aliens")
        val ANIMAL_ACROBATICS: Requester = Requester("animal_acrobatics")
        val CUBE_ROOT: Requester = Requester("cube_root")
        val DIYRE: Requester = Requester("diyre")
        val DOUGH: Requester = Requester("dough")
        val GAME_DEV: Requester = Requester("game_dev")
        val GOOD_SPORTS: Requester = Requester("good_sports")
        val KRIQ: Requester = Requester("kriq")
        val LOCKSTEP_MARTIAN: Requester = Requester("lockstep_martian")
        val MOON_BUNNY: Requester = Requester("moon_bunny")
        val PEAS: Requester = Requester("peas")
        val SHIPSTEERING: Requester = Requester("shipsteering")
        val STOMP_CHOMP_AGRI: Requester = Requester("stomp_chomp_agri")
        val TOSS_BOYS: Requester = Requester("toss_boys")
    }
    
    val localizedName: ReadOnlyVar<String> by lazy { StoryL10N.getVar("inboxItem.contract.requester.$id") }
    
}
