package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BikeStatus
import com.example.data.model.BikeStock
import com.example.data.model.CustomerLead
import com.example.data.model.HERO_MODEL_CATALOG
import com.example.data.model.InsuranceStatus
import com.example.data.model.LeadStatus
import com.example.data.model.PaymentMode
import com.example.data.model.RtoStatus
import com.example.data.model.SaleRecord
import com.example.data.model.StaffMember
import com.example.data.model.StaffRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatInr
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OnIndigoContainer
import com.example.ui.viewmodel.UiState

@Composable
fun SalesCrmScreen(
  salesState: UiState<List<SaleRecord>>,
  customersState: UiState<List<CustomerLead>>,
  bikesState: UiState<List<BikeStock>>,
  staffState: UiState<List<StaffMember>>,
  onRecordSale: (
    bike: BikeStock?,
    modelName: String,
    chassisNumber: String,
    engineNumber: String,
    customerId: String,
    customerName: String,
    customerPhone: String,
    salesExec: StaffMember?,
    paymentMode: PaymentMode,
    financierName: String,
    downPayment: Double,
    totalAmount: Double,
    rtoStatus: RtoStatus,
    insuranceStatus: InsuranceStatus,
  ) -> Unit,
  onUpdateSaleCompliance: (String, RtoStatus, InsuranceStatus) -> Unit,
  onAddCustomerLead: (
    name: String,
    phone: String,
    address: String,
    kycType: String,
    kycNumber: String,
    interestedModel: String,
    leadStatus: LeadStatus,
    testRideDate: String,
  ) -> Unit,
  onUpdateLeadStatus: (String, LeadStatus) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var paymentFilter by remember { mutableStateOf<PaymentMode?>(null) }
  var showNewSaleDialog by remember { mutableStateOf(false) }
  var showNewLeadDialog by remember { mutableStateOf(false) }
  var selectedInvoiceSale by remember { mutableStateOf<SaleRecord?>(null) }

  val inStockBikes =
    (bikesState as? UiState.Success)?.data?.filter { it.status != BikeStatus.SOLD.name }.orEmpty()
  val salesStaff =
    (staffState as? UiState.Success)?.data?.filter {
      it.role == StaffRole.SALES_EXECUTIVE.name || it.role == StaffRole.ADMIN.name
    }.orEmpty()
  val customerList = (customersState as? UiState.Success)?.data.orEmpty()

  Scaffold(
    modifier = modifier,
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          if (selectedTab == 0) showNewSaleDialog = true else showNewLeadDialog = true
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        modifier = Modifier.testTag("sales_crm_fab"),
      ) {
        Icon(Icons.Default.Add, contentDescription = if (selectedTab == 0) "New Sale" else "Add Customer")
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
          modifier = Modifier.testTag("tab_sales_invoices"),
        ) {
          Text("Sales & Invoices")
        }
        SegmentedButton(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          modifier = Modifier.testTag("tab_customers_crm"),
        ) {
          Text("CRM & Leads")
        }
      }

      if (selectedTab == 0) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = paymentFilter == null,
            onClick = { paymentFilter = null },
            label = { Text("All Modes") },
          )
          PaymentMode.entries.forEach { mode ->
            FilterChip(
              selected = paymentFilter == mode,
              onClick = { paymentFilter = if (paymentFilter == mode) null else mode },
              label = { Text(mode.label) },
            )
          }
        }

        when (salesState) {
          UiState.Loading -> CenteredLoading()
          is UiState.Error -> Text(salesState.message, color = MaterialTheme.colorScheme.error)
          is UiState.Success -> {
            val filtered =
              salesState.data.filter { sale ->
                paymentFilter == null || sale.paymentMode == paymentFilter?.name
              }
            if (filtered.isEmpty()) {
              EmptyStateCard(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "No vehicle sales recorded",
                subtitle = "Record Cash, Cheque, or Finance deliveries to generate GST Tax Invoices, Gate Passes & track RTO/Insurance.",
                actionLabel = "Record Bike Sale",
                onAction = { showNewSaleDialog = true },
              )
            } else {
              LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(filtered, key = { it.id }) { sale ->
                  SaleRecordCard(
                    sale = sale,
                    onOpenInvoice = { selectedInvoiceSale = sale },
                    onAdvanceRto = {
                      val nextRto =
                        when (RtoStatus.fromString(sale.rtoStatus)) {
                          RtoStatus.PENDING -> RtoStatus.APPLIED
                          RtoStatus.APPLIED -> RtoStatus.NUMBER_ALLOTTED
                          RtoStatus.NUMBER_ALLOTTED -> RtoStatus.NUMBER_ALLOTTED
                        }
                      onUpdateSaleCompliance(
                        sale.id,
                        nextRto,
                        InsuranceStatus.fromString(sale.insuranceStatus),
                      )
                    },
                    onToggleInsurance = {
                      val nextIns =
                        if (sale.insuranceStatus == InsuranceStatus.ISSUED.name) {
                          InsuranceStatus.PENDING
                        } else {
                          InsuranceStatus.ISSUED
                        }
                      onUpdateSaleCompliance(
                        sale.id,
                        RtoStatus.fromString(sale.rtoStatus),
                        nextIns,
                      )
                    },
                  )
                }
              }
            }
          }
        }
      } else {
        when (customersState) {
          UiState.Loading -> CenteredLoading()
          is UiState.Error -> Text(customersState.message, color = MaterialTheme.colorScheme.error)
          is UiState.Success -> {
            if (customersState.data.isEmpty()) {
              EmptyStateCard(
                icon = Icons.Default.PersonAdd,
                title = "No showroom leads or customers",
                subtitle = "Save customer phone, address, Aadhaar/PAN KYC, test rides, and quotation status.",
                actionLabel = "Add Customer Inquiry",
                onAction = { showNewLeadDialog = true },
              )
            } else {
              LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(customersState.data, key = { it.id }) { customer ->
                  CustomerLeadCard(
                    customer = customer,
                    onAdvanceLead = {
                      val nextStatus =
                        when (LeadStatus.fromString(customer.leadStatus)) {
                          LeadStatus.INQUIRY -> LeadStatus.TEST_RIDE
                          LeadStatus.TEST_RIDE -> LeadStatus.QUOTATION_SENT
                          LeadStatus.QUOTATION_SENT -> LeadStatus.CONVERTED
                          LeadStatus.CONVERTED -> LeadStatus.CONVERTED
                        }
                      onUpdateLeadStatus(customer.id, nextStatus)
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showNewSaleDialog) {
    RecordSaleDialog(
      inStockBikes = inStockBikes,
      customers = customerList,
      salesStaff = salesStaff,
      onDismiss = { showNewSaleDialog = false },
      onConfirm = { bike, model, chassis, engine, custId, custName, custPhone, exec, mode, fin, down, total ->
        onRecordSale(
          bike,
          model,
          chassis,
          engine,
          custId,
          custName,
          custPhone,
          exec,
          mode,
          fin,
          down,
          total,
          RtoStatus.PENDING,
          InsuranceStatus.ISSUED,
        )
        showNewSaleDialog = false
      },
    )
  }

  if (showNewLeadDialog) {
    AddCustomerLeadDialog(
      onDismiss = { showNewLeadDialog = false },
      onConfirm = { name, phone, address, kycType, kycNum, model, status, testRideDate ->
        onAddCustomerLead(name, phone, address, kycType, kycNum, model, status, testRideDate)
        showNewLeadDialog = false
      },
    )
  }

  selectedInvoiceSale?.let { sale ->
    TaxInvoiceAndGatePassDialog(
      sale = sale,
      onDismiss = { selectedInvoiceSale = null },
    )
  }
}

@Composable
private fun SaleRecordCard(
  sale: SaleRecord,
  onOpenInvoice: () -> Unit,
  onAdvanceRto: () -> Unit,
  onToggleInsurance: () -> Unit,
) {
  val payment = PaymentMode.fromString(sale.paymentMode)
  val rto = RtoStatus.fromString(sale.rtoStatus)
  val ins = InsuranceStatus.fromString(sale.insuranceStatus)

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
            text = "${sale.invoiceNumber} • ${sale.gatePassNumber}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(sale.customerName, style = MaterialTheme.typography.titleMedium)
          Text(
            "${sale.modelName} • ${sale.customerPhone}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = formatInr(sale.totalAmount),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = EmeraldSuccess,
        )
      }

      Text(
        "Chassis: ${sale.chassisNumber} • Exec: ${sale.salesExecName}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        StatusPill(
          text =
            if (payment == PaymentMode.FINANCE && sale.financierName.isNotBlank()) {
              "${payment.label}: ${sale.financierName}"
            } else {
              payment.label
            },
          containerColor = IndigoContainer,
          contentColor = OnIndigoContainer,
        )
        StatusPill(
          text = "RTO: ${rto.label}",
          containerColor = if (rto == RtoStatus.NUMBER_ALLOTTED) EmeraldContainer else AmberContainer,
          contentColor = if (rto == RtoStatus.NUMBER_ALLOTTED) OnEmeraldContainer else OnAmberContainer,
        )
        StatusPill(
          text = "Ins: ${ins.label}",
          containerColor = if (ins == InsuranceStatus.ISSUED) EmeraldContainer else AmberContainer,
          contentColor = if (ins == InsuranceStatus.ISSUED) OnEmeraldContainer else OnAmberContainer,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        TextButton(onClick = onAdvanceRto) {
          Text(if (rto == RtoStatus.NUMBER_ALLOTTED) "RTO Done" else "Update RTO")
        }
        TextButton(onClick = onToggleInsurance) {
          Text(if (ins == InsuranceStatus.ISSUED) "Ins Issued" else "Issue Ins")
        }
        FilledTonalButton(onClick = onOpenInvoice) {
          Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Invoice & Gate Pass")
        }
      }
    }
  }
}

@Composable
private fun CustomerLeadCard(
  customer: CustomerLead,
  onAdvanceLead: () -> Unit,
) {
  val status = LeadStatus.fromString(customer.leadStatus)
  OutlinedCard(
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(customer.name, style = MaterialTheme.typography.titleMedium)
          Text(
            "${customer.phone} • ${customer.address}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        StatusPill(
          text = status.label,
          containerColor = if (status == LeadStatus.CONVERTED) EmeraldContainer else AmberContainer,
          contentColor = if (status == LeadStatus.CONVERTED) OnEmeraldContainer else OnAmberContainer,
        )
      }
      Text(
        "Model: ${customer.interestedModel} | KYC: ${customer.kycType} (${customer.kycNumber.ifBlank { "Pending" }})",
        style = MaterialTheme.typography.bodySmall,
      )
      if (customer.testRideDate.isNotBlank()) {
        Text(
          "Test Ride Scheduled: ${customer.testRideDate}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      if (status != LeadStatus.CONVERTED) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onAdvanceLead) {
            Text(
              when (status) {
                LeadStatus.INQUIRY -> "Schedule Test Ride"
                LeadStatus.TEST_RIDE -> "Send Quotation"
                LeadStatus.QUOTATION_SENT -> "Mark Converted"
                LeadStatus.CONVERTED -> "Converted"
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TaxInvoiceAndGatePassDialog(
  sale: SaleRecord,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val invoiceText =
    """
    SUDHA HERO • AUTHORIZED HERO MOTOCORP DEALER
    Mohanpur Bazar, Purnia, Bihar
    ------------------------------------------
    TAX INVOICE: ${sale.invoiceNumber}
    DELIVERY GATE PASS: ${sale.gatePassNumber}
    Date: ${sale.saleDateKey}
    ------------------------------------------
    Customer: ${sale.customerName} (${sale.customerPhone})
    Model: ${sale.modelName}
    Chassis No: ${sale.chassisNumber}
    Engine No: ${sale.engineNumber}
    Sales Exec: ${sale.salesExecName}
    ------------------------------------------
    Payment Mode: ${sale.paymentMode} ${if (sale.financierName.isNotBlank()) "(${sale.financierName})" else ""}
    Taxable Value: ${formatInr(sale.taxableAmount)}
    GST (28%): ${formatInr(sale.gstAmount)}
    TOTAL INVOICE VALUE: ${formatInr(sale.totalAmount)}
    ------------------------------------------
    RTO Status: ${sale.rtoStatus} | Insurance: ${sale.insuranceStatus}
    Authorized Signatory - Sudha Hero
    """.trimIndent()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Tax Invoice & Gate Pass") },
    text = {
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
      ) {
        Column(
          modifier = Modifier.padding(14.dp).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text("SUDHA HERO • MOHANPUR BAZAR, PURNIA", style = MaterialTheme.typography.titleSmall)
          HorizontalDivider()
          Text("Invoice: ${sale.invoiceNumber}", style = MaterialTheme.typography.labelSmall)
          Text("Gate Pass: ${sale.gatePassNumber}", style = MaterialTheme.typography.labelSmall)
          Text("Customer: ${sale.customerName} (${sale.customerPhone})", style = MaterialTheme.typography.bodySmall)
          Text("Vehicle: ${sale.modelName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
          Text("Chassis: ${sale.chassisNumber}", style = MaterialTheme.typography.labelSmall)
          Text("Engine: ${sale.engineNumber}", style = MaterialTheme.typography.labelSmall)
          HorizontalDivider()
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Taxable Value", style = MaterialTheme.typography.bodySmall)
            Text(formatInr(sale.taxableAmount), style = MaterialTheme.typography.bodySmall)
          }
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("CGST + SGST (28%)", style = MaterialTheme.typography.bodySmall)
            Text(formatInr(sale.gstAmount), style = MaterialTheme.typography.bodySmall)
          }
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Grand Total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(formatInr(sale.totalAmount), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val intent =
            Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_SUBJECT, "Sudha Hero Invoice ${sale.invoiceNumber}")
              putExtra(Intent.EXTRA_TEXT, invoiceText)
            }
          context.startActivity(Intent.createChooser(intent, "Share Tax Invoice & Gate Pass"))
        }
      ) {
        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Share / Print")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordSaleDialog(
  inStockBikes: List<BikeStock>,
  customers: List<CustomerLead>,
  salesStaff: List<StaffMember>,
  onDismiss: () -> Unit,
  onConfirm: (
    BikeStock?,
    String,
    String,
    String,
    String,
    String,
    String,
    StaffMember?,
    PaymentMode,
    String,
    Double,
    Double,
  ) -> Unit,
) {
  var selectedBike by remember { mutableStateOf(inStockBikes.firstOrNull()) }
  var selectedCustomer by remember { mutableStateOf<CustomerLead?>(null) }
  var selectedExec by remember { mutableStateOf(salesStaff.firstOrNull()) }
  var modelName by remember { mutableStateOf(selectedBike?.modelName ?: "Splendor+") }
  var chassis by remember { mutableStateOf(selectedBike?.chassisNumber ?: "") }
  var engine by remember { mutableStateOf(selectedBike?.engineNumber ?: "") }
  var custName by remember { mutableStateOf("") }
  var custPhone by remember { mutableStateOf("") }
  var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
  var financierName by remember { mutableStateOf("Hero FinCorp") }
  var downPayment by remember { mutableStateOf("20000") }
  var totalAmount by remember {
    mutableStateOf((selectedBike?.onRoadPrice?.toLong() ?: 91200L).toString())
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Vehicle Sale & Invoice") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (inStockBikes.isNotEmpty()) {
          Text("Select Stock Bike:", style = MaterialTheme.typography.labelMedium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            inStockBikes.take(6).forEach { bike ->
              AssistChip(
                onClick = {
                  selectedBike = bike
                  modelName = bike.modelName
                  chassis = bike.chassisNumber
                  engine = bike.engineNumber
                  totalAmount = bike.onRoadPrice.toLong().toString()
                },
                label = { Text("${bike.modelName} (${bike.chassisNumber.takeLast(4)})") },
                border =
                  if (selectedBike?.id == bike.id) {
                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                  } else {
                    null
                  },
              )
            }
          }
        }
        if (customers.isNotEmpty()) {
          Text("Or Pick CRM Customer:", style = MaterialTheme.typography.labelMedium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            customers.take(5).forEach { lead ->
              AssistChip(
                onClick = {
                  selectedCustomer = lead
                  custName = lead.name
                  custPhone = lead.phone
                },
                label = { Text(lead.name) },
              )
            }
          }
        }
        OutlinedTextField(
          value = custName,
          onValueChange = { custName = it },
          label = { Text("Customer Full Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = custPhone,
          onValueChange = { custPhone = it },
          label = { Text("Customer Phone *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = modelName,
          onValueChange = { modelName = it },
          label = { Text("Model Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = chassis,
            onValueChange = { chassis = it.uppercase() },
            label = { Text("Chassis *") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = engine,
            onValueChange = { engine = it.uppercase() },
            label = { Text("Engine *") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        if (salesStaff.isNotEmpty()) {
          Text("Sales Executive:", style = MaterialTheme.typography.labelMedium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            salesStaff.forEach { exec ->
              AssistChip(
                onClick = { selectedExec = exec },
                label = { Text(exec.name) },
                border =
                  if (selectedExec?.id == exec.id) {
                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                  } else {
                    null
                  },
              )
            }
          }
        }

        Text("Payment Mode:", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          PaymentMode.entries.forEach { mode ->
            AssistChip(
              onClick = { paymentMode = mode },
              label = { Text(mode.label) },
              border =
                if (paymentMode == mode) {
                  androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                  null
                },
            )
          }
        }

        if (paymentMode == PaymentMode.FINANCE) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = financierName,
              onValueChange = { financierName = it },
              label = { Text("Financier") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = downPayment,
              onValueChange = { downPayment = it.filter { c -> c.isDigit() } },
              label = { Text("Down Pay ₹") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
        }

        OutlinedTextField(
          value = totalAmount,
          onValueChange = { totalAmount = it.filter { c -> c.isDigit() } },
          label = { Text("Total On-Road Amount (INR) *") },
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
            selectedBike,
            modelName,
            chassis,
            engine,
            selectedCustomer?.id ?: "",
            custName,
            custPhone,
            selectedExec,
            paymentMode,
            if (paymentMode == PaymentMode.FINANCE) financierName else "",
            if (paymentMode == PaymentMode.FINANCE) downPayment.toDoubleOrNull() ?: 0.0 else 0.0,
            totalAmount.toDoubleOrNull() ?: 0.0,
          )
        },
        enabled =
          custName.isNotBlank() &&
            custPhone.trim().length >= 5 &&
            chassis.trim().length >= 3 &&
            engine.trim().length >= 3 &&
            (totalAmount.toDoubleOrNull() ?: 0.0) > 0.0,
      ) {
        Text("Generate Invoice")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCustomerLeadDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, String, String, String, LeadStatus, String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("Mohanpur Bazar, Purnia") }
  var kycType by remember { mutableStateOf("Aadhaar") }
  var kycNumber by remember { mutableStateOf("") }
  var interestedModel by remember { mutableStateOf(HERO_MODEL_CATALOG.first().modelName) }
  var leadStatus by remember { mutableStateOf(LeadStatus.INQUIRY) }
  var testRideDate by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Customer Inquiry / KYC") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Customer Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("Address") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = kycType,
            onValueChange = { kycType = it },
            label = { Text("KYC Type") },
            singleLine = true,
            modifier = Modifier.weight(0.45f),
          )
          OutlinedTextField(
            value = kycNumber,
            onValueChange = { kycNumber = it },
            label = { Text("KYC ID No.") },
            singleLine = true,
            modifier = Modifier.weight(0.55f),
          )
        }
        OutlinedTextField(
          value = interestedModel,
          onValueChange = { interestedModel = it },
          label = { Text("Interested Hero Model") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(LeadStatus.INQUIRY, LeadStatus.TEST_RIDE, LeadStatus.QUOTATION_SENT).forEach { st ->
            AssistChip(
              onClick = { leadStatus = st },
              label = { Text(st.label) },
              border =
                if (leadStatus == st) {
                  androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                  null
                },
            )
          }
        }
        if (leadStatus == LeadStatus.TEST_RIDE) {
          OutlinedTextField(
            value = testRideDate,
            onValueChange = { testRideDate = it },
            label = { Text("Test Ride Date / Slot") },
            placeholder = { Text("e.g., 16 Nov • 4:00 PM") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(name, phone, address, kycType, kycNumber, interestedModel, leadStatus, testRideDate)
        },
        enabled = name.isNotBlank() && phone.trim().length >= 5,
      ) {
        Text("Save Lead")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
