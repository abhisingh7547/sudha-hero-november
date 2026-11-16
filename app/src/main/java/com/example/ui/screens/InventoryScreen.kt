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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.BikeStatus
import com.example.data.model.BikeStock
import com.example.data.model.HERO_MODEL_CATALOG
import com.example.data.model.SparePart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatInr
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HeroCrimson
import com.example.ui.theme.HeroCrimsonContainer
import com.example.ui.theme.HeroOnCrimsonContainer
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.viewmodel.UiState

@Composable
fun InventoryScreen(
  bikesState: UiState<List<BikeStock>>,
  partsState: UiState<List<SparePart>>,
  initialSubTab: Int = 0,
  onAddBike: (
    modelName: String,
    variant: String,
    color: String,
    chassisNumber: String,
    engineNumber: String,
    exShowroomPrice: Double,
    onRoadPrice: Double,
  ) -> Unit,
  onUpdateBikeStatus: (String, BikeStatus) -> Unit,
  onDeleteBike: (String) -> Unit,
  onAddSparePart: (
    partNumber: String,
    partName: String,
    category: String,
    quantity: Long,
    minThreshold: Long,
    unitPrice: Double,
  ) -> Unit,
  onAdjustPartQty: (SparePart, Long) -> Unit,
  onDeletePart: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedBikeFilter by remember { mutableStateOf<BikeStatus?>(null) }
  var lowStockOnly by remember { mutableStateOf(false) }
  var showAddBikeDialog by remember { mutableStateOf(false) }
  var showAddPartDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier,
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          if (selectedTab == 0) showAddBikeDialog = true else showAddPartDialog = true
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        modifier = Modifier.testTag("inventory_fab"),
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = if (selectedTab == 0) "Add Bike" else "Add Spare Part",
        )
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
          modifier = Modifier.testTag("tab_bikes"),
        ) {
          Text("Bike Stock")
        }
        SegmentedButton(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          modifier = Modifier.testTag("tab_spare_parts"),
        ) {
          Text("Spare Parts")
        }
      }

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = {
          Text(
            if (selectedTab == 0) "Search model, chassis, or engine no."
            else "Search part name, number, or category"
          )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("inventory_search_input"),
      )

      if (selectedTab == 0) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedBikeFilter == null,
            onClick = { selectedBikeFilter = null },
            label = { Text("All") },
          )
          BikeStatus.entries.forEach { status ->
            FilterChip(
              selected = selectedBikeFilter == status,
              onClick = {
                selectedBikeFilter = if (selectedBikeFilter == status) null else status
              },
              label = { Text(status.label) },
            )
          }
        }

        when (bikesState) {
          UiState.Loading -> CenteredLoading()
          is UiState.Error -> Text(bikesState.message, color = MaterialTheme.colorScheme.error)
          is UiState.Success -> {
            val filtered =
              bikesState.data.filter { bike ->
                val matchesStatus = selectedBikeFilter == null || bike.status == selectedBikeFilter?.name
                val q = searchQuery.trim()
                val matchesQuery =
                  q.isEmpty() ||
                    bike.modelName.contains(q, ignoreCase = true) ||
                    bike.chassisNumber.contains(q, ignoreCase = true) ||
                    bike.engineNumber.contains(q, ignoreCase = true) ||
                    bike.color.contains(q, ignoreCase = true)
                matchesStatus && matchesQuery
              }

            if (filtered.isEmpty()) {
              EmptyStateCard(
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                title = "No bikes in showroom stock",
                subtitle = "Add Hero MotoCorp units with chassis & engine numbers to manage real-time inventory.",
                actionLabel = "Add Bike Unit",
                onAction = { showAddBikeDialog = true },
              )
            } else {
              LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(filtered, key = { it.id }) { bike ->
                  BikeStockCard(
                    bike = bike,
                    onStatusChange = { next -> onUpdateBikeStatus(bike.id, next) },
                    onDelete = { onDeleteBike(bike.id) },
                  )
                }
              }
            }
          }
        }
      } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = !lowStockOnly,
            onClick = { lowStockOnly = false },
            label = { Text("All Parts") },
          )
          FilterChip(
            selected = lowStockOnly,
            onClick = { lowStockOnly = !lowStockOnly },
            label = { Text("Low Stock Alerts") },
            leadingIcon = {
              Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp))
            },
          )
        }

        when (partsState) {
          UiState.Loading -> CenteredLoading()
          is UiState.Error -> Text(partsState.message, color = MaterialTheme.colorScheme.error)
          is UiState.Success -> {
            val filtered =
              partsState.data.filter { part ->
                val matchesLow = !lowStockOnly || part.isLowStock
                val q = searchQuery.trim()
                val matchesQuery =
                  q.isEmpty() ||
                    part.partName.contains(q, ignoreCase = true) ||
                    part.partNumber.contains(q, ignoreCase = true) ||
                    part.category.contains(q, ignoreCase = true)
                matchesLow && matchesQuery
              }

            if (filtered.isEmpty()) {
              EmptyStateCard(
                icon = Icons.Default.BuildCircle,
                title = "No spare parts found",
                subtitle = "Track Hero Genuine Parts & accessories with automatic Low Stock Alert thresholds.",
                actionLabel = "Add Spare Part",
                onAction = { showAddPartDialog = true },
              )
            } else {
              LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(filtered, key = { it.id }) { part ->
                  SparePartCard(
                    part = part,
                    onIncrement = { onAdjustPartQty(part, +1L) },
                    onDecrement = { onAdjustPartQty(part, -1L) },
                    onDelete = { onDeletePart(part.id) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddBikeDialog) {
    AddBikeDialog(
      onDismiss = { showAddBikeDialog = false },
      onConfirm = { model, variant, color, chassis, engine, exPrice, onRoad ->
        onAddBike(model, variant, color, chassis, engine, exPrice, onRoad)
        showAddBikeDialog = false
      },
    )
  }

  if (showAddPartDialog) {
    AddSparePartDialog(
      onDismiss = { showAddPartDialog = false },
      onConfirm = { code, name, cat, qty, minThresh, price ->
        onAddSparePart(code, name, cat, qty, minThresh, price)
        showAddPartDialog = false
      },
    )
  }
}

@Composable
private fun BikeStockCard(
  bike: BikeStock,
  onStatusChange: (BikeStatus) -> Unit,
  onDelete: () -> Unit,
) {
  val status = BikeStatus.fromString(bike.status)
  val (pillBg, pillFg) =
    when (status) {
      BikeStatus.IN_STOCK -> EmeraldContainer to OnEmeraldContainer
      BikeStatus.RESERVED -> AmberContainer to OnAmberContainer
      BikeStatus.SOLD -> HeroCrimsonContainer to HeroOnCrimsonContainer
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
          Text(bike.modelName, style = MaterialTheme.typography.titleMedium)
          Text(
            "${bike.variant} • ${bike.color}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        StatusPill(text = status.label, containerColor = pillBg, contentColor = pillFg)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text("CHASSIS NO.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(bike.chassisNumber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("ENGINE NO.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(bike.engineNumber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            "Ex-Showroom: ${formatInr(bike.exShowroomPrice)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            "On-Road: ${formatInr(bike.onRoadPrice)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (status == BikeStatus.IN_STOCK) {
            TextButton(onClick = { onStatusChange(BikeStatus.RESERVED) }) { Text("Reserve") }
          } else if (status == BikeStatus.RESERVED) {
            TextButton(onClick = { onStatusChange(BikeStatus.IN_STOCK) }) { Text("Unreserve") }
          }
          IconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove bike")
          }
        }
      }
    }
  }
}

@Composable
private fun SparePartCard(
  part: SparePart,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onDelete: () -> Unit,
) {
  OutlinedCard(
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
          Text(part.partName, style = MaterialTheme.typography.titleMedium)
          Text(
            "${part.partNumber} • ${part.category}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (part.isLowStock) {
          StatusPill(
            text = "LOW STOCK ALERT",
            containerColor = HeroCrimson,
            contentColor = Color.White,
          )
        } else {
          StatusPill(
            text = "IN STOCK",
            containerColor = EmeraldContainer,
            contentColor = OnEmeraldContainer,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            "MRP: ${formatInr(part.unitPrice)} / unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            "Min Alert Limit: ${part.minThreshold}",
            style = MaterialTheme.typography.bodySmall,
            color = if (part.isLowStock) HeroCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onDecrement,
            enabled = part.quantity > 0,
            modifier = Modifier.size(48.dp),
          ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
          }
          Text(
            text = part.quantity.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp),
          )
          IconButton(
            onClick = onIncrement,
            modifier = Modifier.size(48.dp),
          ) {
            Icon(Icons.Default.Add, contentDescription = "Increase quantity")
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete part")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddBikeDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, String, String, Double, Double) -> Unit,
) {
  var modelName by remember { mutableStateOf("Splendor+") }
  var variant by remember { mutableStateOf("i3S Drum Self Cast") }
  var color by remember { mutableStateOf("Black with Silver") }
  var chassis by remember { mutableStateOf("") }
  var engine by remember { mutableStateOf("") }
  var exPrice by remember { mutableStateOf("76306") }
  var onRoadPrice by remember { mutableStateOf("91200") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Hero Bike to Stock") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          "Quick-Fill Hero Model:",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          HERO_MODEL_CATALOG.take(8).forEach { preset ->
            AssistChip(
              onClick = {
                modelName = preset.modelName
                variant = preset.defaultVariant
                color = preset.defaultColor
                exPrice = preset.exShowroomPrice.toLong().toString()
                onRoadPrice = preset.onRoadPrice.toLong().toString()
              },
              label = { Text(preset.modelName) },
            )
          }
        }
        OutlinedTextField(
          value = modelName,
          onValueChange = { modelName = it },
          label = { Text("Model Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("bike_model_input"),
        )
        OutlinedTextField(
          value = variant,
          onValueChange = { variant = it },
          label = { Text("Variant") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = color,
          onValueChange = { color = it },
          label = { Text("Color") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = chassis,
          onValueChange = { chassis = it.uppercase() },
          label = { Text("Chassis / VIN Number *") },
          placeholder = { Text("e.g., MBLHAW12345678901") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("bike_chassis_input"),
        )
        OutlinedTextField(
          value = engine,
          onValueChange = { engine = it.uppercase() },
          label = { Text("Engine Number *") },
          placeholder = { Text("e.g., HA10AG1234567") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("bike_engine_input"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = exPrice,
            onValueChange = { exPrice = it.filter { c -> c.isDigit() } },
            label = { Text("Ex-Showroom ₹") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = onRoadPrice,
            onValueChange = { onRoadPrice = it.filter { c -> c.isDigit() } },
            label = { Text("On-Road ₹") },
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
            modelName,
            variant,
            color,
            chassis,
            engine,
            exPrice.toDoubleOrNull() ?: 0.0,
            onRoadPrice.toDoubleOrNull() ?: 0.0,
          )
        },
        enabled = modelName.isNotBlank() && chassis.trim().length >= 3 && engine.trim().length >= 3,
        modifier = Modifier.testTag("confirm_add_bike_button"),
      ) {
        Text("Add Stock")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSparePartDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, Long, Long, Double) -> Unit,
) {
  var partNumber by remember { mutableStateOf("") }
  var partName by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Engine") }
  var quantity by remember { mutableStateOf("10") }
  var threshold by remember { mutableStateOf("5") }
  var price by remember { mutableStateOf("") }
  val categories = listOf("Engine", "Brakes", "Electrical", "Body", "Lubricants", "Accessories")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Spare Part") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = partNumber,
          onValueChange = { partNumber = it.uppercase() },
          label = { Text("Hero Part Code *") },
          placeholder = { Text("e.g., HGP-10W30-900") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = partName,
          onValueChange = { partName = it },
          label = { Text("Part / Accessory Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it.filter { c -> c.isDigit() } },
            label = { Text("Qty") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = threshold,
            onValueChange = { threshold = it.filter { c -> c.isDigit() } },
            label = { Text("Low Alert") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = price,
          onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
          label = { Text("Unit Price (INR) *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            partNumber,
            partName,
            category,
            quantity.toLongOrNull() ?: 0L,
            threshold.toLongOrNull() ?: 5L,
            price.toDoubleOrNull() ?: 0.0,
          )
        },
        enabled = partNumber.isNotBlank() && partName.isNotBlank() && (price.toDoubleOrNull() ?: 0.0) > 0.0,
      ) {
        Text("Save Part")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
internal fun CenteredLoading() {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CircularProgressIndicator()
  }
}
