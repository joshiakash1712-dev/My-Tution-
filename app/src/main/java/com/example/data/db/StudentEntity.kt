package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.Student

@Entity(tableName = "students")
data class StudentEntity(
  @PrimaryKey val id: String,
  val name: String,
  val photo: String,
  val mobile: String,
  val parentName: String,
  val parentContact: String,
  val email: String,
  val dob: String,
  val gender: String,
  val address: String,
  val school: String,
  val className: String,
  val batch: String,
  val stream: String,
  val admissionDate: String,
  val status: String,
  val attendancePercent: Int,
  val overallAvg: Int,
  val rank: Int,
  val strongestSubject: String,
  val weakestSubject: String,
  val recentScoresSerialized: String,
  val syncedToFirestore: Boolean = false,
  val lastUpdated: Long = System.currentTimeMillis()
) {
  fun toDomain(): Student {
    val scores = if (recentScoresSerialized.isBlank()) {
      emptyList()
    } else {
      recentScoresSerialized.split(",").mapNotNull { part ->
        val pair = part.split("|")
        if (pair.size == 2) {
          pair[0] to (pair[1].toIntOrNull() ?: 0)
        } else null
      }
    }

    return Student(
      id = id,
      name = name,
      photo = photo,
      mobile = mobile,
      parentName = parentName,
      parentContact = parentContact,
      email = email,
      dob = dob,
      gender = gender,
      address = address,
      school = school,
      className = className,
      batch = batch,
      stream = stream,
      admissionDate = admissionDate,
      status = status,
      attendancePercent = attendancePercent,
      overallAvg = overallAvg,
      rank = rank,
      strongestSubject = strongestSubject,
      weakestSubject = weakestSubject,
      recentScores = scores
    )
  }

  companion object {
    fun fromDomain(student: Student, synced: Boolean = false): StudentEntity {
      val scoresSerialized = student.recentScores.joinToString(",") { "${it.first}|${it.second}" }
      return StudentEntity(
        id = student.id,
        name = student.name,
        photo = student.photo,
        mobile = student.mobile,
        parentName = student.parentName,
        parentContact = student.parentContact,
        email = student.email,
        dob = student.dob,
        gender = student.gender,
        address = student.address,
        school = student.school,
        className = student.className,
        batch = student.batch,
        stream = student.stream,
        admissionDate = student.admissionDate,
        status = student.status,
        attendancePercent = student.attendancePercent,
        overallAvg = student.overallAvg,
        rank = student.rank,
        strongestSubject = student.strongestSubject,
        weakestSubject = student.weakestSubject,
        recentScoresSerialized = scoresSerialized,
        syncedToFirestore = synced
      )
    }
  }
}
