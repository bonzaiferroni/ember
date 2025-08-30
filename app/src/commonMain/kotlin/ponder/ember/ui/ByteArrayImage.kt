package ponder.ember.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data

@Composable
fun ByteArrayImage(bytes: ByteArray, modifier: Modifier = Modifier) {
    val bmp = remember(bytes) {
        val data = Data.makeFromBytes(bytes)
        val codec = Codec.makeFromData(data)
        val dst = Bitmap().apply { allocPixels(codec.imageInfo) }
        codec.readPixels(dst)
        dst.asImageBitmap()
    }
    Image(bitmap = bmp, contentDescription = null, modifier = modifier)
}