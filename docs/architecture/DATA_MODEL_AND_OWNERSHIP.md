# Unified OTT Platform — Data Model & Ownership Architecture

- **Document Version:** 1.0.0
- **Document Status:** PROPOSED DATA ARCHITECTURE (Phase 2 Deliverable)
- **Author:** Principal Data Architect, Application Security Architect
- **Storage Engine:** Cloud Firestore (Document NoSQL) & Amazon S3 (Binary Media Objects)

---

## 1. Storage Organization & Collection Hierarchy

Cloud Firestore organizes data into root collections and targeted subcollections to maximize query performance, maintain multi-tenant logical partitioning, and enforce least-privilege security rules.

```text
Cloud Firestore Root
├── /tenants/{tenantId}                 [Root Collection: Tenant branding & configuration]
├── /users/{userId}                     [Root Collection: User identity & role metadata]
│   ├── /myList/{contentId}             [Subcollection: User's personal saved titles]
│   └── /watchProgress/{contentId}      [Subcollection: Cross-device resume timestamps]
├── /content/{contentId}                [Root Collection: Movies, Series & Standalone Media]
│   └── /seasons/{seasonId}             [Subcollection: Series seasons]
│       └── /episodes/{episodeId}       [Subcollection: Season episodes with HLS assets]
├── /categories/{categoryId}            [Root Collection: Taxonomy, genres & rails]
├── /plans/{planId}                     [Root Collection: Subscription pricing & billing periods]
├── /subscriptions/{subscriptionId}     [Root Collection: Server-authoritative entitlement records]
├── /mediaUploads/{uploadId}            [Root Collection: Ingestion session & transcode tracking]
├── /webhook_events/{eventId}           [Root Collection: Idempotency log for payments/callbacks]
├── /_rateLimits/{docId}                [Root Collection: Server-side sliding-window counters]
└── /audit_logs/{logId}                 [Root Collection: Administrative audit trail]
```

### 1.1 Structural Justification

- **Subcollections vs. Root Collections:** 
  - `myList` and `watchProgress` are scoped as user subcollections because they are strictly private to individual users and inherit the parent `/users/{userId}` security boundary.
  - `seasons` and `episodes` are scoped as subcollections under `/content/{contentId}` (when `contentType == "SERIES"`) to ensure clean hierarchical cascading, scoped deletion, and query isolation.
  - `subscriptions` and `mediaUploads` are maintained as top-level root collections rather than subcollections because they require multi-document queries across tenants by administrative functions and webhook reconciliation workers.

---

## 2. Core Entity Catalog

### 2.1 Tenant (`/tenants/{tenantId}`)
- **Purpose:** Represents an independent OTT brand or SaaS customer on the platform.
- **Key Fields:**
  - `id` (String): Unique tenant slug/ID (e.g. `tenant_sundari`).
  - `name` (String): Brand display name.
  - `slug` (String): URL-safe brand identifier.
  - `logoUrl` (String): Hosted logo asset URL.
  - `primaryColor` (String): Hex brand color (e.g., `#E50914`).
  - `secondaryColor` (String): Hex accent color.
  - `status` (String): `active` | `suspended`.
  - `createdAt` / `updatedAt` (ServerTimestamp).
- **Tenant Ownership:** System-level root entity.
- **Authoritative Writer:** Platform Super-Admin / Operator via Admin SDK.
- **Permitted Readers:** Public (unauthenticated read of active tenant branding for website white-labeling).
- **Permitted Writers:** Denied to all clients (`allow write: if false`).
- **Sensitive Fields:** `status` (account suspension flag).

---

### 2.2 User Profile (`/users/{userId}`)
- **Purpose:** Represents a subscriber or administrative user. Document ID matches Firebase Auth UID.
- **Key Fields:**
  - `id` (String): Firebase Auth UID.
  - `tenantId` (String): Mandatory tenant scope.
  - `email` (String): Registered email address.
  - `name` (String): Display name.
  - `photoUrl` (String): Optional avatar URL.
  - `isAdmin` (Boolean): Server-authoritative administrative privilege flag.
  - `status` (String): `ACTIVE` | `SUSPENDED`.
  - `createdAt` / `lastLoginAt` (ServerTimestamp).
- **Authoritative Writer:** Serverless Backend / Admin SDK.
- **Permitted Readers:** Owning user (`isCurrentUser(userId)`) or Tenant Admin (`isTenantAdmin(tenantId)`).
- **Permitted Writers:** 
  - **Client Create:** Permitted on first login, but `firestore.rules:43-44` strictly forbids client from setting `isAdmin` or `tenantId`.
  - **Client Update:** Permitted for display fields (`name`, `photoUrl`), but `tenantId` and `isAdmin` must match existing values.
- **Sensitive Fields:** `isAdmin`, `tenantId`, `status`.

---

### 2.3 Content: Movie / Series Root (`/content/{contentId}`)
- **Purpose:** Represents a standalone movie, documentary, or parent series container.
- **Key Fields:**
  - `id` (String): Unique content ID (e.g. `mov_98a7sdf`).
  - `tenantId` (String): Mandatory tenant scope.
  - `contentType` (String): `MOVIE` | `SERIES` | `STANDALONE`.
  - `title` (String): Content title.
  - `description` (String): Synopsis/description.
  - `posterUrl` (String): S3 path to poster image (`uploads/.../poster.webp`).
  - `backdropUrl` (String): S3 path to wide banner image.
  - `duration` (Long): Total running time in seconds (for movies).
  - `releaseDate` (Timestamp): Theatrical or digital release date.
  - `categoryIds` (Array of Strings): Mapped genre/category references.
  - `language` (String): Primary audio language.
  - `featured` (Boolean): Flag for hero banner display.
  - `status` (String): `DRAFT` | `PUBLISHED` | `ARCHIVED`.
  - `mainPlaybackAsset` (Map):
    - `assetType` (String): `"MAIN_VIDEO"`
    - `hlsPath` (String): Relative path to master playlist (`content/{tenant}/{id}/main/hls/{uploadId}.m3u8`).
    - `duration` (Long): Exact transcode duration in seconds.
  - `trailerPlaybackAsset` (Map):
    - `assetType` (String): `"TRAILER"`
    - `hlsPath` (String): Relative path to trailer playlist (`content/{tenant}/{id}/trailer/hls/{uploadId}.m3u8`).
  - `createdAt` / `updatedAt` (ServerTimestamp).
- **Authoritative Writer:** Cloud Functions (for status & assets); Tenant Admin (for metadata).
- **Permitted Readers:** Signed-in users if `status == "PUBLISHED"`; Tenant Admins if `status == "DRAFT"`.
- **Permitted Writers:** Tenant Admin (`isTenantAdmin(tenantId)`), with strict field locking:
  - `firestore.rules:95-96` rejects client updates modifying `status`, `mainPlaybackAsset`, `trailerPlaybackAsset`, `posterUrl`, or `backdropUrl`.
- **Sensitive Fields:** `mainPlaybackAsset.hlsPath`, `status`, `tenantId`.

---

### 2.4 Series Subcollections: Seasons & Episodes
- **Path:** `/content/{seriesId}/seasons/{seasonId}/episodes/{episodeId}`
- **Purpose:** Represents episodic content within a TV series.
- **Key Fields (Season):**
  - `id` (String): Season identifier (e.g. `season_1`).
  - `seasonNumber` (Int): 1, 2, 3...
  - `title` (String): "Season 1".
  - `episodeCount` (Int): Total episodes.
- **Key Fields (Episode):**
  - `id` (String): Episode identifier (e.g. `ep_101`).
  - `episodeNumber` (Int): 1, 2, 3...
  - `title` (String): Episode title.
  - `description` (String): Episode synopsis.
  - `duration` (Long): Episode length in seconds.
  - `thumbnailUrl` (String): Episode still image.
  - `mainPlaybackAsset` (Map): Transcoded HLS asset (`hlsPath`).
  - `status` (String): `DRAFT` | `PUBLISHED`.
- **Authoritative Writer:** Tenant Admin (metadata) and Cloud Functions (HLS asset linking).
- **Permitted Readers:** Subscribers of the parent series tenant.

---

### 2.5 Media Ingestion Session (`/mediaUploads/{uploadId}`)
- **Purpose:** Tracks an active upload and MediaConvert transcoding pipeline lifecycle.
- **Key Fields:**
  - `id` (String): Unique upload UUID.
  - `contentId` (String): Target content document reference.
  - `tenantId` (String): Owning tenant ID.
  - `mediaType` (String): `MAIN_VIDEO` | `TRAILER` | `POSTER` | `BACKDROP`.
  - `sourceKey` (String): S3 mezzanine object path (`uploads/{tenant}/{contentId}/source/{type}/{id}`).
  - `status` (String): `PENDING_UPLOAD` | `UPLOADING` | `PROCESSING` | `READY` | `FAILED`.
  - `jobId` (String): AWS MediaConvert Job ID.
  - `mimeType` (String): File MIME type.
  - `fileSize` (Long): Declared file size in bytes.
  - `errorCode` (String): Error diagnosis if failed.
  - `createdBy` (String): Admin UID who initiated upload.
  - `createdAt` / `updatedAt` (ServerTimestamp).
- **Authoritative Writer:** Cloud Functions exclusively (`media.ts`).
- **Permitted Readers:** Tenant Admins (`isTenantAdmin(tenantId)`).
- **Permitted Writers:** Denied to all clients (`allow write: if false` in `firestore.rules:108`).

---

### 2.6 Subscription (`/subscriptions/{subscriptionId}`)
- **Purpose:** Records user entitlement to premium streaming content.
- **Key Fields:**
  - `id` (String): Unique subscription record ID.
  - `tenantId` (String): Owning tenant ID.
  - `userId` (String): Subscriber UID.
  - `planId` (String): Associated plan ID.
  - `gatewaySubscriptionId` (String): External gateway ID (e.g. `sub_1NxyzABC`).
  - `status` (String): `PENDING` | `ACTIVE` | `EXPIRED` | `CANCELLED` | `FAILED`.
  - `startDate` (ServerTimestamp): Effective start time.
  - `expiryDate` (Timestamp): Exact expiration timestamp.
  - `createdAt` / `updatedAt` (ServerTimestamp).
- **Authoritative Writer:** Serverless Backend exclusively (`paymentWebhook` via Admin SDK).
- **Permitted Readers:** Owning subscriber (`resource.data.userId == request.auth.uid`).
- **Permitted Writers:** Denied to all clients (`allow write: if false` in `firestore.rules:67`).
- **Sensitive Fields:** `status`, `expiryDate`, `gatewaySubscriptionId`.

---

### 2.7 Subscription Plan (`/plans/{planId}`)
- **Purpose:** Defines pricing tiers offered to subscribers.
- **Key Fields:**
  - `id` (String): Plan ID (e.g. `plan_monthly_inr`).
  - `tenantId` (String): Owning tenant ID.
  - `name` (String): "Standard Monthly".
  - `description` (String): "HD streaming on mobile and web".
  - `price` (Double): Decimal amount (e.g. 199.00).
  - `currency` (String): 3-letter currency code (e.g. `INR`, `USD`).
  - `billingPeriod` (String): `monthly` | `yearly`.
  - `active` (Boolean): Availability flag.
- **Authoritative Writer:** Tenant Admin.
- **Permitted Readers:** Signed-in subscribers where `active == true` (`firestore.rules:141`).
- **Permitted Writers:** Tenant Admin (`isTenantAdmin(tenantId)`).

---

### 2.8 Webhook Idempotency Log (`/webhook_events/{eventId}`)
- **Purpose:** Guarantees exactly-once processing for payment and transcoding callbacks.
- **Key Fields:**
  - `id` (String): Gateway event ID or SNS message ID.
  - `type` (String): Event type (e.g., `payment.success`, `subscription.activated`).
  - `processedAt` (ServerTimestamp): Processing timestamp.
  - `error` (String): Failure reason if payload validation failed.
- **Authoritative Writer:** Cloud Functions exclusively.
- **Permitted Access:** No client access (`allow read, write: if false`). Retention: 90 days.

---

### 2.9 User Watch Progress (`/users/{userId}/watchProgress/{contentId}`)
- **Purpose:** Stores video playback position for cross-device resume functionality.
- **Key Fields:**
  - `contentId` (String): Target title ID.
  - `positionMs` (Long): Playback position in milliseconds.
  - `durationMs` (Long): Total content duration.
  - `completed` (Boolean): True if watched past 90% threshold.
  - `updatedAt` (ServerTimestamp).
- **Authoritative Writer:** Owning user (`isCurrentUser(userId)`).
- **Permitted Readers:** Owning user only.
- **Permitted Writers:** Owning user only.

---

## 3. Client vs. Server Write Boundary Matrix

The table below defines the authoritative boundaries governing write permissions. Direct client updates to server-authoritative fields are blocked by database security rules and API middleware.

| Entity | Field | Allowed Writer | Enforcement Location | Security Risk if Tampered |
|---|---|---|---|---|
| `/users/{uid}` | `isAdmin` | Cloud Functions / Admin SDK only | `firestore.rules:43` | Privilege escalation to platform administrator |
| `/users/{uid}` | `tenantId` | Cloud Functions / Admin SDK only | `firestore.rules:44` | Unauthorized cross-tenant data access |
| `/users/{uid}` | `name`, `photoUrl` | Authenticated User | `firestore.rules:47` | None (User profile customization) |
| `/content/{id}` | `status` | `changeContentStatus` Cloud Function | `firestore.rules:95` | Unfinished/broken video published publicly |
| `/content/{id}` | `mainPlaybackAsset` | `mediaConvertWebhook` Cloud Function | `firestore.rules:95` | Unauthorized media hijacking or path traversal |
| `/content/{id}` | `posterUrl`, `backdropUrl`| `completeMediaUpload` Cloud Function | `firestore.rules:96` | Arbitrary asset injection |
| `/content/{id}` | `title`, `description` | Tenant Admin | `firestore.rules:92` | Catalog content update (Permitted for Admin) |
| `/subscriptions/{id}` | `status`, `expiryDate` | `paymentWebhook` Cloud Function | `firestore.rules:67` | Free access without payment; revenue loss |
| `/mediaUploads/{id}` | All Fields | Cloud Functions (`media.ts`) | `firestore.rules:108` | Forging transcode status |
| `/_rateLimits/{id}` | `calls` | Cloud Functions (`playback.ts`) | `firestore.rules:122` | Bypassing playback rate limits |
| `/webhook_events/{id}`| All Fields | Cloud Functions (`index.ts`) | `firestore.rules:115` | Replaying old webhook transactions |

---

## 4. Indexing & Query Considerations

Firestore requires composite indexes for complex queries containing multiple equality and inequality operators. The following index definitions are required in `firestore.indexes.json`:

```json
{
  "indexes": [
    {
      "collectionGroup": "content",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "tenantId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "featured", "order": "ASCENDING" },
        { "fieldPath": "releaseDate", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "content",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "tenantId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "categoryIds", "arrayConfig": "CONTAINS" }
      ]
    },
    {
      "collectionGroup": "subscriptions",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "userId", "order": "ASCENDING" },
        { "fieldPath": "tenantId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" }
      ]
    }
  ]
}
```
