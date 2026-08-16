package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FirestoreStudent(
  val id: String = "",
  val name: String = "",
  val grade: String = "",
  val subjectsEnrolled: List<String> = emptyList(),
  val contactInfo: String = "",
  val parentName: String = "",
  val parentContact: String = ""
) {
  // Convert to Map for Firestore upload
  fun toMap(): Map<String, Any> {
    return mapOf(
      "id" to id,
      "name" to name,
      "grade" to grade,
      "subjectsEnrolled" to subjectsEnrolled,
      "contactInfo" to contactInfo,
      "parentName" to parentName,
      "parentContact" to parentContact
    )
  }

  companion object {
    // Helper to construct from Firestore document map
    fun fromMap(id: String, map: Map<String, Any>): FirestoreStudent {
      return FirestoreStudent(
        id = id,
        name = map["name"] as? String ?: "",
        grade = map["grade"] as? String ?: "",
        subjectsEnrolled = (map["subjectsEnrolled"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        contactInfo = map["contactInfo"] as? String ?: "",
        parentName = map["parentName"] as? String ?: "",
        parentContact = map["parentContact"] as? String ?: ""
      )
    }
  }
}

data class FirestoreAnnouncement(
  val id: String = "",
  val title: String = "",
  val message: String = "",
  val sender: String = "Admin Office",
  val targetAudience: String = "Everyone", // "Everyone", "Parents Only", "Teachers Only", "Students Only"
  val timestamp: Long = System.currentTimeMillis(),
  val dateStr: String = "",
  val batchName: String? = null,
  val type: String = "Announcement"
) {
  fun toMap(): Map<String, Any> {
    return mapOf(
      "id" to id,
      "title" to title,
      "message" to message,
      "sender" to sender,
      "targetAudience" to targetAudience,
      "timestamp" to timestamp,
      "dateStr" to dateStr,
      "batchName" to (batchName ?: ""),
      "type" to type
    )
  }

  companion object {
    fun fromMap(id: String, map: Map<String, Any>): FirestoreAnnouncement {
      return FirestoreAnnouncement(
        id = id,
        title = map["title"] as? String ?: "",
        message = map["message"] as? String ?: "",
        sender = map["sender"] as? String ?: "Admin Office",
        targetAudience = map["targetAudience"] as? String ?: "Everyone",
        timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        dateStr = map["dateStr"] as? String ?: "",
        batchName = map["batchName"] as? String,
        type = map["type"] as? String ?: "Announcement"
      )
    }
  }
}

object FirestoreService {
  private const val TAG = "FirestoreService"
  private const val COLLECTION_NAME = "student_profiles"
  private const val ANNOUNCEMENTS_COLLECTION = "announcements"

  private val _syncStatus = MutableStateFlow<String>("Initialized in Fallback/Local Mode (Awaiting Config)")
  val syncStatus = _syncStatus.asStateFlow()

  private val _remoteProfiles = MutableStateFlow<List<FirestoreStudent>>(emptyList())
  val remoteProfiles = _remoteProfiles.asStateFlow()

  private val _remoteAnnouncements = MutableStateFlow<List<FirestoreAnnouncement>>(
    listOf(
      FirestoreAnnouncement(
        id = "ANNC_DEF_1",
        title = "📢 Welcome to FSI My Tuition Platform",
        message = "Classes, test schedules, attendance tracking, and parent reports are live across all batches.",
        sender = "Institute Director",
        targetAudience = "Everyone",
        dateStr = "Today",
        type = "Announcement"
      ),
      FirestoreAnnouncement(
        id = "ANNC_DEF_2",
        title = "📝 Quarterly Parent-Teacher Meeting Scheduled",
        message = "Parent-Teacher meeting is scheduled for Saturday at 10:00 AM in Main Hall.",
        sender = "Admin Office",
        targetAudience = "Parents Only",
        dateStr = "Yesterday",
        type = "Announcement"
      ),
      FirestoreAnnouncement(
        id = "ANNC_DEF_3",
        title = "📚 Monthly Faculty Syllabus Submission",
        message = "All subject faculty members please submit updated monthly syllabus completion logs by Friday.",
        sender = "Academic Head",
        targetAudience = "Teachers Only",
        dateStr = "2 days ago",
        type = "Announcement"
      )
    )
  )
  val remoteAnnouncements = _remoteAnnouncements.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading = _isLoading.asStateFlow()

  private val isFirebaseInitialized: Boolean by lazy {
    try {
      FirebaseApp.getInstance()
      true
    } catch (e: IllegalStateException) {
      false
    }
  }

  // Safely initialize Firestore to prevent app crashes if google-services.json is missing or invalid
  val db: FirebaseFirestore? by lazy {
    try {
      if (!isFirebaseInitialized) {
        Log.i(TAG, "FirebaseApp not initialized. Falling back to Local memory database for Firestore.")
        _syncStatus.value = "Offline Fallback Mode (google-services.json not configured)"
        null
      } else {
        val instance = FirebaseFirestore.getInstance()
        _syncStatus.value = "Firestore Service Connected Successfully"
        instance
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to initialize Firebase Firestore. Fallback to Local memory database.", e)
      _syncStatus.value = "Offline Fallback Mode (google-services.json not configured)"
      null
    }
  }

  fun isAvailable(): Boolean {
    return db != null
  }

  // CREATE/ADD Student Profile to Firestore
  fun createStudentProfile(
    student: FirestoreStudent,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
  ) {
    val database = db
    if (database == null) {
      // Local Fallback simulation
      _remoteProfiles.value = _remoteProfiles.value + student
      _syncStatus.value = "Local memory mock update: Student '${student.name}' added."
      onSuccess()
      return
    }

    database.collection(COLLECTION_NAME)
      .document(student.id)
      .set(student.toMap())
      .addOnSuccessListener {
        Log.d(TAG, "Student profile '${student.name}' successfully written!")
        _syncStatus.value = "Student '${student.name}' synchronized to Cloud Firestore!"
        fetchStudentProfiles() // Refresh local copy
        onSuccess()
      }
      .addOnFailureListener { e ->
        Log.e(TAG, "Error writing student profile to Firestore", e)
        _syncStatus.value = "Cloud Sync Error: ${e.message}"
        onFailure(e)
      }
  }

  // READ all Student Profiles from Firestore collection
  fun fetchStudentProfiles(
    onComplete: ((List<FirestoreStudent>) -> Unit)? = null
  ) {
    _isLoading.value = true
    CoroutineScope(Dispatchers.Main).launch {
      delay(1000) // smooth shimmer loading delay
      val database = db
      if (database == null) {
        _isLoading.value = false
        onComplete?.invoke(_remoteProfiles.value)
        return@launch
      }

      _syncStatus.value = "Fetching live profiles from Firestore..."
      database.collection(COLLECTION_NAME)
        .get()
        .addOnSuccessListener { result ->
          val list = result.map { doc ->
            FirestoreStudent.fromMap(doc.id, doc.data)
          }
          _remoteProfiles.value = list
          _syncStatus.value = "Fetched ${list.size} profiles from Cloud Firestore collection"
          _isLoading.value = false
          onComplete?.invoke(list)
        }
        .addOnFailureListener { e ->
          Log.e(TAG, "Error fetching profiles", e)
          _syncStatus.value = "Error fetching live profiles: ${e.message}"
          _isLoading.value = false
          onComplete?.invoke(_remoteProfiles.value)
        }
    }
  }

  // UPDATE existing Student Profile in Firestore collection
  fun updateStudentProfile(
    student: FirestoreStudent,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
  ) {
    val database = db
    if (database == null) {
      _remoteProfiles.value = _remoteProfiles.value.map {
        if (it.id == student.id) student else it
      }
      _syncStatus.value = "Local update: ${student.name}'s profile modified."
      onSuccess()
      return
    }

    database.collection(COLLECTION_NAME)
      .document(student.id)
      .update(student.toMap())
      .addOnSuccessListener {
        Log.d(TAG, "Student ${student.id} profile updated!")
        _syncStatus.value = "Updated profile for ${student.name} in Firestore"
        fetchStudentProfiles()
        onSuccess()
      }
      .addOnFailureListener { e ->
        Log.e(TAG, "Error updating student profile", e)
        _syncStatus.value = "Update Error: ${e.message}"
        onFailure(e)
      }
  }

  // DELETE Student Profile from Firestore collection
  fun deleteStudentProfile(
    studentId: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
  ) {
    val database = db
    if (database == null) {
      _remoteProfiles.value = _remoteProfiles.value.filterNot { it.id == studentId }
      _syncStatus.value = "Local remove: Student ID $studentId deleted."
      onSuccess()
      return
    }

    database.collection(COLLECTION_NAME)
      .document(studentId)
      .delete()
      .addOnSuccessListener {
        Log.d(TAG, "Student profile $studentId deleted!")
        _syncStatus.value = "Deleted Student profile ID $studentId from Firestore"
        fetchStudentProfiles()
        onSuccess()
      }
      .addOnFailureListener { e ->
        Log.e(TAG, "Error deleting profile", e)
        _syncStatus.value = "Delete Error: ${e.message}"
        onFailure(e)
      }
  }

  // SEED live Firestore database with sample student profiles initially
  fun seedDatabaseWithDefaults(onSuccess: () -> Unit) {
    val sampleStudents = emptyList<FirestoreStudent>()

    onSuccess()
  }

  // CHECK if attendance document exists in Firestore for today's date and batch
  fun checkBatchAttendanceExists(
    batchId: String,
    dateStr: String,
    onResult: (Boolean) -> Unit
  ) {
    val database = db
    if (database == null) {
      val localRecords = AppRepository.attendance.value
      val exists = localRecords.any { it.batchName.equals(batchId, ignoreCase = true) && it.date == dateStr }
      onResult(exists)
      return
    }

    val docId = "${batchId.replace(" ", "_")}_${dateStr.replace("/", "-")}"
    database.collection("attendance")
      .document(docId)
      .get()
      .addOnSuccessListener { snapshot ->
        val exists = snapshot.exists()
        onResult(exists)
      }
      .addOnFailureListener {
        val localRecords = AppRepository.attendance.value
        val exists = localRecords.any { it.batchName.equals(batchId, ignoreCase = true) && it.date == dateStr }
        onResult(exists)
      }
  }

  // SAVE or update batch attendance document in Firestore
  fun saveBatchAttendanceToFirestore(
    batchId: String,
    dateStr: String,
    markedBy: String,
    records: List<AttendanceRecord>,
    onComplete: (() -> Unit)? = null
  ) {
    val database = db
    val docId = "${batchId.replace(" ", "_")}_${dateStr.replace("/", "-")}"
    val data = mapOf(
      "batchId" to batchId,
      "date" to dateStr,
      "markedBy" to markedBy,
      "timestamp" to System.currentTimeMillis(),
      "editableUntil" to System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
      "totalStudents" to records.size,
      "presentCount" to records.count { it.status == "Present" },
      "absentCount" to records.count { it.status == "Absent" },
      "records" to records.map { rec ->
        mapOf(
          "studentId" to rec.studentId,
          "studentName" to rec.studentName,
          "status" to rec.status,
          "timestamp" to rec.timestamp,
          "reason" to (rec.reason ?: "")
        )
      }
    )

    if (database == null) {
      onComplete?.invoke()
      return
    }

    database.collection("attendance")
      .document(docId)
      .set(data)
      .addOnSuccessListener {
        Log.d("FirestoreService", "Batch attendance document '$docId' saved to Firestore")
        onComplete?.invoke()
      }
      .addOnFailureListener { e ->
        Log.e("FirestoreService", "Failed to save batch attendance to Firestore", e)
        onComplete?.invoke()
      }
  }

  // PUBLISH Announcement to centralized Firestore collection
  fun publishAnnouncement(
    announcement: FirestoreAnnouncement,
    onSuccess: (() -> Unit)? = null,
    onFailure: ((Exception) -> Unit)? = null
  ) {
    // Always update local StateFlow immediately for responsive local UI
    _remoteAnnouncements.update { list ->
      listOf(announcement) + list.filterNot { it.id == announcement.id }
    }

    // Also push a local NotificationItem to trigger system push notifications
    AppRepository.addNotification(
      NotificationItem(
        id = announcement.id,
        title = announcement.title,
        message = "${announcement.message}\n\n— ${announcement.sender} [Audience: ${announcement.targetAudience}]",
        time = announcement.dateStr.ifBlank { AppRepository.getCurrentTimeStr() },
        type = "Announcement",
        recipientRole = announcement.targetAudience,
        isRead = false
      )
    )

    val database = db
    if (database == null) {
      Log.d(TAG, "Offline/Fallback mode: Announcement saved locally.")
      onSuccess?.invoke()
      return
    }

    database.collection(ANNOUNCEMENTS_COLLECTION)
      .document(announcement.id)
      .set(announcement.toMap())
      .addOnSuccessListener {
        Log.d(TAG, "Announcement '${announcement.title}' published to Firestore collection '$ANNOUNCEMENTS_COLLECTION'")
        _syncStatus.value = "Announcement broadcasted to Cloud Firestore!"
        onSuccess?.invoke()
      }
      .addOnFailureListener { e ->
        Log.e(TAG, "Failed to publish announcement to Firestore", e)
        onFailure?.invoke(e)
      }
  }

  // FETCH Announcements from centralized Firestore collection
  fun fetchAnnouncements(
    onComplete: ((List<FirestoreAnnouncement>) -> Unit)? = null
  ) {
    val database = db
    if (database == null) {
      onComplete?.invoke(_remoteAnnouncements.value)
      return
    }

    database.collection(ANNOUNCEMENTS_COLLECTION)
      .get()
      .addOnSuccessListener { result ->
        val list = result.map { doc ->
          FirestoreAnnouncement.fromMap(doc.id, doc.data)
        }.sortedByDescending { it.timestamp }

        if (list.isNotEmpty()) {
          _remoteAnnouncements.value = list
        }
        _syncStatus.value = "Fetched ${list.size} live announcements from Firestore"
        onComplete?.invoke(_remoteAnnouncements.value)
      }
      .addOnFailureListener { e ->
        Log.e(TAG, "Error fetching announcements from Firestore", e)
        onComplete?.invoke(_remoteAnnouncements.value)
      }
  }

  // REALTIME LISTENER for centralized Announcements collection
  fun listenToAnnouncements() {
    val database = db ?: return
    try {
      database.collection(ANNOUNCEMENTS_COLLECTION)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            Log.e(TAG, "Realtime announcements listener error: ${error.message}")
            return@addSnapshotListener
          }

          if (snapshot != null && !snapshot.isEmpty) {
            val list = snapshot.documents.map { doc ->
              FirestoreAnnouncement.fromMap(doc.id, doc.data ?: emptyMap())
            }.sortedByDescending { it.timestamp }

            _remoteAnnouncements.value = list
            Log.d(TAG, "Realtime update: ${list.size} announcements loaded from Firestore.")
          }
        }
    } catch (e: Exception) {
      Log.e(TAG, "Error setting snapshot listener for announcements", e)
    }
  }
}
