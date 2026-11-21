const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_trader_123";
const BOB_UID = "bob_trader_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validSignalPayload(uid, overrides = {}) {
  const past = new Date(Date.now() - 2000);
  return {
    userId: uid,
    symbol: "XAUUSD",
    timeframe: "M15",
    side: "BUY",
    strategy: "3-Module PA",
    entry: 2642.5,
    sl: 2635.0,
    tp1: 2652.5,
    tp2: 2662.5,
    rr: "1:2.6",
    status: "ACTIVE",
    timestamp: Date.now() - 5000,
    pnlPercent: 0.0,
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

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

test("Unauthenticated user: cannot read or write signals or stats", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("signals").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("signals").doc("sig_1").set(
      validSignalPayload(ALICE_UID)
    )
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("stats").doc("summary").get());
});

test("Authenticated owner: valid signal create, status progression, and terminal lock work", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("signals").doc("sig_xau_1");

  await assertSucceeds(ref.set(validSignalPayload(ALICE_UID)));

  // Query list aligned with Kotlin observeSignals()
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("signals").orderBy("timestamp", "desc").get()
  );

  // ACTIVE -> TP1_HIT succeeds
  await assertSucceeds(
    ref.update({
      status: "TP1_HIT",
      pnlPercent: 1.4,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // TP1_HIT -> TP2_HIT succeeds
  await assertSucceeds(
    ref.update({
      status: "TP2_HIT",
      pnlPercent: 2.6,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state TP2_HIT cannot be mutated again
  await assertFails(
    ref.update({
      status: "ACTIVE",
      pnlPercent: 0.0,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Security bounds: reject @DocumentId id field, ghost field, future timestamp, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Reject redundant `id` key in document body
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("signals").doc("sig_bad_id").set(
      validSignalPayload(ALICE_UID, { id: "sig_bad_id" })
    )
  );

  // Reject ghost field (Shadow Update)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("signals").doc("sig_ghost").set(
      validSignalPayload(ALICE_UID, { isAdmin: true })
    )
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("signals").doc("sig_future").set(
      validSignalPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) })
    )
  );

  // Cross-user isolation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("signals").doc("sig_alice").set(
      validSignalPayload(ALICE_UID)
    )
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("signals").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("signals").doc("sig_alice").update({
      status: "SL_HIT",
      pnlPercent: -1.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Stats summary: owner can create and update stats, cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const statsRef = aliceDb.collection("users").doc(ALICE_UID).collection("stats").doc("summary");

  await assertSucceeds(
    statsRef.set({
      userId: ALICE_UID,
      todayWins: 3,
      todayLosses: 1,
      todayNetPnl: 4.2,
      yesterdayWins: 4,
      yesterdayLosses: 2,
      yesterdayNetPnl: 3.1,
      winRate: 70.0,
      activeCount: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    statsRef.update({
      todayWins: 4,
      todayLosses: 1,
      todayNetPnl: 5.8,
      yesterdayWins: 4,
      yesterdayLosses: 2,
      yesterdayNetPnl: 3.1,
      winRate: 72.7,
      activeCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("stats").doc("summary").get()
  );
});
