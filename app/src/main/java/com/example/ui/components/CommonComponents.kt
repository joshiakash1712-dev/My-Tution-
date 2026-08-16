package com.example.ui.components
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserRole
import com.example.data.AppRepository
import com.example.data.DarkThemeMode
import com.example.data.FirestoreService
import com.example.data.FirestoreAnnouncement
import com.example.data.NotificationItem
import com.example.ui.theme.rem
import com.example.ui.theme.em
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GeoHeader(
  currentRole: UserRole,
  onRoleClick: () -> Unit = {},
  onProfileClick: () -> Unit = {},
  searchQuery: String = "",
  onSearchChange: (String) -> Unit = {},
  searchPlaceholder: String = "Search..."
) {
  val notifications by AppRepository.notifications.collectAsState()
  val unreadCount = notifications.count { !it.isRead }
  var showNotificationsModal by remember { mutableStateOf(false) }

  if (showNotificationsModal) {
    NotificationsModalDialog(onDismiss = { showNotificationsModal = false })
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
      .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
  ) {
    // Top Row: Fadat Science Institute Logo + Name & Subtitle | Notification Bell + Avatar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        // Institute Shield / Emblem
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.School,
            contentDescription = "FSI Logo",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
          )
        }

        Column {
          Text(
            text = "Fadat Science Institute",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Empowering minds. Building futures.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Actions: Notification Bell + Profile Avatar
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Notification bell with badge
        Box(contentAlignment = Alignment.TopEnd) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .clickable { showNotificationsModal = true },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.Notifications,
              contentDescription = "Notifications",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }
          if (unreadCount > 0) {
            Box(
              modifier = Modifier
                .offset(x = 2.dp, y = (-2).dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "$unreadCount",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // Profile Avatar with online green status dot
        Box(
          contentAlignment = Alignment.BottomEnd,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable {
              onProfileClick()
              onRoleClick()
            }
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Color(0xFF2563EB)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = currentRole.displayName.take(1).uppercase(),
              color = Color.White,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(Color(0xFF22C55E))
              .border(1.5.dp, Color.White, CircleShape)
          )
        }
      }
    }
  }
}

object AppRoleSwitcher {
  var onSwitch: ((UserRole) -> Unit)? = null
  fun triggerSwitch(role: UserRole) {
    onSwitch?.invoke(role)
  }
}

@Composable
fun HeroFeatureCard(
  title: String,
  subtitle: String,
  tagText: String = "LIVE NOW",
  icon: ImageVector = Icons.Default.Analytics,
  onClick: () -> Unit = {}
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(28.dp))
      .background(MaterialTheme.colorScheme.primaryContainer)
      .clickable { onClick() }
      .padding(22.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(10.dp)
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
          )
        }

        Surface(
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
          shape = CircleShape
        ) {
          Text(
            text = tagText,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Column {
        Text(
          text = title,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontSize = 20.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
          fontSize = 13.sp,
          fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun QuickActionGrid(
  card1Title: String,
  card1Icon: ImageVector,
  card1Click: () -> Unit,
  card2Title: String,
  card2Icon: ImageVector,
  card2Click: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Left Card: Secondary Container background
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.secondaryContainer)
        .clickable { card1Click() }
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
          imageVector = card1Icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(26.dp)
        )
        Text(
          text = card1Title,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    // Right Card: Tertiary Container background
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.tertiaryContainer)
        .clickable { card2Click() }
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
          imageVector = card2Icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.size(26.dp)
        )
        Text(
          text = card2Title,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun GeoSectionCard(
  title: String,
  actionText: String = "View all",
  onActionClick: (() -> Unit)? = null,
  content: @Composable () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(28.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp))
      .padding(18.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      if (onActionClick != null) {
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = actionText.uppercase(),
          color = MaterialTheme.colorScheme.primary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.clickable { onActionClick() }
        )
      }
    }
    content()
  }
}

@Composable
fun GeoListItem(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconBg: Color = Color.Unspecified,
  iconTint: Color = Color.Unspecified,
  trailing: @Composable () -> Unit = {
    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
  },
  onClick: () -> Unit
) {
  val resolvedIconBg = if (iconBg == Color.Unspecified) MaterialTheme.colorScheme.secondaryContainer else iconBg
  val resolvedIconTint = if (iconTint == Color.Unspecified) MaterialTheme.colorScheme.onSecondaryContainer else iconTint

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() }
      .padding(vertical = 8.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(resolvedIconBg),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = resolvedIconTint,
        modifier = Modifier.size(22.dp)
      )
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = subtitle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    trailing()
  }
}

@Composable
fun GeoBottomNav(
  tabs: List<Pair<String, ImageVector>>,
  selectedIndex: Int,
  onTabSelected: (Int) -> Unit
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .fillMaxWidth()
      .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
  ) {
    tabs.forEachIndexed { idx, tab ->
      val isSelected = selectedIndex == idx
      NavigationBarItem(
        selected = isSelected,
        onClick = { onTabSelected(idx) },
        icon = {
          Icon(
            imageVector = tab.second,
            contentDescription = tab.first,
            modifier = Modifier.size(22.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        },
        label = {
          Text(
            text = tab.first,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(
          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
          selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
      )
    }
  }
}

@Composable
fun MetricBox(title: String, bigVal: String, subVal: String, bg: Color, tx: Color, modifier: Modifier = Modifier) {
  Box(modifier = modifier.clip(RoundedCornerShape(24.dp)).background(bg).padding(16.dp)) {
    Column {
      Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = tx.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
      Spacer(Modifier.height(6.dp))
      Text(bigVal, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = tx, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Spacer(Modifier.height(2.dp))
      Text(subVal, fontSize = 11.sp, color = tx.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsModalDialog(onDismiss: () -> Unit) {
  val notifications by AppRepository.notifications.collectAsState()
  val firestoreAnnouncements by FirestoreService.remoteAnnouncements.collectAsState()
  var selectedFilter by remember { mutableStateOf("All") }
  var isSyncing by remember { mutableStateOf(false) }
  var showClearAllConfirmDialog by remember { mutableStateOf(false) }

  val unreadCount = notifications.count { !it.isRead }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val filterOptions = listOf(
    "All" to notifications.size + firestoreAnnouncements.size,
    "Unread" to unreadCount,
    "📢 Announcements" to (notifications.count { it.type.equals("Announcement", ignoreCase = true) } + firestoreAnnouncements.size),
    "💳 Fees" to notifications.count { it.type.equals("Fee", ignoreCase = true) },
    "📝 Tests" to notifications.count { it.type.equals("Test", ignoreCase = true) },
    "📅 Attendance" to notifications.count { it.type.equals("Absence", ignoreCase = true) || it.type.equals("Attendance", ignoreCase = true) },
    "📚 Homework" to notifications.count { it.type.equals("Homework", ignoreCase = true) || it.type.equals("Task", ignoreCase = true) }
  )

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    dragHandle = { BottomSheetDefaults.DragHandle() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.88f)
        .padding(horizontal = 16.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // --- HEADER & QUICK ACTIONS ---
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
          }
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "Notification Center",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              if (unreadCount > 0) {
                Surface(
                  color = MaterialTheme.colorScheme.error,
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text(
                    text = "$unreadCount New",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
            Text(
              text = "Institute Alerts, Updates & Academic Notices",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Action Buttons: Sync, Mark All Read, Delete All, Close
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              isSyncing = true
              FirestoreService.fetchAnnouncements {
                isSyncing = false
              }
            }
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Sync Cloud Feed",
              tint = if (isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }

          if (notifications.isNotEmpty()) {
            IconButton(
              onClick = { AppRepository.markAllNotificationsAsRead() }
            ) {
              Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Mark All Read",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }

            IconButton(
              onClick = { showClearAllConfirmDialog = true }
            ) {
              Icon(
                imageVector = Icons.Outlined.DeleteSweep,
                contentDescription = "Delete All Notifications",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // --- QUICK ACTION CHIPS (MARK ALL READ & CLEAR ALL SHORTCUT BUTTONS) ---
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          onClick = { AppRepository.markAllNotificationsAsRead() },
          enabled = unreadCount > 0,
          shape = RoundedCornerShape(10.dp),
          color = if (unreadCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = BorderStroke(1.dp, if (unreadCount > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.DoneAll,
              contentDescription = null,
              tint = if (unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = "Mark All Read",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (unreadCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
          }
        }

        Surface(
          onClick = { showClearAllConfirmDialog = true },
          enabled = notifications.isNotEmpty(),
          shape = RoundedCornerShape(10.dp),
          color = if (notifications.isNotEmpty()) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = BorderStroke(1.dp, if (notifications.isNotEmpty()) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Outlined.DeleteSweep,
              contentDescription = null,
              tint = if (notifications.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = "Delete All",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (notifications.isNotEmpty()) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
          }
        }
      }

      // --- FILTER PILLS ---
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        filterOptions.forEach { (filterName, count) ->
          val isSelected = selectedFilter == filterName
          Surface(
            onClick = { selectedFilter = filterName },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            border = BorderStroke(
              1.dp,
              if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = filterName,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
              if (count > 0) {
                Surface(
                  shape = CircleShape,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer
                ) {
                  Text(
                    text = "$count",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                  )
                }
              }
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      // --- NOTIFICATIONS LIST ---
      val filteredList = remember(notifications, firestoreAnnouncements, selectedFilter) {
        when (selectedFilter) {
          "All" -> notifications
          "Unread" -> notifications.filter { !it.isRead }
          "📢 Announcements" -> notifications.filter { it.type.equals("Announcement", ignoreCase = true) }
          "💳 Fees" -> notifications.filter { it.type.equals("Fee", ignoreCase = true) }
          "📝 Tests" -> notifications.filter { it.type.equals("Test", ignoreCase = true) }
          "📅 Attendance" -> notifications.filter { it.type.equals("Absence", ignoreCase = true) || it.type.equals("Attendance", ignoreCase = true) }
          "📚 Homework" -> notifications.filter { it.type.equals("Homework", ignoreCase = true) || it.type.equals("Task", ignoreCase = true) }
          else -> notifications
        }
      }

      val showRemoteAnnouncements = selectedFilter == "All" || selectedFilter == "📢 Announcements"

      if (filteredList.isEmpty() && (!showRemoteAnnouncements || firestoreAnnouncements.isEmpty())) {
        // Premium Empty State
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(24.dp)
          ) {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.NotificationsOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
              )
            }
            Text(
              text = if (selectedFilter == "Unread") "All Caught Up!" else "No Notifications Found",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (selectedFilter == "Unread")
                "You have reviewed all current notices and alerts."
              else
                "There are no items matching this category. Institutional alerts, fee invoices, test schedules, and homework notices will appear here.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              lineHeight = 18.sp
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 16.dp)
        ) {
          // 1. Cloud Firestore Broadcasts
          if (showRemoteAnnouncements && firestoreAnnouncements.isNotEmpty()) {
            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CloudDone,
                  contentDescription = null,
                  tint = Color(0xFF0284C7),
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "LIVE CLOUD BROADCASTS (${firestoreAnnouncements.size})",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF0284C7)
                )
              }
            }

            items(firestoreAnnouncements) { annc ->
              val audienceBg = when {
                annc.targetAudience.contains("Parent", ignoreCase = true) -> Color(0xFFFEF3C7)
                annc.targetAudience.contains("Teacher", ignoreCase = true) -> Color(0xFFDCFCE7)
                else -> Color(0xFFDBEAFE)
              }
              val audienceTextColor = when {
                annc.targetAudience.contains("Parent", ignoreCase = true) -> Color(0xFF92400E)
                annc.targetAudience.contains("Teacher", ignoreCase = true) -> Color(0xFF166534)
                else -> Color(0xFF1E40AF)
              }

              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                  // Left Accent Bar (Blue for Cloud Broadcasts)
                  Box(
                    modifier = Modifier
                      .width(5.dp)
                      .fillMaxHeight()
                      .background(Color(0xFF0284C7))
                  )
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                      ) {
                        Box(
                          modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.15f)),
                          contentAlignment = Alignment.Center
                        ) {
                          Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                          )
                        }
                        Text(
                          text = annc.title,
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                      }
                      Surface(
                        color = audienceBg,
                        shape = RoundedCornerShape(6.dp)
                      ) {
                        Text(
                          text = annc.targetAudience,
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          color = audienceTextColor,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }

                    Text(
                      text = annc.message,
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      lineHeight = 17.sp
                    )

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Sender: ${annc.sender}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      Text(
                        text = annc.dateStr.ifBlank { "Live Broadcast" },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                      )
                    }
                  }
                }
              }
            }

            if (filteredList.isNotEmpty()) {
              item {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = "LOCAL NOTIFICATIONS & ALERTS (${filteredList.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          }

          // 2. Local Notifications List
          items(filteredList, key = { it.id }) { item ->
            val isUnread = !item.isRead

            val categoryColor = when (item.type.lowercase()) {
              "announcement" -> Color(0xFF0284C7)
              "fee" -> Color(0xFFD97706)
              "test", "exam" -> Color(0xFF059669)
              "absence", "attendance" -> Color(0xFFDC2626)
              "homework", "task" -> Color(0xFF7C3AED)
              else -> Color(0xFF0284C7)
            }

            val categoryIcon = when (item.type.lowercase()) {
              "announcement" -> Icons.Default.Campaign
              "fee" -> Icons.Default.Payments
              "test", "exam" -> Icons.Default.AssignmentTurnedIn
              "absence", "attendance" -> Icons.Default.EventBusy
              "homework", "task" -> Icons.Default.MenuBook
              else -> Icons.Default.Notifications
            }

            val categoryLabel = when (item.type.lowercase()) {
              "announcement" -> "NOTICE"
              "fee" -> "FEE ALERT"
              "test", "exam" -> "EXAM"
              "absence", "attendance" -> "ATTENDANCE"
              "homework", "task" -> "TASK"
              else -> item.type.uppercase()
            }

            Surface(
              onClick = { AppRepository.markNotificationAsRead(item.id) },
              color = if (isUnread) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
              },
              border = BorderStroke(
                1.dp,
                if (isUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
              ),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(modifier = Modifier.fillMaxWidth()) {
                // Left Accent Stripe for Unread / Category
                Box(
                  modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (isUnread) categoryColor else Color.Transparent)
                )

                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  // Top Row: Category Badge + Title + Unread Indicator + Single Item Delete
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(30.dp)
                          .clip(CircleShape)
                          .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          imageVector = categoryIcon,
                          contentDescription = null,
                          tint = categoryColor,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                      Surface(
                        color = categoryColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                      ) {
                        Text(
                          text = categoryLabel,
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          color = categoryColor,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                      if (isUnread) {
                        Surface(
                          color = MaterialTheme.colorScheme.primary,
                          shape = RoundedCornerShape(4.dp)
                        ) {
                          Text(
                            text = "NEW",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                          )
                        }
                      }
                    }

                    // Single Item Delete Button
                    IconButton(
                      onClick = { AppRepository.deleteNotification(item.id) },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete notification",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }

                  // Notification Title
                  Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )

                  // Notification Message
                  Text(
                    text = item.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                  )

                  // Bottom Info Row: Time Stamp + Target Role / Tap Hint
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(12.dp)
                      )
                      Text(
                        text = item.time,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                      )
                    }

                    if (isUnread) {
                      Text(
                        text = "Tap to mark read",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                      )
                    } else {
                      Text(
                        text = "Read",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // --- DELETE ALL CONFIRMATION DIALOG ---
  if (showClearAllConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearAllConfirmDialog = false },
      icon = {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.errorContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.DeleteSweep,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(26.dp)
          )
        }
      },
      title = {
        Text(
          text = "Clear All Notifications?",
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      text = {
        Text(
          text = "This will permanently remove all ${notifications.size} local notifications from your device activity log.",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 18.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            AppRepository.deleteAllNotifications()
            showClearAllConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Delete All", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearAllConfirmDialog = false }) {
          Text("Cancel", fontWeight = FontWeight.SemiBold)
        }
      },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(20.dp)
    )
  }
}
