package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BikeStatus
import com.example.data.model.InsuranceStatus
import com.example.data.model.JobStatus
import com.example.data.model.LeadStatus
import com.example.data.model.PaymentMode
import com.example.data.model.RtoStatus
import com.example.data.model.ServiceType
import com.example.data.model.StaffRole
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DealershipRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun addAndObserveBikeStock_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = DealershipRepository(firestore, auth)
    val chassis = "MBLHAW${UUID.randomUUID().toString().take(8).uppercase()}"

    val addResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addBike(
          modelName = "Splendor+",
          variant = "i3S Drum Self Cast",
          color = "Black with Silver",
          chassisNumber = chassis,
          engineNumber = "HA10AG123456",
          exShowroomPrice = 76306.0,
          onRoadPrice = 91200.0,
        )
      }
    assertTrue("addBike failed: ${addResult.exceptionOrNull()}", addResult.isSuccess)
    val bikeId = addResult.getOrThrow()

    val bikes = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBikes(uid).first { list -> list.any { it.id == bikeId } }
    }
    val created = bikes.first { it.id == bikeId }
    assertEquals("Splendor+", created.modelName)
    assertEquals(BikeStatus.IN_STOCK.name, created.status)
  }

  @Test
  fun recordBikeSale_batchUpdatesBikeStaffCustomerAndCreatesSale() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = DealershipRepository(firestore, auth)

    val bikeId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addBike(
          modelName = "HF Deluxe",
          variant = "i3S Alloy",
          color = "Nexus Blue",
          chassisNumber = "MBL${UUID.randomUUID().toString().take(8).uppercase()}",
          engineNumber = "HA11EG987654",
          exShowroomPrice = 66408.0,
          onRoadPrice = 79500.0,
        ).getOrThrow()
      }

    val staffId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addStaffMember(
          name = "Rajesh Kumar",
          phone = "9876543210",
          role = StaffRole.SALES_EXECUTIVE,
          monthlySalary = 16000.0,
          monthlyTarget = 12L,
          incentivePerUnit = 600.0,
        ).getOrThrow()
      }

    val customerId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addCustomerLead(
          name = "Suresh Yadav",
          phone = "9123456789",
          address = "Mohanpur Bazar, Purnia",
          kycType = "Aadhaar",
          kycNumber = "9988-7766-5544",
          interestedModel = "HF Deluxe",
          leadStatus = LeadStatus.TEST_RIDE,
          testRideDate = "2026-11-16",
        ).getOrThrow()
      }

    val bike = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBikes(uid).first { list -> list.any { it.id == bikeId } }.first { it.id == bikeId }
    }
    val exec = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeStaff(uid).first { list -> list.any { it.id == staffId } }.first { it.id == staffId }
    }

    val saleResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.recordBikeSale(
          bike = bike,
          modelName = bike.modelName,
          chassisNumber = bike.chassisNumber,
          engineNumber = bike.engineNumber,
          customerId = customerId,
          customerName = "Suresh Yadav",
          customerPhone = "9123456789",
          salesExec = exec,
          paymentMode = PaymentMode.FINANCE,
          financierName = "Hero FinCorp",
          downPayment = 20000.0,
          totalAmount = 79500.0,
          rtoStatus = RtoStatus.APPLIED,
          insuranceStatus = InsuranceStatus.ISSUED,
        )
      }
    assertTrue("recordBikeSale failed: ${saleResult.exceptionOrNull()}", saleResult.isSuccess)

    val updatedBikes = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeBikes(uid).first { list -> list.any { it.id == bikeId && it.status == BikeStatus.SOLD.name } }
    }
    assertEquals(BikeStatus.SOLD.name, updatedBikes.first { it.id == bikeId }.status)

    val updatedExec = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeStaff(uid).first { list -> list.any { it.id == staffId && it.achievedCount == 1L } }
    }
    assertEquals(1L, updatedExec.first { it.id == staffId }.achievedCount)
  }

  @Test
  fun jobCardLifecycle_andAttendanceCheckInOut_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = DealershipRepository(firestore, auth)

    val mechId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addStaffMember(
          name = "Vikash Mechanic",
          phone = "9801122334",
          role = StaffRole.MECHANIC,
          monthlySalary = 14000.0,
          monthlyTarget = 30L,
          incentivePerUnit = 150.0,
        ).getOrThrow()
      }
    val mech = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeStaff(uid).first { list -> list.any { it.id == mechId } }.first { it.id == mechId }
    }

    val attId = withTimeout(DEFAULT_TIMEOUT_MS) { repo.checkInStaff(mech).getOrThrow() }
    val checkOutResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.checkOutAttendance(attId) }
    assertTrue(checkOutResult.isSuccess)

    val jcId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createJobCard(
          customerName = "Manoj Paswan",
          customerPhone = "9001122334",
          vehicleModel = "Xtreme 125R",
          registrationNumber = "BR11CD4567",
          serviceType = ServiceType.PAID_SERVICE,
          issueDescription = "Engine oil change and brake pad replacement",
          requiredPartsSummary = "10W30 Oil, Front Brake Pad",
          mechanic = mech,
          estimatedCost = 1250.0,
        ).getOrThrow()
      }

    val jobCard = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeJobCards(uid).first { list -> list.any { it.id == jcId } }.first { it.id == jcId }
    }
    val deliverResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.updateJobCardStatus(jobCard, JobStatus.DELIVERED, 1300.0)
      }
    assertTrue("deliverResult failed: ${deliverResult.exceptionOrNull()}", deliverResult.isSuccess)
  }

  @Test
  fun crossUserAccess_bobCannotMutateAliceSparePart() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = DealershipRepository(firestore, auth)
    val partId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.addSparePart(
          partNumber = "HGP-${UUID.randomUUID().toString().take(6).uppercase()}",
          partName = "Hero Genuine Oil Filter",
          category = "Engine",
          quantity = 10L,
          minThreshold = 4L,
          unitPrice = 140.0,
        ).getOrThrow()
      }

    signInTestUser(BOB_EMAIL)
    val bobRepo = DealershipRepository(firestore, auth)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.updateSparePartQuantity(partId, 0L) }
    assertTrue(result.isFailure)
    val firestoreException = result.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  @Test
  fun unauthenticatedObserveBikes_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = DealershipRepository(firestore, auth)

    var thrown: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeBikes("some_uid").first() }
    } catch (t: Throwable) {
      thrown = t
    }
    val firestoreException =
      checkNotNull(thrown?.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $thrown"
      }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice.sudha@test.com"
    const val BOB_EMAIL = "bob.outsider@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
