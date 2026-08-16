package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.Batch

@Entity(tableName = "batches")
data class BatchEntity(
  @PrimaryKey val id: String,
  val name: String,
  val schedule: String,
  val studentCount: Int,
  val teacherName: String,
  val syncedToFirestore: Boolean = false,
  val lastUpdated: Long = System.currentTimeMillis()
) {
  fun toDomain(): Batch {
    return Batch(
      id = id,
      name = name,
      schedule = schedule,
      studentCount = studentCount,
      teacherName = teacherName
    )
  }

  companion object {
    fun fromDomain(batch: Batch, synced: Boolean = false): BatchEntity {
      return BatchEntity(
        id = batch.id,
        name = batch.name,
        schedule = batch.schedule,
        studentCount = batch.studentCount,
        teacherName = batch.teacherName,
        syncedToFirestore = synced
      )
    }
  }
}
