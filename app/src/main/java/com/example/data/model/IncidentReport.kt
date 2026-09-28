package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class IncidentCategory(val displayName: String, val iconName: String) {
    EMERGENCY("Medical / Physical Emergency", "medical_services"),
    SUSPICIOUS_ACTIVITY("Suspicious Activity / Person", "visibility"),
    HARASSMENT("Harassment / Threat / Assault", "security"),
    THEFT("Theft / Property Loss", "inventory_2"),
    FACILITY_HAZARD("Facility Hazard / Damage", "warning"),
    FIRE_HAZMAT("Fire / Gas / Chemical Hazard", "local_fire_department"),
    MENTAL_HEALTH("Mental Health / Wellness Check", "favorite"),
    SAFEWALK("SafeWalk / Escort Request", "directions_walk"),
    OTHER("Other Safety Issue", "help_outline")
}

enum class UrgencyLevel(val label: String, val levelScore: Int) {
    CRITICAL("Critical - Level 1", 4),
    HIGH("High - Level 2", 3),
    MODERATE("Moderate - Level 3", 2),
    LOW("Low - Level 4", 1)
}

enum class IncidentStatus(val label: String) {
    REPORTED("Reported"),
    TRIAGED("AI Triaged"),
    DISPATCHED("Responder Dispatched"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved")
}

@Entity(tableName = "incidents")
data class IncidentReport(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String,
    val urgency: String,
    val locationName: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUri: String? = null,
    val isAnonymous: Boolean = false,
    val reporterName: String = "Student",
    val reporterPhone: String = "",
    val status: String = IncidentStatus.REPORTED.name,
    val responderName: String? = null,
    val responderNotes: String? = null,
    val aiThreatScore: Int = 50,
    val aiSuggestedUnit: String = "Campus Security",
    val aiImmediateAdvice: String = "Stay in a secure and illuminated location.",
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)
