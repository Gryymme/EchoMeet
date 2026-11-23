package com.example.chatapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageUtils {

    private const val TAG = "ImageUtils"

    fun saveProfileImage(uid: String, bitmap: Bitmap, context: Context) {
        try {
            val file = File(context.filesDir, "$uid.jpg")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            Log.d(TAG, "Profile image saved locally: ${file.absolutePath}") // ✅ Log success
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "Error saving profile image: ${e.message}", e) // ✅ Log error
        }
    }

    fun loadProfileImage(uid: String, context: Context): Bitmap? {
        val file = File(context.filesDir, "$uid.jpg")
        return if (file.exists()) {
            Log.d(TAG, "Profile image loaded from local storage: ${file.absolutePath}") // ✅ Log image found
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            Log.d(TAG, "No profile image found for UID: $uid") // ✅ Log image not found
            null
        }
    }
}