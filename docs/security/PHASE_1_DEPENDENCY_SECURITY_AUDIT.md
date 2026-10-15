# Phase 1 — Dependency Security Audit

**Document Status:** Complete  
**Audit Date:** 2026-10-15  
**Auditor:** Repository Reliability & Application Security Engineering  
**Scope:** Firebase Cloud Functions backend (`functions/`), Android application (`app/`, `gradle/`), Infrastructure as Code (`staging/`), Root multi-repo workspace (`OTT/`).

---

## 1. Executive Summary

A comprehensive dependency and vulnerability audit was conducted across all active codebases in the OTT platform repository.

- **Firebase Cloud Functions (`functions/`):** 43 vulnerabilities were identified by `npm audit` (31 High, 12 Moderate, 0 Critical, 0 Low). 100% of the High-severity findings originate from either devDependencies (`jest`, `micromatch`, `babel-jest`, `expect`) or transitive runtime dependencies (`node-forge` via `firebase-admin@12.0.0`). In-depth code inspection confirms that the vulnerable code paths (such as custom-buffer UUID generation and RSA PKCS#1 v1.5 certificate parsing) are not reachable from external user input. Remediation through `npm audit fix --force` was evaluated and rejected because it demands breaking major version upgrades (`firebase-admin` v12 → v14.5.0 and `jest` v29 → v30.5.2) without backward-compatibility guarantees.
- **Android Application (`app/`):** Uses Gradle 8.7 with version catalog (`gradle/libs.versions.toml`), Android Gradle Plugin 8.3.2, Kotlin 2.0.0, and JDK 17 (Eclipse Adoptium 17.0.17). All 47 unit tests pass and all dependencies resolve cleanly from trusted Google and MavenCentral repositories.
- **Infrastructure (`staging/`):** OpenTofu v1.12.6 with AWS Provider v5.100.0 successfully initializes (`tofu init -backend=false`) and validates (`tofu validate`).
- **External Repositories (`ott-backend`, `ott-website`):** Inspected and confirmed to be newly initialized empty Git repositories on `main` branch with zero dependencies currently declared.

---

## 2. Dependency Audit Scope and Inventory

### 2.1 Manifests and Lockfiles Inspected

| Component | Manifest Path | Lockfile Path | Toolchain Version |
|---|---|---|---|
| **Firebase Cloud Functions** | `functions/package.json` | `functions/package-lock.json` | Node.js 18 (local v24.11.0, npm 11.6.2) |
| **Android OTT Application** | `app/build.gradle.kts`, `build.gradle.kts` | `gradle/libs.versions.toml` | Gradle 8.7, AGP 8.3.2, Kotlin 2.0.0, JDK 17 |
| **AWS Staging Infrastructure** | `staging/main.tf`, `staging/variables.tf` | `staging/.terraform.lock.hcl` | OpenTofu v1.12.6, AWS Provider v5.100.0 |
| **Standalone Backend Repo** | `Backend/` | None (Empty Git repo) | Git remote `https://github.com/kunnaaalll/ott-backend.git` |
| **Standalone Website Repo** | `Website/` | None (Empty Git repo) | Git remote `https://github.com/kunnaaalll/ott-website.git` |

---

## 3. Vulnerability Findings & Impact Analysis

### 3.1 Node.js Backend (`functions`) Audit Results

**Command Executed:**
```bash
npm audit --json
```
**Exit Code:** 1  
**Summary of Vulnerabilities:**
- **Total:** 43
- **Critical:** 0
- **High:** 31
- **Moderate:** 12
- **Low:** 0
- **Informational:** 0

**Dependency Breakdown:**
- Production dependencies scanned: 191
- Development dependencies scanned: 259
- Optional dependencies scanned: 79
- Peer dependencies: 4
- Total package tree: 526

### 3.2 Prioritized Vulnerability Register

| Package | Severity | Category | Vulnerable Range | Path / Dependency Chain | Production Impact Assessment |
|---|---|---|---|---|---|
| `node-forge` | **High** | Cryptography / Signature Parsing | `<1.3.0` (all) | Transitive: `firebase-admin` → `@google-cloud/firestore` / `@google-cloud/storage` → `node-forge` (GHSA-86w9-cpqp-85rv) | **Minimal / Unreachable.** Used internally by Google Cloud client auth for RSA PKCS#1 v1.5 certificate parsing. The Cloud Functions backend relies on Google Application Default Credentials and Node native crypto (`crypto.createHmac`, `crypto.timingSafeEqual`) and `@aws-sdk/cloudfront-signer`. |
| `uuid` | **Moderate** | Buffer Bounds Validation | `<11.1.1` | Direct & Transitive: `uuid@9.0.1` and `firebase-admin` → `gaxios` → `uuid` (GHSA-w5hq-g745-h8pq) | **Zero Impact.** The CVE affects `v3`, `v5`, and `v6` UUID generation when a caller-supplied buffer is provided. The functions backend strictly calls `uuidv4()` with zero arguments in `functions/src/media.ts:81` for generating unique upload IDs. |
| `micromatch` | **High** | ReDoS in Path Expansion | `<4.0.8` | DevDependency: `jest` → `jest-config` / `@jest/core` → `micromatch` | **None (Test Tooling Only).** Only executed locally during Jest test runs. Not bundled or deployed to Firebase Cloud Functions runtime. |
| `sprintf-js` | **Moderate** | Denial of Service | `*` | DevDependency: `jest` → `babel-plugin-istanbul` → `js-yaml` → `argparse` → `sprintf-js` | **None (Test Tooling Only).** Only invoked during test coverage instrumentation. |
| `@jest/*` suite | **High / Mod** | Test Framework Sub-packages | `<30.2.0` | DevDependency: `jest@29.7.0` suite | **None (Test Tooling Only).** |

---

## 4. Evaluation of Remediation Options

### 4.1 Evaluation of `npm audit fix` and `npm audit fix --force`

1. **`npm audit fix` (non-breaking):**
   - Output: `0 vulnerabilities fixed, 43 vulnerabilities remain`.
   - Result: No automated semver-compatible patches exist within the currently declared major ranges.
2. **`npm audit fix --force`:**
   - Evaluated in dry-run mode (`npm audit fix --dry-run`).
   - Proposed Changes:
     - Install `firebase-admin@14.5.0` (Breaking Change: 2 major version jumps from v12.0.0).
     - Install `jest@30.5.2` (Breaking Change: major version jump from v29.7.0).
     - Install `uuid@14.0.3` (Breaking Change: major version jump from v9.0.1).
   - **Decision:** **REJECTED.** Applying `npm audit fix --force` violates Section 3.1 and Section 5 of the Phase 1 engineering guidelines. Major upgrades to `firebase-admin` alter Firestore transaction types, credential handling, and error prototypes, introducing high regression risks without resolving any active production exploit.

### 4.2 Remediation Decisions Summary

- **Applied:** Preserved lockfile integrity (`functions/package-lock.json`).
- **Deferred:** `firebase-admin` upgrade to v13/v14 until Phase 2 integration planning, where breaking SDK changes can be tested against the live Firebase staging project.
- **Deferred:** `jest` v30 upgrade until tooling stabilization, as Jest 29.7.0 reliably executes all 35 backend tests in 1.7 seconds without errors.

---

## 5. Android Toolchain & Dependency Verification

### 5.1 Toolchain Compatibility Matrix

| Tool / Library | Declared Version | Verified Runtime | Status | Notes |
|---|---|---|---|---|
| **Gradle** | 8.7 | Gradle 8.7 (wrapper) | **VERIFIED** | Wrapper permissions and distribution checksum validated |
| **Java / JDK** | 17 | Eclipse Adoptium 17.0.17+10 (aarch64) | **VERIFIED** | Successfully discovered in `~/.gradle/jdks/` |
| **Android Gradle Plugin** | 8.3.2 | AGP 8.3.2 | **VERIFIED** | Compatible with Gradle 8.7 |
| **Kotlin** | 2.0.0 | Kotlin 2.0.0 (Compose plugin 2.0.0) | **VERIFIED** | Language and compiler plugin aligned |
| **Android Compile SDK** | 34 | Android SDK 34 (UpsideDownCake) | **VERIFIED** | Installed in `/Users/kunal/Library/Android/sdk` |
| **Android Min SDK** | 26 | Android 8.0 (Oreo) | **VERIFIED** | |
| **Build Tools** | 34.0.0 / 35.0.0 | 34.0.0 | **VERIFIED** | Present in SDK build-tools directory |

### 5.2 Android Third-Party Libraries

- **Media3 / ExoPlayer:** `1.3.1` (HLS streaming, signed cookie header attachment)
- **Firebase BOM:** `33.1.0` (Auth, Firestore, Functions, Analytics, Crashlytics)
- **Hilt:** `2.51.1` with KSP `2.0.0-1.0.21` (Dependency injection)
- **Coil:** `2.6.0` (Poster and thumbnail image loading)
- **Turbine / MockK / Mockito:** Unit testing stack (47 tests passing)

All Android dependencies resolve from Google's Maven repository and MavenCentral without deprecated or vulnerable insecure HTTP repository URLs.

---

## 6. Infrastructure Dependencies (`staging/`)

- **OpenTofu CLI:** v1.12.6 (darwin_arm64)
- **AWS Provider:** `hashicorp/aws` v5.100.0
- **Provider Status:** Successfully downloaded, verified against cryptographic checksums in `.terraform.lock.hcl`, and structurally validated with zero schema errors.

---

## 7. Remaining Security Risks and Human Actions

1. **Dependency Modernization (Phase 2):** When a live staging Firebase project is available, plan a coordinated upgrade of `firebase-admin` from v12 to the latest LTS release with automated end-to-end regression validation.
2. **Lockfile Monitoring:** Continue tracking upstream security advisories on `@aws-sdk/*` (currently at `^3.620.0`) and `firebase-functions` (`^4.5.0`).
