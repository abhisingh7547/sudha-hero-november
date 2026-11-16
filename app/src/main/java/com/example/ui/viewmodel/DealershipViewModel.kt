package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceRecord
import com.example.data.model.BikeStatus
import com.example.data.model.BikeStock
import com.example.data.model.CustomerLead
import com.example.data.model.ExpenseRecord
import com.example.data.model.InsuranceStatus
import com.example.data.model.JobCard
import com.example.data.model.JobStatus
import com.example.data.model.LeadStatus
import com.example.data.model.PaymentMode
import com.example.data.model.RtoStatus
import com.example.data.model.SaleRecord
import com.example.data.model.ServiceType
import com.example.data.model.SparePart
import com.example.data.model.StaffMember
import com.example.data.model.StaffRole
import com.example.data.model.UserProfile
import com.example.data.remote.DealershipRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

data class DealershipDashboardMetrics(
  val bikesSoldToday: Int = 0,
  val servicesToday: Int = 0,
  val todayRevenue: Double = 0.0,
  val totalSalesRevenue: Double = 0.0,
  val totalServiceRevenue: Double = 0.0,
  val totalExpenses: Double = 0.0,
  val salesProfit: Double = 0.0,
  val workshopProfit: Double = 0.0,
  val inStockBikesCount: Int = 0,
  val lowStockPartsCount: Int = 0,
  val activeJobCardsCount: Int = 0,
  val presentStaffToday: Int = 0,
  val topModels: List<Pair<String, Int>> = emptyList(),
  val topStaff: List<StaffMember> = emptyList(),
)

class DealershipViewModel(
  private val repository: DealershipRepository,
  private val currentUserId: String,
  private val userDisplayName: String,
  private val userEmail: String,
) : ViewModel() {

  private val _selectedRoleOverride = MutableStateFlow<StaffRole?>(null)
  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  val userProfile: StateFlow<UiState<UserProfile?>> =
    repository
      .observeUserProfile(currentUserId)
      .toUiState("Failed to load profile")

  val bikes: StateFlow<UiState<List<BikeStock>>> =
    repository.observeBikes(currentUserId).toUiState("Failed to load bike inventory")

  val spareParts: StateFlow<UiState<List<SparePart>>> =
    repository.observeSpareParts(currentUserId).toUiState("Failed to load spare parts")

  val staff: StateFlow<UiState<List<StaffMember>>> =
    repository.observeStaff(currentUserId).toUiState("Failed to load staff records")

  val attendance: StateFlow<UiState<List<AttendanceRecord>>> =
    repository.observeAttendance(currentUserId).toUiState("Failed to load attendance")

  val customers: StateFlow<UiState<List<CustomerLead>>> =
    repository.observeCustomers(currentUserId).toUiState("Failed to load customer CRM")

  val sales: StateFlow<UiState<List<SaleRecord>>> =
    repository.observeSales(currentUserId).toUiState("Failed to load sales records")

  val jobCards: StateFlow<UiState<List<JobCard>>> =
    repository.observeJobCards(currentUserId).toUiState("Failed to load job cards")

  val expenses: StateFlow<UiState<List<ExpenseRecord>>> =
    repository.observeExpenses(currentUserId).toUiState("Failed to load expenses")

  val activeRole: StateFlow<StaffRole> =
    combine(userProfile, _selectedRoleOverride) { profileState, overrideRole ->
        overrideRole
          ?: (profileState as? UiState.Success)?.data?.role?.let { StaffRole.fromString(it) }
          ?: StaffRole.ADMIN
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = StaffRole.ADMIN,
      )

  val metrics: StateFlow<DealershipDashboardMetrics> =
    combine(
        combine(bikes, spareParts, staff) { b, p, s -> Triple(b, p, s) },
        combine(attendance, sales, jobCards, expenses) { a, sl, jc, ex ->
          Quadruple(a, sl, jc, ex)
        },
      ) { (bikesState, partsState, staffState), (attState, salesState, jobsState, expState) ->
        val bikeList = (bikesState as? UiState.Success)?.data.orEmpty()
        val partList = (partsState as? UiState.Success)?.data.orEmpty()
        val staffList = (staffState as? UiState.Success)?.data.orEmpty()
        val attList = (attState as? UiState.Success)?.data.orEmpty()
        val salesList = (salesState as? UiState.Success)?.data.orEmpty()
        val jobList = (jobsState as? UiState.Success)?.data.orEmpty()
        val expenseList = (expState as? UiState.Success)?.data.orEmpty()

        val today = DealershipRepository.todayDateKey()
        val todaySales = salesList.filter { it.saleDateKey == today }
        val todayJobs = jobList.filter { it.serviceDateKey == today }
        val deliveredJobs = jobList.filter { it.status == JobStatus.DELIVERED.name || it.finalCost > 0.0 }

        val todaySalesRev = todaySales.sumOf { it.totalAmount }
        val todayServiceRev =
          todayJobs
            .filter { it.status == JobStatus.DELIVERED.name || it.finalCost > 0.0 }
            .sumOf { if (it.finalCost > 0) it.finalCost else it.estimatedCost }

        val totalSalesRev = salesList.sumOf { it.totalAmount }
        val totalServiceRev =
          deliveredJobs.sumOf { if (it.finalCost > 0) it.finalCost else it.estimatedCost }
        val totalExp = expenseList.sumOf { it.amount }

        // Dealership margin estimation: ~11% average vehicle dealer margin + ~38% workshop margin
        val salesGrossProfit = totalSalesRev * 0.11
        val workshopGrossProfit = totalServiceRev * 0.38

        val topModels =
          salesList
            .groupBy { it.modelName }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        val topStaff =
          staffList
            .sortedWith(
              compareByDescending<StaffMember> { it.achievedCount }.thenByDescending {
                it.totalIncentiveEarned
              }
            )
            .take(5)

        DealershipDashboardMetrics(
          bikesSoldToday = todaySales.size,
          servicesToday = todayJobs.size,
          todayRevenue = todaySalesRev + todayServiceRev,
          totalSalesRevenue = totalSalesRev,
          totalServiceRevenue = totalServiceRev,
          totalExpenses = totalExp,
          salesProfit = salesGrossProfit,
          workshopProfit = workshopGrossProfit,
          inStockBikesCount = bikeList.count { it.status == BikeStatus.IN_STOCK.name },
          lowStockPartsCount = partList.count { it.isLowStock },
          activeJobCardsCount = jobList.count { it.status != JobStatus.DELIVERED.name },
          presentStaffToday = attList.count { it.dateKey == today },
          topModels = topModels,
          topStaff = topStaff,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DealershipDashboardMetrics(),
      )

  init {
    viewModelScope.launch {
      repository.ensureUserProfile(
        displayName = userDisplayName,
        email = userEmail,
        defaultRole = StaffRole.ADMIN,
      )
    }
  }

  private fun <T> Flow<T>.toUiState(fallbackError: String): StateFlow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, fallbackError, rootError)
        emit(UiState.Error(rootError.localizedMessage ?: fallbackError))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  fun clearSnackbar() {
    _snackbarMessage.value = null
  }

  fun switchWorkspaceRole(role: StaffRole) {
    _selectedRoleOverride.value = role
    _snackbarMessage.value = "Switched workspace view to ${role.label}"
  }

  fun addBike(
    modelName: String,
    variant: String,
    color: String,
    chassisNumber: String,
    engineNumber: String,
    exShowroomPrice: Double,
    onRoadPrice: Double,
  ) = runAction("Bike added to showroom stock") {
    repository.addBike(modelName, variant, color, chassisNumber, engineNumber, exShowroomPrice, onRoadPrice)
  }

  fun updateBikeStatus(bikeId: String, status: BikeStatus) =
    runAction("Bike status updated to ${status.label}") {
      repository.updateBikeStatus(bikeId, status)
    }

  fun deleteBike(bikeId: String) = runAction("Bike removed from stock") {
    repository.deleteBike(bikeId)
  }

  fun addSparePart(
    partNumber: String,
    partName: String,
    category: String,
    quantity: Long,
    minThreshold: Long,
    unitPrice: Double,
  ) = runAction("Spare part added to inventory") {
    repository.addSparePart(partNumber, partName, category, quantity, minThreshold, unitPrice)
  }

  fun adjustSparePartQty(part: SparePart, delta: Long) =
    runAction("Updated ${part.partName} stock") {
      val nextQty = (part.quantity + delta).coerceAtLeast(0L)
      repository.updateSparePartQuantity(part.id, nextQty)
    }

  fun deleteSparePart(partId: String) = runAction("Spare part deleted") {
    repository.deleteSparePart(partId)
  }

  fun addStaffMember(
    name: String,
    phone: String,
    role: StaffRole,
    monthlySalary: Double,
    monthlyTarget: Long,
    incentivePerUnit: Double,
  ) = runAction("Staff member onboarded") {
    repository.addStaffMember(name, phone, role, monthlySalary, monthlyTarget, incentivePerUnit)
  }

  fun recordStaffAdvance(staff: StaffMember, additionalAdvance: Double) =
    runAction("Advance recorded for ${staff.name}") {
      repository.recordStaffAdvance(staff.id, staff.advanceTaken + additionalAdvance)
    }

  fun deleteStaffMember(staffId: String) = runAction("Staff member removed") {
    repository.deleteStaffMember(staffId)
  }

  fun checkInStaff(staff: StaffMember) = runAction("${staff.name} checked in") {
    repository.checkInStaff(staff)
  }

  fun checkOutAttendance(record: AttendanceRecord) = runAction("${record.staffName} checked out") {
    repository.checkOutAttendance(record.id)
  }

  fun addCustomerLead(
    name: String,
    phone: String,
    address: String,
    kycType: String,
    kycNumber: String,
    interestedModel: String,
    leadStatus: LeadStatus,
    testRideDate: String,
  ) = runAction("Customer profile saved") {
    repository.addCustomerLead(
      name,
      phone,
      address,
      kycType,
      kycNumber,
      interestedModel,
      leadStatus,
      testRideDate,
    )
  }

  fun updateCustomerLeadStatus(customerId: String, status: LeadStatus) =
    runAction("Lead updated to ${status.label}") {
      repository.updateCustomerLeadStatus(customerId, status)
    }

  fun recordBikeSale(
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
  ) = runAction("Tax invoice & gate pass generated") {
    repository.recordBikeSale(
      bike,
      modelName,
      chassisNumber,
      engineNumber,
      customerId,
      customerName,
      customerPhone,
      salesExec,
      paymentMode,
      financierName,
      downPayment,
      totalAmount,
      rtoStatus,
      insuranceStatus,
    )
  }

  fun updateSaleCompliance(
    saleId: String,
    rtoStatus: RtoStatus,
    insuranceStatus: InsuranceStatus,
  ) = runAction("RTO & Insurance status updated") {
    repository.updateSaleCompliance(saleId, rtoStatus, insuranceStatus)
  }

  fun createJobCard(
    customerName: String,
    customerPhone: String,
    vehicleModel: String,
    registrationNumber: String,
    serviceType: ServiceType,
    issueDescription: String,
    requiredPartsSummary: String,
    mechanic: StaffMember?,
    estimatedCost: Double,
  ) = runAction("Workshop job card created") {
    repository.createJobCard(
      customerName,
      customerPhone,
      vehicleModel,
      registrationNumber,
      serviceType,
      issueDescription,
      requiredPartsSummary,
      mechanic,
      estimatedCost,
    )
  }

  fun updateJobCardStatus(jobCard: JobCard, newStatus: JobStatus, finalCost: Double) =
    runAction("Job card ${jobCard.jobCardNumber} marked ${newStatus.label}") {
      repository.updateJobCardStatus(jobCard, newStatus, finalCost)
    }

  fun addExpense(title: String, category: String, amount: Double) =
    runAction("Expense recorded") { repository.addExpense(title, category, amount) }

  fun deleteExpense(expenseId: String) = runAction("Expense deleted") {
    repository.deleteExpense(expenseId)
  }

  private fun <T> runAction(successMessage: String, block: suspend () -> Result<T>) {
    viewModelScope.launch {
      _isBusy.value = true
      val result = block()
      _isBusy.value = false
      _snackbarMessage.value =
        if (result.isSuccess) {
          successMessage
        } else {
          result.exceptionOrNull()?.localizedMessage ?: "Operation failed"
        }
    }
  }

  private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

  private companion object {
    const val TAG = "DealershipVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
