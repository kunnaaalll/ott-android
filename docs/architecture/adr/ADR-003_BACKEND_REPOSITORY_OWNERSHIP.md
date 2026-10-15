# ADR-003: Backend Source Repository Ownership (Android Repository vs Standalone Backend Repository)

- **Status:** PROPOSED (Architectural Recommendation)
- **Date:** 2026-10-16
- **Authors:** Principal Systems Architect, Engineering Lead

---

## 1. Context and Problem Statement

The workspace currently contains three Git repositories:
1. `Android - OTT/` (`ott-android.git`): Active repository containing the Android app, OpenTofu staging infrastructure, Firestore rules, and the Firebase Cloud Functions backend in `functions/`.
2. `Backend/` (`ott-backend.git`): Cloned remote repository on branch `main` with 0 commits (unpopulated).
3. `Website/` (`ott-website.git`): Cloned remote repository on branch `main` with 0 commits (unpopulated).

We must decide the authoritative long-term repository home for the backend source code (Cloud Functions, Firestore rules, Firestore indexes, and backend test suites).

---

## 2. Options Considered

### Option A: Retain Backend Source within `ott-android` Repository
Continue developing Cloud Functions inside `Android - OTT/functions`.
- **Pros:** Zero immediate file migration needed; current 35 unit tests pass directly in place.
- **Cons:** Coupling between Android mobile client and backend cloud infrastructure; web developers must clone the entire Android codebase and Gradle toolchain to work on backend APIs; CI/CD triggers on the Android repo fire for both mobile and cloud backend commits; confusing separation of concerns.

### Option B: Migrate Backend Source to Standalone `ott-backend` Repository
Move `functions/`, `firestore.rules`, `firestore.indexes.json`, and `firebase.json` into the `Backend/` (`ott-backend.git`) repository during Phase 3.
- **Pros:**
  - Clear domain boundaries: `ott-backend` owns backend business logic, security rules, and cloud APIs.
  - Independent CI/CD pipelines: Backend testing, linting, and Firebase deployments run without invoking Gradle or Android toolchains.
  - Multi-client alignment: Both the Android app and the unified Website/CMS consume the backend as an independent upstream service.
  - Matches the established Git workspace structure where `ott-backend` was explicitly created by the organization.
- **Cons:** Requires a structured file move and Git commit in Phase 3.

### Option C: Monorepo Consolidation
Merge all three repositories into a single Git monorepo (e.g., using Nx or Turborepo).
- **Pros:** Single PRs spanning client and backend changes.
- **Cons:** Disrupts established remote Git repository structures (`ott-android.git`, `ott-backend.git`, `ott-website.git`); complex tooling needed for mixed Kotlin/Gradle and TypeScript/Node workspaces.

---

## 3. Decision and Recommendation

**Adopt Option B: Migrate Backend Source into Standalone `ott-backend` Repository during Phase 3.**

### Rationale:
1. **Organizational Intent:** The user and organization explicitly provisioned the `ott-backend` repository. Leaving it indefinitely empty while hosting cloud backend logic inside a mobile app repo creates technical debt.
2. **Clean Separation of Concerns:** Android client developers and Web/Backend developers can operate with autonomous CI/CD pipelines and isolated toolchains (JDK 17 + Gradle for Android vs Node 18/20 for Backend).
3. **Low Migration Risk:** The backend code is self-contained in `functions/` and `firestore.rules`. Moving it into `Backend/` in Phase 3 is a clean, low-risk operation that preserves all test scripts and dependencies.

---

## 4. Consequences and Phased Execution Plan

1. **Phase 2 (Current):** No code is moved. All documentation explicitly records `Android - OTT/functions/` as the current verified source, and `ott-backend` as the planned target repository.
2. **Phase 3:** Execute the migration of `functions/`, `firestore.rules`, `firebase.json`, and backend tests into `Backend/`, verify 35 passing tests in the new location, and establish backend CI.
3. **Android Clean-up:** Once verified in `Backend/`, remove obsolete duplicate backend folders from `Android - OTT` to eliminate drift.

---

## 5. Evidence and Approval Status

- **Evidence:** Verified existence of empty Git remote `https://github.com/kunnaaalll/ott-backend.git` in `Backend/`.
- **Approval Status:** **RECOMMENDED — REQUIRES OPERATOR APPROVAL BEFORE PHASE 3 EXECUTION**.
