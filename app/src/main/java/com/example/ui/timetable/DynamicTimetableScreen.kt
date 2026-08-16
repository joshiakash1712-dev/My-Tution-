package com.example.ui.timetable

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AppRepository
import com.example.data.DynamicTimetableEntry
import com.example.data.UserRole
import com.example.ui.components.GeoSectionCard
import com.example.ui.components.HeroFeatureCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun DynamicTimetableScreen() {
  val currentRole by AppRepository.currentRole.collectAsState()
  val allTimetableEntries by AppRepository.dynamicTimetable.collectAsState()

  var selectedTab by remember { mutableStateOf(0) } // 0 = Active, 1 = History
  var showAddDialog by remember { mutableStateOf(false) }

  // We periodically trigger a recomposition to keep the countdown timers fresh (every 30 seconds)
  var ticks by remember { mutableStateOf(0L) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(30000L)
      ticks++
    }
  }

  val currentTime = System.currentTimeMillis()
  val twentyTwoHoursInMillis = 22 * 60 * 60 * 1000L

  val activeEntries = remember(allTimetableEntries, ticks) {
    allTimetableEntries.filter { item ->
      val elapsed = System.currentTimeMillis() - item.createdAt
      elapsed in 0L until twentyTwoHoursInMillis
    }.sortedBy { it.createdAt }
  }

  val historyEntries = remember(allTimetableEntries, ticks) {
    allTimetableEntries.filter { item ->
      val elapsed = System.currentTimeMillis() - item.createdAt
      elapsed >= twentyTwoHoursInMillis || elapsed < 0L // future or older than 22h goes to history
    }.sortedByDescending { it.createdAt }
  }

  if (showAddDialog) {
    AddTimetableDialog(
      allEntries = allTimetableEntries,
      onDismiss = { showAddDialog = false },
      onSave = { subject, batch, startTime, endTime, room, offsetMillis ->
        val finalTime = System.currentTimeMillis() - offsetMillis
        AppRepository.addDynamicTimetableEntry(
          subject = subject,
          batch = batch,
          startTime = startTime,
          endTime = endTime,
          room = room,
          createdAt = finalTime
        )
        showAddDialog = false
      }
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("dynamic_timetable_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Header Hero Card
    item {
      HeroFeatureCard(
        title = "Dynamic Timetable Board",
        subtitle = "Admin-scheduled classes remain live for 22 hours, then archive automatically.",
        tagText = "REALTIME SYNC",
        icon = Icons.Default.Schedule
      )
    }

    // 2. Control Row: Segmented Tab Indicator & Admin Quick Action Button
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Rounded custom tab buttons
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          TabButton(
            text = "Active (${activeEntries.size})",
            isSelected = selectedTab == 0,
            onClick = { selectedTab = 0 }
          )
          TabButton(
            text = "History Archive (${historyEntries.size})",
            isSelected = selectedTab == 1,
            onClick = { selectedTab = 1 }
          )
        }

        // Add class button visible only to ADMIN
        if (currentRole == UserRole.ADMIN) {
          Button(
            onClick = { showAddDialog = true },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.testTag("admin_add_timetable_button")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Schedule class",
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Set Timetable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 3. Timetable Content List
    val displayList = if (selectedTab == 0) activeEntries else historyEntries

    if (displayList.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = if (selectedTab == 0) Icons.Default.HourglassEmpty else Icons.Default.HistoryToggleOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(48.dp)
            )
            Text(
              text = if (selectedTab == 0) "No active dynamic classes scheduled" else "No archived history classes",
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selectedTab == 0 && currentRole == UserRole.ADMIN) {
              Text(
                text = "Tap the 'Set Timetable' button above to schedule your first dynamic class.",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 24.dp)
              )
            }
          }
        }
      }
    } else {
      items(displayList, key = { it.id }) { item ->
        val elapsed = System.currentTimeMillis() - item.createdAt
        val remainingMillis = twentyTwoHoursInMillis - elapsed
        
        DynamicTimetableCard(
          entry = item,
          isHistory = selectedTab == 1,
          remainingMillis = remainingMillis,
          onDelete = if (currentRole == UserRole.ADMIN) { { AppRepository.deleteDynamicTimetableEntry(item.id) } } else null
        )
      }
    }
  }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun DynamicTimetableCard(
  entry: DynamicTimetableEntry,
  isHistory: Boolean,
  remainingMillis: Long,
  onDelete: (() -> Unit)? = null
) {
  val formattedDate = remember(entry.createdAt) {
    SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(entry.createdAt))
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = 1.dp,
        color = if (isHistory) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
      ),
    colors = CardDefaults.cardColors(
      containerColor = if (isHistory) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
    ),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Top header: Subject Title & Delete if admin
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = entry.subject,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Scheduled by: ${entry.teacherName}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        if (onDelete != null) {
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete timetable entry",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      // Details block: Batch, Room, Class Hours
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Batch Tag
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Groups,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = entry.batch,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Room Tag
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer,
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.MeetingRoom,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = entry.room,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Class Timing Info
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${entry.startTime} - ${entry.endTime}",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      // Bottom footer: Timing status & 22h limit countdown
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Posted At",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
          )
          Text(
            text = formattedDate,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Remaining time display / Status Tag
        if (isHistory) {
          Surface(
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = CircleShape
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
              )
              Text(
                text = "Archived History",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          // Calculate remaining hours and minutes
          val totalMinutes = (remainingMillis / (1000 * 60)).coerceAtLeast(0)
          val hours = totalMinutes / 60
          val minutes = totalMinutes % 60

          Surface(
            color = if (hours < 2) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = if (hours < 2) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(12.dp)
              )
              Text(
                text = "Live: ${hours}h ${minutes}m left",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (hours < 2) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimetableDialog(
  allEntries: List<DynamicTimetableEntry>,
  onDismiss: () -> Unit,
  onSave: (subject: String, batch: String, startTime: String, endTime: String, room: String, offsetMillis: Long) -> Unit
) {
  var subject by remember { mutableStateOf("") }
  var selectedBatch by remember { mutableStateOf("JEE Apex Morning") }
  var startTime by remember { mutableStateOf("10:00 AM") }
  var endTime by remember { mutableStateOf("11:30 AM") }
  var room by remember { mutableStateOf("Room 201") }

  // Development testing offset option:
  // 0 = Live now
  // 1 = 21 Hours Ago (Transitions in 1hr)
  // 2 = 23 Hours Ago (Already in History)
  var testingOffsetOption by remember { mutableStateOf(0) }

  val batchesList = listOf("JEE Apex Morning", "NEET Zenith", "Class 12 Regular", "Class 11 Foundation")
  val roomsList = listOf("Room 201", "Room 202", "Hall 1", "Hall 2", "Lab A", "Lab B")

  var showBatchDropdown by remember { mutableStateOf(false) }
  var showRoomDropdown by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Title block
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Schedule Dynamic Class",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Text(
          text = "Admins can schedule flash lectures and meetings. It stays active for exactly 22 hours.",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Fields
        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Subject / Lecture Topic") },
          placeholder = { Text("e.g. Physics • Modern Physics Revision") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        // Batch Dropdown Selection
        Box(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = selectedBatch,
            onValueChange = {},
            readOnly = true,
            label = { Text("Target Student Batch") },
            trailingIcon = {
              IconButton(onClick = { showBatchDropdown = !showBatchDropdown }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Batch")
              }
            },
            modifier = Modifier.fillMaxWidth()
          )
          DropdownMenu(
            expanded = showBatchDropdown,
            onDismissRequest = { showBatchDropdown = false },
            modifier = Modifier.fillMaxWidth(0.8f)
          ) {
            batchesList.forEach { batchName ->
              DropdownMenuItem(
                text = { Text(batchName) },
                onClick = {
                  selectedBatch = batchName
                  showBatchDropdown = false
                }
              )
            }
          }
        }

        // Room Dropdown Selection
        Box(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = room,
            onValueChange = {},
            readOnly = true,
            label = { Text("Classroom / Hall") },
            trailingIcon = {
              IconButton(onClick = { showRoomDropdown = !showRoomDropdown }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Room")
              }
            },
            modifier = Modifier.fillMaxWidth()
          )
          DropdownMenu(
            expanded = showRoomDropdown,
            onDismissRequest = { showRoomDropdown = false },
            modifier = Modifier.fillMaxWidth(0.8f)
          ) {
            roomsList.forEach { rName ->
              DropdownMenuItem(
                text = { Text(rName) },
                onClick = {
                  room = rName
                  showRoomDropdown = false
                }
              )
            }
          }
        }

        // Row of timings
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text("Start Time") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = { Text("End Time") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        // Beautiful Developer testing section to simulate different times!
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "🛠️ DEV SIMULATION SETTING",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
              text = "Choose post time offset to instantly test 22-hour auto-archive behavior:",
              fontSize = 9.sp,
              color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
              modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              DevOffsetButton(
                text = "Live Now",
                isSelected = testingOffsetOption == 0,
                onClick = { testingOffsetOption = 0 }
              )
              DevOffsetButton(
                text = "21.5h ago\n(Exp in 30m)",
                isSelected = testingOffsetOption == 1,
                onClick = { testingOffsetOption = 1 }
              )
              DevOffsetButton(
                text = "23h ago\n(History)",
                isSelected = testingOffsetOption == 2,
                onClick = { testingOffsetOption = 2 }
              )
            }
          }
        }

        // Conflict Warning Block
        val conflictWarning = remember(allEntries, selectedBatch, room, startTime, endTime) {
          checkScheduleConflict(allEntries, selectedBatch, room, startTime, endTime)
        }

        if (conflictWarning != null) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Conflict warning",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = conflictWarning,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
              )
            }
          }
        }

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              if (subject.isNotBlank()) {
                val offsetMillis = when (testingOffsetOption) {
                  0 -> 0L
                  1 -> (21.5 * 60 * 60 * 1000L).toLong()
                  2 -> (23 * 60 * 60 * 1000L).toLong()
                  else -> 0L
                }
                onSave(subject, selectedBatch, startTime, endTime, room, offsetMillis)
              }
            },
            enabled = subject.isNotBlank() && conflictWarning == null,
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Set Active")
          }
        }
      }
    }
  }
}

@Composable
fun RowScope.DevOffsetButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .weight(1f)
      .clip(RoundedCornerShape(8.dp))
      .background(
        if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface
      )
      .border(
        width = 1.dp,
        color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
        shape = RoundedCornerShape(8.dp)
      )
      .clickable { onClick() }
      .padding(vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      color = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
      fontSize = 8.5.sp,
      lineHeight = 11.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center
    )
  }
}

fun parseTimeTextToMinutes(timeStr: String): Int {
  try {
    val clean = timeStr.trim().uppercase()
    val isPm = clean.endsWith("PM")
    val isAm = clean.endsWith("AM")
    val content = clean.replace("AM", "").replace("PM", "").trim()
    val parts = content.split(":")
    if (parts.isNotEmpty()) {
      var hour = parts[0].toIntOrNull() ?: 0
      val minute = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
      if (isPm && hour < 12) hour += 12
      if (isAm && hour == 12) hour = 0
      return hour * 60 + minute
    }
  } catch (e: Exception) {
    // Return a default fallback
  }
  return -1
}

fun checkScheduleConflict(
  allEntries: List<DynamicTimetableEntry>,
  newBatch: String,
  newRoom: String,
  newStartStr: String,
  newEndStr: String
): String? {
  val startMin = parseTimeTextToMinutes(newStartStr)
  val endMin = parseTimeTextToMinutes(newEndStr)
  if (startMin == -1 || endMin == -1 || startMin >= endMin) {
    return null // invalid times, skip conflict check
  }

  for (entry in allEntries) {
    // Only check active entries (less than 22 hours old)
    val elapsed = System.currentTimeMillis() - entry.createdAt
    val twentyTwoHoursInMillis = 22 * 60 * 60 * 1000L
    if (elapsed in 0L until twentyTwoHoursInMillis) {
      val entryStart = parseTimeTextToMinutes(entry.startTime)
      val entryEnd = parseTimeTextToMinutes(entry.endTime)
      if (entryStart == -1 || entryEnd == -1 || entryStart >= entryEnd) continue

      // Overlap check
      val overlaps = (startMin < entryEnd && endMin > entryStart)
      if (overlaps) {
        if (entry.room.trim().equals(newRoom.trim(), ignoreCase = true)) {
          return "⚠️ Room Conflict: ${entry.room} is already booked for ${entry.subject} (${entry.startTime} - ${entry.endTime})!"
        }
        if (entry.batch.trim().equals(newBatch.trim(), ignoreCase = true)) {
          return "⚠️ Batch Conflict: ${entry.batch} already has a scheduled class: ${entry.subject} (${entry.startTime} - ${entry.endTime})!"
        }
      }
    }
  }
  return null
}
