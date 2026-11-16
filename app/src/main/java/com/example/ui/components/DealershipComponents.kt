package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.StaffRole
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HeroCrimson
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

fun formatInr(amount: Double): String {
  val locale = Locale.Builder().setLanguage("en").setRegion("IN").build()
  val formatter = NumberFormat.getCurrencyInstance(locale)
  formatter.maximumFractionDigits = 0
  return formatter.format(amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowroomTopAppBar(
  activeRole: StaffRole,
  lowStockCount: Int,
  onRoleSelected: (StaffRole) -> Unit,
  onLowStockClick: () -> Unit,
  onSignOutClick: () -> Unit,
) {
  var roleMenuExpanded by remember { mutableStateOf(false) }

  TopAppBar(
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    title = {
      Column {
        Text(
          text = "Sudha Hero",
          style = MaterialTheme.typography.titleLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "Mohanpur Bazar, Purnia",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    },
    actions = {
      Box {
        FilterChip(
          selected = true,
          onClick = { roleMenuExpanded = true },
          label = { Text(activeRole.shortLabel, maxLines = 1) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.AdminPanelSettings,
              contentDescription = "Current Role",
              modifier = Modifier.size(16.dp),
            )
          },
          trailingIcon = {
            Icon(
              imageVector = Icons.Default.SwapHoriz,
              contentDescription = "Switch role",
              modifier = Modifier.size(14.dp),
            )
          },
          modifier = Modifier.testTag("role_switcher_chip"),
        )
        DropdownMenu(
          expanded = roleMenuExpanded,
          onDismissRequest = { roleMenuExpanded = false },
        ) {
          StaffRole.entries.forEach { role ->
            DropdownMenuItem(
              text = { Text(role.label) },
              onClick = {
                roleMenuExpanded = false
                onRoleSelected(role)
              },
            )
          }
        }
      }

      if (lowStockCount > 0) {
        IconButton(
          onClick = onLowStockClick,
          modifier = Modifier.testTag("low_stock_bell_button"),
        ) {
          BadgedBox(
            badge = {
              Badge(containerColor = MaterialTheme.colorScheme.primary) {
                Text(lowStockCount.toString())
              }
            }
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = "Low Stock Alerts",
              tint = AmberAccent,
            )
          }
        }
      }

      IconButton(
        onClick = onSignOutClick,
        modifier = Modifier.testTag("sign_out_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = "Sign Out",
        )
      }
    },
  )
}

@Composable
fun StatusPill(
  text: String,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(8.dp),
    modifier = modifier,
  ) {
    Text(
      text = text,
      color = contentColor,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      maxLines = 1,
    )
  }
}

@Composable
fun KpiMetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  ElevatedCard(
    onClick = { onClick?.invoke() },
    enabled = onClick != null,
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Box(
          modifier =
            Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(accentColor.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun FinancialPerformanceChart(
  salesProfit: Double,
  workshopProfit: Double,
  totalExpenses: Double,
  modifier: Modifier = Modifier,
) {
  val items =
    listOf(
      Triple("Sales Profit", salesProfit, EmeraldSuccess),
      Triple("Workshop Profit", workshopProfit, AmberAccent),
      Triple("Expenses", totalExpenses, HeroCrimson),
    )
  val maxVal = max(1000.0, items.maxOf { it.second })

  OutlinedCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Showroom P&L Breakdown",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Sales margin vs Workshop profit vs Expenses",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Canvas(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(top = 8.dp),
      ) {
        val barCount = items.size
        val slotWidth = size.width / barCount
        val barWidth = (slotWidth * 0.44f).coerceAtMost(84.dp.toPx())
        val baselineY = size.height - 8.dp.toPx()
        val drawableHeight = baselineY - 8.dp.toPx()

        // Horizontal reference lines
        for (i in 0..2) {
          val y = 8.dp.toPx() + drawableHeight * (i / 2f)
          drawLine(
            color = Color.Gray.copy(alpha = 0.2f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
          )
        }

        items.forEachIndexed { index, (_, amount, color) ->
          val ratio = (amount / maxVal).toFloat().coerceIn(0f, 1f)
          val barHeight = max(6.dp.toPx(), drawableHeight * ratio)
          val left = slotWidth * index + (slotWidth - barWidth) / 2f
          val top = baselineY - barHeight

          drawRoundRect(
            color = color.copy(alpha = 0.16f),
            topLeft = Offset(left, 8.dp.toPx()),
            size = Size(barWidth, drawableHeight),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
          )
          drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        items.forEach { (label, amount, color) ->
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier =
                  Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text = formatInr(amount),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  OutlinedCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier =
          Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp),
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(Modifier.height(4.dp))
        Button(
          onClick = onAction,
          modifier = Modifier.height(48.dp),
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}
