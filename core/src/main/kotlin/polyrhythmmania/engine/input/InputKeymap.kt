package polyrhythmmania.engine.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.ControllerMapping
import com.badlogic.gdx.controllers.ControllerPowerLevel
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import polyrhythmmania.util.RodinSpecialChars


data class InputKeymapKeyboard(
        val buttonA: Int = Input.Keys.J,
        val buttonDpadUp: Int = Input.Keys.W,
        val buttonDpadDown: Int = Input.Keys.S,
        val buttonDpadLeft: Int = Input.Keys.A,
        val buttonDpadRight: Int = Input.Keys.D,
) {
    companion object {
        const val TEXT_BUTTON_A: String = "${RodinSpecialChars.BORDERED_A}"
        const val TEXT_BUTTON_DPAD_ANY: String = "${RodinSpecialChars.BORDERED_DPAD}"
        const val TEXT_BUTTON_DPAD_UP: String = "${RodinSpecialChars.BORDERED_DPAD}${RodinSpecialChars.BORDERED_JOY_U}"
        const val TEXT_BUTTON_DPAD_DOWN: String = "${RodinSpecialChars.BORDERED_DPAD}${RodinSpecialChars.BORDERED_JOY_D}"
        const val TEXT_BUTTON_DPAD_LEFT: String = "${RodinSpecialChars.BORDERED_DPAD}${RodinSpecialChars.BORDERED_JOY_L}"
        const val TEXT_BUTTON_DPAD_RIGHT: String = "${RodinSpecialChars.BORDERED_DPAD}${RodinSpecialChars.BORDERED_JOY_R}"
        
        fun fromJson(obj: JsonObject): InputKeymapKeyboard {
            return InputKeymapKeyboard(
                    obj["a"]?.asInt() ?: Input.Keys.J,
                    obj["dpadUp"]?.asInt() ?: Input.Keys.W,
                    obj["dpadDown"]?.asInt() ?: Input.Keys.S,
                    obj["dpadLeft"]?.asInt() ?: Input.Keys.A,
                    obj["dpadRight"]?.asInt() ?: Input.Keys.D,
            )
        }
    }

    fun toJson(): JsonObject {
        return Json.`object`().also { obj ->
            obj.add("a", this.buttonA)
            obj.add("dpadUp", this.buttonDpadUp)
            obj.add("dpadDown", this.buttonDpadDown)
            obj.add("dpadLeft", this.buttonDpadLeft)
            obj.add("dpadRight", this.buttonDpadRight)
        }
    }
}
