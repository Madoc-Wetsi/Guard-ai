package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CampusAlert
import com.example.data.model.EmergencyContact
import com.example.data.model.IncidentReport
import kotlinx.coroutines.flow.Flow

@Dao
interface CampusGuardDao {

    // Incidents
    @Query("SELECT * FROM incidents ORDER BY createdAt DESC")
    fun getAllIncidents(): Flow<List<IncidentReport>>

    @Query("SELECT * FROM incidents WHERE id = :id")
    fun getIncidentById(id: Long): Flow<IncidentReport?>

    @Query("SELECT * FROM incidents WHERE status = :status ORDER BY createdAt DESC")
    fun getIncidentsByStatus(status: String): Flow<List<IncidentReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentReport): Long

    @Update
    suspend fun updateIncident(incident: IncidentReport)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteIncident(id: Long)

    @Query("UPDATE incidents SET status = :status, responderName = :responder, responderNotes = :notes WHERE id = :id")
    suspend fun updateIncidentStatus(id: Long, status: String, responder: String?, notes: String?)

    // Alerts
    @Query("SELECT * FROM campus_alerts WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getActiveAlerts(): Flow<List<CampusAlert>>

    @Query("SELECT * FROM campus_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<CampusAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: CampusAlert): Long

    @Update
    suspend fun updateAlert(alert: CampusAlert)

    @Query("UPDATE campus_alerts SET isActive = 0 WHERE id = :alertId")
    suspend fun dismissAlert(alertId: Long)

    // Emergency Contacts
    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC, name ASC")
    fun getAllContacts(): Flow<List<EmergencyContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<EmergencyContact>)
}
