package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.StaffMember
import com.example.data.model.StaffRole
import com.example.data.remote.DealershipRepository
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatInr
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.viewmodel.UiState

@Composable
fun StaffHrScreen(
  staffState: UiState<List<StaffMember>>,
  attendanceState: UiState<List<AttendanceRecord>>,
  onAddStaff: (
    name: String,
    phone: String,
    role: StaffRole,
    monthlySalary: Double,
    monthlyTarget: Long,
    incentivePerUnit: Double,
  ) -> Unit,
  onRecordAdvance: (StaffMember, Double) -> Unit,
  onDeleteStaff: (String) -> Unit,
  onCheckIn: (StaffMember) -> Unit,
  onCheckOut: (AttendanceRecord) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var showAddStaffDialog by remember { mutableStateOf(false) }
  var advanceTargetStaff by remember { mutableStateOf<StaffMember?>(null) }

  val staffList = (staffState as? UiState.Success)?.data.orEmpty()
  val attendanceList = (attendanceState as? UiState.Success)?.data.orEmpty()
  val today = DealershipRepository.todayDateKey()

  Scaffold(
    modifier = modifier,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddStaffDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        modifier = Modifier.testTag("add_staff_fab"),
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Staff Member")
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        SegmentedButton(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          modifier = Modifier.testTag("tab_attendance"),
        ) {
          Text("Attendance")
        }
        SegmentedButton(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          modifier = Modifier.testTag("tab_payroll"),
        ) {
          Text("Payroll & Targets")
        }
      }

      if (selectedTab == 0) {
        if (staffState is UiState.Loading || attendanceState is UiState.Loading) {
          CenteredLoading()
        } else if (staffList.isEmpty()) {
          EmptyStateCard(
            icon = Icons.Default.Badge,
            title = "No showroom staff onboarded",
            subtitle = "Add Sales Executives, Service Managers, and Mechanics to track digital check-in/out and incentives.",
            actionLabel = "Add Staff Member",
            onAction = { showAddStaffDialog = true },
          )
        } else {
          val todayMap = attendanceList.filter { it.dateKey == today }.associateBy { it.staffId }
          LazyColumn(
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              Text(
                text = "Today's Shift Roster ($today)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            items(staffList, key = { it.id }) { member ->
              val todayRecord = todayMap[member.id]
              AttendanceStaffRowCard(
                member = member,
                todayRecord = todayRecord,
                onCheckIn = { onCheckIn(member) },
                onCheckOut = { todayRecord?.let(onCheckOut) },
              )
            }
          }
        }
      } else {
        when (staffState) {
          UiState.Loading -> CenteredLoading()
          is UiState.Error -> Text(staffState.message, color = MaterialTheme.colorScheme.error)
          is UiState.Success -> {
            if (staffState.data.isEmpty()) {
              EmptyStateCard(
                icon = Icons.Default.Groups,
                title = "No payroll profiles yet",
                subtitle = "Track monthly salary, advance payments, unit targets, and automatic commission incentives.",
                actionLabel = "Add Staff Member",
                onAction = { showAddStaffDialog = true },
              )
            } else {
              LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(staffState.data, key = { it.id }) { member ->
                  StaffPayrollCard(
                    member = member,
                    onRecordAdvance = { advanceTargetStaff = member },
                    onDelete = { onDeleteStaff(member.id) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddStaffDialog) {
    AddStaffDialog(
      onDismiss = { showAddStaffDialog = false },
      onConfirm = { name, phone, role, salary, target, incentive ->
        onAddStaff(name, phone, role, salary, target, incentive)
        showAddStaffDialog = false
      },
    )
  }

  advanceTargetStaff?.let { member ->
    RecordAdvanceDialog(
      staff = member,
      onDismiss = { advanceTargetStaff = null },
      onConfirm = { amount ->
        onRecordAdvance(member, amount)
        advanceTargetStaff = null
      },
    )
  }
}

@Composable
private fun AttendanceStaffRowCard(
  member: StaffMember,
  todayRecord: AttendanceRecord?,
  onCheckIn: () -> Unit,
  onCheckOut: () -> Unit,
) {
  val role = StaffRole.fromString(member.role)
  OutlinedCard(
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(member.name, style = MaterialTheme.typography.titleMedium)
        Text(role.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (todayRecord != null) {
          Text(
            text =
              if (todayRecord.checkOutTime.isNotBlank()) {
                "In: ${todayRecord.checkInTime} • Out: ${todayRecord.checkOutTime}"
              } else {
                "Checked In at ${todayRecord.checkInTime}"
              },
            style = MaterialTheme.typography.labelSmall,
            color = EmeraldSuccess,
          )
        }
      }

      when {
        todayRecord == null -> {
          Button(onClick = onCheckIn, modifier = Modifier.height(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Check In")
          }
        }
        todayRecord.status != AttendanceStatus.CHECKED_OUT.name -> {
          FilledTonalButton(onClick = onCheckOut, modifier = Modifier.height(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Check Out")
          }
        }
        else -> {
          StatusPill(
            text = "Shift Done",
            containerColor = EmeraldContainer,
            contentColor = OnEmeraldContainer,
          )
        }
      }
    }
  }
}

@Composable
private fun StaffPayrollCard(
  member: StaffMember,
  onRecordAdvance: () -> Unit,
  onDelete: () -> Unit,
) {
  val role = StaffRole.fromString(member.role)
  val progress =
    if (member.monthlyTarget > 0) {
      (member.achievedCount.toFloat() / member.monthlyTarget.toFloat()).coerceIn(0f, 1f)
    } else {
      0f
    }

  ElevatedCard(
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth(),
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
        Column(modifier = Modifier.weight(1f)) {
          Text(member.name, style = MaterialTheme.typography.titleMedium)
          Text(
            "${role.label} • ${member.phone}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        StatusPill(
          text = "Net: ${formatInr(member.netPayableSalary)}",
          containerColor = EmeraldContainer,
          contentColor = OnEmeraldContainer,
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
            "Target Progress: ${member.achievedCount} / ${member.monthlyTarget}",
            style = MaterialTheme.typography.labelMedium,
          )
          Text(
            "Incentive: ${formatInr(member.totalIncentiveEarned)}",
            style = MaterialTheme.typography.labelMedium,
            color = EmeraldSuccess,
            fontWeight = FontWeight.Bold,
          )
        }
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier.fillMaxWidth().height(8.dp),
        )
      }

      HorizontalDivider()

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("Base: ${formatInr(member.monthlySalary)}", style = MaterialTheme.typography.bodySmall)
          Text(
            "Advance: ${formatInr(member.advanceTaken)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (member.advanceTaken > 0) OnAmberContainer else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          TextButton(onClick = onRecordAdvance) { Text("+ Advance") }
          IconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete staff member")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddStaffDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, StaffRole, Double, Long, Double) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var role by remember { mutableStateOf(StaffRole.SALES_EXECUTIVE) }
  var salary by remember { mutableStateOf("16000") }
  var target by remember { mutableStateOf("12") }
  var incentive by remember { mutableStateOf("500") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Onboard Staff Member") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Staff Full Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          StaffRole.entries.forEach { r ->
            AssistChip(
              onClick = { role = r },
              label = { Text(r.shortLabel) },
              border =
                if (role == r) {
                  androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                  null
                },
            )
          }
        }
        OutlinedTextField(
          value = salary,
          onValueChange = { salary = it.filter { c -> c.isDigit() } },
          label = { Text("Monthly Salary (INR)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = target,
            onValueChange = { target = it.filter { c -> c.isDigit() } },
            label = { Text("Monthly Target") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = incentive,
            onValueChange = { incentive = it.filter { c -> c.isDigit() } },
            label = { Text("Incentive/Unit ₹") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            name,
            phone,
            role,
            salary.toDoubleOrNull() ?: 0.0,
            target.toLongOrNull() ?: 0L,
            incentive.toDoubleOrNull() ?: 0.0,
          )
        },
        enabled = name.isNotBlank() && phone.trim().length >= 5,
      ) {
        Text("Save Staff")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun RecordAdvanceDialog(
  staff: StaffMember,
  onDismiss: () -> Unit,
  onConfirm: (Double) -> Unit,
) {
  var amount by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Record Advance • ${staff.name}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          "Current advance taken: ${formatInr(staff.advanceTaken)}",
          style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
          value = amount,
          onValueChange = { amount = it.filter { c -> c.isDigit() } },
          label = { Text("Additional Advance (INR)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0) },
        enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0,
      ) {
        Text("Add Advance")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
