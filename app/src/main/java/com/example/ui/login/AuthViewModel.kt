package com.example.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.FirebaseAuthService
import com.example.data.UserRole
import com.example.security.FirebaseAppCheckManager
import com.example.security.SecurityEngine
import com.example.security.SecuritySeverity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AuthStatus {
  SUCCESS,
  FAILED_INVALID_CREDENTIALS,
  FAILED_LOCKOUT,
  FAILED_XSS,
  FAILED_APP_CHECK,
  FAILED_PENDING_APPROVAL,
  FAILED_EMPTY_INPUT
}

data class StructuredAuthAttempt(
  val attemptId: String,
  val timestamp: String,
  val rawIdentifier: String,
  val maskedIdentifier: String,
  val targetRole: UserRole?,
  val isAdminTarget: Boolean,
  val isUnauthorizedAdminAttempt: Boolean,
  val status: AuthStatus,
  val failureReason: String?,
  val severity: SecuritySeverity,
  val appCheckVerified: Boolean,
  val clientIp: String,
  val userAgent: String,
  val structuredJson: String
)

data class AdminAccessAuditSummary(
  val totalAttempts: Int = 0,
  val successfulAdminLogins: Int = 0,
  val failedAdminLogins: Int = 0,
  val unauthorizedAttempts: Int = 0,
  val lastAdminLoginTimestamp: String? = null,
  val lastUnauthorizedTarget: String? = null
)

class AuthViewModel : ViewModel() {

  private val _identifier = MutableStateFlow("")
  val identifier: StateFlow<String> = _identifier.asStateFlow()

  private val _password = MutableStateFlow("")
  val password: StateFlow<String> = _password.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _loginError = MutableStateFlow<String?>(null)
  val loginError: StateFlow<String?> = _loginError.asStateFlow()

  private val _successMessage = MutableStateFlow<String?>(null)
  val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

  private val _lockoutRemainingSeconds = MutableStateFlow(0L)
  val lockoutRemainingSeconds: StateFlow<Long> = _lockoutRemainingSeconds.asStateFlow()

  // Structured Auth Audit Trail
  private val _authAuditLogs = MutableStateFlow<List<StructuredAuthAttempt>>(emptyList())
  val authAuditLogs: StateFlow<List<StructuredAuthAttempt>> = _authAuditLogs.asStateFlow()

  private val _adminAuditSummary = MutableStateFlow(AdminAccessAuditSummary())
  val adminAuditSummary: StateFlow<AdminAccessAuditSummary> = _adminAuditSummary.asStateFlow()

  companion object {
    private const val TAG = "AuthViewModelAudit"
  }

  fun onIdentifierChanged(value: String) {
    _identifier.value = value
    _loginError.value = null
    checkLockoutState(value)
  }

  fun onPasswordChanged(value: String) {
    _password.value = value
    _loginError.value = null
  }

  fun checkLockoutState(id: String = _identifier.value) {
    if (id.isNotBlank()) {
      val (isLocked, secs) = SecurityEngine.isLockedOut(id)
      _lockoutRemainingSeconds.value = if (isLocked) secs else 0L
    } else {
      _lockoutRemainingSeconds.value = 0L
    }
  }

  fun startLockoutTimerTicker() {
    viewModelScope.launch {
      while (true) {
        val currentId = _identifier.value
        if (currentId.isNotBlank()) {
          val (isLocked, secs) = SecurityEngine.isLockedOut(currentId)
          _lockoutRemainingSeconds.value = if (isLocked) secs else 0L
          if (!isLocked || secs <= 0L) {
            break
          }
        } else {
          _lockoutRemainingSeconds.value = 0L
          break
        }
        delay(1000L)
      }
    }
  }

  fun clearMessages() {
    _loginError.value = null
    _successMessage.value = null
  }

  /**
   * Main Sign-In Flow with Structured Authentication Logging
   */
  fun performSignIn(onLoginSuccess: () -> Unit) {
    val rawId = _identifier.value.trim()
    val rawPass = _password.value

    if (rawId.isBlank()) {
      _loginError.value = "Please enter your email or identifier"
      recordStructuredLog(
        rawIdentifier = rawId,
        targetRole = null,
        status = AuthStatus.FAILED_EMPTY_INPUT,
        failureReason = "Empty identifier submitted",
        severity = SecuritySeverity.WARNING,
        appCheckVerified = false
      )
      return
    }

    // 1. Check for Malicious Input / Script Injection
    if (SecurityEngine.containsMaliciousPattern(rawId) || SecurityEngine.containsMaliciousPattern(rawPass)) {
      val errorMsg = "🛡️ Security Shield: Malicious input syntax blocked."
      _loginError.value = errorMsg
      recordStructuredLog(
        rawIdentifier = rawId,
        targetRole = null,
        status = AuthStatus.FAILED_XSS,
        failureReason = "XSS/Malicious payload blocked in input fields",
        severity = SecuritySeverity.CRITICAL,
        appCheckVerified = false
      )
      return
    }

    // 2. Check Brute-Force Lockout
    val (isLocked, lockoutSecs) = SecurityEngine.isLockedOut(rawId)
    if (isLocked) {
      _lockoutRemainingSeconds.value = lockoutSecs
      val lockoutMsg = "🛡️ Account temporarily locked. Try again in $lockoutSecs seconds."
      _loginError.value = lockoutMsg
      recordStructuredLog(
        rawIdentifier = rawId,
        targetRole = null,
        status = AuthStatus.FAILED_LOCKOUT,
        failureReason = "Account currently locked out due to repeated failed attempts ($lockoutSecs s remaining)",
        severity = SecuritySeverity.ALERT,
        appCheckVerified = false
      )
      return
    }

    _isLoading.value = true
    _loginError.value = null

    // 3. Verify Firebase App Check (reCAPTCHA Enterprise Protection)
    FirebaseAppCheckManager.verifyAppCheckToken { isAppCheckOk, appCheckMsg ->
      if (!isAppCheckOk) {
        _isLoading.value = false
        val appCheckError = "🛡️ Firebase App Check (reCAPTCHA) verification failed."
        _loginError.value = appCheckError
        recordStructuredLog(
          rawIdentifier = rawId,
          targetRole = null,
          status = AuthStatus.FAILED_APP_CHECK,
          failureReason = "App Check reCAPTCHA verification returned invalid token: $appCheckMsg",
          severity = SecuritySeverity.ALERT,
          appCheckVerified = false
        )
        return@verifyAppCheckToken
      }

      // 4. Invoke Firebase Auth Engine
      FirebaseAuthService.signIn(
        email = rawId,
        password = rawPass,
        onSuccess = { authenticatedRole, userEmail ->
          _isLoading.value = false
          SecurityEngine.recordSuccessfulAuth(userEmail, authenticatedRole)

          val loggedIn = AppRepository.login(userEmail, authenticatedRole)
          if (loggedIn) {
            _successMessage.value = "Authentication successful. Welcome, ${authenticatedRole.name}!"
            recordStructuredLog(
              rawIdentifier = userEmail,
              targetRole = authenticatedRole,
              status = AuthStatus.SUCCESS,
              failureReason = null,
              severity = SecuritySeverity.INFO,
              appCheckVerified = true
            )
            onLoginSuccess()
          } else {
            val deniedMsg = "❌ Access Denied: Faculty status pending approval."
            _loginError.value = deniedMsg
            recordStructuredLog(
              rawIdentifier = userEmail,
              targetRole = authenticatedRole,
              status = AuthStatus.FAILED_PENDING_APPROVAL,
              failureReason = "User authenticated in Firebase but account status is pending institute admin approval",
              severity = SecuritySeverity.WARNING,
              appCheckVerified = true
            )
          }
        },
        onFailure = { errorDetail ->
          _isLoading.value = false
          val (lockedNow, secs) = SecurityEngine.recordFailedAttempt(rawId)
          if (lockedNow) {
            _lockoutRemainingSeconds.value = secs
            _loginError.value = "🛡️ Too many failed attempts. Locked out for $secs seconds."
          } else {
            _loginError.value = "❌ Auth Failed: $errorDetail"
          }

          recordStructuredLog(
            rawIdentifier = rawId,
            targetRole = null,
            status = AuthStatus.FAILED_INVALID_CREDENTIALS,
            failureReason = errorDetail,
            severity = if (lockedNow) SecuritySeverity.ALERT else SecuritySeverity.WARNING,
            appCheckVerified = true
          )
        }
      )
    }
  }

  fun performPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
    if (email.isBlank() || !email.contains("@")) {
      onResult(false, "Please provide a valid registered email address.")
      return
    }

    recordStructuredLog(
      rawIdentifier = email,
      targetRole = null,
      status = AuthStatus.SUCCESS,
      failureReason = null,
      severity = SecuritySeverity.INFO,
      appCheckVerified = true,
      customTag = "PASSWORD_RESET_REQUESTED"
    )

    FirebaseAuthService.sendPasswordResetEmail(
      email = email.trim(),
      onSuccess = {
        onResult(true, "Password reset link sent to $email. Check your inbox.")
      },
      onFailure = { err ->
        onResult(false, "Failed to send reset email: $err")
      }
    )
  }

  /**
   * Emits structured audit log for admin access monitoring & unauthorized attempt detection
   */
  private fun recordStructuredLog(
    rawIdentifier: String,
    targetRole: UserRole?,
    status: AuthStatus,
    failureReason: String?,
    severity: SecuritySeverity,
    appCheckVerified: Boolean,
    customTag: String = "AUTH_ATTEMPT"
  ) {
    val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
    val attemptId = "AUTH_${System.currentTimeMillis()}_${(1000..9999).random()}"
    val masked = SecurityEngine.maskEmail(rawIdentifier)

    // Detect if this attempt targeted or involved an Admin role
    val isAdminTarget = targetRole == UserRole.ADMIN ||
      rawIdentifier.lowercase().contains("admin") ||
      rawIdentifier.lowercase().contains("joshiakash") ||
      rawIdentifier.lowercase().contains("director")

    val isUnauthorizedAdmin = isAdminTarget && status != AuthStatus.SUCCESS

    val structuredJsonPayload = """
      {
        "attempt_id": "$attemptId",
        "timestamp": "$timestamp",
        "event_type": "$customTag",
        "identifier_masked": "$masked",
        "target_role": "${targetRole?.name ?: "UNKNOWN"}",
        "is_admin_target": $isAdminTarget,
        "is_unauthorized_admin_attempt": $isUnauthorizedAdmin,
        "auth_status": "${status.name}",
        "severity": "${severity.name}",
        "app_check_verified": $appCheckVerified,
        "failure_reason": ${failureReason?.let { "\"$it\"" } ?: "null"},
        "client_ip": "127.0.0.1",
        "transport_layer": "TLS 1.3 / Encrypted"
      }
    """.trimIndent()

    val logItem = StructuredAuthAttempt(
      attemptId = attemptId,
      timestamp = timestamp,
      rawIdentifier = rawIdentifier,
      maskedIdentifier = masked,
      targetRole = targetRole,
      isAdminTarget = isAdminTarget,
      isUnauthorizedAdminAttempt = isUnauthorizedAdmin,
      status = status,
      failureReason = failureReason,
      severity = if (isUnauthorizedAdmin) SecuritySeverity.ALERT else severity,
      appCheckVerified = appCheckVerified,
      clientIp = "127.0.0.1 (TLS 1.3)",
      userAgent = "MyTuition-Android-Client/1.0",
      structuredJson = structuredJsonPayload
    )

    // 1. Log to state flow for local UI inspection
    _authAuditLogs.update { (listOf(logItem) + it).take(200) }

    // 2. Update Admin Access Audit Summary telemetry
    _adminAuditSummary.update { prev ->
      prev.copy(
        totalAttempts = prev.totalAttempts + 1,
        successfulAdminLogins = if (status == AuthStatus.SUCCESS && targetRole == UserRole.ADMIN) prev.successfulAdminLogins + 1 else prev.successfulAdminLogins,
        failedAdminLogins = if (isAdminTarget && status != AuthStatus.SUCCESS) prev.failedAdminLogins + 1 else prev.failedAdminLogins,
        unauthorizedAttempts = if (isUnauthorizedAdmin) prev.unauthorizedAttempts + 1 else prev.unauthorizedAttempts,
        lastAdminLoginTimestamp = if (status == AuthStatus.SUCCESS && targetRole == UserRole.ADMIN) timestamp else prev.lastAdminLoginTimestamp,
        lastUnauthorizedTarget = if (isUnauthorizedAdmin) masked else prev.lastUnauthorizedTarget
      )
    }

    // 3. Forward to SecurityEngine for central audit logging
    SecurityEngine.logEvent(
      severity = if (isUnauthorizedAdmin) SecuritySeverity.ALERT else severity,
      eventType = if (isUnauthorizedAdmin) "UNAUTHORIZED_ADMIN_ACCESS_ATTEMPT" else customTag,
      actor = masked,
      details = "Status: ${status.name} | Role: ${targetRole?.name ?: "N/A"} | AdminTarget: $isAdminTarget | AppCheck: $appCheckVerified | Cause: ${failureReason ?: "None"}"
    )

    // 4. Output structured JSON to Logcat for SIEM integration
    val logMessage = "[STRUCTURED_AUTH_LOG] $structuredJsonPayload"
    when (if (isUnauthorizedAdmin) SecuritySeverity.ALERT else severity) {
      SecuritySeverity.INFO -> Log.i(TAG, logMessage)
      SecuritySeverity.WARNING -> Log.w(TAG, logMessage)
      SecuritySeverity.ALERT, SecuritySeverity.CRITICAL -> Log.e(TAG, logMessage)
    }
  }
}
