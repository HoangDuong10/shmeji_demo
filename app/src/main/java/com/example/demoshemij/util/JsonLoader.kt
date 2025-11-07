package com.example.demoshemij.util

import android.content.Context
import com.example.demoshemij.domain.CharacterList
import com.google.gson.Gson
import java.io.IOException

/**
 * Utility để load và parse JSON data
 */
object JsonLoader {
    
    /**
     * Load file data.json từ assets
     */
    fun loadCharacterData(context: Context): CharacterList? {
        return try {
            android.util.Log.d("JsonLoader", "Loading data.json from assets...")
            val jsonString = context.assets.open("data.json").bufferedReader().use { it.readText() }
            android.util.Log.d("JsonLoader", "JSON loaded, length: ${jsonString.length}")
            
            val gson = Gson()
            val data = gson.fromJson(jsonString, CharacterList::class.java)
            android.util.Log.d("JsonLoader", "JSON parsed, characters count: ${data?.characters?.size}")
            data
        } catch (e: IOException) {
            android.util.Log.e("JsonLoader", "Failed to load JSON", e)
            e.printStackTrace()
            null
        } catch (e: Exception) {
            android.util.Log.e("JsonLoader", "Failed to parse JSON", e)
            e.printStackTrace()
            null
        }
    }
}
