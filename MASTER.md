# OTT SaaS Platform
## Android Application — Architecture & Product Specification

**Document Status:** Architecture Baseline  
**Platform:** Android  
**Business Model:** Subscription-only  
**Infrastructure Model:** Serverless / Managed Cloud  
**Video Storage:** AWS S3  
**Video Delivery:** AWS CloudFront  
**Streaming Format:** HLS  
**Android Player:** Jetpack Media3 / ExoPlayer  
**Authentication:** Firebase Authentication  
**Database:** Cloud Firestore  
**Payment:** Existing Payment Gateway  
**Traditional Backend / VPS:** None

---

# 1. Executive Summary

The product is a subscription-based OTT SaaS platform for Android.

The objective is to provide a premium OTT experience while keeping the underlying infrastructure simple, inexpensive to operate, and free from traditional server hosting.

The application will not maintain a conventional backend server or VPS.

Instead, managed cloud services will provide the required infrastructure:

- Firebase Authentication for identity
- Firestore for application data
- Serverless Cloud Functions for trusted backend operations
- Existing payment gateway for subscriptions
- AWS S3 for video storage
- AWS CloudFront for secure video delivery
- HLS for adaptive video streaming
- Android Media3 for playback

The application will be **subscription-only**.

There will be no freemium content model.

A user must have an active subscription to access protected OTT content.

The architecture should remain intentionally lean while allowing the platform to evolve into a multi-tenant SaaS product.

---

# 2. Product Principles

The platform should follow five primary principles.

## 2.1 Simple Infrastructure

Do not build or maintain:

- VPS servers
- Dedicated application servers
- Nginx infrastructure
- Docker-based backend hosting
- Kubernetes
- Complex microservices

Use managed/serverless infrastructure instead.

---

## 2.2 Premium User Experience

The infrastructure may be simple, but the application must not feel like an MVP.

The user experience should feel comparable to modern premium OTT products.

Focus on:

- Cinematic visual design
- Fast navigation
- Smooth transitions
- High-quality posters and artwork
- Trailer previews
- Excellent video playback
- Minimal UI clutter
- Clear subscription experience
- Reliable loading states
- Resume playback
- Professional player controls

---

## 2.3 Subscription-Only

There is no freemium tier.

The basic entitlement model is:

```text
User
 ↓
Authentication
 ↓
Subscription Status
 ↓
Active?
 ├── NO → Subscription Screen
 └── YES → Full OTT Access
```

---

## 2.4 Secure Paid Content

The application must never rely exclusively on client-side logic to determine whether a user has access to premium content.

The Android application is considered an untrusted environment.

Trusted operations such as:

- Payment verification
- Subscription entitlement
- Subscription expiry
- Playback authorization

must be handled by trusted server-side/serverless infrastructure.

---

## 2.5 SaaS Ready

The architecture should support multiple OTT customers in the future.

The platform should therefore introduce the concept of a `tenant_id` from the beginning.

Example:

```text
Tenant A
 ├── Users
 ├── Plans
 ├── Content
 └── Branding

Tenant B
 ├── Users
 ├── Plans
 ├── Content
 └── Branding
```

This does not mean the first release needs a complex SaaS administration system.

It means the data model should not prevent it later.

---

# 3. High-Level Architecture

```text
                         ANDROID APPLICATION
                                  │
             ┌────────────────────┼────────────────────┐
             │                    │                    │
             ▼                    ▼                    ▼
       Firebase Auth       Firestore Data       Payment Gateway
             │                    │                    │
             │                    │                    │
             │                    │              Subscription
             │                    │                 Payment
             │                    │                    │
             │                    │                    ▼
             │                    │              Webhook
             │                    │                    │
             │                    │                    ▼
             │                    │             Cloud Function
             │                    │                    │
             │                    ◄────────────────────┘
             │
             │
             ▼
       Authenticated User
             │
             ▼
       Subscription Check
             │
             ▼
       Playback Authorization
             │
             ▼
       Signed CloudFront Access
             │
             ▼
          CLOUDFRONT
             │
             ▼
          PRIVATE S3
             │
             ▼
       HLS Video Stream
             │
             ▼
      Android Media3 Player
```

---

# 4. Technology Stack

| Layer | Technology |
|---|---|
| Android | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM / Clean Architecture principles |
| Networking | Retrofit / OkHttp where required |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
| Serverless Logic | Firebase Cloud Functions |
| Payments | Existing Payment Gateway |
| Video Storage | AWS S3 |
| CDN | AWS CloudFront |
| Streaming | HLS |
| Video Player | Android Media3 / ExoPlayer |
| Analytics | Firebase Analytics |
| Crash Reporting | Firebase Crashlytics |
| Source Control | Git |
| CI/CD | GitHub Actions or equivalent |

---

# 5. Backendless vs Serverless

The platform should be described as **serverless**, rather than strictly backendless.

A completely backendless architecture would place important security decisions inside the Android application.

That is not appropriate for premium paid video.

For example, the following is insecure:

```kotlin
if (subscriptionActive) {
    showPremiumContent()
}
```

An attacker could modify the application and bypass this logic.

Instead:

```text
Payment Gateway
       ↓
Trusted Webhook
       ↓
Cloud Function
       ↓
Firestore
       ↓
Subscription Entitlement
       ↓
Playback Authorization
```

There is still no traditional server to host or maintain.

Cloud Functions provide the small amount of trusted backend logic required.

---

# 6. Authentication

Firebase Authentication will handle user identity.

Potential authentication methods:

- Email/password
- Google Sign-In
- Phone authentication, if required

The first version should only implement authentication methods that are actually required.

Every authenticated user receives a Firebase UID.

Example:

```text
Firebase UID
    │
    ▼
users/{uid}
```

The Firebase UID becomes the primary user reference throughout the application.

---

# 7. Subscription Architecture

The subscription system is the core business logic.

The existing payment gateway remains responsible for payment processing.

The platform should not duplicate the payment gateway's functionality.

The gateway-specific implementation should be isolated behind an abstraction.

Example:

```text
PaymentGatewayAdapter

createSubscription()
verifyPayment()
getSubscriptionStatus()
cancelSubscription()
handleWebhook()
```

The actual method names depend on the selected payment gateway.

---

# 8. Subscription Flow

## New Subscription

```text
User opens application
        ↓
Login / Register
        ↓
Subscription Screen
        ↓
Select Plan
        ↓
Payment Gateway
        ↓
Payment Completed
        ↓
Gateway Webhook
        ↓
Cloud Function
        ↓
Verify Payment
        ↓
Update Firestore
        ↓
Subscription = ACTIVE
        ↓
Application refreshes entitlement
        ↓
User enters Home
```

---

# 9. Subscription States

The system should support at least:

```text
PENDING
ACTIVE
EXPIRED
CANCELLED
FAILED
```

Optional future states:

```text
PAUSED
REFUNDED
GRACE_PERIOD
```

The Android application should not simply check whether a subscription document exists.

It should check its actual status and validity period.

Example:

```text
status = ACTIVE
expiryDate > currentTime
```

---

# 10. Payment Webhook

The payment gateway should send events to a serverless endpoint.

Example:

```text
Payment Gateway
      │
      │ HTTPS Webhook
      ▼
Cloud Function
      │
      ├── Validate signature
      ├── Validate event
      ├── Verify payment
      ├── Identify user
      ├── Identify plan
      └── Update Firestore
```

Webhook processing must be **idempotent**.

If the gateway sends the same event twice, the subscription should not be duplicated or corrupted.

A webhook event ID should be stored or otherwise checked to prevent duplicate processing.

---

# 11. Firestore Data Model

The initial database should remain simple.

## tenants

```text
tenants/{tenantId}

{
    name,
    slug,
    logoUrl,
    primaryColor,
    secondaryColor,
    createdAt,
    status
}
```

---

## users

```text
users/{uid}

{
    tenantId,
    email,
    name,
    photoUrl,
    createdAt,
    lastLoginAt
}
```

---

## plans

```text
plans/{planId}

{
    tenantId,
    name,
    description,
    price,
    currency,
    billingPeriod,
    active
}
```

Example:

```text
Monthly
₹199
1 Month
```

or:

```text
Yearly
₹1999
12 Months
```

---

## subscriptions

```text
subscriptions/{subscriptionId}

{
    tenantId,
    userId,
    planId,
    gatewaySubscriptionId,
    status,
    startDate,
    expiryDate,
    createdAt,
    updatedAt
}
```

The `gatewaySubscriptionId` should map the internal subscription to the payment gateway's subscription.

---

## content

```text
content/{contentId}

{
    tenantId,
    title,
    description,
    posterUrl,
    backdropUrl,
    trailerUrl,
    streamUrl,
    duration,
    releaseDate,
    categoryIds,
    status,
    createdAt,
    updatedAt
}
```

For secure production playback, `streamUrl` should not contain a permanent public URL.

It can instead reference the CloudFront/S3 asset.

---

# 12. Content Structure

A content item may contain:

```text
Title
Description
Poster
Backdrop
Trailer
Main Video
Duration
Release Date
Category
Language
Subtitle Availability
```

Example:

```text
Movie
 ├── Poster
 ├── Backdrop
 ├── Trailer
 ├── Main Stream
 ├── Description
 ├── Duration
 └── Metadata
```

---

# 13. S3 Architecture

The S3 bucket must remain private.

Recommended logical structure:

```text
s3://ott-content/

    tenant-a/

        content/

            movie-001/

                master.m3u8

                1080p/
                    segment001.ts
                    segment002.ts

                720p/
                    segment001.ts
                    segment002.ts

                480p/
                    segment001.ts
                    segment002.ts

            movie-002/

        trailers/

        posters/

        backdrops/

    tenant-b/
        ...
```

The exact generated HLS directory structure may vary depending on the transcoding pipeline.

---

# 14. Video Processing Pipeline

Original video files should not simply be served as large MP4 files.

Recommended:

```text
Original Video
      ↓
S3 Upload
      ↓
Transcoding
      ↓
Multiple Resolutions
      ↓
HLS Packaging
      ↓
S3
      ↓
CloudFront
      ↓
Android Media3
```

Typical output:

```text
1080p
720p
480p
360p
```

The master playlist allows the player to select an appropriate quality automatically.

---

# 15. CloudFront

CloudFront acts as the CDN layer between the Android application and S3.

```text
Android
   ↓
CloudFront
   ↓
Private S3
```

S3 should not be directly exposed to users.

CloudFront should be configured so that only the intended delivery mechanism can access the bucket.

---

# 16. Secure Playback

For premium content, the application should not receive a permanent public video URL.

Instead:

```text
Android
   ↓
Request playback
   ↓
Trusted authorization
   ↓
Subscription validated
   ↓
Short-lived signed access
   ↓
CloudFront
   ↓
S3
```

The signed access should expire.

This reduces the usefulness of copied URLs.

Exact CloudFront signing implementation should be finalized during AWS implementation.

---

# 17. Android Application Architecture

Recommended structure:

```text
app/
│
├── core/
│   ├── network/
│   ├── auth/
│   ├── database/
│   ├── analytics/
│   └── player/
│
├── data/
│   ├── repository/
│   ├── models/
│   └── datasource/
│
├── domain/
│   ├── models/
│   └── usecases/
│
└── feature/
    ├── splash/
    ├── auth/
    ├── subscription/
    ├── home/
    ├── search/
    ├── content/
    ├── player/
    ├── mylist/
    └── profile/
```

The implementation should avoid unnecessary abstraction.

The architecture should remain understandable to a small development team.

---

# 18. Primary Navigation

Recommended bottom navigation:

```text
Home
Search
My List
Profile
```

The application should not have excessive navigation.

The user should be able to reach content within a few interactions.

---

# 19. Home Screen

The Home screen is the primary premium experience.

Suggested hierarchy:

```text
Hero / Featured Content

Continue Watching

New Releases

Popular

Categories / Collections

Recommended
```

Not every section needs to exist in the first release.

The important point is to maintain strong visual hierarchy.

---

# 20. Hero Trailer Autoplay

The featured content area should support automatic trailer playback.

### Initial State

When the Home screen loads:

```text
Poster displayed
        ↓
Trailer begins loading
        ↓
Trailer starts automatically
        ↓
Audio MUTED
```

The poster acts as the immediate fallback while the trailer loads.

---

# 21. Trailer Controls

The hero trailer should contain a minimal control layer.

Example:

```text
┌───────────────────────────────────────┐
│                                       │
│            TRAILER VIDEO              │
│                                       │
│                                       │
│ Title                                 │
│ Description                           │
│                                       │
│ [ Watch Now ] [ + My List ]      🔇   │
└───────────────────────────────────────┘
```

The mute button should support:

```text
Muted
   ↓
Tap
   ↓
Unmuted
```

Audio should never automatically begin at full volume.

---

# 22. Trailer Lifecycle

Only one trailer should be playing at a time.

When the user scrolls away:

```text
Trailer
   ↓
Pause
```

When the user returns:

```text
Resume / Restart
```

The exact behavior can be finalized through UX testing.

The system must prevent multiple simultaneous trailer players.

This is important for:

- Battery
- Memory
- CPU
- Network bandwidth
- App performance

---

# 23. Trailer Autoplay Rules

Recommended behavior:

### On screen entry

```text
Load poster
↓
Prepare trailer
↓
Autoplay muted
```

### User taps mute

```text
Muted → Unmuted
```

### User scrolls away

```text
Pause + release/retain player depending on lifecycle
```

### User selects another featured item

```text
Stop current trailer
↓
Load next trailer
↓
Autoplay muted
```

### App goes to background

```text
Pause trailer
```

### App returns

Resume/restart according to UX policy.

---

# 24. Main Video Player

The full content player should be separate from the trailer preview system.

Use Media3 / ExoPlayer.

Required controls:

- Play/Pause
- Seek
- ±10 second skip
- Volume
- Fullscreen
- Subtitle selection
- Audio track selection where applicable
- Quality selection
- Playback speed
- Resume position

Future features:

- Picture-in-picture
- Chromecast
- Downloads
- Multiple audio languages
- DRM

These should not be forced into MVP unless required.

---

# 25. Resume Playback

When a user exits a video:

```text
Movie A
Position = 37:42
```

The position can be stored.

When the user returns:

```text
Continue watching from 37:42?
```

For MVP, this can be implemented with Firestore.

A later optimization can batch/sync playback events to reduce writes.

---

# 26. Subscription Screen UX

Because the application is subscription-only, this screen is extremely important.

The design should communicate:

```text
What do I get?
        ↓
Which plan?
        ↓
How much?
        ↓
Subscribe
```

Example:

```text
WATCH EVERYTHING

Unlimited access to the complete library.

✓ Full content library
✓ New releases
✓ HD / 4K where available
✓ Continue watching
✓ Premium streaming

₹XXX / Month

[ START SUBSCRIPTION ]
```

Avoid clutter.

---

# 27. Loading States

Premium UX requires intentional loading states.

Do not display blank white/black screens.

Use:

- Poster placeholders
- Skeleton loaders
- Shimmer where appropriate
- Smooth content transitions
- Image placeholders
- Player loading indicators

The application should always communicate what is happening.

---

# 28. Error Handling

Examples:

### Subscription expired

```text
Your subscription has expired.

Renew your subscription to continue watching.

[ Renew Subscription ]
```

### Network error

```text
Couldn't connect.

Check your internet connection and try again.

[ Retry ]
```

### Playback error

```text
This video couldn't be played.

[ Try Again ]
```

Errors should be understandable to normal users.

Do not expose technical errors.

---

# 29. Security Requirements

Minimum security requirements:

- Firebase Authentication
- Firestore Security Rules
- Private S3 bucket
- CloudFront delivery
- Signed/controlled playback access
- HTTPS everywhere
- Payment webhook validation
- Webhook idempotency
- No permanent public premium video URLs
- No payment credentials stored in the Android application
- No secret keys embedded in APK
- No AWS credentials inside the Android application

---

# 30. Secrets

Never place the following inside the Android APK:

```text
AWS Secret Key
AWS Access Key
Payment Gateway Secret
Webhook Secret
Cloud Function Secret
Database Admin Credentials
```

The Android application is not a trusted environment.

Secrets belong in server-side/serverless infrastructure.

---

# 31. Firestore Security

Firestore Security Rules should ensure that users cannot simply modify their own subscription status.

For example, a client must never be able to write:

```text
subscription.status = ACTIVE
```

The application should only read its entitlement.

Subscription state should be controlled by trusted server-side functions.

---

# 32. Analytics

Firebase Analytics can track:

```text
app_open
login
subscription_started
subscription_success
subscription_failed
content_opened
trailer_started
trailer_muted
trailer_unmuted
video_started
video_completed
video_paused
video_error
search
add_to_list
```

This allows the team to understand:

- Which content performs best
- Which trailers attract attention
- Subscription conversion
- Playback failures
- User engagement

---

# 33. Crash Reporting

Firebase Crashlytics should be included from the beginning.

Important crash areas:

- Authentication
- Subscription flow
- Player
- HLS playback
- Network
- Compose UI
- App lifecycle

Player errors should also be logged as non-fatal events where appropriate.

---

# 34. Performance Requirements

The application should prioritize:

- Fast launch
- Fast image loading
- Efficient caching
- Minimal unnecessary Firestore reads
- Lazy-loaded content lists
- Efficient video lifecycle management
- Single active trailer player
- CDN delivery
- Adaptive bitrate streaming

The Home screen should not download the entire content catalogue at once.

---

# 35. MVP Scope

## Included

### Authentication
- Login
- Registration
- Logout

### Subscription
- Subscription plans
- Existing payment gateway
- Payment confirmation
- Subscription status
- Expiry handling

### Content
- Home
- Categories
- Content details
- Search
- My List

### Video
- HLS
- Media3
- Trailer autoplay
- Muted autoplay
- Mute/unmute
- Full video player
- Resume playback

### Infrastructure
- Firebase
- Firestore
- Cloud Functions
- S3
- CloudFront

---

# 36. Explicitly Out of MVP

Avoid adding unnecessary complexity initially.

Do not require:

- Live streaming
- DRM
- Offline downloads
- Smart TV application
- iOS application
- Recommendation AI
- Social features
- Chat
- Advanced personalization
- Multiple payment providers
- Complex microservices
- Kubernetes
- Dedicated backend servers

These can be introduced later based on business requirements.

---

# 37. Multi-Tenant SaaS Strategy

Every major entity should have a tenant relationship.

Example:

```text
tenantId
    ↓
User
    ↓
Subscription
    ↓
Content
    ↓
Plan
```

The Android application can be configured for a particular tenant.

Future architecture could support:

```text
OTT Platform
      │
      ├── Tenant A
      │     └── Android App A
      │
      ├── Tenant B
      │     └── Android App B
      │
      └── Tenant C
            └── Android App C
```

Branding can eventually be controlled through tenant configuration:

```text
Logo
Primary Color
Secondary Color
App Name
Content
Subscription Plans
```

---

# 38. Environments

At minimum:

```text
Development
Staging
Production
```

Each environment should have isolated:

- Firebase project/configuration
- Firestore
- Cloud Functions
- S3 paths/buckets where appropriate
- CloudFront configuration where appropriate
- Payment gateway credentials

Production credentials must never be used during development.

---

# 39. Development Phases

## Phase 1 — Foundation

- Android project
- Compose architecture
- Firebase setup
- Authentication
- Firestore
- Basic navigation
- Design system

---

## Phase 2 — Subscription

- Payment gateway integration
- Subscription plans
- Payment flow
- Webhook
- Cloud Function
- Firestore entitlement
- Subscription UI

---

## Phase 3 — Content

- Content model
- Home
- Categories
- Content details
- Search
- My List

---

## Phase 4 — Video

- S3
- HLS
- CloudFront
- Media3
- Secure playback
- Main player

---

## Phase 5 — Premium UX

- Hero trailer autoplay
- Muted autoplay
- Mute/unmute
- Trailer lifecycle
- Smooth transitions
- Skeleton loading
- Resume playback
- Premium animations

---

## Phase 6 — QA & Security

- Subscription edge cases
- Expired subscriptions
- Payment failures
- Duplicate webhooks
- Playback authorization
- APK security review
- Network failure testing
- Player testing
- Performance testing

---

# 40. End-to-End Final Architecture

The final system should conceptually look like this:

```text
                         ┌──────────────────┐
                         │   ANDROID APP    │
                         │                  │
                         │ Jetpack Compose  │
                         │ Media3 Player    │
                         └────────┬─────────┘
                                  │
                ┌─────────────────┼─────────────────┐
                │                 │                 │
                ▼                 ▼                 ▼
          Firebase Auth       Firestore       Payment Gateway
                │                 ▲                 │
                │                 │                 │
                │                 │              Webhook
                │                 │                 │
                │                 │                 ▼
                │                 │          Cloud Function
                │                 │                 │
                │                 ◄─────────────────┘
                │
                ▼
          Authenticated User
                │
                ▼
        Subscription Entitlement
                │
                ▼
       Playback Authorization
                │
                ▼
       Signed CloudFront Access
                │
                ▼
          ┌───────────────┐
          │  CloudFront   │
          └───────┬───────┘
                  │
                  ▼
          ┌───────────────┐
          │ Private S3    │
          │               │
          │ HLS Videos    │
          │ Trailers      │
          │ Posters       │
          └───────────────┘
```

---

# 41. Key Architectural Decision

The platform should **not** be described as having "no backend" in the technical documentation.

The correct description is:

> **Serverless OTT SaaS architecture with no traditional application server or VPS.**

Firebase and Cloud Functions provide managed backend capabilities where trusted server-side execution is required.

This gives the project:

- No server maintenance
- No VPS
- No traditional hosting
- Low operational overhead
- Secure subscription verification
- Secure video delivery
- Scalable infrastructure
- Premium Android UX

while keeping the architecture simple enough for an MVP.

---

# 42. Open Decisions

The following should be finalized before implementation:

1. **Payment gateway name and API**
2. Payment gateway webhook capabilities
3. Subscription plan structure
4. Monthly/yearly plans
5. Refund/cancellation behavior
6. Exact CloudFront signed-access mechanism
7. Video transcoding provider
8. Supported video resolutions
9. Supported languages/subtitles
10. Whether DRM is required
11. Whether multiple Android applications will represent different SaaS tenants
12. Whether content will be uploaded manually or through an admin dashboard
13. Admin/content management requirements
14. Analytics requirements
15. Maximum concurrent devices per subscription

---

# 43. Recommended MVP Philosophy

The first release should do only four things exceptionally well:

**1. Subscribe**

The payment experience must be reliable.

**2. Discover**

Users should immediately understand what content is available.

**3. Preview**

Trailers should automatically bring the catalogue to life.

**4. Watch**

Video playback should be fast, smooth and reliable.

Everything else can come later.

The architecture should therefore remain small underneath while the product experience feels premium on top.

---

# 44. Firebase Configuration & Documentation (Phase 2)

## 44.1 Firebase Project Creation
1. Go to the Firebase Console (https://console.firebase.google.com/).
2. Create a new Firebase project.
3. Add an Android app to the project with the package name `com.ott.platform`.
4. Download the `google-services.json` file.

## 44.2 Configuration Location
Place the downloaded `google-services.json` file in the `app/` directory of the project:
`app/google-services.json`

Once the file is in place, the build scripts will automatically pick it up, but you must manually uncomment the Google Services and Crashlytics plugins in `app/build.gradle.kts`:
```kotlin
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
```

## 44.3 Required Firebase Services
- **Firebase Authentication**: Enable Email/Password and Google Sign-In providers.
- **Cloud Firestore**: Provision a database in production mode.
- **Firebase Analytics**: Enable Google Analytics for the project.
- **Firebase Crashlytics**: Enable Crashlytics in the Firebase console.

## 44.4 Firestore Setup & Security Rules
The initial security rules have been added to the project in `firestore.rules`.
Deploy these rules to your Firebase project using the Firebase CLI:
`firebase deploy --only firestore:rules`

## 44.5 Known Limitations
While Firebase credentials (`google-services.json`) are unavailable:
- The app will compile and run with the Firebase abstractions, but actual Firebase calls (login, registration) will crash or fail at runtime because the SDK is not initialized properly without the JSON config.
- Google Sign-In will not work without OAuth 2.0 Client IDs configured in Firebase and Play Console.
- Analytics and Crashlytics will not track data.

---

# Phase 3: Subscription Architecture

## Overview
The platform enforces a subscription-only model. Access to premium content is strictly gated by an active entitlement state stored in Firestore. The Android application is an untrusted environment and does not have the capability to create or authorize its own subscriptions. Instead, it relies on a trusted serverless layer (Cloud Functions) that responds to webhooks from an external Payment Gateway.

## Data Model

**Subscription State:**
```text
subscriptions/{subscriptionId}
{
    tenantId,
    userId,
    planId,
    gatewaySubscriptionId,
    status, // E.g., ACTIVE, PENDING, EXPIRED, CANCELLED, FAILED
    startDate,
    expiryDate,
    createdAt,
    updatedAt
}
```

## Security Rules
The Android application has **read-only** access to its own `subscriptions/{subscriptionId}` document. 
Modifying the subscription `status`, `expiryDate`, or `tenantId` is completely blocked by Firestore Security Rules. Only the serverless Cloud Functions (with Firebase Admin privileges) can mutate this state.

## Webhook Architecture & Idempotency
Payment gateway events are processed by a Firebase Cloud Function (`paymentWebhook`).
- **Signature Validation:** The Cloud Function verifies the cryptographic signature attached to the incoming webhook by the gateway. **(STATUS: UNIMPLEMENTED / GATEWAY CONFIGURATION REQUIRED)**. Currently, the function rejects all webhooks because it lacks the specific gateway's signing logic.
- **Idempotency:** Webhooks can be retried. The Cloud Function executes a Firestore transaction that checks `webhook_events/{eventId}`. If the event ID exists, processing is skipped. This prevents duplicate subscription extensions or conflicting updates.
- **Entitlement Authorization:** Upon capturing a `payment.success` or `subscription.activated` event, the function writes the subscription doc to `ACTIVE` and pushes the `expiryDate` forward. 

## Payment Flow
1. **User Initiates:** User selects a `Plan` from the Android app's `SubscriptionScreen`.
2. **App Requests Payment:** The app calls `PaymentGateway.initiatePayment()`. The local state becomes `Confirming`.
3. **Gateway Processes:** The user completes payment in the gateway UI/overlay.
4. **Gateway Webhook:** The gateway sends a success webhook to the Firebase Cloud Function.
5. **Function Verifies:** The function validates the signature, ensures idempotency, and updates the `subscriptions` collection to `ACTIVE`.
6. **App Observes:** The Android app, observing Firestore, receives the snapshot update. The `SubscriptionState` changes to `Active`, automatically gating the user into the main content area.

## Gateway-Specific Configuration Requirements
**Payment Gateway Integration: PARTIALLY IMPLEMENTED / GATEWAY CONFIGURATION REQUIRED**

The implementation currently uses an abstract `ExternalPaymentGatewayAdapter` which throws a `PaymentGatewayNotConfiguredException`. To finalize the gateway integration, the following components are required:
- **Gateway name** (e.g. Stripe, Razorpay)
- **Android SDK/API requirements** to launch the payment UI flow
- **Public/client key** for initialization
- **Webhook secret** (`GATEWAY_WEBHOOK_SECRET`)
- **Webhook signature format** (e.g. `Stripe-Signature` or `x-razorpay-signature`)
- **Webhook event schema** (e.g. extracting `subscription_id`, `userId`, `tenantId`)
- **Subscription creation API** to configure plans on the gateway side
- **Subscription verification API** for manual checks if needed

## Known Limitations (Phase 3)
- The current cancel policy assumes subscriptions remain active until their expiry date, even if marked as `CANCELLED` (no-renew). This may need adjustment based on the exact billing policy of the chosen gateway.
- Refund states and grace periods are modeled loosely and default to inactive if not explicitly handled by the webhook.

---

# Phase 4A: Video Infrastructure

## Architecture Overview
The platform uses a scalable, serverless video delivery pipeline:
`Original Video (Source) → AWS S3 (Input) → [Optional MediaConvert/Transcoding] → AWS S3 (Private HLS Assets) → AWS CloudFront (OAC) → Playback Authorization → Android Media3`

This ensures that the Android client never accesses S3 directly, video assets are served globally via CDN, and access is strictly gated by backend authorization logic.

## Video Asset Model
Firestore stores logical identities for video assets, not permanent public URLs. The `Content` model includes `mainPlaybackAsset` and `trailerPlaybackAsset` represented by a `VideoAsset` abstraction:
```kotlin
data class VideoAsset(
    val contentId: String = "",
    val tenantId: String = "",
    val assetType: String = "", // e.g. "MAIN", "TRAILER"
    val hlsPath: String = "",   // e.g. "content/tenant_001/movie_123/hls/master.m3u8"
    val duration: Long = 0L
)
```

## Security & Playback Authorization
1. **S3 Privacy**: The AWS S3 bucket must have Block Public Access enabled. It must not be publicly readable.
2. **CloudFront OAC**: AWS CloudFront is configured to use Origin Access Control (OAC) to read from the S3 bucket securely.
3. **Authorization Flow**:
   - The Android client requests playback authorization for a specific `contentId`.
   - The Firebase Cloud Function (`authorizePlayback`) verifies the Firebase Auth token.
   - The function retrieves the requested `Content` to identify the `tenantId` and verifies it is published.
   - The function checks the `subscriptions` collection for an `ACTIVE` and unexpired subscription for that user and tenant.
   - If authorized, the function will generate and return a short-lived CloudFront signed URL or signed cookies.
   - The Android client passes this short-lived URL to the Media3 player.
4. **No Secrets in Client**: The Android APK does NOT contain any AWS Access Keys, CloudFront Key Pairs, or other privileged credentials.

## AWS Setup Requirements (Manual Deployment Steps)
Because actual AWS credentials are not stored in the repository, the following manual setup is required in the AWS Console:
1. **S3 Bucket**: Create a private S3 bucket (e.g., `ott-video-assets`). Ensure "Block all public access" is turned ON.
2. **CloudFront Distribution**: Create a distribution pointing to the S3 bucket as its origin.
3. **Origin Access Control (OAC)**: Configure CloudFront OAC for the origin and update the S3 Bucket Policy to allow `s3:GetObject` ONLY from the CloudFront distribution ARN.
4. **HLS Transcoding**: Set up AWS MediaConvert (or a similar tool) to transcode uploaded MP4s into segmented HLS (e.g., 360p, 480p, 720p, 1080p variant streams + master playlist) outputting to the S3 bucket.
5. **CloudFront Signing**: Generate a CloudFront Key Pair. Configure a Cache Behavior that requires signed URLs or signed cookies.
6. **Cloud Function Environment**: Provide the CloudFront Key Pair ID, Private Key, and Domain to the Firebase Cloud Functions via Firebase Secret Manager or environment variables:
   - `CLOUDFRONT_KEY_PAIR_ID`
   - `CLOUDFRONT_PRIVATE_KEY`
   - `CLOUDFRONT_DOMAIN`

## Current Limitations & Configuration Dependencies
- **CloudFront Signing Logic**: The exact CloudFront URL signing generation is marked as `UNIMPLEMENTED` and throws a `PLAYBACK_NOT_CONFIGURED` exception. It requires the AWS SDK (e.g., `@aws-sdk/cloudfront-signer`) and the keys above to be configured. The Android client will currently fail to play videos because the actual signed URL is not generated yet.
- **Hero Autoplay**: The UI elements (ExoPlayer hero autoplay, mute/unmute UI) are NOT implemented in Phase 4A. They are deferred to Phase 4B. This phase only establishes the infrastructure and contract needed for Phase 4B to consume `PlaybackAuthorization`.

## Phase 4B & 4B.1: Playback Implementation & Hardening
- **Media3/ExoPlayer integration**: Custom premium player UI, Media3 dependencies, and state management via `PlaybackViewModel`.
- **Playback Authorization Contract**: Full race-condition protection and failure handling mapped to user UI.
- **Hero Trailer Lifecycle**:
  - Hero uses `HeroTrailerController` and `HeroTrailerPlayer`.
  - Implements explicit `isActive` tracking via `rememberLazyListState` visibility.
  - Automatically cancels pending authorizations when scrolling away.
  - Pauses, stops, and releases the player correctly when the hero is inactive or out of view.
  - Enforces one active player and defaults to muted.
- **Race Protection**: Stale authorization responses are explicitly ignored if the active content ID changes.
- **Player Lifecycle**: `DisposableEffect` ensures Media3 players are securely released upon screen exit without memory leaks.
- **CloudFront Validation**: `playback.ts` explicitly verifies that the requested `hlsPath` belongs to the correct `tenantId` and `contentId` prefix.
- **Status of CloudFront Signing**: **Implemented contract, deployment-dependent signing implementation pending.** The system throws a `PLAYBACK_NOT_CONFIGURED` exception safely if AWS keys are not provided. AWS SDK and explicit configuration are required for true secure playback.

## Phase 5: OTT Content Experience
- **Home Architecture**: Extends `ContentRepository` for dynamic content sections (Featured, New Releases, Trending, Popular). Binds `HomeViewModel` to fetch lists and display them in horizontally scrolling reusable `ContentRow`s.
- **Search**: Implemented with a 500ms debounce using Kotlin Coroutines `Flow.debounce()`. Uses client-side filtering on published content for MVP. `SearchViewModel` tracks state, `SearchScreen` handles the empty state and results grid.
- **My List**: Architecture implemented using a user-isolated Firestore collection: `users/{userId}/myList/{contentId}`. Handled via `MyListRepository` and bound to `MyListViewModel` to fetch the user's saved content IDs, mapping them to full `Content` metadata.
- **Content Detail**: Integrated `ContentDetailViewModel` to display content metadata, related content (same category), and a toggleable "My List" button alongside the existing Media3 `PlaybackViewModel`.
- **Security & Authorization**: The client cannot alter tenant or admin roles. MyList operations use `request.auth.uid` filtering enforced by existing Firestore rules (e.g., `match /myList/{contentId} { allow read, write: if request.auth != null && request.auth.uid == userId; }`). Tenant isolation remains respected in all repository read operations.
- **Watch Progress**: Watch progress deferred.
- **Status of Tests**: Tests written for all ViewModels, simulating the repository flow. Local execution is limited due to missing Gradle wrapper, but code paths are covered correctly.
- **Completion**: Android implementation complete. Production AWS playback configuration pending.

## Phase 6: Admin CMS & Content Management
- **Admin Architecture**: A set of ViewModels (`AdminDashboardViewModel`, `AdminContentViewModel`, `AdminCategoryViewModel`, `AdminPlanViewModel`) enforce an initial client-side check (`user.isAdmin`), acting as the gateway to admin operations. UI visibility is not the security mechanism; security is enforced by Firestore rules.
- **Role Model**: Added an explicit `isAdmin: Boolean = false` field to the `User` model. This field cannot be changed by normal client-side writes, ensuring roles can only be set securely via backend (e.g. Firebase Admin SDK).
- **Tenant Isolation**: Admins can only interact with their own tenant. The `isTenantAdmin()` function was introduced in `firestore.rules` which asserts `isAdmin() && getUserTenant() == tenantId`. Every create/update operation requires the incoming request payload's `tenantId` to match the admin's verified tenant, preventing cross-tenant modifications.
- **Content Management**: Extended `ContentRepository` with admin-only methods (`observeAllContentAdmin`, `searchAllContentAdmin`, `saveContent`, `updateContentStatus`). Content includes states like `DRAFT`, `PUBLISHED`, `ARCHIVED`, and a `featured` toggle. Normal users are strictly restricted to reading `PUBLISHED` content.
- **Categories**: Added `Category` model and repository. Users can read active categories, while tenant admins can create, update, and soft-delete/deactivate them.
- **Plans**: Added `Plan` model and repository. The Admin CMS handles creation and deactivation of subscription plans. Subscription statuses (`Subscription.kt`) cannot be changed manually by admins through the client; they remain server-authoritative depending on the payment gateway webhook.
- **Security Verification**: `firestore.rules` was overhauled. Users cannot elevate their role, cannot change their tenant, and cannot write content, plans, or categories. 
- **Tests**: `AdminContentViewModelTest` was written testing role authorization, tenant isolation checks on save, and publish status updates. The tests were not executed due to missing gradle wrapper. 
- **Limitations**: No AWS credentials exposed, no insecure direct S3 uploads implemented, video upload infrastructure remains an external dependency, payment gateways remain an external dependency.
- **Completion**: Phase 6 CMS architecture is complete, awaiting AWS integration for direct uploads.

## Phase 6.1: Firestore Security & Admin Authorization Hardening
- **Security Audit & Hardening**: The Firestore authorization model underwent a dedicated security pass to ensure robust tenant isolation and resistance to privilege escalation.
- **Tenant Isolation for Updates**: Existing document updates are now authorized based on the existing document's trusted `resource.data.tenantId`, not the proposed `request.resource.data.tenantId`. This prevents admins from taking ownership of or modifying another tenant's resources.
- **Protected Fields**: Normal users are prevented from manipulating privileged fields such as `isAdmin` and `tenantId`. A user cannot elevate their privileges (`isAdmin = true`) or switch tenants.
- **Content & Administrative Authorization**: The `create`, `update`, and `delete` operations for `content`, `categories`, and `plans` enforce tenant boundaries strictly. An admin must belong to the tenant matching the existing document (`update`/`delete`) or the proposed document (`create`), and tenant IDs cannot be changed post-creation.
- **Subscription Security**: Subscriptions remain strictly read-only for clients. The Android application cannot create, update, or delete subscription state, maintaining the server-authoritative architecture driven by payment webhooks.
- **My List Ownership**: The `myList` collection ensures strict user isolation; a user can only read and write to their own list (`request.auth.uid == userId`). Watch progress, if implemented, follows the same isolation pattern.
- **Consumer Content Reads**: Published content remains readable for normal consumers, while unpublished or administrative content remains securely isolated.
- **No New Features**: This phase focused exclusively on security hardening without introducing new product functionality or AWS dependencies.

## Phase 7: Secure Media Upload, Processing & Content Publishing Pipeline
- **Media Ingestion Architecture**:
  ```text
  Admin Android
   ↓
  Firebase callable function (createMediaUpload)
   ↓
  admin + tenant + content authorization
   ↓
  short-lived S3 upload authorization
   ↓
  direct private S3 upload
   ↓
  trusted upload confirmation (completeMediaUpload)
   ↓
  MediaConvert
   ↓
  private HLS
   ↓
  processing completion (mediaConvertWebhook)
   ↓
  Firestore VideoAsset (updated by webhook)
  ```
- **Media Types**: Supported types include `MAIN_VIDEO`, `TRAILER`, `POSTER`, `BACKDROP`.
- **Storage Layout**: 
  Source files: `uploads/{tenantId}/{contentId}/source/{mediaType}/{uploadId}`
  Processed files: `content/{tenantId}/{contentId}/hls/{mediaType}/`
- **Upload Lifecycle**: Explicit state tracking in `mediaUploads` collection: `PENDING_UPLOAD`, `UPLOADING`, `UPLOADED`, `PROCESSING`, `READY`, `FAILED`, `CANCELLED`.
- **Processing Lifecycle**: Images (`POSTER`, `BACKDROP`) mark as `READY` immediately. Video (`MAIN_VIDEO`, `TRAILER`) triggers AWS MediaConvert and transitions to `PROCESSING` until a webhook confirms completion.
- **Tenant Isolation**: S3 bucket prefixes are strongly typed to the `tenantId`. Server-side validation restricts admins to initiating and finalizing uploads strictly for their assigned tenant. Stale results or duplicate triggers are handled using transaction-based idempotency checks.
- **Publication Rules**: Client-side publishing directly updating the status is disabled. A server-authoritative callable `publishContent` asserts all criteria (like `mainPlaybackAsset` being `READY`) are met before setting status to `PUBLISHED`.
- **Environment Configuration**: Functions require `S3_UPLOAD_BUCKET`, `AWS_REGION`, `MEDIACONVERT_ENDPOINT`, and `MEDIACONVERT_ROLE`.
- **AWS IAM Permissions**: Role must allow `s3:PutObject` for generating signed URLs and `mediaconvert:CreateJob` for starting conversions. MediaConvert itself needs a role for reading input and writing output to the S3 bucket.
- **Android Upload Behavior**: Android acts as an untrusted client fetching an authorization string (signed URL), uploading binary payloads directly via OkHttp/HttpURLConnection, tracking local progress, and invoking the completion callback on the Cloud Function. It does NOT invent paths or status states.
- **Failure Behavior**: Transcode failures emit status `FAILED` with diagnostics; network failures surface errors on Android.
- **Replacement Behavior**: Currently working assets are not disconnected until a new transcoding job fully succeeds (maintains HLS paths atomically).
- **Known Limitations**: Testing couldn't run successfully due to missing `gradlew`. Real CloudFront signing logic inside playback remains unimplemented. S3 CORS and EventBridge configs are external dependencies to be setup out-of-band.

## Phase 7.1: Media Pipeline Production Security & Verification Hardening
- **Implemented**:
  - **Upload authorization**: Tenant-scoped, server-generated presigned S3 URLs (`uuidv4`).
  - **Presigned S3 upload**: Temporary access with 4-hour expiration (14400s) to handle up to 10GB slow uploads.
  - **Media upload records**: Upload records stored in Firestore mapped to `jobId`.
  - **MediaConvert job creation**: Job creation mapped to upload record.
  - **Processing state**: Webhook verifies object existence using S3 `HeadObjectCommand` before processing.
  - **Publication validation**: Server-authoritative `changeContentStatus` ensures `mainPlaybackAsset` is present and valid before publishing.
  - **Webhook security**: MediaConvert completion events via SNS are cryptographically verified using `sns-validator` package.
  - **Stale-job protection & Idempotency**: Webhook logic cross-checks `jobId` and skips if already `READY` or `FAILED`.
  - **Android memory safety**: Streaming mode enabled (`setFixedLengthStreamingMode`) to prevent OutOfMemory issues during 10GB uploads.
- **External configuration required**:
  - S3 bucket (Must be private, Block Public Access enabled)
  - IAM (Application needs `s3:PutObject`, `s3:GetObject`, `mediaconvert:CreateJob`. MediaConvert needs its own role)
  - EventBridge/SNS (Must route MediaConvert COMPLETE/ERROR events to SNS topic)
  - Cloud Functions deployment (Requires `S3_UPLOAD_BUCKET`, `AWS_REGION`, etc.)
- **Still not implemented / Limitations**:
  - **CloudFront signed playback URLs**: Marked UNIMPLEMENTED.
  - **Production playback verification**: Not end-to-end verified with real video playback.
  - **Android File Selection**: The Android client simply takes a `File` object. Handling of `content://` URIs and `ContentResolver` for real Android file selection is NOT IMPLEMENTED.
  - **Android Upload Cancellation**: Clean cancellation and background upload operations (e.g., WorkManager) are NOT IMPLEMENTED. Only basic coroutine cancellation exists which is insufficient.
  - **Orphan-cleanup**: Media replacement does not delete old assets.

## Phase 7.2: Android Media Selection & Upload Reliability
- **Android media selection**:
  - **Picker API**: Relies on modern `content://` URIs, decoupled from `File`. (UI implementations would use `ActivityResultContracts.OpenDocument`).
  - **URI handling**: Implemented via `MediaSource` and `MediaSourceProvider` wrapping `ContentResolver`.
  - **ContentResolver**: Safely queried for `DISPLAY_NAME`, `SIZE`, and `MIME_TYPE`.
  - **Supported media types**: Controlled by MIME-type filters requested during picker invocation (e.g., `video/*` or `image/*`).
- **Upload behavior**:
  - **Streaming**: Fully streams bytes from `ContentResolver.openInputStream` directly to `HttpURLConnection.outputStream` with an 8KB buffer. `setFixedLengthStreamingMode` limits memory to a chunk size without loading full files (e.g. multi-GB videos) into memory.
  - **Progress**: Computed natively from streamed bytes `((uploadedBytes / fileSize) * 100)` or returned as indeterminate if `sizeBytes` is missing.
  - **Cancellation**: Coroutine cancellation inherently drops out of the IO block and safely closes streams using `.use { }` scoping.
  - **Retry**: Basic fallback mapped back to the `AdminContentViewModel`. Permanent errors (like 403 S3 Expired Auth) will require the user to retry the upload and generate a new signed URL.
  - **Process-death behavior**: Inherits standard `viewModelScope` semantics. If the app is killed, the coroutine dies, network closes, and the upload aborts.
- **Current limitation**:
  - **Persistent background upload is not implemented.** (No WorkManager).
  - **Orphan-cleanup**: Remains unresolved.
