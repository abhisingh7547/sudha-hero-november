package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ExpenseRecord
import com.example.data.model.StaffRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinancialPerformanceChart
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatInr
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HeroCrimson
import com.example.ui.theme.IndigoInfo
import com.example.ui.theme.OnAmberContainer
import com.example.ui.viewmodel.DealershipDashboardMetrics
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
  metrics: DealershipDashboardMetrics,
  expensesState: UiState<List<ExpenseRecord>>,
  activeRole: StaffRole,
  isWideScreen: Boolean,
  onNavigateToInventory: () -> Unit,
  onNavigateToSales: () -> Unit,
  onNavigateToWorkshop: () -> Unit,
  onNavigateToStaff: () -> Unit,
  onAddExpense: (title: String, category: String, amount: Double) -> Unit,
  onDeleteExpense: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showAddExpenseDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Today's Hero Summary Banner
    item {
      ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(if (isWideScreen) 180.dp else 168.dp)
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_showroom_banner_1794842294259),
            contentDescription = "Sudha Hero Showroom Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .background(
                  Brush.horizontalGradient(
                    colors =
                      listOf(
                        Color.Black.copy(alpha = 0.86f),
                        Color.Black.copy(alpha = 0.45f),
                      )
                  )
                )
          )
          Column(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "TODAY'S SHOWROOM PULSE",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFFCA5A5),
                )
                Text(
                  text = "Sudha Hero • Purnia",
                  style = MaterialTheme.typography.headlineSmall,
                  color = Color.White,
                )
              }
              Surface(
                color = HeroCrimson,
                shape = RoundedCornerShape(10.dp),
              ) {
                Text(
                  text = activeRole.shortLabel,
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              SummaryHeroStat(
                label = "Today's Revenue",
                value = formatInr(metrics.todayRevenue),
              )
              SummaryHeroStat(
                label = "Bikes Sold",
                value = metrics.bikesSoldToday.toString(),
              )
              SummaryHeroStat(
                label = "Services",
                value = metrics.servicesToday.toString(),
              )
            }
          }
        }
      }
    }

    // Low Stock Alert Banner
    if (metrics.lowStockPartsCount > 0) {
      item {
        Surface(
          onClick = onNavigateToInventory,
          color = AmberContainer,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("low_stock_alert_banner"),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Default.WarningAmber,
              contentDescription = "Low Stock Alert",
              tint = OnAmberContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${metrics.lowStockPartsCount} Spare Part(s) Below Threshold",
                style = MaterialTheme.typography.titleSmall,
                color = OnAmberContainer,
              )
              Text(
                text = "Tap to restock Hero Genuine Parts",
                style = MaterialTheme.typography.bodySmall,
                color = OnAmberContainer.copy(alpha = 0.85f),
              )
            }
            Text(
              text = "View",
              style = MaterialTheme.typography.labelLarge,
              color = OnAmberContainer,
            )
          }
        }
      }
    }

    // Quick Actions
    item {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AssistChip(
          onClick = onNavigateToSales,
          label = { Text("New Sale / CRM") },
          leadingIcon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, Modifier.size(16.dp)) },
          modifier = Modifier.testTag("quick_action_sale"),
        )
        AssistChip(
          onClick = onNavigateToWorkshop,
          label = { Text("Workshop Jobs") },
          leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, Modifier.size(16.dp)) },
          modifier = Modifier.testTag("quick_action_job"),
        )
        AssistChip(
          onClick = onNavigateToInventory,
          label = { Text("Add Stock") },
          leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null, Modifier.size(16.dp)) },
          modifier = Modifier.testTag("quick_action_stock"),
        )
        AssistChip(
          onClick = onNavigateToStaff,
          label = { Text("Staff Attendance") },
          leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, Modifier.size(16.dp)) },
          modifier = Modifier.testTag("quick_action_staff"),
        )
      }
    }

    // KPI Grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          KpiMetricCard(
            title = "Bikes In Stock",
            value = metrics.inStockBikesCount.toString(),
            subtitle = "Ready for delivery",
            icon = Icons.AutoMirrored.Filled.DirectionsBike,
            accentColor = HeroCrimson,
            onClick = onNavigateToInventory,
            modifier = Modifier.weight(1f),
          )
          KpiMetricCard(
            title = "Active Job Cards",
            value = metrics.activeJobCardsCount.toString(),
            subtitle = "In workshop bays",
            icon = Icons.Default.Build,
            accentColor = AmberAccent,
            onClick = onNavigateToWorkshop,
            modifier = Modifier.weight(1f),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          KpiMetricCard(
            title = "Total Sales Rev",
            value = formatInr(metrics.totalSalesRevenue),
            subtitle = "Showroom billing",
            icon = Icons.Default.CurrencyRupee,
            accentColor = EmeraldSuccess,
            onClick = onNavigateToSales,
            modifier = Modifier.weight(1f),
          )
          KpiMetricCard(
            title = "Staff Present",
            value = metrics.presentStaffToday.toString(),
            subtitle = "Checked in today",
            icon = Icons.Default.Groups,
            accentColor = IndigoInfo,
            onClick = onNavigateToStaff,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // Financial Chart + Leaderboards (side by side on wide screens)
    item {
      if (isWideScreen) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          FinancialPerformanceChart(
            salesProfit = metrics.salesProfit,
            workshopProfit = metrics.workshopProfit,
            totalExpenses = metrics.totalExpenses,
            modifier = Modifier.weight(1.1f),
          )
          TopPerformersCard(
            topModels = metrics.topModels,
            topStaff = metrics.topStaff.map { it.name to it.achievedCount.toInt() },
            modifier = Modifier.weight(0.9f),
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          FinancialPerformanceChart(
            salesProfit = metrics.salesProfit,
            workshopProfit = metrics.workshopProfit,
            totalExpenses = metrics.totalExpenses,
          )
          TopPerformersCard(
            topModels = metrics.topModels,
            topStaff = metrics.topStaff.map { it.name to it.achievedCount.toInt() },
          )
        }
      }
    }

    // Operating Expenses Section (Admin & Manager focus)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Showroom & Workshop Expenses",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Total recorded: ${formatInr(metrics.totalExpenses)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        FilledTonalButton(
          onClick = { showAddExpenseDialog = true },
          modifier = Modifier.height(48.dp).testTag("add_expense_button"),
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Expense")
        }
      }
    }

    item {
      val list = (expensesState as? UiState.Success)?.data.orEmpty()
      if (list.isEmpty()) {
        EmptyStateCard(
          icon = Icons.Default.CurrencyRupee,
          title = "No operating expenses logged",
          subtitle = "Record showroom electricity, rent, freight, or workshop supplies to track net profit.",
          actionLabel = "Record Expense",
          onAction = { showAddExpenseDialog = true },
        )
      } else {
        OutlinedCard(
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            list.take(6).forEachIndexed { idx, expense ->
              if (idx > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(expense.title, style = MaterialTheme.typography.titleSmall)
                  Text(
                    "${expense.category} • ${expense.expenseDateKey}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = formatInr(expense.amount),
                  style = MaterialTheme.typography.titleSmall,
                  color = HeroCrimson,
                  fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { onDeleteExpense(expense.id) }) {
                  Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete expense",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddExpenseDialog) {
    AddExpenseDialog(
      onDismiss = { showAddExpenseDialog = false },
      onConfirm = { title, category, amount ->
        onAddExpense(title, category, amount)
        showAddExpenseDialog = false
      },
    )
  }
}

@Composable
private fun SummaryHeroStat(label: String, value: String) {
  Column {
    Text(
      text = value,
      style = MaterialTheme.typography.headlineSmall,
      color = Color.White,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = Color.White.copy(alpha = 0.78f),
    )
  }
}

@Composable
private fun TopPerformersCard(
  topModels: List<Pair<String, Int>>,
  topStaff: List<Pair<String, Int>>,
  modifier: Modifier = Modifier,
) {
  OutlinedCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AmberAccent)
        Text("Top Models & Star Staff", style = MaterialTheme.typography.titleMedium)
      }

      Text(
        "Best-Selling Hero Models",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (topModels.isEmpty()) {
        Text(
          "Record bike deliveries to see top-performing models.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        topModels.forEach { (model, count) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(model, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            StatusPill(
              text = "$count sold",
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      HorizontalDivider()

      Text(
        "Staff Leaderboard",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (topStaff.isEmpty()) {
        Text(
          "Add staff members to track monthly targets and incentives.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        topStaff.forEach { (name, achieved) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
              "$achieved units",
              style = MaterialTheme.typography.labelLarge,
              color = EmeraldSuccess,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddExpenseDialog(
  onDismiss: () -> Unit,
  onConfirm: (title: String, category: String, amount: Double) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("ELECTRICITY") }
  var amountText by remember { mutableStateOf("") }
  val categories = listOf("ELECTRICITY", "RENT", "TRANSPORT", "MARKETING", "MISC")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Record Showroom Expense") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Expense Title") },
          placeholder = { Text("e.g., Purnia Freight / Generator Fuel") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("expense_title_input"),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          categories.forEach { cat ->
            AssistChip(
              onClick = { category = cat },
              label = { Text(cat) },
              border =
                if (category == cat) {
                  androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                  null
                },
            )
          }
        }
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text("Amount (INR)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("expense_amount_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull() ?: 0.0
          if (title.isNotBlank() && amt > 0.0) {
            onConfirm(title, category, amt)
          }
        },
        enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
