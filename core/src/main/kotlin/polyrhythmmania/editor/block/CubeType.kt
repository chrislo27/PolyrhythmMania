package polyrhythmmania.editor.block

enum class CubeType {
    NONE,
    PISTON,
    PLATFORM, 
    ;
    
    companion object {
        val VALUES: List<CubeType> = values().toList()
    }
}
