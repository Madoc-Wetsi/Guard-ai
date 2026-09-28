package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CampusGuardDatabase
import com.example.data.model.AiTriageResult
import com.example.data.model.CampusAlert
import com.example.data.model.EmergencyContact
import com.example.data.model.IncidentCategory
import com.example.data.model.IncidentReport
import com.example.data.model.IncidentStatus
import com.example.data.model.UrgencyLevel
import com.example.data.repository.CampusSafetyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UserRole {
    STUDENT,
    ADMIN_SECURITY
}

enum class AppScreen {
    HOME,
    REPORT_INCIDENT,
    INCIDENT_DETAIL,
    EMERGENCY_SOS,
    ADMIN_DASHBOARD,
    COUNSELING_SUPPORT
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "advisor"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SosState(
    val isCountingDown: Boolean = false,
    val countdownSeconds: Int = 3,
    val isSosActive: Boolean = false,
    val activatedAt: Long? = null,
    val currentLatitude: Double = 37.7749,
    val currentLongitude: Double = -122.4194,
    val campusLocationName: String = "Central Campus Quad (Near Science Tower)",
    val dispatchStatus: String = "Broadcasting High-Priority Beacon..."
)

data class ReportFormState(
    val title: String = "",
    val description: String = "",
    val selectedCategory: IncidentCategory = IncidentCategory.SUSPICIOUS_ACTIVITY,
    val selectedUrgency: UrgencyLevel = UrgencyLevel.MODERATE,
    val locationName: String = "Main Library - South Entrance",
    val latitude: Double? = 37.7749,
    val longitude: Double? = -122.4194,
    val photoUri: String? = null,
    val isAnonymous: Boolean = false,
    val studentName: String = "Alex Chen",
    val studentPhone: String = "555-0182",
    val isAnalyzingAi: Boolean = false,
    val aiTriageResult: AiTriageResult? = null,
    val isSubmitting: Boolean = false,
    val submissionSuccessId: Long? = null
)

class CampusSafetyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CampusSafetyRepository

    init {
        val db = CampusGuardDatabase.getDatabase(application, viewModelScope)
        repository = CampusSafetyRepository(db.campusGuardDao())
    }

    val incidents: StateFlow<List<IncidentReport>> = repository.allIncidents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAlerts: StateFlow<List<CampusAlert>> = repository.activeAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emergencyContacts: StateFlow<List<EmergencyContact>> = repository.emergencyContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentRole = MutableStateFlow(UserRole.STUDENT)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _screenStack = MutableStateFlow(listOf(AppScreen.HOME))

    private val _selectedIncidentId = MutableStateFlow<Long?>(null)
    val selectedIncidentId: StateFlow<Long?> = _selectedIncidentId.asStateFlow()

    private val _sosState = MutableStateFlow(SosState())
    val sosState: StateFlow<SosState> = _sosState.asStateFlow()

    private var sosCountdownJob: Job? = null

    private val _reportFormState = MutableStateFlow(ReportFormState())
    val reportFormState: StateFlow<ReportFormState> = _reportFormState.asStateFlow()

    private val _advisorMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                sender = "advisor",
                text = "Hello! I am your CAMPUSGUARD AI Safety & Support Advisor. How can I assist you today? You can ask about campus safety, SafeWalk escorts, reporting procedures, or confidential counseling resources."
            )
        )
    )
    val advisorMessages: StateFlow<List<ChatMessage>> = _advisorMessages.asStateFlow()

    private val _isAdvisorTyping = MutableStateFlow(false)
    val isAdvisorTyping: StateFlow<Boolean> = _isAdvisorTyping.asStateFlow()

    fun toggleRole() {
        val nextRole = if (_currentRole.value == UserRole.STUDENT) UserRole.ADMIN_SECURITY else UserRole.STUDENT
        _currentRole.value = nextRole
        if (nextRole == UserRole.ADMIN_SECURITY) {
            navigateTo(AppScreen.ADMIN_DASHBOARD)
        } else {
            navigateTo(AppScreen.HOME)
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        _screenStack.update { it + screen }
    }

    fun navigateBack(): Boolean {
        val current = _screenStack.value
        if (current.size > 1) {
            val updated = current.dropLast(1)
            _screenStack.value = updated
            _currentScreen.value = updated.last()
            return true
        }
        return false
    }

    fun selectIncident(id: Long) {
        _selectedIncidentId.value = id
        navigateTo(AppScreen.INCIDENT_DETAIL)
    }

    // --- SOS Emergency Feature ---
    fun startSosCountdown() {
        vibratePhone(200)
        _sosState.update { it.copy(isCountingDown = true, countdownSeconds = 3) }
        sosCountdownJob?.cancel()
        sosCountdownJob = viewModelScope.launch {
            for (sec in 3 downTo 1) {
                _sosState.update { it.copy(countdownSeconds = sec) }
                vibratePhone(150)
                delay(1000)
            }
            triggerSosEmergency()
        }
    }

    fun cancelSosCountdown() {
        sosCountdownJob?.cancel()
        _sosState.update { it.copy(isCountingDown = false, countdownSeconds = 3) }
    }

    private fun triggerSosEmergency() {
        _sosState.update {
            it.copy(
                isCountingDown = false,
                isSosActive = true,
                activatedAt = System.currentTimeMillis(),
                dispatchStatus = "🚨 DISPATCH NOTIFIED: Campus Police Patrol En Route"
            )
        }
        vibrateEmergencyPattern()

        // Auto-log critical emergency in Room DB so security dashboard sees it immediately
        viewModelScope.launch {
            repository.submitIncident(
                title = "EMERGENCY SOS: Student Triggered Alert Beacon",
                category = IncidentCategory.EMERGENCY.displayName,
                description = "Automated high-priority SOS emergency beacon triggered from student device. Immediate dispatch required.",
                urgency = UrgencyLevel.CRITICAL.name,
                locationName = _sosState.value.campusLocationName,
                latitude = _sosState.value.currentLatitude,
                longitude = _sosState.value.currentLongitude,
                photoUri = null,
                isAnonymous = false,
                reporterName = "Alex Chen (SOS Beacon)",
                reporterPhone = "555-0182",
                aiTriageResult = AiTriageResult(
                    category = IncidentCategory.EMERGENCY.displayName,
                    urgencyLevel = UrgencyLevel.CRITICAL,
                    threatScore = 99,
                    suggestedResponderUnit = "Campus Police Rapid Response & EMS",
                    keyHazards = listOf("Student in distress / potential physical threat", "Live tracking active"),
                    studentSafetyGuidance = "Emergency dispatch active. Stay in well-lit area. Officers have your GPS coordinates.",
                    dispatcherBrief = "HIGH PRIORITY SOS BEACON: Alex Chen at ${_sosState.value.campusLocationName}"
                )
            )
        }
        navigateTo(AppScreen.EMERGENCY_SOS)
    }

    fun deactivateSos() {
        _sosState.update { it.copy(isSosActive = false, isCountingDown = false) }
        vibratePhone(100)
        navigateTo(AppScreen.HOME)
    }

    // --- Incident Reporting Form ---
    fun updateReportTitle(title: String) = _reportFormState.update { it.copy(title = title) }
    fun updateReportDescription(desc: String) = _reportFormState.update { it.copy(description = desc) }
    fun updateReportCategory(cat: IncidentCategory) = _reportFormState.update { it.copy(selectedCategory = cat) }
    fun updateReportUrgency(urgency: UrgencyLevel) = _reportFormState.update { it.copy(selectedUrgency = urgency) }
    fun updateReportLocation(location: String) = _reportFormState.update { it.copy(locationName = location) }
    fun updateReportPhoto(uriString: String?) = _reportFormState.update { it.copy(photoUri = uriString) }
    fun toggleAnonymous(isAnonymous: Boolean) = _reportFormState.update { it.copy(isAnonymous = isAnonymous) }

    fun runAiPreTriage() {
        val form = _reportFormState.value
        if (form.description.isBlank() && form.title.isBlank()) return

        _reportFormState.update { it.copy(isAnalyzingAi = true) }
        viewModelScope.launch {
            val result = repository.analyzeIncident(
                title = form.title.ifBlank { "Campus Incident" },
                category = form.selectedCategory.displayName,
                description = form.description,
                locationName = form.locationName
            )
            _reportFormState.update {
                it.copy(
                    isAnalyzingAi = false,
                    aiTriageResult = result,
                    selectedUrgency = result.urgencyLevel
                )
            }
        }
    }

    fun submitIncidentReport(onSuccess: (Long) -> Unit) {
        val form = _reportFormState.value
        if (form.title.isBlank() && form.description.isBlank()) return

        _reportFormState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val newId = repository.submitIncident(
                title = form.title.ifBlank { "${form.selectedCategory.displayName} Reported" },
                category = form.selectedCategory.displayName,
                description = form.description,
                urgency = form.selectedUrgency.name,
                locationName = form.locationName,
                latitude = form.latitude,
                longitude = form.longitude,
                photoUri = form.photoUri,
                isAnonymous = form.isAnonymous,
                reporterName = form.studentName,
                reporterPhone = form.studentPhone,
                aiTriageResult = form.aiTriageResult
            )
            _reportFormState.update {
                it.copy(
                    isSubmitting = false,
                    submissionSuccessId = newId,
                    title = "",
                    description = "",
                    photoUri = null,
                    aiTriageResult = null
                )
            }
            onSuccess(newId)
        }
    }

    // --- Admin Security Actions ---
    fun updateIncidentStatus(
        incidentId: Long,
        newStatus: IncidentStatus,
        responderName: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.updateIncidentStatus(
                id = incidentId,
                status = newStatus.name,
                responderName = responderName,
                notes = notes
            )
        }
    }

    fun broadcastCampusAlert(
        title: String,
        message: String,
        severity: String,
        affectedArea: String
    ) {
        viewModelScope.launch {
            repository.broadcastAlert(title, message, severity, affectedArea)
        }
    }

    fun dismissCampusAlert(alertId: Long) {
        viewModelScope.launch {
            repository.dismissAlert(alertId)
        }
    }

    // --- AI Safety & Counseling Advisor Chat ---
    fun sendAdvisorMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(sender = "user", text = text)
        _advisorMessages.update { it + userMsg }
        _isAdvisorTyping.value = true

        viewModelScope.launch {
            val reply = repository.askSafetyAdvisor(text)
            _advisorMessages.update {
                it + ChatMessage(sender = "advisor", text = reply)
            }
            _isAdvisorTyping.value = false
        }
    }

    // --- Phone Dialer Helper ---
    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback
        }
    }

    private fun vibratePhone(durationMillis: Long) {
        try {
            val app = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(durationMillis)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }

    private fun vibrateEmergencyPattern() {
        try {
            val app = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 300, 200, 300, 200, 500)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(longArrayOf(0, 300, 200, 300), -1)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
