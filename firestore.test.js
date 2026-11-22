const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    appName: "Counter Demo",
    packageName: "com.droidblocks.counter",
    accentColorHex: "#22D3EE",
    widgets: [
      {
        id: "textview1",
        type: "TEXT_VIEW",
        text: "Count: 0",
        hint: "",
        textSize: 22,
        textColor: "#F8FAFC",
        backgroundColor: "#00000000",
        widthMode: "MATCH_PARENT",
        heightMode: "WRAP_CONTENT",
        paddingDp: 12,
        gravity: "CENTER",
        cornerRadius: 8,
        progress: 50,
        checked: false,
      },
    ],
    variables: [
      {
        name: "count",
        type: "INT",
        initialValue: "0",
      },
    ],
    events: {
      onCreate: [],
      button1_onClick: [
        {
          id: "blk_1",
          type: "INCREASE_VAR",
          params: { varName: "count", amount: "1" },
          children: [],
          elseChildren: [],
        },
      ],
    },
    createdAt: now,
    updatedAt: now,
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

test("Unauthenticated user: cannot read, list, or write projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID))
  );
});

test("Authenticated owner: valid create, list, get, partial update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projectsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("projects");

  // Empty list query succeeds
  await assertSucceeds(projectsCol.get());

  // Create succeeds
  await assertSucceeds(projectsCol.doc("proj_1").set(validProjectPayload(ALICE_UID)));

  // Get & list succeed
  await assertSucceeds(projectsCol.doc("proj_1").get());
  await assertSucceeds(projectsCol.get());

  // Partial update succeeds (createdAt & userId preserved)
  await assertSucceeds(
    projectsCol.doc("proj_1").update({
      appName: "Counter Pro",
      widgets: [],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Delete succeeds
  await assertSucceeds(projectsCol.doc("proj_1").delete());
});

test("Authenticated user: cannot read, list, update, or delete another user's projects", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("projects")
      .doc("bob_proj")
      .set(validProjectPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobCol = aliceDb.collection("users").doc(BOB_UID).collection("projects");

  await assertFails(bobCol.get());
  await assertFails(bobCol.doc("bob_proj").get());
  await assertFails(
    bobCol.doc("bob_proj").update({
      appName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobCol.doc("bob_proj").delete());
});

test("Validation guards: reject ghost fields, immutable tampering, future timestamps, and invalid IDs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projectsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("projects");

  // Reject ghost field (id or isAdmin)
  await assertFails(
    projectsCol.doc("proj_ghost").set(validProjectPayload(ALICE_UID, { id: "proj_ghost" }))
  );

  // Reject future timestamp
  await assertFails(
    projectsCol
      .doc("proj_future")
      .set(validProjectPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Reject oversized appName (> 40 chars)
  await assertFails(
    projectsCol
      .doc("proj_long")
      .set(validProjectPayload(ALICE_UID, { appName: "A".repeat(41) }))
  );

  // Reject invalid document ID
  await assertFails(
    projectsCol.doc("bad id!").set(validProjectPayload(ALICE_UID))
  );

  // Create valid doc, then verify immutable fields cannot change
  await assertSucceeds(projectsCol.doc("proj_imm").set(validProjectPayload(ALICE_UID)));
  await assertFails(
    projectsCol.doc("proj_imm").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    projectsCol.doc("proj_imm").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
