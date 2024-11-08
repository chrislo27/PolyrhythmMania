package polyrhythmmania.storymode.contract


enum class JingleType(val soundID: String?) {
    NONE(null),
    GBA("jingle_gba"), ARCADE("jingle_arcade"), MODERN("jingle_modern"),
}
