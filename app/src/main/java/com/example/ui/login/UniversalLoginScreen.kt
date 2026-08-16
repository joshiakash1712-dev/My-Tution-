package com.example.ui.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppRepository
import com.example.data.UserRole
import com.example.data.FirebaseAuthService
import com.example.data.DarkThemeMode
import com.example.security.SecurityEngine

@Composable
fun UniversalLoginScreen(
  onLoginSuccess: () -> Unit,
  authViewModel: AuthViewModel = viewModel()
) {
  val identifier by authViewModel.identifier.collectAsState()
  val password by authViewModel.password.collectAsState()
  val isLoading by authViewModel.isLoading.collectAsState()
  val loginError by authViewModel.loginError.collectAsState()
  val successMessage by authViewModel.successMessage.collectAsState()
  val lockoutRemainingSeconds by authViewModel.lockoutRemainingSeconds.collectAsState()

  var isPasswordVisible by remember { mutableStateOf(false) }
  var showForgotDialog by remember { mutableStateOf(false) }
  var rememberMe by remember { mutableStateOf(true) }

  val darkThemeModeState by AppRepository.darkThemeMode.collectAsState()
  val isSystemDark = isSystemInDarkTheme()
  val isDark = when (darkThemeModeState) {
    DarkThemeMode.LIGHT -> false
    DarkThemeMode.DARK -> true
    DarkThemeMode.SYSTEM -> isSystemDark
  }

  // Ticker for lockout timer only running when an active lockout countdown is present
  LaunchedEffect(lockoutRemainingSeconds) {
    if (lockoutRemainingSeconds > 0) {
      authViewModel.startLockoutTimerTicker()
    }
  }

  // Real-time detected role preview based on typed identifier
  val detectedRole = remember(identifier) {
    val clean = identifier.trim().lowercase()
    when {
      clean.isEmpty() -> null
      clean.contains("admin") || clean == "joshiakash1209@gmail.com" || clean == "joshiakash1712@gmail.com" || clean == "admin@fsi.com" -> UserRole.ADMIN
      clean.contains("teacher") || clean.contains("faculty") || clean.startsWith("tch") -> UserRole.TEACHER
      clean.contains("parent") || clean.startsWith("par") -> UserRole.PARENT
      clean.contains("student") || clean.startsWith("stu") -> UserRole.STUDENT
      else -> null
    }
  }

  // Atmospheric background gradient brushes
  val backgroundBrush = if (isDark) {
    Brush.verticalGradient(
      colors = listOf(
        Color(0xFF0B1120),
        Color(0xFF0F172A),
        Color(0xFF090D16)
      )
    )
  } else {
    Brush.verticalGradient(
      colors = listOf(
        Color(0xFFEBF2FA),
        Color(0xFFF1F5F9),
        Color(0xFFE2E8F0)
      )
    )
  }

  val cardContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
  val cardBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
  val brandColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0061A4)
  val brandGradient = if (isDark) {
    Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
  } else {
    Brush.linearGradient(listOf(Color(0xFF0061A4), Color(0xFF0284C7)))
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(backgroundBrush)
      .safeDrawingPadding()
      .padding(horizontal = 20.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    // Theme Switcher Button at top-right (1-tap instant switch between Light & Dark)
    IconButton(
      onClick = {
        val nextMode = if (isDark) DarkThemeMode.LIGHT else DarkThemeMode.DARK
        AppRepository.setDarkThemeMode(nextMode)
      },
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(8.dp)
        .size(44.dp)
        .background(
          color = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF),
          shape = CircleShape
        )
        .border(
          width = 1.dp,
          color = cardBorderColor,
          shape = CircleShape
        )
        .shadow(elevation = 3.dp, shape = CircleShape)
    ) {
      val icon = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode
      Icon(
        imageVector = icon,
        contentDescription = if (isDark) "Switch to Light theme" else "Switch to Dark theme",
        tint = brandColor,
        modifier = Modifier.size(20.dp)
      )
    }

    // Main Sign In Card Container
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 460.dp)
        .shadow(
          elevation = if (isDark) 12.dp else 8.dp,
          shape = RoundedCornerShape(28.dp),
          ambientColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFF64748B).copy(alpha = 0.2f),
          spotColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF0F172A).copy(alpha = 0.15f)
        )
        .verticalScroll(rememberScrollState()),
      colors = CardDefaults.cardColors(containerColor = cardContainerColor),
      border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor),
      shape = RoundedCornerShape(28.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // App Identity Header
        Box(
          modifier = Modifier
            .size(68.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(brandGradient)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.School,
            contentDescription = "My Tuition App Logo",
            tint = Color.White,
            modifier = Modifier.size(38.dp)
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "My Tuition (FSI)",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
            letterSpacing = (-0.5).sp
          )

          Text(
            text = "Universal AI-Powered ERP Portal",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
          )
        }

        // Lockout Notice
        if (lockoutRemainingSeconds > 0) {
          Surface(
            color = if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFCA5A5)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFDC2626)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              }
              Column {
                Text(
                  text = "Brute-Force Protection Active",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                )
                Text(
                  text = "Lockout expires in $lockoutRemainingSeconds second${if (lockoutRemainingSeconds == 1L) "" else "s"}.",
                  fontSize = 11.sp,
                  color = if (isDark) Color(0xFFF87171) else Color(0xFF7F1D1D)
                )
              }
            }
          }
        }

        // Loading Indicator Banner
        if (isLoading) {
          Surface(
            color = brandColor.copy(alpha = if (isDark) 0.2f else 0.1f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, brandColor.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = brandColor,
                strokeWidth = 2.5.dp
              )
              Text(
                text = "Authenticating secure credentials...",
                fontSize = 13.sp,
                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A),
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // Success message banner
        if (successMessage != null && !isLoading) {
          Surface(
            color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFF86EFAC)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
              Text(
                successMessage ?: "",
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D),
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // Error message banner
        if (loginError != null) {
          val isUnregisteredNotice = loginError?.contains("not found in tuition database", ignoreCase = true) == true
          Surface(
            color = if (isDark) {
              if (isUnregisteredNotice) Color(0xFF451A03) else Color(0xFF450A0A)
            } else {
              if (isUnregisteredNotice) Color(0xFFFFFBEB) else Color(0xFFFEE2E2)
            },
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isDark) {
                if (isUnregisteredNotice) Color(0xFFB45309) else Color(0xFF991B1B)
              } else {
                if (isUnregisteredNotice) Color(0xFFFCD34D) else Color(0xFFFCA5A5)
              }
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = if (isUnregisteredNotice) Icons.Default.AdminPanelSettings else Icons.Default.Error,
                contentDescription = null,
                tint = if (isUnregisteredNotice) Color(0xFFD97706) else Color(0xFFDC2626),
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = loginError ?: "",
                fontSize = 12.sp,
                color = if (isDark) {
                  if (isUnregisteredNotice) Color(0xFFFDE68A) else Color(0xFFFCA5A5)
                } else {
                  if (isUnregisteredNotice) Color(0xFF92400E) else Color(0xFFB91C1C)
                },
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // --- SIGN IN FORM ---
        OutlinedTextField(
          value = identifier,
          onValueChange = { text ->
            authViewModel.onIdentifierChanged(text)
          },
          label = { Text("Smart Identifier / Email") },
          placeholder = { Text("Email, Mobile, Username, or Student ID") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Identifier Icon",
              tint = if (identifier.isNotEmpty()) brandColor else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            )
          },
          trailingIcon = {
            if (identifier.isNotEmpty()) {
              IconButton(onClick = { authViewModel.onIdentifierChanged("") }) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Clear identifier",
                  modifier = Modifier.size(18.dp),
                  tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
              }
            }
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = brandColor,
            unfocusedBorderColor = cardBorderColor,
            focusedLabelColor = brandColor,
            cursorColor = brandColor,
            focusedContainerColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
            unfocusedContainerColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.3f) else Color(0xFFF8FAFC)
          )
        )

        // Live Role Detection preview pill
        AnimatedVisibility(
          visible = detectedRole != null,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
        ) {
          if (detectedRole != null) {
            val (roleTitle, roleIcon, roleColor) = when (detectedRole) {
              UserRole.ADMIN -> Triple("Administrator Portal", Icons.Default.Shield, Color(0xFF7C3AED))
              UserRole.TEACHER -> Triple("Faculty & Teacher Portal", Icons.Default.MenuBook, Color(0xFF0284C7))
              UserRole.STUDENT -> Triple("Student Learning Portal", Icons.Default.School, Color(0xFF16A34A))
              UserRole.PARENT -> Triple("Parent Progress Portal", Icons.Default.FamilyRestroom, Color(0xFFEA580C))
            }

            Surface(
              color = roleColor.copy(alpha = if (isDark) 0.2f else 0.1f),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, roleColor.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = roleIcon,
                  contentDescription = null,
                  tint = roleColor,
                  modifier = Modifier.size(15.dp)
                )
                Text(
                  text = "Portal Route: $roleTitle",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isDark) Color(0xFFF8FAFC) else roleColor
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = password,
          onValueChange = { text ->
            authViewModel.onPasswordChanged(text)
          },
          label = { Text("Secure Password") },
          placeholder = { Text("Enter account password") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Password Icon",
              tint = if (password.isNotEmpty()) brandColor else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            )
          },
          trailingIcon = {
            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
              Icon(
                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Toggle password visibility",
                tint = if (isPasswordVisible) brandColor else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
              )
            }
          },
          visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = brandColor,
            unfocusedBorderColor = cardBorderColor,
            focusedLabelColor = brandColor,
            cursorColor = brandColor,
            focusedContainerColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
            unfocusedContainerColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.3f) else Color(0xFFF8FAFC)
          )
        )

        // Remember Me & Forgot Password Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
              ) { rememberMe = !rememberMe }
          ) {
            Checkbox(
              checked = rememberMe,
              onCheckedChange = { rememberMe = it },
              colors = CheckboxDefaults.colors(checkedColor = brandColor)
            )
            Text(
              text = "Remember me",
              fontSize = 12.sp,
              color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
            )
          }

          Text(
            text = "Forgot Password?",
            color = brandColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .clickable { showForgotDialog = true }
              .padding(vertical = 4.dp, horizontal = 6.dp)
          )
        }

        val failedCount = SecurityEngine.getFailedAttemptCount(identifier)
        if (failedCount > 0 && lockoutRemainingSeconds == 0L) {
          Text(
            text = "⚠️ $failedCount failed attempt${if (failedCount == 1) "" else "s"}. Lockout triggers at 3 attempts.",
            fontSize = 11.sp,
            color = Color(0xFFD97706),
            fontWeight = FontWeight.Medium
          )
        }

        // --- SUBMIT BUTTON ---
        Button(
          onClick = {
            authViewModel.performSignIn(onLoginSuccess = onLoginSuccess)
          },
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (lockoutRemainingSeconds > 0) Color(0xFFDC2626) else brandColor
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
              elevation = if (isLoading || lockoutRemainingSeconds > 0) 0.dp else 4.dp,
              shape = RoundedCornerShape(16.dp),
              spotColor = brandColor.copy(alpha = 0.4f)
            ),
          enabled = !isLoading && lockoutRemainingSeconds == 0L
        ) {
          if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
          } else if (lockoutRemainingSeconds > 0) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(imageVector = Icons.Default.LockClock, contentDescription = null, tint = Color.White)
              Text(
                "Locked Out (${lockoutRemainingSeconds}s)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
              )
            }
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(imageVector = Icons.Default.Login, contentDescription = null, tint = Color.White)
              Text("Secure Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }
          }
        }
      }
    }
  }

  // --- FORGOT PASSWORD / FORMAL SECURITY ASSISTANCE DIALOG ---
  if (showForgotDialog) {
    AlertDialog(
      onDismissRequest = { showForgotDialog = false },
      icon = {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(brandColor.copy(alpha = if (isDark) 0.2f else 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "Security Protocol",
            tint = brandColor,
            modifier = Modifier.size(26.dp)
          )
        }
      },
      title = {
        Text(
          text = "Password Reset Protocol",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.VerifiedUser,
                  contentDescription = null,
                  tint = brandColor,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "Institutional Security Notice",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                )
              }
              Text(
                text = "To safeguard student records and institutional data integrity, automated public password resets are restricted.",
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                lineHeight = 17.sp
              )
            }
          }

          Text(
            text = "How to Reset Your Password:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
          )

          Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AdminPanelSettings,
              contentDescription = null,
              tint = brandColor,
              modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
            Text(
              text = "Contact your Assigned Teacher or Institute Administrator to request a verified credential reset.",
              fontSize = 12.sp,
              color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
              lineHeight = 17.sp
            )
          }

          Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = Color(0xFF16A34A),
              modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
            Text(
              text = "Authorized faculty can immediately update and issue temporary access credentials via the Admin & Faculty Console.",
              fontSize = 12.sp,
              color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
              lineHeight = 17.sp
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { showForgotDialog = false },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = brandColor),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
        ) {
          Text(
            text = "Understood",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White
          )
        }
      },
      containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF),
      shape = RoundedCornerShape(20.dp)
    )
  }
}

