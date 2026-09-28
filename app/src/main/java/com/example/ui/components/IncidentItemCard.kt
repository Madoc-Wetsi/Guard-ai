package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IncidentCategory
import com.example.data.model.IncidentReport
import com.example.data.model.IncidentStatus
import com.example.data.model.UrgencyLevel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafetyBluePrimary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncidentItemCard(
    incident: IncidentReport,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val urgency = try {
        UrgencyLevel.valueOf(incident.urgency)
    } catch (e: Exception) {
        UrgencyLevel.MODERATE
    }

    val (urgencyColor, urgencyBg) = when (urgency) {
        UrgencyLevel.CRITICAL -> AlertRed to AlertRed.copy(alpha = 0.15f)
        UrgencyLevel.HIGH -> WarningAmber to WarningAmber.copy(alpha = 0.15f)
        UrgencyLevel.MODERATE -> SafetyBluePrimary to SafetyBluePrimary.copy(alpha = 0.15f)
        UrgencyLevel.LOW -> SafeGreen to SafeGreen.copy(alpha = 0.15f)
    }

    val statusObj = try {
        IncidentStatus.valueOf(incident.status)
    } catch (e: Exception) {
        IncidentStatus.REPORTED
    }

    val statusColor = when (statusObj) {
        IncidentStatus.RESOLVED -> SafeGreen
        IncidentStatus.DISPATCHED, IncidentStatus.IN_PROGRESS -> WarningAmber
        IncidentStatus.TRIAGED -> SafetyBluePrimary
        IncidentStatus.REPORTED -> Color(0xFF64748B)
    }

    val categoryIcon = getCategoryIcon(incident.category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("incident_item_card_${incident.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category Badge + Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(urgencyBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = incident.category,
                            tint = urgencyColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = incident.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusObj.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = incident.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description snippet
            Text(
                text = incident.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Location & AI Threat Score & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SafetyBluePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = incident.locationName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // AI Threat badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(urgencyBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AI Threat: ${incident.aiThreatScore}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = urgencyColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatTimestamp(incident.createdAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when {
        category.contains("Medical", ignoreCase = true) || category.contains("Emergency", ignoreCase = true) -> Icons.Default.MedicalServices
        category.contains("Suspicious", ignoreCase = true) -> Icons.Default.Visibility
        category.contains("Harass", ignoreCase = true) || category.contains("Assault", ignoreCase = true) -> Icons.Default.Security
        category.contains("Theft", ignoreCase = true) -> Icons.Default.Inventory2
        category.contains("Fire", ignoreCase = true) -> Icons.Default.LocalFireDepartment
        category.contains("Mental", ignoreCase = true) || category.contains("Wellness", ignoreCase = true) -> Icons.Default.Favorite
        category.contains("Walk", ignoreCase = true) || category.contains("Escort", ignoreCase = true) -> Icons.AutoMirrored.Filled.DirectionsWalk
        else -> Icons.Default.Warning
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diffMillis = System.currentTimeMillis() - timestamp
    val diffMinutes = diffMillis / (60 * 1000)
    val diffHours = diffMinutes / 60
    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffHours < 24 -> "${diffHours}h ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
