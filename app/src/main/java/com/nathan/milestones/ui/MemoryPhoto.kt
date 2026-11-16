package com.nathan.milestones.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shows a saved photo. It loads in the background (so scrolling stays smooth),
 * shrinks big photos to save memory, and turns sideways phone photos the right way up.
 */
@Composable
fun MemoryPhoto(path: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { loadPhoto(path, 1080)?.asImageBitmap() }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

private fun loadPhoto(path: String, maxSize: Int): Bitmap? {
    // 1. Read only the size first
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    // 2. Load a smaller version (a 4000px photo becomes ~1000px)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxSize || bounds.outHeight / (sample * 2) >= maxSize) {
        sample *= 2
    }
    val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return null

    // 3. Rotate if the camera saved it sideways
    val degrees = try {
        when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } catch (e: Exception) {
        0f
    }
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}