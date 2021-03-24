package polyrhythmmania.util


data class Coord2(val x: Int, val y: Int) {
    fun add(other: Coord2): Coord2 = Coord2(this.x + other.x, this.y + other.y)
    fun add(x: Int, y: Int): Coord2 = Coord2(this.x + x, this.y + y)
    
    fun toCoord3(z: Int): Coord3 = Coord3(x, y, z)
}