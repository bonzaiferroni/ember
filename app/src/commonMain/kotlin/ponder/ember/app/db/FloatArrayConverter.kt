package ponder.ember.app.db

import androidx.room.TypeConverter

object FloatArrayConverter {
    @TypeConverter
    @JvmStatic
    fun toBytes(floats: FloatArray): ByteArray {
        val out = ByteArray(floats.size * 4)
        var p = 0
        for (f in floats) {
            val bits = f.toRawBits()
            out[p++] = (bits and 0xFF).toByte()
            out[p++] = ((bits ushr 8) and 0xFF).toByte()
            out[p++] = ((bits ushr 16) and 0xFF).toByte()
            out[p++] = ((bits ushr 24) and 0xFF).toByte()
        }
        return out
    }

    @TypeConverter
    @JvmStatic
    fun fromBytes(bytes: ByteArray): FloatArray {
        require(bytes.size % 4 == 0) { "Byte size must be multiple of 4" }
        val out = FloatArray(bytes.size / 4)
        var p = 0
        for (i in out.indices) {
            val b0 = (bytes[p++].toInt() and 0xFF)
            val b1 = (bytes[p++].toInt() and 0xFF) shl 8
            val b2 = (bytes[p++].toInt() and 0xFF) shl 16
            val b3 = (bytes[p++].toInt() and 0xFF) shl 24
            out[i] = Float.fromBits(b0 or b1 or b2 or b3)
        }
        return out
    }
}