package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.storymode.StoryL10N


sealed class Condition(val name: ReadOnlyVar<String>) {
    
    companion object {
        val PASS_THE_LEVEL = TextOnly(StoryL10N.getVar("contract.condition.passLevel"))
    }
    
    class Debug(name: ReadOnlyVar<String>) : Condition(name) {
        constructor(name: String) : this(ReadOnlyVar.const(name))
    }
    
    class TextOnly(name: ReadOnlyVar<String>) : Condition(name)
    
}
