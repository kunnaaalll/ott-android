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