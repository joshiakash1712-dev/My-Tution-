package com.example.data

enum class UserRole(val displayName: String, val subtitle: String) {
  ADMIN("Administrator", "Institute Director & System Admin"),
  TEACHER("Teacher / Faculty", "Teaching Staff & Class Manager"),
  STUDENT("Student", "Learner Portal"),
  PARENT("Parent / Guardian", "Parent Monitoring & Fee Portal")
}

enum class DarkThemeMode(val displayName: String) {
  LIGHT("Light Mode"),
  DARK("Dark Mode"),
  SYSTEM("System Default")
}

data class SavedAccount(
  val identifier: String,
  val role: UserRole,
  val displayName: String
)

data class Student(
  val id: String,
  val name: String,
  val photo: String = "",
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
  val status: String = "Active",
  val attendancePercent: Int = 85,
  val overallAvg: Int = 80,
  val rank: Int = 1,
  val strongestSubject: String = "Physics",
  val weakestSubject: String = "Mathematics",
  val recentScores: List<Pair<String, Int>> = emptyList()
)

data class Teacher(
  val id: String,
  val name: String,
  val subject: String,
  val contact: String,
  val qualification: String,
  val experience: String,
  val assignedBatches: List<String>,
  val classesTaken: Int,
  val attendancePercent: Int,
  val feedbackRating: Float,
  val salary: Int,
  val incentives: Int,
  val deductions: Int,
  val status: String,
  val email: String = "faculty@mytuition.com"
)

data class Batch(
  val id: String,
  val name: String,
  val schedule: String,
  val studentCount: Int,
  val teacherName: String
)

data class Subject(
  val id: String,
  val name: String,
  val completionPercent: Int,
  val completedChapters: List<String>,
  val pendingChapters: List<String>
)

data class AttendanceRecord(
  val id: String,
  val date: String,
  val studentId: String,
  val studentName: String,
  val batchName: String,
  val status: String, // "Present" | "Absent" | "Late"
  val timestamp: Long = System.currentTimeMillis(),
  val lastModifiedTimestamp: Long = System.currentTimeMillis(),
  val day: Int = 0,
  val month: Int = 0,
  val monthName: String = "",
  val year: Int = 0,
  val time: String = "",
  val reason: String? = null,
  val recordedBy: String? = null
)

data class TestRecord(
  val id: String,
  val testName: String,
  val subject: String,
  val date: String,
  val batch: String,
  val totalMarks: Int,
  val studentMarks: Map<String, Int>, // studentId -> score
  val remarks: String,
  val aiAnalysisStrong: List<String>,
  val aiAnalysisWeak: List<String>,
  val aiSuggestion: String
)

data class FeeRecord(
  val id: String,
  val studentName: String,
  val feeAmount: Int,
  val dueDate: String,
  val paidAmount: Int,
  val pendingAmount: Int,
  val paymentStatus: String, // "Paid" | "Pending" | "Overdue"
  val month: String = "November 2025",
  val remindedCount: Int = 0,
  val lastReminded: String = ""
)

data class LeadRecord(
  val id: String,
  val name: String,
  val mobile: String,
  val className: String,
  val stream: String,
  val source: String,
  val inquiryDate: String,
  val assignedCounselor: String,
  val status: String // "New" | "Contacted" | "Follow-Up" | "Demo Scheduled" | "Admitted" | "Rejected"
)

data class OnlineFormSubmission(
  val id: String,
  val name: String,
  val mobile: String,
  val email: String,
  val school: String,
  val className: String,
  val stream: String,
  val preferredBatch: String,
  val submissionDate: String,
  val status: String // "Pending" | "Approved" | "Rejected"
)

data class QuestionItem(
  val id: String,
  val subject: String,
  val chapter: String,
  val topic: String,
  val difficulty: String, // "Easy" | "Medium" | "Hard"
  val type: String, // "MCQ" | "Subjective"
  val questionText: String,
  val options: List<String>,
  val correctOption: Int,
  val solution: String,
  val tags: List<String>
)

data class DoubtItem(
  val id: String,
  val studentName: String,
  val subject: String,
  val chapter: String,
  val questionText: String,
  val date: String,
  val reply: String?,
  val status: String, // "Pending" | "Resolved"
  val resolutionTime: String?,
  val photoUrl: String? = null
)

data class HomeworkItem(
  val id: String,
  val title: String,
  val subject: String,
  val dueDate: String,
  val description: String,
  val status: String // "Pending" | "Submitted" | "Evaluated"
)

data class TimetableItem(
  val id: String,
  val day: String, // "Monday" .. "Saturday"
  val startTime: String,
  val endTime: String,
  val subject: String,
  val batch: String,
  val room: String
)

data class DynamicTimetableEntry(
  val id: String,
  val subject: String,
  val batch: String,
  val startTime: String,
  val endTime: String,
  val room: String,
  val createdAt: Long, // milliseconds timestamp
  val teacherName: String = "Subject Faculty"
)

data class StudyLogItem(
  val id: String,
  val date: String,
  val hours: Float,
  val subjects: String,
  val chapters: String
)

data class GoalItem(
  val targetMarks: Int,
  val targetPercent: Int,
  val targetRank: Int,
  val targetAttendance: Int,
  val weeklyStudyHours: Int
)

data class BadgeItem(
  val id: String,
  val name: String,
  val description: String,
  val iconName: String,
  val earned: Boolean
)

data class NotificationItem(
  val id: String,
  val title: String,
  val message: String,
  val time: String,
  val type: String, // "Absence" | "Fee" | "Test" | "Announcement"
  val recipientRole: String,
  val isRead: Boolean = false
)

data class ParentMeetingItem(
  val id: String,
  val studentName: String,
  val parentName: String,
  val teacherName: String,
  val date: String,
  val time: String,
  val notes: String,
  val actionItems: String,
  val status: String // "Scheduled" | "Completed"
)
