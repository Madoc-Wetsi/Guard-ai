package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CampusGuard AI", appName)
  }

  @Test
  fun `verify incident urgency and categorization logic`() {
    val incident = com.example.data.model.IncidentReport(
      title = "Medical Alert",
      description = "Dizziness in hall",
      category = com.example.data.model.IncidentCategory.EMERGENCY.displayName,
      urgency = com.example.data.model.UrgencyLevel.CRITICAL.name,
      locationName = "Science Hall"
    )
    assertEquals("Critical - Level 1", com.example.data.model.UrgencyLevel.valueOf(incident.urgency).label)
    assertEquals(false, incident.isAnonymous)
  }
}
