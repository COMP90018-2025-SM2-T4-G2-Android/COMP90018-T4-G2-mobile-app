package com.cashpal.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import com.cashpal.app.di.ServiceLocator

class DataRepository(private val context: Context) {
    
    private val gson = Gson()
    private val firebaseRepository = ServiceLocator.getRepository()
    
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
    
    // Firebase integration methods
    fun getFirebaseRepository() = firebaseRepository
    
    fun isUserSignedIn(): Boolean = firebaseRepository.isUserSignedIn()
    
    fun getCurrentUserId(): String? = firebaseRepository.getCurrentUser()?.uid
}
