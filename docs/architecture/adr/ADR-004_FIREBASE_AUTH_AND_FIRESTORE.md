# ADR-004: Firebase Authentication and Cloud Firestore as Core Application Platform

- **Status:** PROPOSED (Established Direction)
- **Date:** 2026-10-16
- **Authors:** Principal Systems Architect, Data Architect

---

## 1. Context and Problem Statement

The OTT platform requires:
- Secure consumer identity management (email/password, OAuth, session management, secure password hashing, token issuance).
- Real-time catalog and metadata database supporting multi-tenant queries, category listings, and user state (watchlists, subscription state).
- Fine-grained client-side security rules that prevent direct client tampering with sensitive fields (`isAdmin`, `tenantId`, `status`, `mainPlaybackAsset`).

We must evaluate whether to continue using Firebase Authentication and Cloud Firestore as the application data layer or introduce a relational database (e.g., PostgreSQL / MySQL via AWS RDS) or specialized document store (MongoDB).

---

## 2. Options Considered

### Option 1: Firebase Authentication + Cloud Firestore
- Fully managed Google Cloud serverless database and auth service.
- Declarative security rules (`firestore.rules`) providing field-level write protection.
- Native real-time listeners for instant client UI updates (watchlist sync, subscription activation).
- Native SDKs for both Android (Kotlin Coroutines) and Web (JavaScript/TypeScript).

### Option 2: Self-Managed Relational Database (PostgreSQL / RDS Aurora) + Custom Auth
- Relational schema with foreign keys and SQL queries.
- Requires building and maintaining a custom auth service (or integrating Supabase/Auth0), managing connection pooling, VPC peering, Prisma/Drizzle ORM, and database migrations.

---

## 3. Decision and Recommendation

**Retain and Standardize on Option 1: Firebase Authentication + Cloud Firestore.**

### Rationale:
1. **Adherence to Platform Principles:** Principle 2.1 in `MASTER.md` explicitly mandates avoiding complex server hosting, VPCs, or traditional database administration.
2. **Existing Implementation Investment:** The entire Android client data layer (`UserRepositoryImpl`, `ContentRepositoryImpl`, `SubscriptionRepositoryImpl`, `CategoryRepositoryImpl`, `PlanRepositoryImpl`, `MyListRepositoryImpl`) is fully implemented with Firebase Firestore SDKs and passes 47 unit tests.
3. **Robust Security Rules:** `firestore.rules` already implements default-deny security, client-side profile creation guards, and server-authoritative field locks using `affectedKeys()`.
4. **Operational Simplicity:** No database maintenance windows, failover clusters, or connection pool configuration required.

---

## 4. Consequences and Mitigations

### Positive:
- Instant real-time state synchronization across devices.
- Built-in multi-region reliability and automatic scaling.
- Generous free tier and low operating cost for early-stage platform operations.

### Negative / Risks:
- Query flexibility: Firestore lacks SQL `JOIN` operations. Mitigated by denormalized document references and composite indexes defined in `firestore.indexes.json`.
- Full-text search: Firestore does not support native fuzzy search. Mitigated in Phase 6 by integrating Algolia or implementing deterministic prefix indexing for title and tag search.

---

## 5. Evidence and Approval Status

- **Evidence:** `firestore.rules`, `Android - OTT/app/src/main/java/com/ott/platform/data/repository/impl/`.
- **Approval Status:** **ESTABLISHED DIRECTION — RECONFIRMED**.
