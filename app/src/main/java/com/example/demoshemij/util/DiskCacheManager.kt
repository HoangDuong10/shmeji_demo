//package com.example.demoshemij.util
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import okhttp3.internal.cache.DiskLruCache
//import java.io.File
//import java.security.MessageDigest
//
//class DiskCacheManager(private val context: Context) {
//    private val diskCache: DiskLruCache
//    private val cacheDir: File = File(context.cacheDir, "image_cache")
//
//    companion object {
//        private const val APP_VERSION = 1
//        private const val VALUE_COUNT = 1
//        private const val MAX_SIZE = 50L * 1024 * 1024 // 50MB
//    }
//
//    init {
//        if (!cacheDir.exists()) {
//            cacheDir.mkdirs()
//        }
//        diskCache = DiskLruCache.open(cacheDir, APP_VERSION, VALUE_COUNT, MAX_SIZE)
//    }
//
//    fun get(key: String): Bitmap? {
//        return try {
//            val snapshot = diskCache.get(key.toMd5()) ?: return null
//            val inputStream = snapshot.getInputStream(0)
//            BitmapFactory.decodeStream(inputStream).also {
//                snapshot.close()
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            null
//        }
//    }
//
//    fun put(key: String, bitmap: Bitmap) {
//        try {
//            val editor = diskCache.edit(key.toMd5()) ?: return
//            val outputStream = editor.newOutputStream(0)
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
//            outputStream.close()
//            editor.commit()
//            diskCache.flush()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    fun clear() {
//        diskCache.delete()
//    }
//
//    private fun String.toMd5(): String {
//        val md = MessageDigest.getInstance("MD5")
//        val digest = md.digest(this.toByteArray())
//        return digest.joinToString("") { "%02x".format(it) }
//    }
//}