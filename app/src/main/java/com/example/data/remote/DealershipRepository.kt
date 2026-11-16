package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
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
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DealershipRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun <T> observeWorkspaceCollection(
    collectionPath: String,
    uid: String,
    clazz: Class<T>,
  ): Flow<List<T>> = flow {
    emitAll(
      db.collection(collectionPath)
        .whereArrayContains("memberUids", uid)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(clazz, ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, collectionPath)
          }
          throw error
        }
    )
  }

  fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
    val path = "users/$uid"
    emitAll(
      db.collection("users")
        .document(uid)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observeBikes(uid: String): Flow<List<BikeStock>> =
    observeWorkspaceCollection("bikes", uid, BikeStock::class.java)
      .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }

  fun observeSpareParts(uid: String): Flow<List<SparePart>> =
    observeWorkspaceCollection("spare_parts", uid, SparePart::class.java)
      .map { list -> list.sortedBy { it.partName.lowercase(Locale.ROOT) } }

  fun observeStaff(uid: String): Flow<List<StaffMember>> =
    observeWorkspaceCollection("staff", uid, StaffMember::class.java)
      .map { list -> list.sortedBy { it.name.lowercase(Locale.ROOT) } }

  fun observeAttendance(uid: String): Flow<List<AttendanceRecord>> =
    observeWorkspaceCollection("attendance", uid, AttendanceRecord::class.java)
      .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }

  fun observeCustomers(uid: String): Flow<List<CustomerLead>> =
    observeWorkspaceCollection("customers", uid, CustomerLead::class.java)
      .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }

  fun observeSales(uid: String): Flow<List<SaleRecord>> =
    observeWorkspaceCollection("sales", uid, SaleRecord::class.java)
      .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }

  fun observeJobCards(uid: String): Flow<List<JobCard>> =
    observeWorkspaceCollection("job_cards", uid, JobCard::class.java)
      .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }

  fun observeExpenses(uid: String): Flow<List<ExpenseRecord>> =
    observeWorkspaceCollection("expenses", uid, ExpenseRecord::class.java)
      .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }

  // --- User Profile & Role ---

  suspend fun ensureUserProfile(
    displayName: String,
    email: String,
    defaultRole: StaffRole = StaffRole.ADMIN,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val payload =
          mapOf(
            "userId" to uid,
            "displayName" to displayName.ifBlank { "Showroom User" }.take(100),
            "email" to email.take(160),
            "role" to defaultRole.name,
            "dealershipName" to "Sudha Hero • Mohanpur Bazar, Purnia",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateUserRole(role: StaffRole): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      docRef.update(mapOf("role" to role.name, "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- Bike Stock ---

  suspend fun addBike(
    modelName: String,
    variant: String,
    color: String,
    chassisNumber: String,
    engineNumber: String,
    exShowroomPrice: Double,
    onRoadPrice: Double,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("bikes").document()
    val payload =
      mapOf(
        "ownerId" to uid,
        "memberUids" to listOf(uid),
        "modelName" to modelName.trim().take(80),
        "variant" to variant.trim().take(80),
        "color" to color.trim().take(50),
        "chassisNumber" to chassisNumber.trim().uppercase(Locale.ROOT).take(40),
        "engineNumber" to engineNumber.trim().uppercase(Locale.ROOT).take(40),
        "exShowroomPrice" to exShowroomPrice.coerceAtLeast(0.0),
        "onRoadPrice" to onRoadPrice.coerceAtLeast(0.0),
        "status" to BikeStatus.IN_STOCK.name,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateBikeStatus(bikeId: String, status: BikeStatus): Result<Unit> {
    requireUserId()
    val docRef = db.collection("bikes").document(bikeId)
    return try {
      docRef.update(mapOf("status" to status.name, "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteBike(bikeId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("bikes").document(bikeId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- Spare Parts ---

  suspend fun addSparePart(
    partNumber: String,
    partName: String,
    category: String,
    quantity: Long,
    minThreshold: Long,
    unitPrice: Double,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("spare_parts").document()
    val payload =
      mapOf(
        "ownerId" to uid,
        "memberUids" to listOf(uid),
        "partNumber" to partNumber.trim().uppercase(Locale.ROOT).take(50),
        "partName" to partName.trim().take(100),
        "category" to category.trim().take(50),
        "quantity" to quantity.coerceAtLeast(0L),
        "minThreshold" to minThreshold.coerceAtLeast(0L),
        "unitPrice" to unitPrice.coerceAtLeast(0.0),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateSparePartQuantity(partId: String, newQuantity: Long): Result<Unit> {
    requireUserId()
    val docRef = db.collection("spare_parts").document(partId)
    return try {
      docRef
        .update(
          mapOf(
            "quantity" to newQuantity.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteSparePart(partId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("spare_parts").document(partId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- Staff & HR ---

  suspend fun addStaffMember(
    name: String,
    phone: String,
    role: StaffRole,
    monthlySalary: Double,
    monthlyTarget: Long,
    incentivePerUnit: Double,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("staff").document()
    val payload =
      mapOf(
        "ownerId" to uid,
        "memberUids" to listOf(uid),
        "name" to name.trim().take(80),
        "phone" to phone.trim().take(20),
        "role" to role.name,
        "monthlySalary" to monthlySalary.coerceAtLeast(0.0),
        "advanceTaken" to 0.0,
        "monthlyTarget" to monthlyTarget.coerceAtLeast(0L),
        "achievedCount" to 0L,
        "incentivePerUnit" to incentivePerUnit.coerceAtLeast(0.0),
        "isActive" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun recordStaffAdvance(staffId: String, newAdvanceTotal: Double): Result<Unit> {
    requireUserId()
    val docRef = db.collection("staff").document(staffId)
    return try {
      docRef
        .update(
          mapOf(
            "advanceTaken" to newAdvanceTotal.coerceAtLeast(0.0),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteStaffMember(staffId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("staff").document(staffId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- Attendance ---

  suspend fun checkInStaff(staff: StaffMember): Result<String> {
    val uid = requireUserId()
    val dateKey = todayDateKey()
    val docId = "${staff.id}_$dateKey"
    val docRef = db.collection("attendance").document(docId)
    val payload =
      mapOf(
        "ownerId" to uid,
        "memberUids" to listOf(uid),
        "staffId" to staff.id,
        "staffName" to staff.name.take(80),
        "staffRole" to staff.role.take(40),
        "dateKey" to dateKey,
        "checkInTime" to currentFormattedTime(),
        "checkOutTime" to "",
        "status" to AttendanceStatus.PRESENT.name,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun checkOutAttendance(attendanceId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("attendance").document(attendanceId)
    return try {
      docRef
        .update(
          mapOf(
            "checkOutTime" to currentFormattedTime(),
            "status" to AttendanceStatus.CHECKED_OUT.name,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- Customer CRM ---

  suspend fun addCustomerLead(
    name: String,
    phone: String,
    address: String,
    kycType: String,
    kycNumber: String,
    interestedModel: String,
    leadStatus: LeadStatus,
    testRideDate: String,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("customers").document()
    val payload =
      mapOf(
        "ownerId" to uid,
        "memberUids" to listOf(uid),
        "name" to name.trim().take(100),
        "phone" to phone.trim().take(20),
        "address" to address.trim().take(250),
        "kycType" to kycType.trim().take(40),
        "kycNumber" to kycNumber.trim().take(40),
        "interestedModel" to interestedModel.trim().take(80),
        "leadStatus" to leadStatus.name,
        "testRideDate" to testRideDate.trim().take(30),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateCustomerLeadStatus(customerId: String, status: LeadStatus): Result<Unit> {
    requireUserId()
    val docRef = db.collection("customers").document(customerId)
    return try {
      docRef
        .update(mapOf("leadStatus" to status.name, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- Sales, Invoicing & Gate Pass ---

  suspend fun recordBikeSale(
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
  ): Result<String> {
    val uid = requireUserId()
    val saleDoc = db.collection("sales").document()
    val shortSuffix = saleDoc.id.take(4).uppercase(Locale.ROOT)
    val invoiceNo = "SH-PUR-${todayYearShort()}-$shortSuffix"
    val gatePassNo = "GP-${todayYearShort()}-$shortSuffix"
    val taxableAmount = (totalAmount / 1.28)
    val gstAmount = (totalAmount - taxableAmount).coerceAtLeast(0.0)

    val salePayload =
      mapOf(
        "ownerId" to uid,
        "memberUids" to listOf(uid),
        "invoiceNumber" to invoiceNo,
        "gatePassNumber" to gatePassNo,
        "bikeId" to (bike?.id ?: ""),
        "modelName" to (bike?.modelName ?: modelName).trim().take(80),
        "chassisNumber" to (bike?.chassisNumber ?: chassisNumber).trim().uppercase(Locale.ROOT).take(40),
        "engineNumber" to (bike?.engineNumber ?: engineNumber).trim().uppercase(Locale.ROOT).take(40),
        "customerId" to customerId.take(128),
        "customerName" to customerName.trim().take(100),
        "customerPhone" to customerPhone.trim().take(20),
        "salesExecId" to (salesExec?.id ?: ""),
        "salesExecName" to (salesExec?.name ?: "Showroom Counter").take(80),
        "paymentMode" to paymentMode.name,
        "financierName" to financierName.trim().take(80),
        "downPayment" to downPayment.coerceAtLeast(0.0),
        "totalAmount" to totalAmount.coerceAtLeast(0.0),
        "taxableAmount" to taxableAmount,
        "gstAmount" to gstAmount,
        "rtoStatus" to rtoStatus.name,
        "insuranceStatus" to insuranceStatus.name,
        "saleDateKey" to todayDateKey(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )

    return try {
      val batch = db.batch()
      batch.set(saleDoc, salePayload)
      if (bike != null && bike.id.isNotBlank()) {
        batch.update(
          db.collection("bikes").document(bike.id),
          mapOf("status" to BikeStatus.SOLD.name, "updatedAt" to FieldValue.serverTimestamp()),
        )
      }
      if (salesExec != null && salesExec.id.isNotBlank()) {
        batch.update(
          db.collection("staff").document(salesExec.id),
          mapOf(
            "achievedCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      if (customerId.isNotBlank()) {
        batch.update(
          db.collection("customers").document(customerId),
          mapOf(
            "leadStatus" to LeadStatus.CONVERTED.name,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
      Result.success(saleDoc.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, saleDoc.path)
      Result.failure(e)
    }
  }

  suspend fun updateSaleCompliance(
    saleId: String,
    rtoStatus: RtoStatus,
    insuranceStatus: InsuranceStatus,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection("sales").document(saleId)
    return try {
      docRef
        .update(
          mapOf(
            "rtoStatus" to rtoStatus.name,
            "insuranceStatus" to insuranceStatus.name,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- Workshop Job Cards ---

  suspend fun createJobCard(
    customerName: String,
    customerPhone: String,
    vehicleModel: String,
    registrationNumber: String,
    serviceType: ServiceType,
    issueDescription: String,
    requiredPartsSummary: String,
    mechanic: StaffMember?,
    estimatedCost: Double,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("job_cards").document()
    val jcNo = "JC-${todayYearShort()}-${docRef.id.take(4).uppercase(Locale.ROOT)}"
    val payload =
      mapOf(
        "ownerId" to uid,
        "memberUids" to listOf(uid),
        "jobCardNumber" to jcNo,
        "customerName" to customerName.trim().take(100),
        "customerPhone" to customerPhone.trim().take(20),
        "vehicleModel" to vehicleModel.trim().take(80),
        "registrationNumber" to registrationNumber.trim().uppercase(Locale.ROOT).take(30),
        "serviceType" to serviceType.name,
        "issueDescription" to issueDescription.trim().take(400),
        "requiredPartsSummary" to requiredPartsSummary.trim().take(300),
        "mechanicId" to (mechanic?.id ?: ""),
        "mechanicName" to (mechanic?.name ?: "Unassigned").take(80),
        "estimatedCost" to estimatedCost.coerceAtLeast(0.0),
        "finalCost" to 0.0,
        "status" to JobStatus.OPEN.name,
        "serviceDateKey" to todayDateKey(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateJobCardStatus(
    jobCard: JobCard,
    newStatus: JobStatus,
    finalCost: Double = jobCard.finalCost.takeIf { it > 0 } ?: jobCard.estimatedCost,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection("job_cards").document(jobCard.id)
    return try {
      val batch = db.batch()
      batch.update(
        docRef,
        mapOf(
          "status" to newStatus.name,
          "finalCost" to finalCost.coerceAtLeast(0.0),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      if (newStatus == JobStatus.DELIVERED && jobCard.status != JobStatus.DELIVERED.name && jobCard.mechanicId.isNotBlank()) {
        batch.update(
          db.collection("staff").document(jobCard.mechanicId),
          mapOf(
            "achievedCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- Expenses ---

  suspend fun addExpense(title: String, category: String, amount: Double): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("expenses").document()
    val payload =
      mapOf(
        "ownerId" to uid,
        "memberUids" to listOf(uid),
        "title" to title.trim().take(120),
        "category" to category.trim().take(50),
        "amount" to amount.coerceAtLeast(0.0),
        "expenseDateKey" to todayDateKey(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteExpense(expenseId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("expenses").document(expenseId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  companion object {
    fun todayDateKey(): String =
      SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun todayYearShort(): String =
      SimpleDateFormat("yy", Locale.US).format(Date())

    private fun currentFormattedTime(): String =
      SimpleDateFormat("hh:mm a", Locale.US).format(Date())
  }
}
