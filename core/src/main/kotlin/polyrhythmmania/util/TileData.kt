package polyrhythmmania.util


class TileData<T>(val sizeX: Int, val sizeY: Int, val sizeZ: Int, init: (x: Int, y: Int, z: Int) -> T) {
    
    private val totalSize: Int = sizeX * sizeY * sizeZ
    private val totalXYCount: Int = sizeX * sizeY
    private val data: Array<Any?> = (0..<totalSize).map { i -> init(indexToX(i), indexToY(i), indexToZ(i)) }.toTypedArray()
    
    @Suppress("UNCHECKED_CAST")
    operator fun get(x: Int, y: Int, z: Int): T? {
        if (!contains(x, y, z)) return null
        return data[coordToIndex(x, y, z)] as T
    }
    
    @Suppress("UNCHECKED_CAST")
    operator fun get(coord: Coord3): T? {
        if (!contains(coord)) return null
        return data[coordToIndex(coord)] as T
    }
    
    @Suppress("UNCHECKED_CAST")
    fun getValue(x: Int, y: Int, z: Int): T {
        return data[coordToIndex(x, y, z)] as T
    }

    @Suppress("UNCHECKED_CAST")
    fun getValue(coord: Coord3): T {
        return data[coordToIndex(coord)] as T
    }
    
    @Suppress("UNCHECKED_CAST")
    fun getOrDefault(x: Int, y: Int, z: Int, defaultValue: T): T {
        if (!contains(x, y, z)) return defaultValue
        return data[coordToIndex(x, y, z)] as T
    }

    @Suppress("UNCHECKED_CAST")
    fun getOrDefault(coord: Coord3, defaultValue: T): T {
        if (!contains(coord)) return defaultValue
        return data[coordToIndex(coord)] as T
    }

    @Suppress("UNCHECKED_CAST")
    operator fun set(x: Int, y: Int, z: Int, value: T): T? {
        if (!contains(x, y, z)) return null
        val prev = data[coordToIndex(x, y, z)] as T
        data[coordToIndex(x, y, z)] = value
        return prev
    }

    @Suppress("UNCHECKED_CAST")
    operator fun set(coord: Coord3, value: T): T? {
        if (!contains(coord)) return null
        val prev = data[coordToIndex(coord)] as T
        data[coordToIndex(coord)] = value
        return prev
    }
    
    @Suppress("UNCHECKED_CAST")
    fun toList(): List<T> = data.map { it as T }

    inline fun forEach(action: (item: T, x: Int, y: Int, z: Int) -> Unit) {
        for (i in 0..<(sizeX * sizeY * sizeZ)) {
            val x = indexToX(i)
            val y = indexToY(i)
            val z = indexToZ(i)
            action(getValue(x, y, z), x, y, z)
        }
    }

    fun contains(coord: Coord3): Boolean = coord.x in 0..<sizeX && coord.y in 0..<sizeY && coord.z in 0..<sizeZ
    fun contains(x: Int, y: Int, z: Int): Boolean = x in 0..<sizeX && y in 0..<sizeY && z in 0..<sizeZ
    
    fun coordToIndex(x: Int, y: Int, z: Int): Int = (z * totalXYCount) + (y * sizeX) + (x)
    fun coordToIndex(coord: Coord3): Int = (coord.z * totalXYCount) + (coord.y * sizeX) + (coord.x)
    
    fun indexToX(index: Int): Int = (index % sizeX)
    fun indexToY(index: Int): Int = (index / sizeX % sizeY)
    fun indexToZ(index: Int): Int = (index / totalXYCount % sizeZ)
    
}