# ADR-001: Unified Web Codebase with Integrated /admin

- **Status:** PROPOSED (Established Architectural Preference)
- **Date:** 2026-10-16
- **Authors:** Principal Software Architect, Frontend Lead

---

## 1. Context and Problem Statement

The platform requires both a consumer-facing streaming web application (browse, search, stream HLS video, manage subscriptions) and an internal administrative Content Management System (CMS) for content ingestion, transcode monitoring, catalog publishing, subscription plan configuration, and user/tenant oversight.

We must decide whether to develop the consumer streaming website and the administrative CMS as two independent web projects (separate repositories, separate deployments) or within a single, unified web application codebase under `Website/` with `/admin` sub-routes.

---

## 2. Options Considered

### Option 1: Unified Frontend Codebase (`Website/`) with Integrated `/admin`
A single web application codebase (Next.js App Router or React Vite SPA) where public routes (`/`, `/browse`, `/watch/:id`, `/subscription`) and administrative routes (`/admin/*`) coexist.
- Server-side role checks and route middleware guard all `/admin/*` endpoints against non-admin callers.
- Shared authentication provider, shared UI component library (design tokens, modals, form controls), and unified API client.

### Option 2: Split Web Applications (Two Independent Repositories / Deployments)
Developing `ott-consumer-web` and `ott-admin-cms` as distinct projects with separate deployments (e.g., `watch.ott.example.com` and `admin.ott.example.com`).

---

## 3. Decision and Recommendation

**Adopt Option 1: Unified Frontend Codebase (`Website/`) with Integrated `/admin`.**

### Rationale:
1. **Operational Cost & Overhead:** Hosting a single web application significantly reduces build pipelines, hosting overhead, CDN distribution fees, and DNS complexity.
2. **Component & Logic Reuse:** Both consumer and CMS interfaces share design system tokens, typography, Firebase Authentication state, Firestore data types, error handling, and API client wrappers.
3. **Streamlined Developer Experience:** Eliminates cross-repository synchronization, multiple local dev servers, and duplicate dependency maintenance.
4. **Enforced Security Boundary:** Administrative route protection is enforced via server-side checks and Firebase Auth custom claims / Firestore user document verification (`isAdmin == true`). Security is guaranteed at the backend API and data layer regardless of UI bundling.

---

## 4. Consequences and Trade-offs

### Positive:
- Single Git repository (`ott-website.git`).
- Shared UI primitives, design tokens, and API client.
- Single deployment pipeline.

### Negative / Risks:
- Bundle size: Code splitting and dynamic imports (`React.lazy` or Next.js dynamic routing) must be enforced so consumer viewers do not download administrative bundles.
- Route guarding discipline: Strict client route guards and server-authoritative backend enforcement are mandatory to avoid exposing administrative views to standard subscribers.

---

## 5. Evidence and Approval Status

- **Evidence:** Documented in project requirements and Phase 1 architecture preferences.
- **Approval Status:** **ESTABLISHED ARCHITECTURAL PREFERENCE — PENDING OPERATOR SIGN-OFF**.
