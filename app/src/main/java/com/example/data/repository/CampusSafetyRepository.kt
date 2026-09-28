package com.example.data.repository

import com.example.data.local.CampusGuardDao
import com.example.data.model.AiTriageResult
import com.example.data.model.CampusAlert
import com.example.data.model.EmergencyContact
import com.example.data.model.IncidentReport
import com.example.data.model.IncidentStatus
import com.example.data.remote.GeminiTriageService
import kotlinx.coroutines.flow.Flow

class CampusSafetyRepository(
    private val dao: CampusGuardDao,
    private val aiService: GeminiTriageService = GeminiTriageService()
) {

    val allIncidents: Flow<List<IncidentReport>> = dao.getAllIncidents()
    val activeAlerts: Flow<List<CampusAlert>> = dao.getActiveAlerts()
    val allAlerts: Flow<List<CampusAlert>> = dao.getAllAlerts()
    val emergencyContacts: Flow<List<EmergencyContact>> = dao.getAllContacts()

    fun getIncidentById(id: Long): Flow<IncidentReport?> = dao.getIncidentById(id)

    suspend fun analyzeIncident(
        title: String,
        category: String,
        description: String,
        locationName: String
    ): AiTriageResult {
        return aiService.analyzeIncident(title, category, description, locationName)
    }

    suspend fun submitIncident(
        title: String,
        category: String,
        description: String,
        urgency: String,
        locationName: String,
        latitude: Double?,
        longitude: Double?,
        photoUri: String?,
        isAnonymous: Boolean,
        reporterName: String,
        reporterPhone: String,
        aiTriageResult: AiTriageResult?
    ): Long {
        val triage = aiTriageResult ?: aiService.analyzeIncident(title, category, description, locationName)

        val incident = IncidentReport(
            title = title,
            description = description,
            category = triage.category.ifBlank { category },
            urgency = triage.urgencyLevel.name,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            photoUri = photoUri,
            isAnonymous = isAnonymous,
            reporterName = if (isAnonymous) "Anonymous Student" else reporterName,
            reporterPhone = if (isAnonymous) "" else reporterPhone,
            status = IncidentStatus.TRIAGED.name,
            aiThreatScore = triage.threatScore,
            aiSuggestedUnit = triage.suggestedResponderUnit,
            aiImmediateAdvice = triage.studentSafetyGuidance,
            createdAt = System.currentTimeMillis()
        )
        return dao.insertIncident(incident)
    }

    suspend fun updateIncidentStatus(
        id: Long,
        status: String,
        responderName: String?,
        notes: String?
    ) {
        dao.updateIncidentStatus(id, status, responderName, notes)
    }

    suspend fun broadcastAlert(
        title: String,
        message: String,
        severity: String,
        affectedArea: String
    ): Long {
        val alert = CampusAlert(
            title = title,
            message = message,
            severity = severity,
            affectedArea = affectedArea,
            timestamp = System.currentTimeMillis(),
            isActive = true
        )
        return dao.insertAlert(alert)
    }

    suspend fun dismissAlert(alertId: Long) {
        dao.dismissAlert(alertId)
    }

    suspend fun askSafetyAdvisor(query: String, context: String = ""): String {
        return aiService.getSafetyAdvisorResponse(query, context)
    }
}
