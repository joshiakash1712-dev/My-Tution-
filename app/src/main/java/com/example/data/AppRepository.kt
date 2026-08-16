package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.data.db.BatchEnrollmentEntity

object AppRepository {
  private var preferences: android.content.SharedPreferences? = null
  private var studentBatchRepo: StudentBatchRepository? = null
  private var appContext: android.content.Context? = null

  private val _isLoggedIn = MutableStateFlow(false)
  val isLoggedIn = _isLoggedIn.asStateFlow()

  private val _currentUserIdentifier = MutableStateFlow("")
  val currentUserIdentifier = _currentUserIdentifier.asStateFlow()

  private val _passwordResetNotice = MutableStateFlow<String?>(null)
  val passwordResetNotice = _passwordResetNotice.asStateFlow()

  private val _currentRole = MutableStateFlow(UserRole.ADMIN)
  val currentRole = _currentRole.asStateFlow()

  private val _savedAccounts = MutableStateFlow<List<SavedAccount>>(emptyList())
  val savedAccounts = _savedAccounts.asStateFlow()

  private val _darkThemeMode = MutableStateFlow(DarkThemeMode.SYSTEM)
  val darkThemeMode = _darkThemeMode.asStateFlow()

  private val _batchEnrollments = MutableStateFlow<List<BatchEnrollmentEntity>>(emptyList())
  val batchEnrollments = _batchEnrollments.asStateFlow()

  fun initialize(context: android.content.Context) {
    appContext = context.applicationContext
    preferences = context.getSharedPreferences("my_tuition_fsi_prefs", android.content.Context.MODE_PRIVATE)
    
    com.example.utils.NotificationHelper.createNotificationChannel(context.applicationContext)

    FirestoreService.fetchAnnouncements()
    FirestoreService.listenToAnnouncements()

    if (_notifications.value.isEmpty()) {
      _notifications.value = listOf(
        NotificationItem(
          id = "NOT_INIT_1",
          title = "📢 Welcome to FSI My Tuition!",
          message = "Stay updated with class schedules, tests, homework, and institute announcements.",
          time = getCurrentTimeStr(),
          type = "Announcement",
          recipientRole = "All",
          isRead = false
        ),
        NotificationItem(
          id = "NOT_INIT_2",
          title = "📢 Mid-Term Examination Schedule Announced",
          message = "Mid-term tests for 10th & 12th standards commence next Monday. Check test tab for syllabus.",
          time = "Yesterday",
          type = "Announcement",
          recipientRole = "All",
          isRead = false
        )
      )
    }

    val repo = StudentBatchRepository(context.applicationContext)
    studentBatchRepo = repo

    CoroutineScope(Dispatchers.IO).launch {
      repo.allStudents.collect { list ->
        if (list.isEmpty()) {
          val demoStudents = SeedData.get15DemoStudents()
          repo.bulkInsertStudents(demoStudents)
        } else {
          _students.value = list
        }
      }
    }

    CoroutineScope(Dispatchers.IO).launch {
      repo.allBatches.collect { list ->
        if (list.isEmpty()) {
          SeedData.defaultBatches.forEach { b ->
            repo.insertBatch(b)
          }
        } else {
          _batches.value = list
        }
      }
    }

    CoroutineScope(Dispatchers.IO).launch {
      repo.allEnrollments.collect { list ->
        _batchEnrollments.value = list
      }
    }

    CoroutineScope(Dispatchers.IO).launch {
      repo.allAttendance.collect { list ->
        if (list.isNotEmpty()) {
          _attendance.value = list
        }
      }
    }

    // Rule 1: Automatic non-permanent rolling lifecycle cleanup (purge records older than 90 days)
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val cutoffTimestamp = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000L)
        repo.purgeExpiredAttendance(cutoffTimestamp)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    val persistedLoggedIn = preferences?.getBoolean("is_logged_in", false) ?: false
    val persistedIdentifier = preferences?.getString("user_identifier", "") ?: ""
    val persistedRoleStr = preferences?.getString("user_role", UserRole.ADMIN.name) ?: UserRole.ADMIN.name
    val persistedThemeStr = preferences?.getString("dark_theme_mode", DarkThemeMode.SYSTEM.name) ?: DarkThemeMode.SYSTEM.name
    
    val persistedRole = try {
      UserRole.valueOf(persistedRoleStr)
    } catch (e: Exception) {
      UserRole.ADMIN
    }

    val persistedTheme = try {
      DarkThemeMode.valueOf(persistedThemeStr)
    } catch (e: Exception) {
      DarkThemeMode.SYSTEM
    }

    _isLoggedIn.value = persistedLoggedIn
    _currentUserIdentifier.value = persistedIdentifier
    _currentRole.value = persistedRole
    _darkThemeMode.value = persistedTheme
    
    val loadedAccounts = loadSavedAccountsFromPrefs().toMutableList()
    if (persistedLoggedIn && persistedIdentifier.isNotBlank() && loadedAccounts.none { it.identifier == persistedIdentifier }) {
      loadedAccounts.add(SavedAccount(persistedIdentifier, persistedRole, persistedIdentifier.substringBefore("@")))
      saveSavedAccountsToPrefs(loadedAccounts)
    }
    _savedAccounts.value = loadedAccounts

    // Check if Firebase is signed in, sync if needed
    val firebaseAuthUser = FirebaseAuthService.auth?.currentUser
    if (firebaseAuthUser != null) {
      val email = firebaseAuthUser.email ?: persistedIdentifier
      if (!persistedLoggedIn) {
        _isLoggedIn.value = true
        _currentUserIdentifier.value = email
        saveSession(true, email, persistedRole)
      }
    }
    // Seed cryptographic SHA-256 salted credentials for master admins, teachers, and students
    com.example.security.SecurityEngine.seedDefaultCredentials(
      students = _students.value,
      teachers = _teachers.value.ifEmpty { SeedData.defaultTeachers }
    )

    loadAttendanceFromPrefs()
  }

  private fun saveSavedAccountsToPrefs(accounts: List<SavedAccount>) {
    val serialized = accounts.joinToString(";;;") { "${it.identifier}:::${it.role.name}:::${it.displayName}" }
    preferences?.edit()?.putString("saved_accounts_list", serialized)?.apply()
  }

  private fun loadSavedAccountsFromPrefs(): List<SavedAccount> {
    val serialized = preferences?.getString("saved_accounts_list", "") ?: ""
    if (serialized.isBlank()) return emptyList()
    return serialized.split(";;;").mapNotNull { entry ->
      val parts = entry.split(":::")
      if (parts.size >= 3) {
        val id = parts[0]
        val role = try { UserRole.valueOf(parts[1]) } catch (e: Exception) { UserRole.STUDENT }
        val name = parts[2]
        SavedAccount(id, role, name)
      } else null
    }
  }

  private fun saveSession(loggedIn: Boolean, identifier: String, role: UserRole) {
    preferences?.edit()?.apply {
      putBoolean("is_logged_in", loggedIn)
      putString("user_identifier", identifier)
      putString("user_role", role.name)
      apply()
    }
  }

  fun setRole(role: UserRole) {
    _currentRole.value = role
    saveSession(_isLoggedIn.value, _currentUserIdentifier.value, role)
  }

  fun setDarkThemeMode(mode: DarkThemeMode) {
    _darkThemeMode.value = mode
    preferences?.edit()?.apply {
      putString("dark_theme_mode", mode.name)
      apply()
    }
  }

  // Students
  private val _students = MutableStateFlow<List<Student>>(emptyList())
  val students = _students.asStateFlow()

  fun addStudent(student: Student) {
    _students.update { it + student }
    CoroutineScope(Dispatchers.IO).launch {
      studentBatchRepo?.insertStudent(student)
    }
    addNotification(
      NotificationItem(
        id = "NOT_${System.currentTimeMillis()}",
        title = "New Student Admitted",
        message = "${student.name} (${student.id}) admitted to batch ${student.batch}.",
        time = getCurrentTimeStr(),
        type = "Announcement",
        recipientRole = "All"
      )
    )
  }

  fun updateStudentStatus(studentId: String, newStatus: String) {
    _students.update { list ->
      list.map { if (it.id == studentId) it.copy(status = newStatus) else it }
    }
    val updatedStudent = _students.value.find { it.id == studentId }
    if (updatedStudent != null) {
      CoroutineScope(Dispatchers.IO).launch {
        studentBatchRepo?.updateStudent(updatedStudent)
      }
    }
  }

  // Teachers
  private val _teachers = MutableStateFlow<List<Teacher>>(SeedData.defaultTeachers)
  val teachers = _teachers.asStateFlow()

  fun addTeacher(t: Teacher) {
    _teachers.update { it + t }
  }

  // Batches
  private val _batches = MutableStateFlow<List<Batch>>(emptyList())
  val batches = _batches.asStateFlow()

  fun addBatch(b: Batch) {
    _batches.update { it + b }
    CoroutineScope(Dispatchers.IO).launch {
      studentBatchRepo?.insertBatch(b)
    }
  }

  fun enrollStudentInBatch(student: Student, batch: Batch) {
    CoroutineScope(Dispatchers.IO).launch {
      studentBatchRepo?.enrollStudentInBatch(student, batch)
    }
  }

  // Subjects
  private val _subjects = MutableStateFlow<List<Subject>>(SeedData.defaultSubjects)
  val subjects = _subjects.asStateFlow()

  fun addSubject(s: Subject) {
    _subjects.update { it + s }
  }

  fun markChapterCompleted(subjectId: String, chapter: String) {
    _subjects.update { list ->
      list.map { sub ->
        if (sub.id == subjectId && sub.pendingChapters.contains(chapter)) {
          val newPending = sub.pendingChapters - chapter
          val newCompleted = sub.completedChapters + chapter
          val total = newPending.size + newCompleted.size
          val newPct = if (total > 0) (newCompleted.size * 100) / total else 100
          sub.copy(completionPercent = newPct, completedChapters = newCompleted, pendingChapters = newPending)
        } else sub
      }
    }
  }

  private val moshi: Moshi by lazy {
    Moshi.Builder()
      .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
      .build()
  }

  private val attendanceListType by lazy {
    Types.newParameterizedType(List::class.java, AttendanceRecord::class.java)
  }

  private val attendanceAdapter by lazy {
    moshi.adapter<List<AttendanceRecord>>(attendanceListType)
  }

  private fun getDefaultAttendance(): List<AttendanceRecord> {
    return emptyList()
  }

  private fun loadAttendanceFromPrefs() {
    val jsonStr = preferences?.getString("saved_attendance_records", null)
    if (!jsonStr.isNullOrEmpty()) {
      try {
        val records = attendanceAdapter.fromJson(jsonStr)
        if (!records.isNullOrEmpty()) {
          _attendance.value = records
          CoroutineScope(Dispatchers.IO).launch {
            studentBatchRepo?.insertAttendanceList(records)
          }
          return
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  private fun saveAttendanceToPrefs() {
    try {
      val jsonStr = attendanceAdapter.toJson(_attendance.value)
      preferences?.edit()?.putString("saved_attendance_records", jsonStr)?.apply()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // Attendance Records & Policy (Rules 1-5)
  // Rule 2: It can be edited ONLY within 22 hours
  const val ATTENDANCE_EDIT_WINDOW_HOURS = 22
  const val ATTENDANCE_EDIT_WINDOW_MS = 22 * 60 * 60 * 1000L

  private val _attendance = MutableStateFlow<List<AttendanceRecord>>(emptyList())
  val attendance = _attendance.asStateFlow()

  fun isAttendanceEditable(record: AttendanceRecord): Boolean {
    val elapsed = System.currentTimeMillis() - record.timestamp
    return elapsed >= 0 && elapsed <= ATTENDANCE_EDIT_WINDOW_MS
  }

  fun getAttendanceRemainingEditTime(record: AttendanceRecord): Pair<Long, Long> {
    val elapsed = System.currentTimeMillis() - record.timestamp
    val remainingMs = ATTENDANCE_EDIT_WINDOW_MS - elapsed
    if (remainingMs <= 0) return 0L to 0L
    val hours = remainingMs / (60 * 60 * 1000L)
    val minutes = (remainingMs % (60 * 60 * 1000L)) / (60 * 1000L)
    return hours to minutes
  }

  fun getAttendanceForDate(dateStr: String): List<AttendanceRecord> {
    return _attendance.value.filter { it.date == dateStr }
  }

  fun getAttendanceForDateComponents(day: Int, month: Int, year: Int): List<AttendanceRecord> {
    return _attendance.value.filter { record ->
      if (record.day > 0 && record.month > 0 && record.year > 0) {
        record.day == day && record.month == month && record.year == year
      } else {
        val parsed = com.example.data.db.AttendanceRecordEntity.parseDateAndTimeString(record.date, record.timestamp)
        parsed.day == day && parsed.month == month && parsed.year == year
      }
    }
  }

  fun updateAttendanceRecord(recordId: String, newStatus: String, reason: String? = null, recordedBy: String? = null): Boolean {
    var success = false
    var updatedItem: AttendanceRecord? = null
    val finalRecorder = recordedBy ?: _currentUserIdentifier.value.ifBlank { "system_fsi" }
    val now = System.currentTimeMillis()

    _attendance.update { current ->
      current.map { record ->
        if (record.id == recordId) {
          if (isAttendanceEditable(record)) {
            success = true
            val updated = record.copy(
              status = newStatus, 
              lastModifiedTimestamp = now, 
              reason = reason,
              recordedBy = finalRecorder
            )
            updatedItem = updated
            updated
          } else {
            record
          }
        } else {
          record
        }
      }
    }

    if (success && updatedItem != null) {
      saveAttendanceToPrefs()
      CoroutineScope(Dispatchers.IO).launch {
        studentBatchRepo?.updateAttendance(updatedItem!!)
      }
    }
    return success
  }

  fun saveSingleAttendance(
    studentId: String,
    studentName: String,
    batchName: String,
    date: String,
    status: String,
    reason: String? = null,
    recordedBy: String? = null
  ): Boolean {
    val finalRecorder = recordedBy ?: _currentUserIdentifier.value.ifBlank { "system_fsi" }
    var allowed = true
    val now = System.currentTimeMillis()
    val parsed = com.example.data.db.AttendanceRecordEntity.parseDateAndTimeString(date, now)
    var savedRecord: AttendanceRecord? = null

    _attendance.update { current ->
      val existing = current.find { it.studentId == studentId && it.date == date && it.batchName == batchName }
      if (existing != null) {
        if (isAttendanceEditable(existing)) {
          current.map { record ->
            if (record.id == existing.id) {
              val updated = record.copy(
                status = status, 
                lastModifiedTimestamp = now, 
                reason = reason,
                recordedBy = finalRecorder
              )
              savedRecord = updated
              updated
            } else record
          }
        } else {
          allowed = false
          current
        }
      } else {
        // Create new record with exact date, day, month, year, time breakdown
        val newRecord = AttendanceRecord(
          id = "ATT_${now}_$studentId",
          date = date,
          day = parsed.day,
          month = parsed.month,
          monthName = parsed.monthName,
          year = parsed.year,
          time = parsed.time,
          studentId = studentId,
          studentName = studentName,
          batchName = batchName,
          status = status,
          timestamp = now,
          lastModifiedTimestamp = now,
          reason = reason,
          recordedBy = finalRecorder
        )
        savedRecord = newRecord
        current + newRecord
      }
    }

    if (allowed && savedRecord != null) {
      saveAttendanceToPrefs()
      CoroutineScope(Dispatchers.IO).launch {
        studentBatchRepo?.insertAttendance(savedRecord!!)
      }
    }

    // Trigger parent notification if student is marked Absent
    if (allowed && status == "Absent") {
      val stu = _students.value.find { it.id == studentId }
      if (stu != null) {
        val reasonSfx = if (!reason.isNullOrBlank()) " due to $reason" else ""
        addNotification(
          NotificationItem(
            id = "NOT_ABS_${System.currentTimeMillis()}",
            title = "⚠️ Automated Absence Alert",
            message = "Dear Parent (${stu.parentName}), ${stu.name} was marked ABSENT today ($date) in ${batchName}${reasonSfx}. Please contact office.",
            time = getCurrentTimeStr(),
            type = "Absence",
            recipientRole = "Parent"
          )
        )
      }
    }

    return allowed
  }

  fun deleteAttendanceRecord(recordId: String) {
    _attendance.update { list -> list.filter { it.id != recordId } }
    saveAttendanceToPrefs()
    CoroutineScope(Dispatchers.IO).launch {
      studentBatchRepo?.deleteAttendance(recordId)
    }
  }

  // Rule 1: Non-permanent rolling lifecycle retention cleanup
  fun purgeExpiredAttendance(cutoffTimestamp: Long) {
    _attendance.update { list -> list.filter { it.timestamp >= cutoffTimestamp } }
    saveAttendanceToPrefs()
    CoroutineScope(Dispatchers.IO).launch {
      studentBatchRepo?.purgeExpiredAttendance(cutoffTimestamp)
    }
  }

  fun submitBatchAttendance(batchName: String, statusMap: Map<String, String>, recordedBy: String? = null) {
    val date = getCurrentDateStr()
    statusMap.forEach { (stuId, st) ->
      val stuName = _students.value.find { it.id == stuId }?.name ?: "Student"
      saveSingleAttendance(stuId, stuName, batchName, date, st, recordedBy = recordedBy)
    }
  }

  // Tests
  private val _tests = MutableStateFlow<List<TestRecord>>(SeedData.getDefaultTestRecords())
  val tests = _tests.asStateFlow()

  fun createTest(t: TestRecord) {
    _tests.update { listOf(t) + it }
    addNotification(
      NotificationItem(
        id = "NOT_TST_${System.currentTimeMillis()}",
        title = "📅 Upcoming Test Announced",
        message = "${t.testName} (${t.subject}) scheduled for ${t.date} for batch ${t.batch}.",
        time = getCurrentTimeStr(),
        type = "Test",
        recipientRole = "Student"
      )
    )
  }

  fun saveTestMarks(testId: String, marksMap: Map<String, Int>) {
    _tests.update { list ->
      list.map { test ->
        if (test.id == testId) test.copy(studentMarks = marksMap) else test
      }
    }
    // Check low performance triggers parent notification
    marksMap.forEach { (stuId, score) ->
      if (score < 120) { // arbitrary threshold
        val stu = _students.value.find { it.id == stuId }
        if (stu != null) {
          addNotification(
            NotificationItem(
              id = "NOT_LOW_${System.currentTimeMillis()}",
              title = "📉 Academic Performance Alert",
              message = "${stu.name} scored $score marks in recent test. Teacher review recommended.",
              time = getCurrentTimeStr(),
              type = "Test",
              recipientRole = "Parent"
            )
          )
        }
      }
    }
  }

  // Fees & Revenue
  private val _fees = MutableStateFlow<List<FeeRecord>>(SeedData.getDefaultFeeRecords())
  val fees = _fees.asStateFlow()

  fun recordFeePayment(recordId: String, payAmt: Int) {
    _fees.update { list ->
      list.map { rec ->
        if (rec.id == recordId) {
          val newPaid = rec.paidAmount + payAmt
          val newPending = (rec.feeAmount - newPaid).coerceAtLeast(0)
          val newStatus = if (newPending == 0) "Paid" else {
            try {
              val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
              val dueDateObj = sdf.parse(rec.dueDate)
              if (dueDateObj != null && dueDateObj.before(Date())) {
                "Overdue"
              } else {
                "Pending"
              }
            } catch (e: Exception) {
              "Pending"
            }
          }
          rec.copy(paidAmount = newPaid, pendingAmount = newPending, paymentStatus = newStatus)
        } else rec
      }
    }
  }

  fun addFeeRecord(
    studentName: String,
    feeAmount: Int,
    dueDate: String,
    paidAmount: Int,
    month: String
  ) {
    val pendingAmount = (feeAmount - paidAmount).coerceAtLeast(0)
    val paymentStatus = if (pendingAmount == 0) "Paid" else {
      try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dueDateObj = sdf.parse(dueDate)
        if (dueDateObj != null && dueDateObj.before(Date())) {
          "Overdue"
        } else {
          "Pending"
        }
      } catch (e: Exception) {
        "Pending"
      }
    }
    
    _fees.update { list ->
      val newRec = FeeRecord(
        id = "F_${System.currentTimeMillis()}",
        studentName = studentName,
        feeAmount = feeAmount,
        dueDate = dueDate,
        paidAmount = paidAmount,
        pendingAmount = pendingAmount,
        paymentStatus = paymentStatus,
        month = month,
        remindedCount = 0,
        lastReminded = ""
      )
      list + newRec
    }
  }

  fun sendFeeReminder(recordId: String) {
    val currentTimeStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date())
    var studentName = ""
    var month = ""
    var pendingAmt = 0
    var dueDate = ""
    
    _fees.update { list ->
      list.map { rec ->
        if (rec.id == recordId) {
          studentName = rec.studentName
          month = rec.month
          pendingAmt = rec.pendingAmount
          dueDate = rec.dueDate
          rec.copy(
            remindedCount = rec.remindedCount + 1,
            lastReminded = currentTimeStr
          )
        } else rec
      }
    }

    if (studentName.isNotEmpty()) {
      addNotification(
        NotificationItem(
          id = "N_FEE_${System.currentTimeMillis()}",
          title = "💳 Fee Payment Reminder",
          message = "Reminder for $studentName: The monthly fee of ₹$pendingAmt for $month is due on $dueDate. Please pay at the earliest.",
          time = "Just now",
          type = "Fee",
          recipientRole = "Parent"
        )
      )
    }
  }

  fun deleteFeeRecord(recordId: String) {
    _fees.update { current ->
      current.filter { it.id != recordId }
    }
  }

  // Admission CRM Leads
  private val _leads = MutableStateFlow<List<LeadRecord>>(emptyList())
  val leads = _leads.asStateFlow()

  fun addLead(l: LeadRecord) {
    _leads.update { listOf(l) + it }
  }

  fun updateLeadStatus(leadId: String, st: String) {
    _leads.update { list ->
      list.map { if (it.id == leadId) it.copy(status = st) else it }
    }
  }

  // Online Admission Form Submissions
  private val _onlineForms = MutableStateFlow<List<OnlineFormSubmission>>(emptyList())
  val onlineForms = _onlineForms.asStateFlow()

  fun processOnlineForm(formId: String, approve: Boolean) {
    _onlineForms.update { list ->
      list.map { if (it.id == formId) it.copy(status = if (approve) "Approved" else "Rejected") else it }
    }
    if (approve) {
      val f = _onlineForms.value.find { it.id == formId }
      if (f != null) {
        addLead(
          LeadRecord(
            id = "LD_${System.currentTimeMillis()}",
            name = f.name,
            mobile = f.mobile,
            className = f.className,
            stream = f.stream,
            source = "Online Form Portal",
            inquiryDate = getCurrentDateStr(),
            assignedCounselor = "Auto Assign",
            status = "New"
          )
        )
      }
    }
  }

  // Question Bank
  private val _questions = MutableStateFlow<List<QuestionItem>>(emptyList())
  val questions = _questions.asStateFlow()

  fun addQuestion(q: QuestionItem) {
    _questions.update { listOf(q) + it }
  }

  // Doubts
  private val _doubts = MutableStateFlow<List<DoubtItem>>(emptyList())
  val doubts = _doubts.asStateFlow()

  fun submitStudentDoubt(subject: String, chapter: String, text: String, photoUrl: String? = null) {
    val studentName = _students.value.firstOrNull()?.name ?: _currentUserIdentifier.value.ifBlank { "Student" }
    _doubts.update {
      listOf(
        DoubtItem("DB_${System.currentTimeMillis()}", studentName, subject, chapter, text, getCurrentDateStr(), null, "Pending", null, photoUrl)
      ) + it
    }
  }

  fun replyToDoubt(doubtId: String, replyText: String) {
    _doubts.update { list ->
      list.map { if (it.id == doubtId) it.copy(reply = replyText, status = "Resolved", resolutionTime = "12 Mins") else it }
    }
  }

  // Homework
  private val _homework = MutableStateFlow<List<HomeworkItem>>(emptyList())
  val homework = _homework.asStateFlow()

  // Timetable
  private val _timetable = MutableStateFlow<List<TimetableItem>>(emptyList())
  val timetable = _timetable.asStateFlow()

  // Dynamic Timetable with 22-hour duration
  private val _dynamicTimetable = MutableStateFlow<List<DynamicTimetableEntry>>(emptyList())
  val dynamicTimetable = _dynamicTimetable.asStateFlow()

  fun addDynamicTimetableEntry(
    subject: String,
    batch: String,
    startTime: String,
    endTime: String,
    room: String,
    createdAt: Long = System.currentTimeMillis(),
    teacherName: String = "Faculty"
  ) {
    _dynamicTimetable.update { current ->
      val newEntry = DynamicTimetableEntry(
        id = "DTT_${System.currentTimeMillis()}",
        subject = subject,
        batch = batch,
        startTime = startTime,
        endTime = endTime,
        room = room,
        createdAt = createdAt,
        teacherName = teacherName
      )
      listOf(newEntry) + current
    }
  }

  fun deleteDynamicTimetableEntry(id: String) {
    _dynamicTimetable.update { current ->
      current.filter { it.id != id }
    }
  }

  // Study Logs
  private val _studyLogs = MutableStateFlow<List<StudyLogItem>>(emptyList())
  val studyLogs = _studyLogs.asStateFlow()

  fun logDailyStudy(hours: Float, subjects: String, chapters: String) {
    _studyLogs.update {
      listOf(StudyLogItem("SL_${System.currentTimeMillis()}", "Today", hours, subjects, chapters)) + it
    }
  }

  // Student Goals
  private val _studentGoal = MutableStateFlow(
    GoalItem(targetMarks = 0, targetPercent = 0, targetRank = 0, targetAttendance = 0, weeklyStudyHours = 0)
  )
  val studentGoal = _studentGoal.asStateFlow()

  fun updateStudentGoal(g: GoalItem) {
    _studentGoal.value = g
  }

  // Badges
  private val _badges = MutableStateFlow<List<BadgeItem>>(emptyList())
  val badges = _badges.asStateFlow()

  // Notifications
  private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
  val notifications = _notifications.asStateFlow()

  fun addNotification(n: NotificationItem) {
    _notifications.update { listOf(n) + it }
    appContext?.let { ctx ->
      com.example.utils.NotificationHelper.sendPushNotification(ctx, n.title, n.message)
    }
  }

  fun markAllNotificationsAsRead() {
    _notifications.update { list -> list.map { it.copy(isRead = true) } }
  }

  fun markNotificationAsRead(id: String) {
    _notifications.update { list -> list.map { if (it.id == id) it.copy(isRead = true) else it } }
  }

  fun deleteNotification(id: String) {
    _notifications.update { list -> list.filter { it.id != id } }
  }

  fun deleteAllNotifications() {
    _notifications.value = emptyList()
  }

  // Parent Meetings
  private val _meetings = MutableStateFlow<List<ParentMeetingItem>>(emptyList())
  val meetings = _meetings.asStateFlow()

  fun scheduleParentMeeting(teacherName: String, date: String, time: String, notes: String) {
    val student = _students.value.firstOrNull()
    val studentName = student?.name ?: "Student"
    val parentName = student?.parentName?.ifBlank { "Parent" } ?: "Parent"
    _meetings.update {
      listOf(
        ParentMeetingItem("PM_${System.currentTimeMillis()}", studentName, parentName, teacherName, date, time, notes, "To be reviewed at meeting", "Scheduled")
      ) + it
    }
  }

  fun getCurrentDateStr(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
  }

  fun getCurrentTimeStr(): String {
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
  }

  fun login(identifier: String, role: UserRole): Boolean {
    if (role == UserRole.TEACHER) {
      val t = teachers.value.find { it.email.equals(identifier, true) || it.contact.equals(identifier, true) || it.name.equals(identifier, true) }
      if (t?.status == "Pending Approval") {
        return false
      }
    }
    _currentUserIdentifier.value = identifier
    _currentRole.value = role
    _isLoggedIn.value = true
    saveSession(true, identifier, role)

    val currentList = _savedAccounts.value.toMutableList()
    val existingIdx = currentList.indexOfFirst { it.identifier.equals(identifier, true) }
    val displayName = identifier.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val newAcc = SavedAccount(identifier, role, displayName)
    if (existingIdx >= 0) {
      currentList[existingIdx] = newAcc
    } else {
      currentList.add(newAcc)
    }
    _savedAccounts.value = currentList
    saveSavedAccountsToPrefs(currentList)
    return true
  }

  fun switchAccount(account: SavedAccount) {
    _currentUserIdentifier.value = account.identifier
    _currentRole.value = account.role
    _isLoggedIn.value = true
    saveSession(true, account.identifier, account.role)
  }

  fun removeAccount(identifier: String) {
    val updated = _savedAccounts.value.filter { !it.identifier.equals(identifier, true) }
    _savedAccounts.value = updated
    saveSavedAccountsToPrefs(updated)
    if (_currentUserIdentifier.value.equals(identifier, true)) {
      if (updated.isNotEmpty()) {
        switchAccount(updated.first())
      } else {
        logout()
      }
    }
  }

  fun logout() {
    FirebaseAuthService.signOut()
    val currentId = _currentUserIdentifier.value
    val updated = _savedAccounts.value.filter { !it.identifier.equals(currentId, true) }
    _savedAccounts.value = updated
    saveSavedAccountsToPrefs(updated)

    if (updated.isNotEmpty()) {
      switchAccount(updated.first())
    } else {
      _isLoggedIn.value = false
      _currentUserIdentifier.value = ""
      saveSession(false, "", _currentRole.value)
    }
  }

  fun logoutAllAccounts() {
    FirebaseAuthService.signOut()
    _savedAccounts.value = emptyList()
    saveSavedAccountsToPrefs(emptyList())
    _isLoggedIn.value = false
    _currentUserIdentifier.value = ""
    saveSession(false, "", UserRole.STUDENT)
  }

  fun registerStudent(name: String, email: String) {
    val existing = _students.value.find { it.email.equals(email, true) }
    if (existing == null) {
      val newStudent = Student(
        id = "STU${System.currentTimeMillis().toString().takeLast(4)}",
        name = if (name.isBlank()) "Student" else name,
        photo = "",
        mobile = "",
        parentName = "",
        parentContact = "",
        email = email,
        dob = "",
        gender = "",
        address = "",
        school = "",
        className = "Class 12",
        batch = "Batch Alpha",
        stream = "General",
        admissionDate = getCurrentDateStr(),
        status = "Active",
        attendancePercent = 100,
        overallAvg = 0,
        rank = 0,
        strongestSubject = "General",
        weakestSubject = "General",
        recentScores = emptyList()
      )
      _students.update { it + newStudent }
      com.example.security.SecurityEngine.registerSaltedCredential(
        identifier = email,
        rawPassword = "Student@123",
        role = UserRole.STUDENT,
        fullName = name
      )
      com.example.security.SecurityEngine.registerSaltedCredential(
        identifier = newStudent.id,
        rawPassword = "Student@123",
        role = UserRole.STUDENT,
        fullName = name
      )
    }
  }

  fun registerTeacher(name: String, email: String, mobile: String, subject: String) {
    val newT = Teacher(
      id = "FAC_${System.currentTimeMillis()}",
      name = name,
      subject = subject,
      contact = mobile,
      qualification = "Self Registered",
      experience = "New Faculty",
      assignedBatches = listOf("Foundation Target"),
      classesTaken = 0,
      attendancePercent = 100,
      feedbackRating = 5.0f,
      salary = 45000,
      incentives = 0,
      deductions = 0,
      status = "Pending Approval",
      email = email
    )
    _teachers.update { it + newT }
    com.example.security.SecurityEngine.registerSaltedCredential(
      identifier = email,
      rawPassword = "Teacher@123",
      role = UserRole.TEACHER,
      fullName = name
    )
  }

  fun approveTeacher(teacherId: String) {
    _teachers.update { list ->
      list.map { if (it.id == teacherId) it.copy(status = "Active") else it }
    }
  }

  fun resetUserPassword(identifier: String) {
    _passwordResetNotice.value = "Password successfully reset for $identifier. Temporary credentials dispatched via Admin secure channel."
  }

  fun clearPasswordNotice() {
    _passwordResetNotice.value = null
  }

  fun clearAllAppData(context: android.content.Context) {
    preferences?.edit()?.apply {
      putBoolean("is_data_cleared", true)
      remove("saved_attendance_records")
      apply()
    }

    _students.value = emptyList()
    _teachers.value = emptyList()
    _batches.value = emptyList()
    _subjects.value = emptyList()
    _tests.value = emptyList()
    _fees.value = emptyList()
    _attendance.value = emptyList()
    _leads.value = emptyList()
    _onlineForms.value = emptyList()
    _questions.value = emptyList()
    _doubts.value = emptyList()
    _homework.value = emptyList()
    _timetable.value = emptyList()
    _dynamicTimetable.value = emptyList()
    _studyLogs.value = emptyList()
    _notifications.value = emptyList()
    _meetings.value = emptyList()

    CoroutineScope(Dispatchers.IO).launch {
      studentBatchRepo?.clearAllData()
    }

    addNotification(
      NotificationItem(
        id = "NOT_CLR_${System.currentTimeMillis()}",
        title = "🧹 All System Data Purged",
        message = "All sample & legacy database records cleared. Platform is clean and ready for 1,000 real users.",
        time = getCurrentTimeStr(),
        type = "System",
        recipientRole = "All"
      )
    )
  }

  fun initializeCleanInstituteStructure() {
    val cleanBatches = listOf(
      Batch("BATCH_10_B", "Class 10 Board Target", "Mon - Sat (08:00 AM - 12:00 PM)", 0, "Faculty Team"),
      Batch("BATCH_11_S", "11th Science Stream", "Mon - Sat (01:00 PM - 05:00 PM)", 0, "Science Faculty"),
      Batch("BATCH_12_S", "12th Science Stream", "Mon - Sat (08:00 AM - 01:30 PM)", 0, "Senior Faculty"),
      Batch("BATCH_JEE", "JEE Competitive Target", "Mon - Sat (07:00 AM - 02:00 PM)", 0, "IITian Mentors"),
      Batch("BATCH_NEET", "NEET Medical Target", "Mon - Sat (08:00 AM - 02:00 PM)", 0, "Medical Faculty")
    )
    _batches.value = cleanBatches
    _students.value = emptyList()
    _teachers.value = emptyList()
    _fees.value = emptyList()

    CoroutineScope(Dispatchers.IO).launch {
      studentBatchRepo?.clearAllData()
      cleanBatches.forEach { studentBatchRepo?.insertBatch(it) }
    }
  }

  fun generateAndBulkImportRealUsers(count: Int = 1000) {
    val batchesList = listOf(
      Batch("BATCH_10_B", "Class 10 Board Target", "Mon - Sat (08:00 AM - 12:00 PM)", count / 4, "Faculty Team"),
      Batch("BATCH_11_S", "11th Science Stream", "Mon - Sat (01:00 PM - 05:00 PM)", count / 4, "Science Faculty"),
      Batch("BATCH_12_S", "12th Science Stream", "Mon - Sat (08:00 AM - 01:30 PM)", count / 4, "Senior Faculty"),
      Batch("BATCH_JEE_APEX", "JEE Apex Competitive", "Mon - Sat (07:00 AM - 02:00 PM)", count / 4, "IITian Mentors")
    )

    _batches.value = batchesList
    CoroutineScope(Dispatchers.IO).launch {
      batchesList.forEach { studentBatchRepo?.insertBatch(it) }
    }

    val firstNames = listOf(
      "Aarav", "Ananya", "Rohan", "Sanya", "Aditya", "Ishita", "Kabir", "Meera", "Vivaan", "Diya",
      "Priya", "Arjun", "Kavya", "Rahul", "Tanvi", "Neha", "Dev", "Anushka", "Yash", "Sneha",
      "Shreyas", "Pooja", "Vikram", "Riya", "Karan", "Apurva", "Pranav", "Shruti", "Akash", "Swati",
      "Siddharth", "Bhavna", "Gaurav", "Nisha", "Manish", "Preeti", "Alok", "Shweta", "Mayank", "Monal"
    )

    val lastNames = listOf(
      "Sharma", "Patel", "Deshmukh", "Joshi", "Kulkarni", "Verma", "Gupta", "Singh", "Rao", "Nair",
      "Chavan", "Mehta", "Iyer", "Shinde", "Thakur", "Reddy", "Chopra", "Bhasin", "Garg", "Bhat"
    )

    val generatedStudents = (1..count).map { index ->
      val fName = firstNames[index % firstNames.size]
      val lName = lastNames[(index * 3) % lastNames.size]
      val name = "$fName $lName"
      val stuId = "STU" + String.format("%05d", index)
      val batchObj = batchesList[index % batchesList.size]

      val mobile = "+91 98" + String.format("%08d", 10000000 + index)
      val parentMobile = "+91 97" + String.format("%08d", 10000000 + index)
      val email = "${fName.lowercase()}.${lName.lowercase()}$index@mytuition.com"

      val fee = 45000
      val paid = if (index % 2 == 0) 45000 else 25000
      val pending = (fee - paid).coerceAtLeast(0)

      Student(
        id = stuId,
        name = name,
        photo = "",
        mobile = mobile,
        parentName = "Mr. $lName (Parent)",
        parentContact = parentMobile,
        email = email,
        dob = "12/08/2007",
        gender = if (index % 2 == 0) "Male" else "Female",
        address = "Campus Hub Sector ${index % 50 + 1}",
        school = "FSI Educational Academy",
        className = batchObj.name.substringBefore(" "),
        batch = batchObj.name,
        stream = if (batchObj.name.contains("Science") || batchObj.name.contains("JEE")) "Science" else "General",
        admissionDate = getCurrentDateStr(),
        status = "Active",
        attendancePercent = 85 + (index % 15),
        overallAvg = 70 + (index % 28),
        rank = index,
        strongestSubject = if (index % 2 == 0) "Physics" else "Mathematics",
        weakestSubject = if (index % 2 == 0) "Chemistry" else "Physics",
        recentScores = listOf("Physics" to 82, "Chemistry" to 78, "Mathematics" to 88)
      )
    }

    _students.value = generatedStudents

    val newFees = generatedStudents.mapIndexed { idx, stu ->
      val feeAmt = 45000
      val paidAmt = if (idx % 2 == 0) 45000 else 25000
      val pend = feeAmt - paidAmt
      FeeRecord(
        id = "F_REAL_${idx + 1}",
        studentName = stu.name,
        feeAmount = feeAmt,
        dueDate = "10/08/2026",
        paidAmount = paidAmt,
        pendingAmount = pend,
        paymentStatus = if (pend == 0) "Paid" else "Pending",
        month = "July 2026",
        remindedCount = 0,
        lastReminded = ""
      )
    }
    _fees.value = newFees

    CoroutineScope(Dispatchers.IO).launch {
      studentBatchRepo?.bulkInsertStudents(generatedStudents)
    }

    addNotification(
      NotificationItem(
        id = "NOT_BULK_${System.currentTimeMillis()}",
        title = "⚡ 1,000 Real Student Profiles Ready",
        message = "Bulk onboarding completed. $count active student profiles and fee ledgers generated and indexed in database.",
        time = getCurrentTimeStr(),
        type = "System",
        recipientRole = "All"
      )
    )
  }

  fun bulkImportStudentsFromCsv(rawCsv: String) {
    val lines = rawCsv.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    val newStudents = mutableListOf<Student>()
    lines.forEachIndexed { idx, line ->
      val parts = line.split(",")
      if (parts.size >= 2) {
        val name = parts[0].trim()
        val email = parts[1].trim()
        val batch = if (parts.size > 2) parts[2].trim() else "General Batch"
        val mobile = if (parts.size > 3) parts[3].trim() else "+91 98000 00000"
        val parent = if (parts.size > 4) parts[4].trim() else "Parent"

        val s = Student(
          id = "STU_CSV_${System.currentTimeMillis()}_$idx",
          name = name,
          photo = "",
          mobile = mobile,
          parentName = parent,
          parentContact = mobile,
          email = email,
          dob = "",
          gender = "Other",
          address = "City Campus",
          school = "City High",
          className = "Class 12",
          batch = batch,
          stream = "General",
          admissionDate = getCurrentDateStr(),
          status = "Active",
          attendancePercent = 100,
          overallAvg = 0,
          rank = idx + 1,
          strongestSubject = "General",
          weakestSubject = "General",
          recentScores = emptyList()
        )
        newStudents.add(s)
      }
    }

    if (newStudents.isNotEmpty()) {
      _students.update { it + newStudents }
      CoroutineScope(Dispatchers.IO).launch {
        studentBatchRepo?.bulkInsertStudents(newStudents)
      }
      addNotification(
        NotificationItem(
          id = "NOT_CSV_${System.currentTimeMillis()}",
          title = "CSV Import Complete",
          message = "Successfully imported ${newStudents.size} students into roster.",
          time = getCurrentTimeStr(),
          type = "System",
          recipientRole = "All"
        )
      )
    }
  }
}
