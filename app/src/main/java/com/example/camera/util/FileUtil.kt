package com.example.camera.util

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Environment
import android.provider.MediaStore
import com.example.camera.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtil {
    data class folder(
        val name: String,
        val filesNumber: Int
    )

    data class CustomFile(
        val path: String,
        val type: String,
        val thumbnail: Bitmap,
        val time: String
    )

    val files = ArrayList<CustomFile>()

    fun getFiles(context: Context) {
        files.clear()
        openFolder(
            context,
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/BeelineCamera"
        )
    }

    fun addFile(application: Application, filePath: String, type: String) {
        val file = CustomFile(
            filePath, type, getThumbnail(application, File(filePath), type), getFileTime(filePath)
        )
        val tempFiles = ArrayList<CustomFile>()
        tempFiles.addAll(files)
        files.clear()
        files.add(file)
        files.addAll(tempFiles)
    }

    private fun sortFiles() {
        files.sortWith { p0, p1 ->
            if (p1.time > p0.time) {
                1
            } else {
                -1
            }
        }
    }

    private fun openFolder(context: Context, path: String) {
        val allFiles = File(path).listFiles()
        if (allFiles != null) {
            for (file in allFiles) {
                val type = getFileType(file.path)
                if (type == "folder") {
                    openFolder(context, file.path)
                } else if (type == "jpg" || type == "mp4") {
                    files.add(
                        CustomFile(
                            file.path,
                            type,
                            getThumbnail(context, file, type),
                            getFileTime(file.path)
                        )
                    )
                    sortFiles()
                }
            }
        }
    }

    private fun getFileTime(filePath: String): String {
        val file = File(filePath)
        return SimpleDateFormat("yyyyMMddHHmmss").format(Date(file.lastModified()))
    }

    private fun getFileType(filePath: String): String {
        val dot = filePath.lastIndexOf(".")
        return if (dot > 0) {
            filePath.substring(dot + 1).toLowerCase()
        } else {
            "folder"
        }
    }

    private fun getThumbnail(context: Context, file: File, type: String): Bitmap {
        var bitmap: Bitmap? = null
        if (type == "jpg") {
            bitmap = BitmapFactory.decodeFile(file.path)
        } else if (type == "mp4") {
            bitmap = ThumbnailUtils.createVideoThumbnail(
                file.path,
                MediaStore.Video.Thumbnails.MICRO_KIND
            )
        }

        bitmap = if (bitmap != null) {
            val metric = context.resources.displayMetrics
            val width = metric.widthPixels / 3 - 10
            ThumbnailUtils.extractThumbnail(bitmap, width, width)
        } else {
            BitmapFactory.decodeResource(context.resources, R.drawable.model_video)
        }
        return bitmap
    }
}