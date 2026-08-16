package com.example.data.db

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class RoomConverters {
  private val moshi = Moshi.Builder().build()
  private val stringListAdapter = moshi.adapter<List<String>>(
    Types.newParameterizedType(List::class.java, String::class.java)
  )

  @TypeConverter
  fun fromStringList(value: List<String>?): String {
    return stringListAdapter.toJson(value ?: emptyList())
  }

  @TypeConverter
  fun toStringList(value: String?): List<String> {
    if (value.isNullOrEmpty()) return emptyList()
    return try {
      stringListAdapter.fromJson(value) ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }
}
