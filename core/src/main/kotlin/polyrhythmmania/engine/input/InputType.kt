package polyrhythmmania.engine.input

enum class InputType(val isDpad: Boolean) {
    A(false),
    DPAD_ANY(true),
    DPAD_UP(true),
    DPAD_DOWN(true),
    DPAD_LEFT(true),
    DPAD_RIGHT(true),
    ;
    
    fun isInputEquivalent(other: InputType): Boolean {
        if (other == DPAD_ANY && this.isDpad) {
            return true
        }
        if (this == DPAD_ANY && other.isDpad) {
            return true
        }
        
        return other == this
    }
}