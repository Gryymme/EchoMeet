package com.example.chatapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.*

object ImageFileHelper {

    private const val IMAGE_DIR = "profile_images"

    // Save image bitmap to internal storage with filename = uid.jpg
    fun saveProfileImage(context: Context, bitmap: Bitmap, uid: String): String {
        val imageDir = File(context.filesDir, IMAGE_DIR)
        if (!imageDir.exists()) imageDir.mkdirs()

        val file = File(imageDir, "$uid.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath // can store this in DB if needed
    }

    // Load image from internal storage
    fun loadProfileImage(context: Context, uid: String): Bitmap? {
        val file = File(context.filesDir, "$IMAGE_DIR/$uid.jpg")
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
}