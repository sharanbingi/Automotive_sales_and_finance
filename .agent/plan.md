# The real Firebase project has now been created.

Completed external setup:
- Firebase Android app registered
- app/google-services.json installed
- Firebase Authentication enabled
- Cloud Firestore Standard database created
- Firestore database is currently empty

DO NOT create production demo data.
DO NOT manually create Firestore collections.
DO NOT deploy anything yet.

Perform a FINAL PRE-DEPLOYMENT AUDIT of the existing firestore.rules.

Verify that the rules match the current Phase 1–5 multi-tenant architecture.

Expected primary structure:

dealerships/{dealershipId}
dealerships/{dealershipId}/stores/{storeId}
dealerships/{dealershipId}/users/{userId}
dealerships/{dealershipId}/bikes/{bikeId}
dealerships/{dealershipId}/customers/{customerId}
dealerships/{dealershipId}/loans/{loanId}
dealerships/{dealershipId}/transactions/{transactionId}
dealerships/{dealershipId}/notifications/{notificationId}
dealerships/{dealershipId}/auditLogs/{auditLogId}

Also inspect any required root-level /users/{uid} profile structure currently
used by AuthRepository/TenantContext.

Do not remove a root user profile if the current secure authentication
architecture legitimately requires it.

Verify:

1. Authentication is required for protected production data.

2. dealershipId tenant isolation cannot be bypassed.

3. Users cannot change their own:
  - role
  - dealershipId
  - storeId where protected
  - privileged account fields

4. DEALERSHIP_ADMIN cannot access another dealership.

5. STATE_MANAGER and STORE_MANAGER remain correctly scoped.

6. SALES_USER and FINANCE_USER have only their intended permissions.

7. CUSTOMER can access only permitted customer-owned data.

8. Protected loan/payment/transaction financial fields cannot be manipulated
   directly from the Android client.

9. Payment SUCCESS, loan balance changes and settlement cannot be authorized
   merely from client-controlled payment state.

10. Demo Mode does not depend on permissive production Firestore rules.

11. Legacy root production collections are denied unless specifically required
    by the current architecture.

12. There are no fallback dealership IDs or wildcard cross-tenant permissions.

13. Rules syntax is valid and deployment-ready.

14. Compare the rules with the actual repository paths currently used by the
    Android application and Cloud Functions.

15. Identify any repository/rule path mismatch before deployment.

Fix only genuine security/configuration problems discovered.

Preserve all completed Phase 1–5 functionality.

At completion report:

- Firestore rules audit result
- Tenant isolation result
- Role authorization result
- Root /users requirement
- Financial-write protection result
- Repository/rules path consistency
- Files changed
- Any blockers
- Whether firestore.rules is SAFE TO DEPLOY

DO NOT run firebase deploy yet. Project Plan

AUTOMOTIVE SALES & FINANCE - A multi-store automotive/motorcycle dealership management and finance application for Android (com.automotive.salesfinance).
Key features:
1. Multi-state, multi-store architecture with role-based access control (ADMIN, STATE_MANAGER, STORE_MANAGER, SALES_USER, FINANCE_USER, CUSTOMER).
2. Bike Inventory Management: Bike model (chassis/frame number as bikeId, engine number, make, model, year, costPrice, listedPrice, stateCode, storeLocation, status: AVAILABLE/RESERVED/FINANCED, inwardTimestamp). Preventive validation against duplicate chassis/engine numbers.
3. 60-Day Dead Stock Identification and tracking.
4. Customer Management with validation, vehicle mapping, EMI schedules, payment history.
5. Vehicle Loan & Finance module: Reducing balance EMI calculator, financing workflow updating bike status to FINANCED.
6. Payments: Razorpay integration with backend verification architecture (Firebase Cloud Functions / secure server verification), payment screen and history.
7. Dashboard: KPI metrics (Total/Available/Reserved/Financed bikes, Active/Overdue loans, Dead stock, Today's collection, Total stores) and quick alerts.
8. Navigation & Modern Material 3 UI/UX with light/dark theme support, INR currency formatting (₹), bottom bar, drawer, search/filter bars, cards.
9. Firebase Backend Integration: Authentication, Cloud Firestore (nested store-level inventory: stores/{storeId}/bikes/{bikeId}), Firebase Storage, FCM, Cloud Functions.
10. Demo/Data Mode for offline testing with TG_Madhapur store and sample data.
11. Clean Architecture: MVVM, Repositories, Coroutines, StateFlow, ViewModel, Navigation Compose.
12. Security & Validation, Cloud Firestore Security Rules, Cloud Functions for Razorpay verification.

## Project Brief

# Project Brief: Automotive Sales & Finance

## Overview
Automotive Sales & Finance (`com.automotive.salesfinance`) is a multi-store automotive and motorcycle dealership management and finance application for Android. Designed for speed, scalability, and ease of use, this Minimum Viable Product (MVP) streamlines inventory tracking, loan financing, payment collection, and multi-store analytics.

---

## Features

1. **Multi-Store Inventory & Dead Stock Tracking**
   Manage motorcycle inventory across store locations using unique chassis/frame and engine numbers with duplicate validation. Track inventory statuses (`AVAILABLE`, `RESERVED`, `FINANCED`) and flag 60-day dead stock for proactive clearance.

2. **Vehicle Loan & EMI Financing Workflow**
   Built-in reducing balance EMI calculator enabling sales and finance users to structure loan offers, process financing requests, and automatically transition bike status to `FINANCED` upon approval.

3. **Payment Gateway Integration & Verification**
   Seamless digital payment collection via Razorpay integration with secure backend verification using Firebase Cloud Functions, complete with payment processing screens and history logs.

4. **Multi-Store KPI Dashboard**
   Real-time overview of key dealership metrics: total/available/financed inventory counts, dead stock count, active/overdue loans, daily collections, and store alerts.

5. **Customer Management & Vehicle Mapping**
   Maintain customer profiles mapped directly to purchased vehicles, active EMI repayment schedules, and complete transaction histories.

---

## High-Level Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3 (supporting Light/Dark themes and INR `₹` currency formatting)
- **Navigation & Adaptive Strategy**: Jetpack Navigation 3 (state-driven) and **Compose Material Adaptive** library for responsive multi-pane layouts
- **Architecture**: Clean Architecture (MVVM, Repositories, Coroutines, StateFlow, ViewModel)
- **Backend & Cloud Services**: 
  - **Firebase Authentication**: User authentication and role-based access control (ADMIN, STORE_MANAGER, SALES_USER, FINANCE_USER, CUSTOMER)
  - **Cloud Firestore**: Hierarchical document store (`stores/{storeId}/bikes/{bikeId}`)
  - **Firebase Cloud Functions**: Server-side verification for Razorpay payments
  - **Firebase Storage & FCM**: File management and push notifications

## Implementation Steps
**Total Duration:** 3h 31m 22s

### Task_1_CoreArchitectureAndDataLayer: Setup Firebase models, repositories, multi-store context, auth, demo mode, and navigation structure.
- **Status:** COMPLETED
- **Updates:** Task 1 complete: Firebase models, dual-mode repositories (Firestore + Demo Mode), AuthViewModel, utility classes (CurrencyUtils, DateUtils, ValidationUtils), Material 3 theme, navigation routing structure, and unit tests implemented and verified.
- **Acceptance Criteria:**
  - project builds successfully
  - Firebase data layer and repositories created
  - Demo mode and auth setup complete
- **Duration:** 31m 55s

### Task_2_InventoryAndFinancingWorkflow: Build Multi-Store Inventory with Dead Stock Tracking, Vehicle Loan & EMI Financing workflow screens and ViewModels.
- **Status:** COMPLETED
- **Updates:** Task 2 complete: ViewModels (InventoryViewModel, FinanceViewModel) and Jetpack Compose UI Screens (InventoryScreen, AddBikeScreen, BikeDetailsScreen, DeadStockScreen, EmiCalculatorScreen, FinanceScreen, LoanDetailsScreen, LoanApplicationScreen) implemented with reducing balance EMI calculator, multi-store filtering, dead stock tracking (>60 days), and transactional status updates. Build and tests passed.
- **Acceptance Criteria:**
  - Multi-store inventory and dead stock tracking working
  - Loan EMI calculator and financing workflow UI functional
  - build pass
- **Duration:** 9m 36s

### Task_3_MultiTenantDataAndSubscriptionArchitecture: Implement multi-tenant data models, Dealership model, SessionManager, TenantContext, FeatureAccessManager, Subscription plans, Audit logging, and scope existing repositories to dealershipId.
- **Status:** COMPLETED
- **Updates:** Task 3 complete: Multi-tenant SaaS architecture implemented with Dealership model, Subscription & Plan models (Starter, Professional, Business, Enterprise), FeatureAccessManager, TenantContext, SessionManager, AuditLog, SupportTicket models, updated User/Store/Bike/Customer/Loan/Transaction models with dealershipId scoping, updated Repositories, DemoData, and multi-tenant firestore.rules. Build and 39 unit tests passed.
- **Acceptance Criteria:**
  - Multi-tenant data models, Dealership, SessionManager, TenantContext, and FeatureAccessManager implemented
  - Repositories and Firestore rules updated for dealershipId scoping
  - build pass
- **Duration:** 28m 27s

### Task_4_SuperAdminAndDealershipDashboards: Build Super Admin Dashboard, Dealership Admin Dashboard, Subscription Plan UI, Audit Log Viewer, and Support Ticket Management screens and ViewModels.
- **Status:** COMPLETED
- **Updates:** Task 4 complete: Super Admin Dashboard, Dealership Admin Dashboard, Subscription Screen with tier comparison (Starter, Professional, Business, Enterprise), Audit Log Screen, Support Ticket Screen, ViewModels (SuperAdminViewModel, DealershipAdminViewModel, SubscriptionViewModel, SupportViewModel), trial & limit guard banners, and unit tests implemented and verified. Build and 50 unit tests passed.
- **Acceptance Criteria:**
  - Super Admin Dashboard and Dealership Admin Dashboard functional
  - Subscription management, Audit Log UI, and Support Ticket flow integrated
  - build pass
- **Duration:** 13m 17s

### Task_5_PaymentsAndPreservedCoreFeatures: Complete payment processing with Razorpay integration, backend Cloud Functions verification, customer management, reports, and feature access checks across preserved dealership flows.
- **Status:** COMPLETED
- **Updates:** Task 5 complete: Payment processing architecture verified with separate Customer EMI payment and SaaS Subscription payment flows, Cloud Functions backend verification signatures in functions/index.js, preserved core dealership features (Inventory, 60-day Dead Stock, Customers, Reducing Balance EMI Loan Financing, Reports, Settings), and FeatureAccessManager limits enforcement across all screens. Build and 52 unit tests passed.
- **Acceptance Criteria:**
  - API_KEY integration for payment gateway verified
  - Payment flow, Cloud Functions verification, and feature access control verified
  - Customer management and reports integrated with multi-tenant context
  - build pass
- **Duration:** 2h 5m 58s

### Task_6_RunAndVerify: Build and run the application, verify multi-tenant platform, SaaS subscriptions, dashboards, preserved core features, and security rules. Instruct critic_agent to verify app stability, requirement alignment, and report UI issues.
- **Status:** COMPLETED
- **Updates:** Task 6 complete: Project build assembled successfully (assembleDebug), unit tests executed and passed, firestore.rules, functions/index.js, functions/package.json verified, and comprehensive README.md documentation completed.
- **Acceptance Criteria:**
  - project builds successfully
  - make sure all existing tests pass
  - app does not crash
  - verified application stability and alignment with requirements
- **Duration:** 2m 9s

