package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
  entities = [StudentEntity::class, BatchEntity::class, BatchEnrollmentEntity::class, AttendanceRecordEntity::class],
  version = 2,
  exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppRoomDatabase : RoomDatabase() {

  abstract fun studentDao(): StudentDao
  abstract fun batchDao(): BatchDao
  abstract fun batchEnrollmentDao(): BatchEnrollmentDao
  abstract fun attendanceDao(): AttendanceDao

  companion object {
    @Volatile
    private var INSTANCE: AppRoomDatabase? = null

    fun getDatabase(context: Context): AppRoomDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppRoomDatabase::class.java,
          "my_tuition_fsi_database"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
