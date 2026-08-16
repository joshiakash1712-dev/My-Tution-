package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batch_enrollments")
data class BatchEnrollmentEntity(
  @PrimaryKey val id: String,
  val studentId: String,
  val studentName: String,
  val batchId: String,
  val batchName: String,
  val className: String,
  val stream: String,
  val enrollmentDate: String,
  val feeStatus: String = "Up-to-Date",
  val status: String = "Active",
  val syncedToFirestore: Boolean = false,
  val lastUpdated: Long = System.currentTimeMillis()
)
