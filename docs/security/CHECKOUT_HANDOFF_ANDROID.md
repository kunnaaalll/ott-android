# Android side of the checkout handoff

Full design, threat model and server details: `Website/docs/CHECKOUT_HANDOFF.md`.

## Rule

The Android app contains **no** Cashfree credentials, API calls, SDK, WebView checkout or payment-session logic.
It asks the Chumbak backend for a handoff, opens the returned website URL in a Custom Tab, and afterwards reads
the user's subscription from the backend. It never creates, infers or caches an "active" subscription on its own.

## Behaviour

1. **Sign-in** (`WebsiteApiClient.login / register / verifyOtp / loginWithGoogle`) sends `X-Chumbak-Client: android`.
   The backend answers with `appSession.token`, an opaque credential for this app session. It is stored encrypted
   (AES-256-GCM, Android Keystore key) by `AppCredentialStore`. If a sign-in response carries none, any older credential is deleted.
2. **Subscribe tap** → `SubscriptionViewModel.initiatePayment` → `PaymentGateway.startWebCheckout(plan)` →
   `POST /api/checkout/handoff {planId}` with `Authorization: Bearer <app token>`. The app sends only the plan id.
3. The returned URL must pass `CheckoutUrlPolicy` (exactly `https://thechumbak.com/checkout/handoff#t=<43 chars>`, no query,
   no userinfo, default port). Anything else is rejected. The URL is a secret: it is never logged or sent to analytics.
4. `SubscriptionScreen` opens it in a Custom Tab (default browser as fallback). The website exchanges the token, signs the
   browser in, shows the plan and price from the server, and continues to Cashfree.
5. **Return** (`chumbak://subscription/status?order_id=…`, the https `/pricing/status` link, or simply resuming the screen):
   treated as navigation only. `CheckoutConfirmationService` asks the backend for the order status (when an order id is
   present; the backend verifies with Cashfree) and for the subscription (`GET /api/user/subscription`, bearer).
   - backend returns an active subscription → shown as active (and cached as the backend's answer)
   - not yet / backend unreachable → `ConfirmingTimeout` ("check payment status"), nothing created locally
   - order reported failed → retryable error
   - 401 → credential deleted, user signed out and sent to login
6. **Sign-out** calls `POST /api/auth/logout` (revokes the app token and browser sessions opened from this app), then clears local state.

## Removed

- Cashfree production `x-client-id` / `x-client-secret`, direct `api.cashfree.com` order creation and status calls.
- `InAppPaymentSheet` (WebView checkout) → moved to `docs/legacy/InAppPaymentSheet.kt.removed` (it was untracked, so it was kept rather than deleted).
- Local fabrication of `ACTIVE` subscriptions after payment (`createActiveSubscription`, the fallback in `SubscriptionViewModel`, and the one in `MainActivity.handleDeepLink`).
- The `isSubscribed` / `subscriptionStatus` claim in playback authorization requests (the backend no longer honours it either).
- Offline Play-review fallback no longer includes an `ACTIVE` subscription.
- Dead ProGuard keep rules for the removed WebView bridge.

## Tests

`app/src/test/.../core/payment/{CheckoutUrlPolicyTest,CheckoutConfirmationServiceTest,ExternalPaymentGatewayAdapterTest}.kt`
and the rewritten `ui/screens/subscription/SubscriptionViewModelTest.kt` (handoff launch, duplicate taps, expired session,
confirmed / pending / failed return, no fabricated subscription when the backend is unavailable, resume re-check).

Run: `./gradlew --offline :app:testReleaseUnitTest`. Two environment notes:
- Use JDK 17 (Gradle 8.13 cannot create test tasks on JDK 24).
- The debug variant does not build because `google-services.json` has no client for `com.ott.platform.debug`
  (pre-existing, unrelated). The release variant is used for compile and test.
- 9 `PlaybackAuthorizationRepositoryImplTest` cases fail on a missing `getSessionUser()` stub; pre-existing and unrelated.

## Known limitations

- Users already signed in before this release have no app credential. They are asked to sign in once, on first Subscribe.
- `WebsiteApiClient` JSON parsing is not unit tested on the JVM (`org.json` is stubbed in unit tests). Compiles and is exercised
  end to end only against a real backend.
- Custom-scheme return link is not verified like an App Link (no `assetlinks.json`); safe because it triggers only a backend read.
