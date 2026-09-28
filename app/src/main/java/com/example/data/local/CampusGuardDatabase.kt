package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CampusAlert
import com.example.data.model.EmergencyContact
import com.example.data.model.IncidentCategory
import com.example.data.model.IncidentReport
import com.example.data.model.IncidentStatus
import com.example.data.model.UrgencyLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        IncidentReport::class,
        CampusAlert::class,
        EmergencyContact::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CampusGuardDatabase : RoomDatabase() {

    abstract fun campusGuardDao(): CampusGuardDao

    companion object {
        @Volatile
        private var INSTANCE: CampusGuardDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): CampusGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CampusGuardDatabase::class.java,
                    "campusguard_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.campusGuardDao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: CampusGuardDao) {
                // Populate default emergency contacts
                val contacts = listOf(
                    EmergencyContact(
                        name = "Campus Police Dispatch",
                        department = "Department of Public Safety",
                        phoneNumber = "555-0199",
                        description = "Immediate armed and unarmed safety responders, 24/7 patrol dispatch.",
                        is24x7 = true,
                        priority = 1
                    ),
                    EmergencyContact(
                        name = "City Emergency (911)",
                        department = "Municipal Police / Fire / EMS",
                        phoneNumber = "911",
                        description = "Direct line to city municipal emergency operations center.",
                        is24x7 = true,
                        priority = 2
                    ),
                    EmergencyContact(
                        name = "SafeWalk Student Escort",
                        department = "Campus Safety Services",
                        phoneNumber = "555-0144",
                        description = "Free walking or van escort anywhere on campus between 6 PM - 4 AM.",
                        is24x7 = false,
                        priority = 3
                    ),
                    EmergencyContact(
                        name = "Counseling & Psychological (CAPS)",
                        department = "Student Mental Health Services",
                        phoneNumber = "555-0188",
                        description = "Confidential 24/7 crisis counselors and scheduled therapy support.",
                        is24x7 = true,
                        priority = 4
                    ),
                    EmergencyContact(
                        name = "Title IX & Survivor Advocacy",
                        department = "Office of Institutional Equity",
                        phoneNumber = "555-0160",
                        description = "Confidential support, rights consultation, and protective measure coordination.",
                        is24x7 = false,
                        priority = 5
                    ),
                    EmergencyContact(
                        name = "Campus Health Center (Urgent Care)",
                        department = "Student Health Services",
                        phoneNumber = "555-0120",
                        description = "First aid, minor injuries, allergy treatments, walk-in clinic.",
                        is24x7 = false,
                        priority = 6
                    )
                )
                dao.insertContacts(contacts)

                // Populate active campus broadcast alert
                dao.insertAlert(
                    CampusAlert(
                        title = "Flash Flood & Storm Warning on North Campus",
                        message = "Heavy rainfall has caused water pooling near North Science Hall Walkway and Parking Lot C. Please use illuminated South Walkway. Facilities teams are on site.",
                        severity = "HIGH",
                        affectedArea = "North Quad & Lot C",
                        timestamp = System.currentTimeMillis() - (45 * 60 * 1000)
                    )
                )

                // Populate realistic incidents
                val sampleIncidents = listOf(
                    IncidentReport(
                        title = "Broken Exterior Lighting on West Dormitory Path",
                        description = "Three light fixtures along the footpath behind Alder Hall are completely dark. Visibility is near zero after sunset.",
                        category = IncidentCategory.FACILITY_HAZARD.displayName,
                        urgency = UrgencyLevel.MODERATE.name,
                        locationName = "Alder Hall West Path",
                        latitude = 37.7749,
                        longitude = -122.4194,
                        isAnonymous = false,
                        reporterName = "Marcus Vance",
                        reporterPhone = "555-0123",
                        status = IncidentStatus.DISPATCHED.name,
                        responderName = "Unit 3 - Facilities Patrol",
                        responderNotes = "Temporary lighting cart deployed. Electricians scheduled for 08:00 AM repair.",
                        aiThreatScore = 42,
                        aiSuggestedUnit = "Facilities & Grounds Support",
                        aiImmediateAdvice = "Avoid unlit paths; utilize main Central Walkway or call SafeWalk (555-0144).",
                        createdAt = System.currentTimeMillis() - (120 * 60 * 1000)
                    ),
                    IncidentReport(
                        title = "Suspicious Individual Attempting Bike Lockers",
                        description = "Individual in dark hoodie carrying bolt cutters tampering with bike racks outside Science Complex building B.",
                        category = IncidentCategory.SUSPICIOUS_ACTIVITY.displayName,
                        urgency = UrgencyLevel.HIGH.name,
                        locationName = "Science Complex Block B",
                        latitude = 37.7758,
                        longitude = -122.4182,
                        isAnonymous = true,
                        reporterName = "Anonymous Student",
                        reporterPhone = "",
                        status = IncidentStatus.IN_PROGRESS.name,
                        responderName = "Officer Reynolds (Patrol 2)",
                        responderNotes = "Officer on scene. Perimeter checked. CCTV review underway with dispatch.",
                        aiThreatScore = 78,
                        aiSuggestedUnit = "Campus Police Patrol",
                        aiImmediateAdvice = "Do not confront the individual. Maintain safe distance and observe direction of travel if safe.",
                        createdAt = System.currentTimeMillis() - (35 * 60 * 1000)
                    ),
                    IncidentReport(
                        title = "Student Fainted in Library 3rd Floor Study Room",
                        description = "Student experienced sudden dizziness and collapsed during study session. Conscious but confused.",
                        category = IncidentCategory.EMERGENCY.displayName,
                        urgency = UrgencyLevel.CRITICAL.name,
                        locationName = "Main Library 3rd Floor, Room 304",
                        latitude = 37.7741,
                        longitude = -122.4170,
                        isAnonymous = false,
                        reporterName = "Chloe Zhang",
                        reporterPhone = "555-0155",
                        status = IncidentStatus.RESOLVED.name,
                        responderName = "EMT Unit Alpha & Campus Nurse",
                        responderNotes = "Vitals stabilized on scene. Transported to Student Health Center for hydration and observation.",
                        aiThreatScore = 92,
                        aiSuggestedUnit = "Emergency Medical Services (EMS)",
                        aiImmediateAdvice = "Keep person lying flat, elevate legs if possible, ensure open airway, do not give liquids.",
                        createdAt = System.currentTimeMillis() - (240 * 60 * 1000),
                        resolvedAt = System.currentTimeMillis() - (180 * 60 * 1000)
                    )
                )

                for (incident in sampleIncidents) {
                    dao.insertIncident(incident)
                }
            }
        }
    }
}
