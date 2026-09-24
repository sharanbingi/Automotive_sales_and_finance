# Multi-Tenant Two-Wheeler Automotive Sales & Finance Management Platform

A modern, enterprise-grade multi-tenant Android platform built for Two-Wheeler Dealerships, Automotive Retailers, and Vehicle Finance Operations. Powered by **Jetpack Compose (Material Design 3 Expressive)**, **Firebase (Firestore, Authentication, Cloud Functions)**, and **Razorpay Payment Gateway**.

---

## 📋 Table of Contents
1. [Architecture](#1-architecture)
2. [Multi-Tenant Details](#2-multi-tenant-details)
3. [Firestore Structure](#3-firestore-structure)
4. [Authentication](#4-authentication)
5. [Roles & Permissions (RBAC)](#5-roles--permissions-rbac)
6. [Security Rules](#6-security-rules)
7. [Subscriptions & SaaS Plans](#7-subscriptions--saas-plans)
8. [Payments & Razorpay Integration](#8-payments--razorpay-integration)
9. [Cloud Functions Setup](#9-cloud-functions-setup)
10. [Demo Mode](#10-demo-mode)
11. [Backup & Recovery](#11-backup--recovery)
12. [Testing & Validation](#12-testing--validation)
13. [Environment Configuration](#13-environment-configuration)
14. [Production Deployment](#14-production-deployment)

---

## 1. Architecture

The application is engineered using Android industry best practices and clean software architecture:

- **UI Layer**: Built entirely with **Jetpack Compose (Material Design 3 Expressive System)**, adhering to Edge-to-Edge display (`enableEdgeToEdge()`), Dynamic Color theming, and responsive adaptive layouts (`ListDetailPaneScaffold`, `Compose Navigation 3`).
- **State Management**: Reactive state handling using Kotlin `StateFlow`, `SharedFlow`, and Lifecycle-aware ViewModels scoped to Navigation 3 backstack entries.
- **Domain & Repository Layer**: Decoupled repository layer for Auth, Customer, Dealership, Inventory, Loan, Store, Subscription, Support Ticket, and Audit Logging.
- **Data Layer**:
  - **Firebase Firestore**: Production Cloud Database with real-time listeners and multi-tenant security rules.
  - **Firebase Auth**: User identity management supporting Email/Password, Google Sign-in, and verified credential flows.
  - **Local Demo Provider**: In-memory pre-seeded mock dataset (`DemoData.kt`) for instant evaluation and offline demo mode.
- **Backend Services**: Firebase Cloud Functions (Node.js 18 runtime) for server-side Razorpay order creation and HMAC-SHA256 signature verification.

---

## 2. Multi-Tenant Details

The system provides robust multi-tenant data isolation and organizational context management:

- **Tenant Boundary**: Each dealership acts as an isolated tenant identified by a unique `dealershipId`.
- **Multi-Store Scope**: Dealerships can operate multiple physical store branches (`storeId`), with state-level supervisory management (`stateCode`).
- **Context Injection**: `TenantContext` and `SessionManager` maintain the active tenant, store context, and logged-in user role across all ViewModels and Repositories.
- **Cross-Tenant Data Isolation**: Queries and mutations are automatically restricted by `dealershipId` both at the application repository level and enforced strictly at the database level by Firestore Security Rules.
- **Super Admin Oversight**: Platform Super Administrators retain global cross-tenant visibility for onboarding, monitoring, SaaS plan management, and platform analytics.

---

## 3. Firestore Structure

The database schema is organized into tenant-scoped subcollections and global system collections:

### Tenant Subcollections: `/dealerships/{dealershipId}`
- **`/dealerships/{dealershipId}`**: Metadata (name, code, GSTIN, owner, plan, status).
  - **`/stores/{storeId}`**: Store branches under the dealership (name, address, city, state).
    - **`/bikes/{bikeId}`**: Bike inventory assigned to specific store (model, variant, color, price, stock, status, deadStock days).
  - **`/customers/{customerId}`**: Customer profiles (name, phone, email, KYC status, address).
  - **`/loans/{loanId}`**: Loan applications (principal, interest, tenure, EMI, balance, status).
  - **`/transactions/{transactionId}`**: Payment history (amount, paymentMethod, type, status, timestamp).
  - **`/audit_logs/{logId}`**: Immutable audit logs (action, performedBy, userRole, details, timestamp).

### Global Collections
- **`/users/{userId}`**: User profiles (`role`, `dealershipId`, `storeId`, `stateCode`, `email`, `name`).
- **`/subscriptions/{subscriptionId}`**: Active SaaS subscriptions for dealerships (`planId`, `status`, `nextBillingDate`).
- **`/support_tickets/{ticketId}`**: Customer & dealership support tickets (`subject`, `priority`, `status`).
- **`/payment_orders/{orderId}`**: EMI payment intents created by Cloud Functions.
- **`/subscription_orders/{orderId}`**: SaaS subscription orders created by Cloud Functions.

---

## 4. Authentication

- **Provider**: Firebase Authentication.
- **Supported Methods**:
  - Email & Password.
  - Google Sign-In credentials.
  - Verified Credential Manager / OTP-less flows.
- **Auth Flow**:
  1. User authenticates via `AuthRepository`.
  2. System fetches corresponding `/users/{userId}` document from Firestore.
  3. `SessionManager` initializes user role, `dealershipId`, and store scope in `TenantContext`.
  4. App Navigation routes user to the role-appropriate screen (Super Admin, Dealership Admin, Sales Executive, Finance Officer, or Customer portal).

---

## 5. Roles & Permissions (RBAC)

The platform enforces Role-Based Access Control across 7 hierarchy levels:

| Role | Scope | Key Capabilities |
|---|---|---|
| `SUPER_ADMIN` | Global Platform | Onboard dealerships, manage global SaaS subscriptions, view platform analytics, inspect audit logs. |
| `DEALERSHIP_ADMIN` | Dealership Tenant | Manage stores, assign staff roles, view dealership financial reports, upgrade SaaS plans. |
| `STATE_MANAGER` | State Stores | Oversee all stores within assigned `stateCode`, review multi-store sales and inventory. |
| `STORE_MANAGER` | Single Store | Operational oversight of store inventory, dead stock tracking, loan review. |
| `SALES_USER` | Single Store | Add customers, search inventory, book bike sales, submit loan applications. |
| `FINANCE_USER` | Single Store | EMI calculation, loan approval/rejection processing, record manual/online payments. |
| `CUSTOMER` | Self Account | View active vehicle loans, check EMI schedule, pay EMIs via Razorpay, open support tickets. |

---

## 6. Security Rules

Database security is governed by root `firestore.rules`:

- **Authentication Guard**: `isAuthenticated()` verifies caller identity.
- **Tenant Isolation**: `isTenantUser(dealershipId)` ensures requests match caller's assigned `dealershipId` or `SUPER_ADMIN` status.
- **Role Validation**: Helper functions (`isSuperAdmin()`, `isDealershipAdmin()`, `isStaff()`) enforce role privileges for mutations.
- **Field Immutability**: Security rules prevent users from altering critical profile fields (`role`, `dealershipId`, `storeId`, `stateCode`).
- **Audit Protection**: Audit logs are read-restricted to Admins and update/delete restricted to Super Admin.

---

## 7. Subscriptions & SaaS Plans

Tiered pricing plans managed by `FeatureAccessManager`:

1. **STARTER Plan**: Single store support, up to 50 inventory items, standard reports.
2. **PROFESSIONAL Plan**: Up to 5 stores, 500 inventory items, advanced analytics, dead stock alerts, EMI payment tracking.
3. **ENTERPRISE Plan**: Unlimited stores, unlimited inventory, custom branding, multi-state supervisory access, 24/7 priority support, raw data export.

Dealership Admins can review and upgrade their subscription directly from `SubscriptionScreen.kt` using Razorpay.

---

## 8. Payments & Razorpay Integration

The payment pipeline supports both live online Razorpay transactions and simulated verification fallback:

- **EMI Payment Workflow**:
  1. Customer initiates EMI payment on `PaymentScreen.kt`.
  2. Android app invokes Callable Cloud Function `createRazorpayOrder`.
  3. Razorpay Checkout SDK / UI displays payment options (UPI, Cards, Netbanking).
  4. Upon completion, app invokes `verifyRazorpayPayment` passing `orderId`, `paymentId`, and `signature`.
  5. Cloud Function verifies HMAC-SHA256 signature, records `Transaction` in Firestore, updates remaining loan balance, and auto-settles loan if balance reaches zero.
- **SaaS Subscription Workflow**:
  1. Dealership Admin selects plan on `SubscriptionScreen.kt`.
  2. Cloud Function `createSubscriptionOrder` creates order.
  3. Cloud Function `verifySubscriptionPayment` validates HMAC signature, activates dealership subscription plan, and updates `subscriptions` collection.

---

## 9. Cloud Functions Setup

Located in root `/functions` directory:

- **Files**:
  - `functions/index.js`: Main backend logic containing callable functions (`createRazorpayOrder`, `verifyRazorpayPayment`, `createSubscriptionOrder`, `verifySubscriptionPayment`).
  - `functions/package.json`: Dependencies (`firebase-admin`, `firebase-functions`, `razorpay`, `crypto`).
- **Prerequisites**: Node.js 18 & Firebase CLI installed.
- **Local Testing**:
  ```bash
  cd functions
  npm install
  npm run serve
  ```
- **Deployment**:
  ```bash
  firebase deploy --only functions
  ```

---

## 10. Demo Mode

To allow effortless demonstration without configuring active Firebase credentials:

- **Toggle**: Switch between Real Firebase Mode and Demo Mode anytime via Settings screen or `SessionManager.setDemoMode(true)`.
- **Pre-seeded Dataset (`DemoData.kt`)**:
  - **Dealerships**: Apex Motors, Apex North Branch, Speed Two-Wheelers.
  - **Stores**: Apex Downtown Store, Apex Highway Branch, Speed Central Store.
  - **Bikes**: Hero Splendor Plus, Honda Shine 125, TVS Apache RTR 160, Royal Enfield Classic 350, Bajaj Pulsar N160, Ather 450X EV, Ola S1 Pro.
  - **Users**: Pre-configured accounts for all 7 roles with quick-login buttons.
  - **Loans & Customers**: Sample loans across various statuses (`PENDING`, `APPROVED`, `DISBURSED`, `SETTLED`).
  - **Transactions & Audit Logs**: Simulated financial payments and system logs.

---

## 11. Backup & Recovery

- **Automated Firestore Backups**: Scheduled via Google Cloud Scheduler and Cloud Storage:
  ```bash
  gcloud firestore export gs://YOUR_BACKUP_BUCKET_NAME/backups/$(date +%Y%m%d)
  ```
- **Restoration**:
  ```bash
  gcloud firestore import gs://YOUR_BACKUP_BUCKET_NAME/backups/YYYYMMDD
  ```
- **Android App Auto Backup**: Configured via `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` to protect local encryption keys and user preferences safely.

---

## 12. Testing & Validation

### Unit Tests
Execute unit tests to verify repository logic, multi-tenant isolation, payment calculation, and currency formatting:
```bash
./gradlew testDebugUnitTest
```
- `MultiTenantArchitectureTest.kt`: Validates tenant isolation, store context scoping, and RBAC permission checks.
- `InventoryRepositoryTest.kt`: Validates bike stock management and dead stock filtering.
- `PaymentServiceTest.kt`: Validates Razorpay HMAC signature generation and payment flow logic.
- `CurrencyUtilsTest.kt`: Validates Indian Rupee (₹) currency formatting.

### Build Verification
Verify APK generation:
```bash
./gradlew assembleDebug
```

---

## 13. Environment Configuration

1. **`local.properties`**:
   Add Razorpay API keys (optional for real transactions):
   ```properties
   RAZORPAY_KEY_ID=rzp_test_AutoSalesFin2025
   RAZORPAY_KEY_SECRET=rzp_secret_key_testing_98765
   ```
2. **`google-services.json`**:
   Place your Firebase project config file in `app/google-services.json`.

---

## 14. Production Deployment

1. **Firestore Security Rules**:
   ```bash
   firebase deploy --only firestore:rules
   ```
2. **Cloud Functions**:
   ```bash
   cd functions && npm install && firebase deploy --only functions
   ```
3. **Build Production Release APK / Android App Bundle (AAB)**:
   ```bash
   ./gradlew assembleRelease
   # Or for Google Play Store upload
   ./gradlew bundleRelease
   ```
4. **Publish**: Upload the generated `.aab` file from `app/build/outputs/bundle/release/` to the Google Play Console.

---
*Automotive Sales & Finance Platform — Engineered for Excellence.*
