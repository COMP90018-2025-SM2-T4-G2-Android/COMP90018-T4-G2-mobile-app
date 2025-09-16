package com.cashpal.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class DataRepository(private val context: Context) {
    
    private val gson = Gson()
    
    fun loadAppData(): AppData? {
        return try {
            val jsonString = loadJSONFromAssets("app_data.json")
            gson.fromJson(jsonString, AppData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun loadJSONFromAssets(fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
    }
}
