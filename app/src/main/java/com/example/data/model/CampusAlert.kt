package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campus_alerts")
data class CampusAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val severity: String = "HIGH", // CRITICAL, HIGH, INFO
    val affectedArea: String = "Campus-wide",
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val department: String,
    val phoneNumber: String,
    val description: String,
    val is24x7: Boolean = true,
    val priority: Int = 1 // 1 is highest
)

data class AiTriageResult(
    val category: String,
    val urgencyLevel: UrgencyLevel,
    val threatScore: Int, // 1 to 100
    val suggestedResponderUnit: String,
    val keyHazards: List<String>,
    val studentSafetyGuidance: String,
    val dispatcherBrief: String
)
