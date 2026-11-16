package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class StaffRole(val label: String, val shortLabel: String) {
  ADMIN("Admin (Owner)", "Owner"),
  SALES_EXECUTIVE("Sales Executive", "Sales"),
  SERVICE_MANAGER("Service Manager", "Service Mgr"),
  MECHANIC("Staff / Mechanic", "Mechanic");

  companion object {
    fun fromString(value: String): StaffRole =
      entries.firstOrNull { it.name == value } ?: ADMIN
  }
}

enum class BikeStatus(val label: String) {
  IN_STOCK("In Stock"),
  RESERVED("Reserved"),
  SOLD("Sold");

  companion object {
    fun fromString(value: String): BikeStatus =
      entries.firstOrNull { it.name == value } ?: IN_STOCK
  }
}

enum class LeadStatus(val label: String) {
  INQUIRY("Inquiry"),
  TEST_RIDE("Test Ride"),
  QUOTATION_SENT("Quotation"),
  CONVERTED("Converted");

  companion object {
    fun fromString(value: String): LeadStatus =
      entries.firstOrNull { it.name == value } ?: INQUIRY
  }
}

enum class PaymentMode(val label: String) {
  CASH("Cash"),
  CHEQUE("Cheque"),
  FINANCE("Finance (Loan)");

  companion object {
    fun fromString(value: String): PaymentMode =
      entries.firstOrNull { it.name == value } ?: CASH
  }
}

enum class RtoStatus(val label: String) {
  PENDING("Pending"),
  APPLIED("Applied"),
  NUMBER_ALLOTTED("Reg. Allotted");

  companion object {
    fun fromString(value: String): RtoStatus =
      entries.firstOrNull { it.name == value } ?: PENDING
  }
}

enum class InsuranceStatus(val label: String) {
  PENDING("Pending"),
  ISSUED("Policy Issued");

  companion object {
    fun fromString(value: String): InsuranceStatus =
      entries.firstOrNull { it.name == value } ?: PENDING
  }
}

enum class ServiceType(val label: String) {
  FREE_SERVICE("Free Service"),
  PAID_SERVICE("Paid Service"),
  ACCIDENTAL_REPAIR("Accidental Repair");

  companion object {
    fun fromString(value: String): ServiceType =
      entries.firstOrNull { it.name == value } ?: PAID_SERVICE
  }
}

enum class JobStatus(val label: String) {
  OPEN("Open"),
  IN_PROGRESS("In Progress"),
  READY("Ready"),
  DELIVERED("Delivered");

  companion object {
    fun fromString(value: String): JobStatus =
      entries.firstOrNull { it.name == value } ?: OPEN
  }
}

enum class AttendanceStatus(val label: String) {
  PRESENT("Checked In"),
  CHECKED_OUT("Checked Out"),
  HALF_DAY("Half Day");

  companion object {
    fun fromString(value: String): AttendanceStatus =
      entries.firstOrNull { it.name == value } ?: PRESENT
  }
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val role: String = StaffRole.ADMIN.name,
  val dealershipName: String = "Sudha Hero • Mohanpur Bazar, Purnia",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class BikeStock(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val memberUids: List<String> = emptyList(),
  val modelName: String = "",
  val variant: String = "",
  val color: String = "",
  val chassisNumber: String = "",
  val engineNumber: String = "",
  val exShowroomPrice: Double = 0.0,
  val onRoadPrice: Double = 0.0,
  val status: String = BikeStatus.IN_STOCK.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SparePart(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val memberUids: List<String> = emptyList(),
  val partNumber: String = "",
  val partName: String = "",
  val category: String = "Engine",
  val quantity: Long = 0L,
  val minThreshold: Long = 5L,
  val unitPrice: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val isLowStock: Boolean
    get() = quantity <= minThreshold
}

@IgnoreExtraProperties
data class StaffMember(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val memberUids: List<String> = emptyList(),
  val name: String = "",
  val phone: String = "",
  val role: String = StaffRole.SALES_EXECUTIVE.name,
  val monthlySalary: Double = 0.0,
  val advanceTaken: Double = 0.0,
  val monthlyTarget: Long = 0L,
  val achievedCount: Long = 0L,
  val incentivePerUnit: Double = 0.0,
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val totalIncentiveEarned: Double
    get() = achievedCount * incentivePerUnit

  val netPayableSalary: Double
    get() = (monthlySalary + totalIncentiveEarned - advanceTaken).coerceAtLeast(0.0)
}

@IgnoreExtraProperties
data class AttendanceRecord(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val memberUids: List<String> = emptyList(),
  val staffId: String = "",
  val staffName: String = "",
  val staffRole: String = "",
  val dateKey: String = "",
  val checkInTime: String = "",
  val checkOutTime: String = "",
  val status: String = AttendanceStatus.PRESENT.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CustomerLead(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val memberUids: List<String> = emptyList(),
  val name: String = "",
  val phone: String = "",
  val address: String = "",
  val kycType: String = "Aadhaar",
  val kycNumber: String = "",
  val interestedModel: String = "",
  val leadStatus: String = LeadStatus.INQUIRY.name,
  val testRideDate: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SaleRecord(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val memberUids: List<String> = emptyList(),
  val invoiceNumber: String = "",
  val gatePassNumber: String = "",
  val bikeId: String = "",
  val modelName: String = "",
  val chassisNumber: String = "",
  val engineNumber: String = "",
  val customerId: String = "",
  val customerName: String = "",
  val customerPhone: String = "",
  val salesExecId: String = "",
  val salesExecName: String = "",
  val paymentMode: String = PaymentMode.CASH.name,
  val financierName: String = "",
  val downPayment: Double = 0.0,
  val totalAmount: Double = 0.0,
  val taxableAmount: Double = 0.0,
  val gstAmount: Double = 0.0,
  val rtoStatus: String = RtoStatus.PENDING.name,
  val insuranceStatus: String = InsuranceStatus.PENDING.name,
  val saleDateKey: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class JobCard(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val memberUids: List<String> = emptyList(),
  val jobCardNumber: String = "",
  val customerName: String = "",
  val customerPhone: String = "",
  val vehicleModel: String = "",
  val registrationNumber: String = "",
  val serviceType: String = ServiceType.FREE_SERVICE.name,
  val issueDescription: String = "",
  val requiredPartsSummary: String = "",
  val mechanicId: String = "",
  val mechanicName: String = "",
  val estimatedCost: Double = 0.0,
  val finalCost: Double = 0.0,
  val status: String = JobStatus.OPEN.name,
  val serviceDateKey: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ExpenseRecord(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val memberUids: List<String> = emptyList(),
  val title: String = "",
  val category: String = "MISC",
  val amount: Double = 0.0,
  val expenseDateKey: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class HeroModelPreset(
  val modelName: String,
  val defaultVariant: String,
  val defaultColor: String,
  val exShowroomPrice: Double,
  val onRoadPrice: Double,
)

val HERO_MODEL_CATALOG = listOf(
  HeroModelPreset("Splendor+", "i3S Drum Self Cast", "Black with Silver", 76306.0, 91200.0),
  HeroModelPreset("Splendor+ Xtec", "Disc Bluetooth", "Sparkling Beta Blue", 81001.0, 96800.0),
  HeroModelPreset("HF Deluxe", "i3S Self Alloy", "Nexus Blue", 66408.0, 79500.0),
  HeroModelPreset("Passion+", "i3S Drum Alloy", "Sports Red Black", 78950.0, 94300.0),
  HeroModelPreset("Glamour Xtec", "Disc OBD2", "Glossy Black", 89200.0, 106500.0),
  HeroModelPreset("Super Splendor Xtec", "Disc Alloy", "Candy Blazing Red", 86700.0, 103400.0),
  HeroModelPreset("Xtreme 125R", "Single Channel ABS", "Firestorm Red", 99500.0, 118200.0),
  HeroModelPreset("Xtreme 160R 4V", "USD Fork Dual Disc", "Matt Slate Black", 138500.0, 162400.0),
  HeroModelPreset("Karizma XMR", "210cc DOHC ABS", "Iconic Yellow", 181400.0, 211000.0),
  HeroModelPreset("Destini 125 Xtec", "ZX Disc Alloy", "Regal Maroon", 86500.0, 102900.0),
  HeroModelPreset("Pleasure+ Xtec", "Connected LED", "Jubiliant Yellow", 79800.0, 94800.0),
  HeroModelPreset("Xoom 110", "ZX Cornering Light", "Polestar Blue", 82500.0, 98200.0),
)
