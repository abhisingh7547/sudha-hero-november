const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const ts = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_1";
const BOB_UID = "bob_outsider_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read bikes, sales, customers, or job_cards", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("bikes").get());
  await assertFails(unauthDb.collection("sales").get());
  await assertFails(unauthDb.collection("customers").get());
  await assertFails(unauthDb.collection("job_cards").get());
  await assertFails(unauthDb.collection("staff").get());
});

test("Owner can create and query own bikes with memberUids filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("bikes").doc("bike_1").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      modelName: "Splendor+",
      variant: "i3S Drum Self",
      color: "Black with Silver",
      chassisNumber: "MBLHAW12345678901",
      engineNumber: "HA10AG1234567",
      exShowroomPrice: 76306,
      onRoadPrice: 89500,
      status: "IN_STOCK",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("bikes").where("memberUids", "array-contains", ALICE_UID).get()
  );
});

test("Unfiltered list query fails (rules are not filters)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("bikes").get());
  await assertFails(aliceDb.collection("spare_parts").get());
  await assertFails(aliceDb.collection("customers").get());
});

test("Cross-user access: Bob cannot read or mutate Alice's bike or customer PII", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("bikes").doc("alice_bike").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      modelName: "HF Deluxe",
      variant: "Kick Start",
      color: "Red",
      chassisNumber: "MBLHAW99988877766",
      engineNumber: "HA10AG9998887",
      exShowroomPrice: 61000,
      onRoadPrice: 72000,
      status: "IN_STOCK",
      createdAt: ts(),
      updatedAt: ts(),
    });
    await db.collection("customers").doc("alice_cust").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      name: "Ramesh Kumar",
      phone: "9876543210",
      address: "Mohanpur Bazar, Purnia",
      kycType: "Aadhaar",
      kycNumber: "1234-5678-9012",
      interestedModel: "Splendor+",
      leadStatus: "INQUIRY",
      testRideDate: "",
      createdAt: ts(),
      updatedAt: ts(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("bikes").doc("alice_bike").get());
  await assertFails(bobDb.collection("customers").doc("alice_cust").get());
  await assertFails(
    bobDb.collection("bikes").doc("alice_bike").update({
      status: "SOLD",
      updatedAt: ts(),
    })
  );
  await assertFails(bobDb.collection("bikes").doc("alice_bike").delete());
});

test("Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("spare_parts").doc("part_1").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      partNumber: "HGP-101",
      partName: "Engine Oil 10W30",
      category: "Lubricants",
      quantity: 20,
      minThreshold: 5,
      unitPrice: 380,
      ghostField: "hack",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

test("Identity spoofing: Alice cannot create bike owned by Bob", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("bikes").doc("spoof_bike").set({
      ownerId: BOB_UID,
      memberUids: [BOB_UID],
      modelName: "Xtreme 125R",
      variant: "ABS",
      color: "Firestorm Red",
      chassisNumber: "MBLHAW000001",
      engineNumber: "HA10AG000001",
      exShowroomPrice: 95000,
      onRoadPrice: 112000,
      status: "IN_STOCK",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

test("Invalid enum status is rejected on job_cards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("job_cards").doc("jc_bad").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      jobCardNumber: "JC-001",
      customerName: "Suresh Yadav",
      customerPhone: "9123456789",
      vehicleModel: "Glamour",
      registrationNumber: "BR11AB1234",
      serviceType: "FREE_SERVICE",
      issueDescription: "Chain noise and oil change",
      requiredPartsSummary: "Engine oil",
      mechanicId: "mech_1",
      mechanicName: "Vikash",
      estimatedCost: 450,
      finalCost: 0,
      status: "UNKNOWN_STATUS",
      serviceDateKey: "2026-11-16",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

test("Valid sale, attendance, staff, and expense creation succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("staff").doc("staff_1").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      name: "Rajesh Sharma",
      phone: "9801234567",
      role: "SALES_EXECUTIVE",
      monthlySalary: 18000,
      advanceTaken: 2000,
      monthlyTarget: 15,
      achievedCount: 4,
      incentivePerUnit: 500,
      isActive: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("attendance").doc("att_1").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      staffId: "staff_1",
      staffName: "Rajesh Sharma",
      staffRole: "SALES_EXECUTIVE",
      dateKey: "2026-11-16",
      checkInTime: "09:15 AM",
      checkOutTime: "",
      status: "PRESENT",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("sales").doc("sale_1").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      invoiceNumber: "SH-PUR-2026-001",
      gatePassNumber: "GP-2026-001",
      bikeId: "bike_1",
      modelName: "Splendor+",
      chassisNumber: "MBLHAW12345678901",
      engineNumber: "HA10AG1234567",
      customerId: "cust_1",
      customerName: "Amit Kumar",
      customerPhone: "9876543210",
      salesExecId: "staff_1",
      salesExecName: "Rajesh Sharma",
      paymentMode: "FINANCE",
      financierName: "Hero FinCorp",
      downPayment: 22000,
      totalAmount: 89500,
      taxableAmount: 69921.88,
      gstAmount: 19578.12,
      rtoStatus: "APPLIED",
      insuranceStatus: "ISSUED",
      saleDateKey: "2026-11-16",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("expenses").doc("exp_1").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      title: "Showroom Electricity Bill",
      category: "ELECTRICITY",
      amount: 4200,
      expenseDateKey: "2026-11-16",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});
