package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.HERO_MODEL_CATALOG
import com.example.data.model.JobCard
import com.example.data.model.JobStatus
import com.example.data.model.ServiceType
import com.example.data.model.StaffMember
import com.example.data.model.StaffRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatInr
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OnIndigoContainer
import com.example.ui.viewmodel.UiState

@Composable
fun WorkshopScreen(
  jobCardsState: UiState<List<JobCard>>,
  staffState: UiState<List<StaffMember>>,
  onCreateJobCard: (
    customerName: String,
    customerPhone: String,
    vehicleModel: String,
    registrationNumber: String,
    serviceType: ServiceType,
    issueDescription: String,
    requiredPartsSummary: String,
    mechanic: StaffMember?,
    estimatedCost: Double,
  ) -> Unit,
  onUpdateJobStatus: (JobCard, JobStatus, Double) -> Unit,
  modifier: Modifier = Modifier,
) {
  var statusFilter by remember { mutableStateOf<JobStatus?>(null) }
  var showCreateDialog by remember { mutableStateOf(false) }

  val mechanics =
    (staffState as? UiState.Success)?.data?.filter {
      it.role == StaffRole.MECHANIC.name || it.role == StaffRole.SERVICE_MANAGER.name
    }.orEmpty()

  Scaffold(
    modifier = modifier,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showCreateDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        modifier = Modifier.testTag("create_job_card_fab"),
      ) {
        Icon(Icons.Default.Add, contentDescription = "Create Job Card")
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
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = statusFilter == null,
          onClick = { statusFilter = null },
          label = { Text("All Jobs") },
        )
        JobStatus.entries.forEach { st ->
          FilterChip(
            selected = statusFilter == st,
            onClick = { statusFilter = if (statusFilter == st) null else st },
            label = { Text(st.label) },
          )
        }
      }

      when (jobCardsState) {
        UiState.Loading -> CenteredLoading()
        is UiState.Error -> Text(jobCardsState.message, color = MaterialTheme.colorScheme.error)
        is UiState.Success -> {
          val filtered =
            jobCardsState.data.filter { card ->
              statusFilter == null || card.status == statusFilter?.name
            }

          if (filtered.isEmpty()) {
            EmptyStateCard(
              icon = Icons.Default.Build,
              title = "No workshop job cards",
              subtitle = "Open digital job cards for Free & Paid services, track required spare parts, and assign mechanics.",
              actionLabel = "Create Job Card",
              onAction = { showCreateDialog = true },
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(bottom = 88.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(filtered, key = { it.id }) { card ->
                JobCardItem(
                  jobCard = card,
                  onAdvanceStatus = { nextStatus, finalCost ->
                    onUpdateJobStatus(card, nextStatus, finalCost)
                  },
                )
              }
            }
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    CreateJobCardDialog(
      mechanics = mechanics,
      onDismiss = { showCreateDialog = false },
      onConfirm = { name, phone, model, regNo, serviceType, issue, parts, mech, estCost ->
        onCreateJobCard(name, phone, model, regNo, serviceType, issue, parts, mech, estCost)
        showCreateDialog = false
      },
    )
  }
}

@Composable
private fun JobCardItem(
  jobCard: JobCard,
  onAdvanceStatus: (JobStatus, Double) -> Unit,
) {
  val status = JobStatus.fromString(jobCard.status)
  val serviceType = ServiceType.fromString(jobCard.serviceType)
  val (pillBg, pillFg) =
    when (status) {
      JobStatus.OPEN -> AmberContainer to OnAmberContainer
      JobStatus.IN_PROGRESS -> IndigoContainer to OnIndigoContainer
      JobStatus.READY -> EmeraldContainer to OnEmeraldContainer
      JobStatus.DELIVERED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
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
          Text(
            "${jobCard.jobCardNumber} • ${serviceType.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            "${jobCard.vehicleModel} (${jobCard.registrationNumber.ifBlank { "New Bike" }})",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            "${jobCard.customerName} • ${jobCard.customerPhone}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        StatusPill(text = status.label, containerColor = pillBg, contentColor = pillFg)
      }

      Text(
        "Issue: ${jobCard.issueDescription}",
        style = MaterialTheme.typography.bodyMedium,
      )
      if (jobCard.requiredPartsSummary.isNotBlank()) {
        Text(
          "Parts: ${jobCard.requiredPartsSummary}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            "Mechanic: ${jobCard.mechanicName}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
          )
          val costToShow = if (jobCard.finalCost > 0) jobCard.finalCost else jobCard.estimatedCost
          Text(
            if (status == JobStatus.DELIVERED) "Billed: ${formatInr(costToShow)}"
            else "Est. Cost: ${formatInr(costToShow)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        when (status) {
          JobStatus.OPEN -> {
            FilledTonalButton(onClick = { onAdvanceStatus(JobStatus.IN_PROGRESS, jobCard.estimatedCost) }) {
              Text("Start Work")
            }
          }
          JobStatus.IN_PROGRESS -> {
            FilledTonalButton(onClick = { onAdvanceStatus(JobStatus.READY, jobCard.estimatedCost) }) {
              Text("Mark Ready")
            }
          }
          JobStatus.READY -> {
            Button(onClick = { onAdvanceStatus(JobStatus.DELIVERED, jobCard.estimatedCost) }) {
              Text("Deliver & Bill")
            }
          }
          JobStatus.DELIVERED -> Unit
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateJobCardDialog(
  mechanics: List<StaffMember>,
  onDismiss: () -> Unit,
  onConfirm: (
    String,
    String,
    String,
    String,
    ServiceType,
    String,
    String,
    StaffMember?,
    Double,
  ) -> Unit,
) {
  var customerName by remember { mutableStateOf("") }
  var customerPhone by remember { mutableStateOf("") }
  var vehicleModel by remember { mutableStateOf(HERO_MODEL_CATALOG.first().modelName) }
  var regNo by remember { mutableStateOf("") }
  var serviceType by remember { mutableStateOf(ServiceType.FREE_SERVICE) }
  var issue by remember { mutableStateOf("") }
  var parts by remember { mutableStateOf("") }
  var selectedMech by remember { mutableStateOf(mechanics.firstOrNull()) }
  var estimatedCost by remember { mutableStateOf("450") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Workshop Job Card") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = customerName,
          onValueChange = { customerName = it },
          label = { Text("Customer Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = customerPhone,
          onValueChange = { customerPhone = it },
          label = { Text("Phone *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = vehicleModel,
            onValueChange = { vehicleModel = it },
            label = { Text("Bike Model *") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = regNo,
            onValueChange = { regNo = it.uppercase() },
            label = { Text("Reg. No.") },
            placeholder = { Text("BR11...") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ServiceType.entries.forEach { st ->
            AssistChip(
              onClick = { serviceType = st },
              label = { Text(st.label) },
              border =
                if (serviceType == st) {
                  androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                  null
                },
            )
          }
        }
        OutlinedTextField(
          value = issue,
          onValueChange = { issue = it },
          label = { Text("Reported Issue / Service Notes *") },
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = parts,
          onValueChange = { parts = it },
          label = { Text("Required Spare Parts") },
          placeholder = { Text("e.g., 10W30 Engine Oil, Oil Filter") },
          modifier = Modifier.fillMaxWidth(),
        )
        if (mechanics.isNotEmpty()) {
          Text("Assign Mechanic:", style = MaterialTheme.typography.labelMedium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            mechanics.forEach { mech ->
              AssistChip(
                onClick = { selectedMech = mech },
                label = { Text(mech.name) },
                border =
                  if (selectedMech?.id == mech.id) {
                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                  } else {
                    null
                  },
              )
            }
          }
        }
        OutlinedTextField(
          value = estimatedCost,
          onValueChange = { estimatedCost = it.filter { c -> c.isDigit() } },
          label = { Text("Estimated Cost (INR)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            customerName,
            customerPhone,
            vehicleModel,
            regNo,
            serviceType,
            issue,
            parts,
            selectedMech,
            estimatedCost.toDoubleOrNull() ?: 0.0,
          )
        },
        enabled =
          customerName.isNotBlank() &&
            customerPhone.trim().length >= 5 &&
            vehicleModel.isNotBlank() &&
            issue.isNotBlank(),
      ) {
        Text("Open Job Card")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
