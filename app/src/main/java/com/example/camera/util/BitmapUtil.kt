package com.example.camera.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

object BitmapUtil {

    fun Bitmap.mirror(): Bitmap {
        val matrix = Matrix()
        matrix.postScale(-1F, 1F)
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    fun Bitmap.rotate(orientation: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(orientation.toFloat())
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    fun Bitmap.toByteArray(): ByteArray {
        val os = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 100, os)
        return os.toByteArray()
    }

    fun ByteArray.toBitmap(): Bitmap {
        return BitmapFactory.decodeByteArray(this, 0, size)
    }
}