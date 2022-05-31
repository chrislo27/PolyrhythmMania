package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar


sealed class Condition(val name: ReadOnlyVar<String>) {
    
    class Debug(name: ReadOnlyVar<String>) : Condition(name) {
        constructor(name: String) : this(ReadOnlyVar.const(name))
    }
    
}
