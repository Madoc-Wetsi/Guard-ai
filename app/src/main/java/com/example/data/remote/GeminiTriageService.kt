package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiTriageResult
import com.example.data.model.IncidentCategory
import com.example.data.model.UrgencyLevel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiTriageService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * Evaluates incident details with Gemini 3.5 Flash to automatically triage:
     * - Urgency Level (Critical, High, Moderate, Low)
     * - Threat Score (1 - 100)
     * - Immediate safety guidance for the student
     * - Suggested responder unit
     * - Key hazards detected
     * - Dispatcher summary brief
     */
    suspend fun analyzeIncident(
        title: String,
        category: String,
        description: String,
        locationName: String
    ): AiTriageResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("GeminiTriageService", "Using local rule-based triage heuristic.")
            return@withContext computeHeuristicTriage(title, category, description, locationName)
        }

        try {
            val prompt = """
                You are CAMPUSGUARD AI, an emergency dispatch and student safety AI system.
                Analyze the following campus incident report:
                Title: $title
                Category: $category
                Location: $locationName
                Description: $description

                Respond ONLY with a JSON object with these exact keys:
                {
                   "category": "One of: Medical / Physical Emergency, Suspicious Activity / Person, Harassment / Threat / Assault, Theft / Property Loss, Facility Hazard / Damage, Fire / Gas / Chemical Hazard, Mental Health / Wellness Check, SafeWalk / Escort Request, Other Safety Issue",
                   "urgency": "One of: CRITICAL, HIGH, MODERATE, LOW",
                   "threatScore": (integer between 1 and 100),
                   "suggestedResponderUnit": "(e.g., Campus Police Rapid Response, EMS / First Responders, Crisis Counseling Team, Facilities Maintenance)",
                   "keyHazards": ["hazard 1", "hazard 2"],
                   "studentSafetyGuidance": "(concise, actionable, 1-2 sentence safety instructions for the reporting student)",
                   "dispatcherBrief": "(concise, 1-2 sentence technical summary for the campus security dispatcher)"
                }
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("GeminiTriageService", "API call failed with code: ${response.code}")
                return@withContext computeHeuristicTriage(title, category, description, locationName)
            }

            val responseBody = response.body?.string() ?: ""
            val jsonRoot = JSONObject(responseBody)
            val candidates = jsonRoot.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            parseTriageJson(text, category)
        } catch (e: Exception) {
            Log.e("GeminiTriageService", "Gemini triage exception, fallback to heuristic", e)
            computeHeuristicTriage(title, category, description, locationName)
        }
    }

    /**
     * Generates compassionate student safety advice or counseling support response.
     */
    suspend fun getSafetyAdvisorResponse(
        userMessage: String,
        recentContext: String = ""
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalAdvisorResponse(userMessage)
        }

        try {
            val systemInstruction = """
                You are CAMPUSGUARD AI Safety & Student Support Advisor.
                You provide supportive, calming, practical, and clear safety guidance to university students.
                Provide actionable steps, encourage reaching out to campus resources (Campus Police 555-0199, CAPS Counseling 555-0188, SafeWalk 555-0144, or 911 for life threats).
                If the student is in immediate danger, prominently instruct them to activate the in-app SOS button or call emergency services right away.
                Keep responses concise, empathetic, and formatted with bullet points if explaining multiple steps.
            """.trimIndent()

            val prompt = if (recentContext.isNotBlank()) {
                "Recent situation context: $recentContext\n\nStudent question: $userMessage"
            } else {
                "Student question: $userMessage"
            }

            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                }
                put("contents", contentsArray)

                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.6)
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext generateLocalAdvisorResponse(userMessage)
            }

            val responseBody = response.body?.string() ?: ""
            val jsonRoot = JSONObject(responseBody)
            val candidates = jsonRoot.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            parts?.optJSONObject(0)?.optString("text") ?: generateLocalAdvisorResponse(userMessage)
        } catch (e: Exception) {
            Log.e("GeminiTriageService", "Gemini chat advisor exception", e)
            generateLocalAdvisorResponse(userMessage)
        }
    }

    private fun parseTriageJson(jsonString: String, originalCategory: String): AiTriageResult {
        return try {
            val obj = JSONObject(jsonString)
            val cat = obj.optString("category", originalCategory)
            val urgencyStr = obj.optString("urgency", "MODERATE").uppercase()
            val urgency = when {
                urgencyStr.contains("CRITICAL") -> UrgencyLevel.CRITICAL
                urgencyStr.contains("HIGH") -> UrgencyLevel.HIGH
                urgencyStr.contains("LOW") -> UrgencyLevel.LOW
                else -> UrgencyLevel.MODERATE
            }
            val threatScore = obj.optInt("threatScore", 55).coerceIn(1, 100)
            val suggestedUnit = obj.optString("suggestedResponderUnit", "Campus Public Safety")

            val hazardsList = mutableListOf<String>()
            val hazardsArray = obj.optJSONArray("keyHazards")
            if (hazardsArray != null) {
                for (i in 0 until hazardsArray.length()) {
                    hazardsList.add(hazardsArray.getString(i))
                }
            } else {
                hazardsList.add("Immediate situational assessment needed")
            }

            val studentAdvice = obj.optString(
                "studentSafetyGuidance",
                "Remain in a well-lit location, avoid confronting any subjects, and monitor your surroundings."
            )
            val dispatcherBrief = obj.optString(
                "dispatcherBrief",
                "Incident logged with priority: $urgencyStr. Unit dispatch recommended."
            )

            AiTriageResult(
                category = cat,
                urgencyLevel = urgency,
                threatScore = threatScore,
                suggestedResponderUnit = suggestedUnit,
                keyHazards = hazardsList,
                studentSafetyGuidance = studentAdvice,
                dispatcherBrief = dispatcherBrief
            )
        } catch (e: Exception) {
            computeHeuristicTriage("", originalCategory, "", "")
        }
    }

    private fun computeHeuristicTriage(
        title: String,
        category: String,
        description: String,
        locationName: String
    ): AiTriageResult {
        val lowerText = "$title $category $description".lowercase()

        return when {
            lowerText.contains("collapse") || lowerText.contains("unconscious") ||
                    lowerText.contains("faint") || lowerText.contains("weapon") ||
                    lowerText.contains("knife") || lowerText.contains("gun") ||
                    lowerText.contains("bleeding") || lowerText.contains("seizure") ||
                    lowerText.contains("choking") || lowerText.contains("fire") ||
                    lowerText.contains("explosion") || category == IncidentCategory.EMERGENCY.displayName -> {
                AiTriageResult(
                    category = if (lowerText.contains("fire")) IncidentCategory.FIRE_HAZMAT.displayName else IncidentCategory.EMERGENCY.displayName,
                    urgencyLevel = UrgencyLevel.CRITICAL,
                    threatScore = 95,
                    suggestedResponderUnit = "Emergency Medical Services (EMS) & Armed Campus Police",
                    keyHazards = listOf("Imminent threat to life or physical integrity", "Immediate emergency responder intervention required"),
                    studentSafetyGuidance = "CRITICAL: Maintain personal safety distance. If safe, stay on line with dispatch (555-0199 or 911). Do not attempt physical interventions without first responders.",
                    dispatcherBrief = "URGENT DISPATCH: Level 1 emergency reported at $locationName. Real-time GPS enabled."
                )
            }

            lowerText.contains("assault") || lowerText.contains("stalking") ||
                    lowerText.contains("threat") || lowerText.contains("harass") ||
                    lowerText.contains("following me") || lowerText.contains("prowler") ||
                    lowerText.contains("break-in") || lowerText.contains("burglar") ||
                    category == IncidentCategory.HARASSMENT.displayName ||
                    category == IncidentCategory.SUSPICIOUS_ACTIVITY.displayName -> {
                AiTriageResult(
                    category = category,
                    urgencyLevel = UrgencyLevel.HIGH,
                    threatScore = 78,
                    suggestedResponderUnit = "Campus Police Patrol Car 2 & Student Escort Support",
                    keyHazards = listOf("Active security breach / personal safety hazard", "Potential suspect presence on site"),
                    studentSafetyGuidance = "Head toward an occupied building (Library, Student Union) or blue-light emergency tower. Do not engage the individual.",
                    dispatcherBrief = "HIGH PRIORITY: Rapid patrol dispatched to $locationName. Perimeter and camera check advised."
                )
            }

            lowerText.contains("depressed") || lowerText.contains("suicide") ||
                    lowerText.contains("panic") || lowerText.contains("crisis") ||
                    category == IncidentCategory.MENTAL_HEALTH.displayName -> {
                AiTriageResult(
                    category = IncidentCategory.MENTAL_HEALTH.displayName,
                    urgencyLevel = UrgencyLevel.HIGH,
                    threatScore = 72,
                    suggestedResponderUnit = "CAPS Crisis Counseling Team & Peer Support Advocate",
                    keyHazards = listOf("Acute mental health distress", "Confidential de-escalation support needed"),
                    studentSafetyGuidance = "You are not alone. A confidential support team member can connect with you immediately. Call 555-0188 or 988 anytime.",
                    dispatcherBrief = "MENTAL HEALTH ALERT: Non-punitive clinical crisis support requested at $locationName."
                )
            }

            lowerText.contains("escort") || lowerText.contains("walk") ||
                    category == IncidentCategory.SAFEWALK.displayName -> {
                AiTriageResult(
                    category = IncidentCategory.SAFEWALK.displayName,
                    urgencyLevel = UrgencyLevel.MODERATE,
                    threatScore = 30,
                    suggestedResponderUnit = "SafeWalk Student Escort Team",
                    keyHazards = listOf("Late night transit safety precaution"),
                    studentSafetyGuidance = "SafeWalk responder has been alerted. Wait inside the lobby of $locationName until their arrival.",
                    dispatcherBrief = "ROUTINE REQUEST: SafeWalk pickup requested at $locationName."
                )
            }

            lowerText.contains("leak") || lowerText.contains("light") ||
                    lowerText.contains("slip") || lowerText.contains("elevator") ||
                    lowerText.contains("hazard") || category == IncidentCategory.FACILITY_HAZARD.displayName -> {
                AiTriageResult(
                    category = IncidentCategory.FACILITY_HAZARD.displayName,
                    urgencyLevel = UrgencyLevel.MODERATE,
                    threatScore = 40,
                    suggestedResponderUnit = "Campus Facilities & Grounds Department",
                    keyHazards = listOf("Physical infrastructure hazard", "Risk of slip/fall or equipment fault"),
                    studentSafetyGuidance = "Please maintain a safe distance from damaged equipment or slippery surfaces. Facilities crew notified.",
                    dispatcherBrief = "MAINTENANCE DISPATCH: Safety barrier and repair work order created for $locationName."
                )
            }

            else -> {
                AiTriageResult(
                    category = category,
                    urgencyLevel = UrgencyLevel.LOW,
                    threatScore = 25,
                    suggestedResponderUnit = "Campus Safety Officer",
                    keyHazards = listOf("General non-urgent report"),
                    studentSafetyGuidance = "Report received and queued for review by campus public safety staff.",
                    dispatcherBrief = "LOGGED: Routine report queued for shift supervisor review."
                )
            }
        }
    }

    private fun generateLocalAdvisorResponse(userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("follow") || lower.contains("behind me") || lower.contains("scared") -> {
                "🚨 **Immediate Safety Steps if You Feel Followed:**\n\n" +
                        "1. **Head toward people**: Cross the street and walk toward a well-lit, populated building like the Student Union, Dining Hall, or 24/7 Library.\n" +
                        "2. **Use Blue-Light Towers**: Press any campus emergency blue-light call station—it immediately sounds an alarm and sends your exact location to Campus Police.\n" +
                        "3. **Call Campus Police**: Call **555-0199** immediately or tap the red SOS button on your screen.\n" +
                        "4. **Do not go to secluded areas or private vehicles** until an officer or escort arrives."
            }

            lower.contains("safewalk") || lower.contains("escort") || lower.contains("night") -> {
                "🚶 **Campus SafeWalk Service:**\n\n" +
                        "SafeWalk is 100% free and available every night from 6:00 PM to 4:00 AM.\n" +
                        "• **Direct Dispatch**: Call **555-0144** or tap 'SafeWalk' in the emergency dialer.\n" +
                        "• Two trained student safety escorts or a campus security shuttle will accompany you door-to-door anywhere on campus and adjacent parking lots."
            }

            lower.contains("anxiety") || lower.contains("panic") || lower.contains("depressed") || lower.contains("counseling") || lower.contains("talk") -> {
                "💙 **Student Counseling & Mental Health Resources:**\n\n" +
                        "It takes courage to reach out, and support is available 24/7:\n" +
                        "• **Campus Counseling (CAPS)**: Call **555-0188** for free, confidential sessions.\n" +
                        "• **National Crisis Lifeline**: Dial or text **988** (available 24/7).\n" +
                        "• **Crisis Text Line**: Text 'HOME' to 741741.\n\n" +
                        "Would you like to speak with a counselor right now or request an anonymous check-in?"
            }

            lower.contains("anonymous") || lower.contains("privacy") || lower.contains("report") -> {
                "🛡️ **Anonymous Reporting Guarantee:**\n\n" +
                        "CampusGuard AI strictly honors anonymous reporting. When you toggle 'Submit Anonymously', your name, student ID, and phone number are stripped before submission to the security dispatch dashboard. Responders only receive the incident location, category, and evidence necessary to address the threat."
            }

            lower.contains("title ix") || lower.contains("assault") || lower.contains("harassment") -> {
                "⚖️ **Title IX & Survivor Support:**\n\n" +
                        "The university provides comprehensive, confidential support for incidents of harassment, stalking, or assault:\n" +
                        "• **Confidential Advocacy**: Contact **555-0160**.\n" +
                        "• You have the right to request campus protective measures, academic accommodations, dorm room changes, and no-contact directives regardless of whether you choose to file a formal police report."
            }

            else -> {
                "🛡️ **CampusGuard AI Support:**\n\n" +
                        "I am here to help you stay safe and navigate campus safety resources.\n\n" +
                        "• In an immediate physical emergency, tap the **Emergency SOS** button or dial **911**.\n" +
                        "• For campus safety dispatch, call **555-0199**.\n" +
                        "• For SafeWalk nighttime escorts, call **555-0144**.\n" +
                        "• For confidential counseling, call **555-0188**.\n\n" +
                        "What safety question or situation can I help you with?"
            }
        }
    }
}
