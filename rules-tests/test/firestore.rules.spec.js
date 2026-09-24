const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const fs = require('fs');
const assert = require('assert');

describe('Firestore Security Rules Tests', function() {
  let testEnv;
  this.timeout(30000);

  before(async function() {
    testEnv = await initializeTestEnvironment({
      projectId: 'automotive-sales-finance-test',
      firestore: {
        rules: fs.readFileSync('firestore.rules', 'utf8'),
        host: 'localhost',
        port: 8080
      }
    });
  });

  after(async function() {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  beforeEach(async function() {
    this.timeout(10000);
    await testEnv.clearFirestore();
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.doc('users/dealership_admin_user').set({
        role: 'DEALERSHIP_ADMIN',
        dealershipId: 'sharan_motors',
        storeId: 'ALL',
        active: true
      });
      await db.doc('users/store_manager_user').set({
        role: 'STORE_MANAGER',
        dealershipId: 'sharan_motors',
        storeId: 'TG_Madhapur',
        active: true
      });
      await db.doc('users/finance_user').set({
        role: 'FINANCE_USER',
        dealershipId: 'sharan_motors',
        storeId: 'TG_Madhapur',
        active: true
      });
      await db.doc('users/sales_user').set({
        role: 'SALES_USER',
        dealershipId: 'sharan_motors',
        storeId: 'TG_Madhapur',
        active: true
      });
      await db.doc('users/sales_user_all').set({
        role: 'SALES_USER',
        dealershipId: 'sharan_motors',
        storeId: 'ALL',
        active: true
      });
      await db.doc('users/state_manager_user').set({
        role: 'STATE_MANAGER',
        dealershipId: 'sharan_motors',
        storeId: 'TG_Madhapur',
        active: true
      });
      await db.doc('users/state_manager_all').set({
        role: 'STATE_MANAGER',
        dealershipId: 'sharan_motors',
        storeId: 'ALL',
        active: true
      });
      await db.doc('users/admin_user').set({
        role: 'ADMIN',
        dealershipId: 'sharan_motors',
        storeId: 'TG_Madhapur',
        active: true
      });
      await db.doc('users/customer_user').set({
        role: 'CUSTOMER',
        dealershipId: 'sharan_motors',
        active: true
      });
      await db.doc('users/other_tenant_user').set({
        role: 'DEALERSHIP_ADMIN',
        dealershipId: 'other_motors',
        active: true
      });
      await db.doc('users/super_admin_user').set({
        role: 'SUPER_ADMIN',
        dealershipId: 'sharan_motors',
        active: true
      });
      await db.doc('users/manager_store_a').set({
        role: 'STORE_MANAGER',
        dealershipId: 'sharan_motors',
        storeId: 'Store_A',
        active: true
      });

      // Seed expense test documents
      await db.doc('dealerships/other_motors/stores/TG_Madhapur/expenses/exp_72').set({
        expenseId: 'exp_72', dealershipId: 'other_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'other_tenant_user', amountPaise: 1000, approvalStatus: 'DRAFT',
        paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/Store_B/expenses/exp_73').set({
        expenseId: 'exp_73', dealershipId: 'sharan_motors', storeId: 'Store_B',
        createdByUserId: 'usr_store_b', amountPaise: 1000, approvalStatus: 'DRAFT',
        paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_74').set({
        expenseId: 'exp_74', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', amountPaise: 1000, approvalStatus: 'DRAFT',
        paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_75').set({
        expenseId: 'exp_75', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', submittedByUserId: 'store_manager_user',
        amountPaise: 1000, approvalStatus: 'SUBMITTED', paymentStatus: 'UNPAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_76').set({
        expenseId: 'exp_76', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'finance_user', submittedByUserId: 'store_manager_user',
        amountPaise: 1000, approvalStatus: 'SUBMITTED', paymentStatus: 'UNPAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_77').set({
        expenseId: 'exp_77', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', submittedByUserId: 'finance_user',
        amountPaise: 1000, approvalStatus: 'SUBMITTED', paymentStatus: 'UNPAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_78').set({
        expenseId: 'exp_78', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', submittedByUserId: 'store_manager_user',
        amountPaise: 1000, approvalStatus: 'SUBMITTED', paymentStatus: 'UNPAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_79').set({
        expenseId: 'exp_79', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', submittedByUserId: 'store_manager_user',
        amountPaise: 1000, approvalStatus: 'REJECTED', paymentStatus: 'UNPAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_80').set({
        expenseId: 'exp_80', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', submittedByUserId: 'store_manager_user',
        amountPaise: 1000, approvalStatus: 'APPROVED', paymentStatus: 'UNPAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_81').set({
        expenseId: 'exp_81', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', submittedByUserId: 'store_manager_user',
        amountPaise: 1000, approvalStatus: 'SUBMITTED', paymentStatus: 'UNPAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_82').set({
        expenseId: 'exp_82', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', submittedByUserId: 'store_manager_user',
        amountPaise: 1000, approvalStatus: 'APPROVED', paymentStatus: 'PAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_83').set({
        expenseId: 'exp_83', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', submittedByUserId: 'store_manager_user',
        amountPaise: 1000, approvalStatus: 'APPROVED', paymentStatus: 'UNPAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_84').set({
        expenseId: 'exp_84', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', amountPaise: 1000, approvalStatus: 'DRAFT',
        paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_85').set({
        expenseId: 'exp_85', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', amountPaise: 1000, approvalStatus: 'DRAFT',
        paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_86').set({
        expenseId: 'exp_86', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', amountPaise: 1000, approvalStatus: 'DRAFT',
        paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_87').set({
        expenseId: 'exp_87', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', amountPaise: 1000, approvalStatus: 'DRAFT',
        paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_88').set({
        expenseId: 'exp_88', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', amountPaise: 1000, approvalStatus: 'DRAFT',
        paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_89').set({
        expenseId: 'exp_89', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', submittedByUserId: 'store_manager_user',
        amountPaise: 1000, approvalStatus: 'SUBMITTED', paymentStatus: 'UNPAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_90').set({
        expenseId: 'exp_90', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', submittedByUserId: 'store_manager_user',
        amountPaise: 1000, approvalStatus: 'SUBMITTED', paymentStatus: 'UNPAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_91').set({
        expenseId: 'exp_91', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', amountPaise: 1000, approvalStatus: 'DRAFT',
        paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_92').set({
        expenseId: 'exp_92', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', amountPaise: 1000, approvalStatus: 'DRAFT',
        paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_93').set({
        expenseId: 'exp_93', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', amountPaise: 1000, approvalStatus: 'DRAFT',
        paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_94').set({
        expenseId: 'exp_94', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', submittedByUserId: 'store_manager_user',
        amountPaise: 1000, approvalStatus: 'APPROVED', paymentStatus: 'PAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
      await db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_95').set({
        expenseId: 'exp_95', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
        createdByUserId: 'store_manager_user', submittedByUserId: 'store_manager_user',
        amountPaise: 1000, approvalStatus: 'APPROVED', paymentStatus: 'PAID',
        category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now()
      });
    });
  });

  // 1. User Profile & RBAC Rules
  it('Test 1: User can read their own user profile document', async () => {
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertSucceeds(db.doc('users/sales_user').get());
  });

  it('Test 2: User cannot read another user profile outside their dealership tenant context', async () => {
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertFails(db.doc('users/other_tenant_user').get());
  });

  it('Test 3: Dealership admin can read user profiles in their dealership', async () => {
    const db = testEnv.authenticatedContext('dealership_admin_user').firestore();
    await assertSucceeds(db.doc('users/sales_user').get());
  });

  it('Test 4: Dealership admin can create staff user in their dealership but not super_admin', async () => {
    const db = testEnv.authenticatedContext('dealership_admin_user').firestore();
    await assertSucceeds(db.doc('users/new_staff').set({
      role: 'SALES_USER',
      dealershipId: 'sharan_motors',
      active: true
    }));
    await assertFails(db.doc('users/new_admin_attempt').set({
      role: 'SUPER_ADMIN',
      dealershipId: 'sharan_motors',
      active: true
    }));
  });

  it('Test 5: Dealership admin cannot update role or dealershipId of users', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('users/staff_target').set({
        role: 'SALES_USER',
        dealershipId: 'sharan_motors',
        active: true
      });
    });
    const db = testEnv.authenticatedContext('dealership_admin_user').firestore();
    await assertFails(db.doc('users/staff_target').update({
      role: 'DEALERSHIP_ADMIN'
    }));
    await assertSucceeds(db.doc('users/staff_target').update({
      active: false
    }));
  });

  it('Test 6: Customer user can update own profile except protected fields', async () => {
    const db = testEnv.authenticatedContext('customer_user').firestore();
    await assertSucceeds(db.doc('users/customer_user').update({
      displayName: 'Updated Customer Name'
    }));
    await assertFails(db.doc('users/customer_user').update({
      active: false
    }));
  });

  it('Test 7: Super admin can create, read, update, and delete any user profile', async () => {
    const db = testEnv.authenticatedContext('super_admin_user').firestore();
    await assertSucceeds(db.doc('users/any_user').set({
      role: 'SUPER_ADMIN',
      dealershipId: 'any',
      active: true
    }));
    await assertSucceeds(db.doc('users/any_user').get());
    await assertSucceeds(db.doc('users/any_user').update({ active: false }));
    await assertSucceeds(db.doc('users/any_user').delete());
  });

  // 2. Dealership & Multi-Tenancy Rules
  it('Test 8: Tenant user can read their own dealership document', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors').set({ dealershipId: 'sharan_motors', name: 'Sharan Motors' });
    });
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors').get());
  });

  it('Test 9: Tenant user cannot read or write another dealership document', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/other_motors').set({ dealershipId: 'other_motors', name: 'Other Motors' });
    });
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertFails(db.doc('dealerships/other_motors').get());
    await assertFails(db.doc('dealerships/other_motors').set({ dealershipId: 'other_motors', name: 'Hacked' }));
  });

  it('Test 10: Dealership admin can create and update their own dealership', async () => {
    const db = testEnv.authenticatedContext('dealership_admin_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors').set({
      dealershipId: 'sharan_motors',
      name: 'Sharan Motors New',
      subscriptionStatus: 'ACTIVE'
    }));
    await assertSucceeds(db.doc('dealerships/sharan_motors').update({
      name: 'Sharan Motors Updated'
    }));
    await assertFails(db.doc('dealerships/sharan_motors').update({
      subscriptionStatus: 'EXPIRED'
    }));
  });

  it('Test 11: Non-admin staff cannot delete a dealership', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors').set({ dealershipId: 'sharan_motors', name: 'Sharan Motors' });
    });
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors').delete());
  });

  // 3. Inventory & Store Rules (Stores, Bikes, Vehicles)
  it('Test 12: Staff can create and update bikes/vehicles in their dealership', async () => {
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/bikes/bike_1').set({
      dealershipId: 'sharan_motors',
      model: 'Apache RTR 160',
      price: 120000
    }));
    await assertSucceeds(db.doc('dealerships/sharan_motors/bikes/bike_1').update({
      price: 125000
    }));
  });

  it('Test 13: Staff cannot create bikes/vehicles with mismatched dealershipId (cross-tenant write blocked)', async () => {
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/bikes/bike_evil').set({
      dealershipId: 'other_motors',
      model: 'Hacked Bike',
      price: 1000
    }));
  });

  it('Test 14: Staff and Dealership Admin can delete bikes/vehicles in their dealership', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/bikes/bike_del').set({ dealershipId: 'sharan_motors', model: 'Del Model' });
    });
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/bikes/bike_del').delete());
  });

  // 4. Customer Rules
  it('Test 15: Staff and Dealership Admin can create and update customers with matching dealershipId', async () => {
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/customers/cust_1').set({
      dealershipId: 'sharan_motors',
      name: 'John Doe',
      phone: '9876543210'
    }));
    await assertSucceeds(db.doc('dealerships/sharan_motors/customers/cust_1').update({
      phone: '9123456789'
    }));
  });

  it('Test 16: Staff cannot create customers with mismatched dealershipId', async () => {
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/customers/cust_bad').set({
      dealershipId: 'other_motors',
      name: 'Bad Cust'
    }));
  });

  it('Test 17: Only Super Admin or Dealership Admin can delete customers', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/customers/cust_del').set({ dealershipId: 'sharan_motors', name: 'Del Cust' });
    });
    const salesDb = testEnv.authenticatedContext('sales_user').firestore();
    await assertFails(salesDb.doc('dealerships/sharan_motors/customers/cust_del').delete());

    const adminDb = testEnv.authenticatedContext('dealership_admin_user').firestore();
    await assertSucceeds(adminDb.doc('dealerships/sharan_motors/customers/cust_del').delete());
  });

  // 5. Loan & Financial Protection Rules
  it('Test 18: Staff can update general non-financial fields on a loan when financial fields are protected', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_1',
        remainingBalance: 50000,
        paidAmount: 10000,
        loanStatus: 'ACTIVE',
        subscriptionStatus: 'ACTIVE',
        paymentStatus: 'PAID',
        notes: 'Initial note'
      });
    });
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/loans/loan_1').update({
      notes: 'Updated note'
    }));
  });

  it('Test 19: Staff cannot update protected financial fields directly without cash payment flow or super admin', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_1',
        remainingBalance: 50000,
        paidAmount: 10000,
        loanStatus: 'ACTIVE',
        subscriptionStatus: 'ACTIVE',
        paymentStatus: 'PAID'
      });
    });
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/loans/loan_1').update({
      remainingBalance: 0
    }));
  });

  it('Test 20: Missing paise fields result in DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_1',
        remainingBalance: 5000, // Missing Paise field
        paidAmount: 15000, // Missing Paise field
        loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_1'), {
      dealershipId: 'sharan_motors',
      loanId: 'loan_1',
      customerId: 'cust_1',
      paymentMethod: 'CASH',
      status: 'SUCCESS',
      collectedByUserId: 'finance_user',
      amountPaise: 500000,
      idempotencyKey: '1'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), {
      remainingBalance: 0,
      paidAmount: 20000,
      loanStatus: 'SETTLED',
      lastPaymentTransactionId: 'TXN_CASH_1',

    });

    await assertFails(batch.commit());
  });

  it('Test 21: Authorized Cash Collector can perform valid cash payment loan update with complete paise data', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_1',
        remainingBalancePaise: 500000,
        paidAmountPaise: 1500000,
        loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_2'), {
      dealershipId: 'sharan_motors',
      loanId: 'loan_1',
      customerId: 'cust_1',
      paymentMethod: 'CASH',
      status: 'SUCCESS',
      collectedByUserId: 'finance_user',
      amountPaise: 500000,
      idempotencyKey: '2'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), {
      remainingBalancePaise: 0,
      paidAmountPaise: 2000000,
      loanStatus: 'SETTLED',
      lastPaymentTransactionId: 'TXN_CASH_2',

    });

    await assertSucceeds(batch.commit());
  });

  it('Test 22: STORE_MANAGER can perform valid cash payment loan update with complete paise data', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_1',
        remainingBalancePaise: 1000000,
        paidAmountPaise: 500000,
        loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_3'), {
      dealershipId: 'sharan_motors',
      loanId: 'loan_1',
      customerId: 'cust_1',
      paymentMethod: 'CASH',
      status: 'SUCCESS',
      collectedByUserId: 'store_manager_user',
      amountPaise: 200000,
      idempotencyKey: '3'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), {
      remainingBalancePaise: 800000,
      paidAmountPaise: 700000,
      loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_3',

    });

    await assertSucceeds(batch.commit());
  });

  it('Test 23: DEALERSHIP_ADMIN can perform valid cash payment loan update with complete paise data', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_1',
        remainingBalancePaise: 1000000,
        paidAmountPaise: 500000,
        loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('dealership_admin_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_4'), {
      dealershipId: 'sharan_motors',
      loanId: 'loan_1',
      customerId: 'cust_1',
      paymentMethod: 'CASH',
      status: 'SUCCESS',
      collectedByUserId: 'dealership_admin_user',
      amountPaise: 1000000,
      idempotencyKey: '4'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), {
      remainingBalancePaise: 0,
      paidAmountPaise: 1500000,
      loanStatus: 'SETTLED',
      lastPaymentTransactionId: 'TXN_CASH_4',

    });

    await assertSucceeds(batch.commit());
  });

  it('Test 24: Cash payment loan update fails if paid amount math mismatch', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_1',
        remainingBalancePaise: 500000,
        paidAmountPaise: 1500000,
        loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_5'), {
      dealershipId: 'sharan_motors',
      loanId: 'loan_1',
      customerId: 'cust_1',
      paymentMethod: 'CASH',
      status: 'SUCCESS',
      collectedByUserId: 'finance_user',
      amountPaise: 200000,
      idempotencyKey: '5'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), {
      remainingBalancePaise: 300000,
      paidAmountPaise: 1800000, // Should be 1700000
      loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_5',

    });

    await assertFails(batch.commit());
  });

  it('Test 25: Cash payment loan update fails if remaining balance math mismatch', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_1',
        remainingBalancePaise: 500000,
        paidAmountPaise: 1500000,
        loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_6'), {
      dealershipId: 'sharan_motors',
      loanId: 'loan_1',
      customerId: 'cust_1',
      paymentMethod: 'CASH',
      status: 'SUCCESS',
      collectedByUserId: 'finance_user',
      amountPaise: 200000,
      idempotencyKey: '6'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), {
      remainingBalancePaise: 200000, // Should be 300000
      paidAmountPaise: 1700000,
      loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_6',

    });

    await assertFails(batch.commit());
  });

  // 6. Transaction Ledger Rules & Immutability
  it('Test 26: Authorized Cash Collector CANNOT create a valid CASH transaction standalone', async () => {
    const db = testEnv.authenticatedContext('finance_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_new_1').set({
      dealershipId: 'sharan_motors',
      loanId: 'loan_1',
      customerId: 'cust_1',
      paymentMethod: 'CASH',
      status: 'SUCCESS',
      collectedByUserId: 'finance_user',
      amountPaise: 300000,
      idempotencyKey: 'new_1'
    }));
  });

  it('Test 27: Transaction create fails if amountPaise is missing', async () => {
    const db = testEnv.authenticatedContext('finance_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_new_2').set({
      dealershipId: 'sharan_motors',
      loanId: 'loan_1',
      customerId: 'cust_1',
      paymentMethod: 'CASH',
      status: 'SUCCESS',
      collectedByUserId: 'finance_user',
      amount: 3000, // missing amountPaise
      idempotencyKey: 'new_2'
    }));
  });

  it('Test 28: Staff can create a non-SUCCESS digital transaction (e.g. pending UPI/Razorpay initialization)', async () => {
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/transactions/txn_pending_1').set({
      dealershipId: 'sharan_motors',
      paymentMethod: 'UPI',
      status: 'PENDING',
      amountPaise: 500000
    }));
  });

  it('Test 29: Staff cannot create a digital SUCCESS transaction directly without gateway validation', async () => {
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/transactions/txn_forged_success').set({
      dealershipId: 'sharan_motors',
      paymentMethod: 'UPI',
      status: 'SUCCESS',
      amountPaise: 500000
    }));
  });

  it('Test 30: Staff and Dealership Admin cannot update an existing transaction (ledger immutability)', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/transactions/txn_immutable').set({
        dealershipId: 'sharan_motors',
        status: 'SUCCESS',
        amountPaise: 100000
      });
    });
    const db = testEnv.authenticatedContext('dealership_admin_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/transactions/txn_immutable').update({
      amountPaise: 200000
    }));
  });

  it('Test 31: Staff and Dealership Admin cannot delete an existing transaction', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/transactions/txn_del').set({
        dealershipId: 'sharan_motors',
        status: 'SUCCESS',
        amountPaise: 100000
      });
    });
    const db = testEnv.authenticatedContext('dealership_admin_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/transactions/txn_del').delete());
  });

  it('Test 32: Staff can read and create audit logs and support tickets for their dealership, but cannot update or delete audit logs', async () => {
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/audit_logs/log_1').set({
      dealershipId: 'sharan_motors',
      action: 'LOGIN',
      timestamp: Date.now()
    }));
    await assertSucceeds(db.doc('dealerships/sharan_motors/audit_logs/log_1').get());

    await assertFails(db.doc('dealerships/sharan_motors/audit_logs/log_1').update({
      action: 'TAMPERED'
    }));

    await assertSucceeds(db.doc('dealerships/sharan_motors/support_tickets/ticket_1').set({
      dealershipId: 'sharan_motors',
      subject: 'Help needed'
    }));
  });

  it('Test 33: Existing loan missing remainingBalancePaise: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_1',
        paidAmountPaise: 1500000,
        loanStatus: 'ACTIVE'
      });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_33'), {
      dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1',
      paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 500000, idempotencyKey: '33'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), {
      remainingBalancePaise: 0, paidAmountPaise: 2000000, loanStatus: 'SETTLED', lastPaymentTransactionId: 'TXN_CASH_33'
    });
    await assertFails(batch.commit());
  });

  it('Test 34: Existing loan missing paidAmountPaise: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1',
        remainingBalancePaise: 500000, loanStatus: 'ACTIVE'
      });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_34'), {
      dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 500000, idempotencyKey: '34'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), {
      remainingBalancePaise: 0, paidAmountPaise: 500000, loanStatus: 'SETTLED', lastPaymentTransactionId: 'TXN_CASH_34'
    });
    await assertFails(batch.commit());
  });

  it('Test 35: Requested updated loan missing remainingBalancePaise: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 500000, paidAmountPaise: 1500000, loanStatus: 'ACTIVE'
      });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_35'), {
      dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 500000, idempotencyKey: '35'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), {
      paidAmountPaise: 2000000, loanStatus: 'SETTLED', lastPaymentTransactionId: 'TXN_CASH_35'
    });
    await assertFails(batch.commit());
  });

  it('Test 36: Requested updated loan missing paidAmountPaise: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 500000, paidAmountPaise: 1500000, loanStatus: 'ACTIVE'
      });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_36'), {
      dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 500000, idempotencyKey: '36'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), {
      remainingBalancePaise: 0, loanStatus: 'SETTLED', lastPaymentTransactionId: 'TXN_CASH_36'
    });
    await assertFails(batch.commit());
  });

  it('Test 37: amountPaise = 0: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({ dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 500000, paidAmountPaise: 1500000, loanStatus: 'ACTIVE' });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_37'), { dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 0, idempotencyKey: '37' });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 500000, paidAmountPaise: 1500000, loanStatus: 'ACTIVE', lastPaymentTransactionId: 'TXN_CASH_37' });
    await assertFails(batch.commit());
  });

  it('Test 38: amountPaise < 0: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({ dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 500000, paidAmountPaise: 1500000, loanStatus: 'ACTIVE' });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_38'), { dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: -100, idempotencyKey: '38' });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 500100, paidAmountPaise: 1499900, loanStatus: 'ACTIVE', lastPaymentTransactionId: 'TXN_CASH_38' });
    await assertFails(batch.commit());
  });

  it('Test 39: Exact full settlement: ALLOW', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({ dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 363588, paidAmountPaise: 0, loanStatus: 'ACTIVE' });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_39'), { dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 363588, idempotencyKey: '39' });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 0, paidAmountPaise: 363588, loanStatus: 'SETTLED', lastPaymentTransactionId: 'TXN_CASH_39' });
    await assertSucceeds(batch.commit());
  });

  it('Test 40: Full settlement off by one paise: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({ dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 363588, paidAmountPaise: 0, loanStatus: 'ACTIVE' });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_40'), { dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 363587, idempotencyKey: '40' });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 0, paidAmountPaise: 363587, loanStatus: 'SETTLED', lastPaymentTransactionId: 'TXN_CASH_40' });
    await assertFails(batch.commit());
  });

  it('Test 41: Attempt to overpay beyond remaining paise: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({ dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE' });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_41'), { dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 100001, idempotencyKey: '41' });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: -1, paidAmountPaise: 100001, loanStatus: 'SETTLED', lastPaymentTransactionId: 'TXN_CASH_41' });
    await assertFails(batch.commit());
  });

  it('Test 42: Roles attempting CASH collection', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({ dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE' });
    });
    const rolesToFail = ['customer_user', 'sales_user', 'state_manager_user', 'super_admin_user'];
    for (const role of rolesToFail) {
      const db = testEnv.authenticatedContext(role).firestore();
      const batch = db.batch();
      batch.set(db.doc('dealerships/sharan_motors/transactions/txn_cash_' + role), { dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: role, amountPaise: 50000, idempotencyKey: '41' + role });
      batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE', lastPaymentTransactionId: 'txn_cash_' + role });
      await assertFails(batch.commit());
    }
  });

  it('Test 43: Cross-tenant CASH payment: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/other_motors/loans/loan_1').set({ dealershipId: 'other_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE' });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore(); // finance_user is in sharan_motors
    const batch = db.batch();
    batch.set(db.doc('dealerships/other_motors/transactions/TXN_CASH_43'), { dealershipId: 'other_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 50000, idempotencyKey: '43' });
    batch.update(db.doc('dealerships/other_motors/loans/loan_1'), { remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE', lastPaymentTransactionId: 'TXN_CASH_43' });
    await assertFails(batch.commit());
  });

  it('Test 44: Forged dealershipId, collectedByUserId, mismatched customer, mismatched loan', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({ dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE' });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();

    // Forged dealershipId
    let batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_44a'), { dealershipId: 'other_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 50000, idempotencyKey: '44a' });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE', lastPaymentTransactionId: 'TXN_CASH_44a' });
    await assertFails(batch.commit());

    // Forged collectedByUserId
    batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_44b'), { dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'dealership_admin_user', amountPaise: 50000, idempotencyKey: '44b' });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE', lastPaymentTransactionId: 'TXN_CASH_44b' });
    await assertFails(batch.commit());

    // Mismatched customer
    batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_44c'), { dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_wrong', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 50000, idempotencyKey: '44c' });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE', lastPaymentTransactionId: 'TXN_CASH_44c' });
    await assertFails(batch.commit());

    // Mismatched loan
    batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_44d'), { dealershipId: 'sharan_motors', loanId: 'loan_wrong', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 50000, idempotencyKey: '44d' });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE', lastPaymentTransactionId: 'TXN_CASH_44d' });
    await assertFails(batch.commit());
  });

  it('Test 45: CASH transaction with non-SUCCESS state where SUCCESS is required: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({ dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE' });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_45'), { dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'PENDING', collectedByUserId: 'finance_user', amountPaise: 50000, idempotencyKey: '45' });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE', lastPaymentTransactionId: 'TXN_CASH_45' });
    await assertFails(batch.commit());
  });

  it('Test 46: Loan update modifying an unrelated field in addition to payment fields: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({ dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE', interestRate: 10 });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_46'), { dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 50000, idempotencyKey: '46' });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE', lastPaymentTransactionId: 'TXN_CASH_46', interestRate: 5 }); // Not allowed
    await assertFails(batch.commit());
  });

  it('Test 47: Matching transaction amount Double appears correct but amountPaise is missing: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({ dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE' });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_47'), { dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amount: 500, idempotencyKey: '47' }); // Only Double is present
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE', lastPaymentTransactionId: 'TXN_CASH_47' });
    await assertFails(batch.commit());
  });

  it('Test 48: Legacy Double delta differs from paise delta but exact paise fields are correct: ALLOW', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_1').set({ dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_1', remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE' });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_48'), { dealershipId: 'sharan_motors', loanId: 'loan_1', customerId: 'cust_1', paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user', amountPaise: 50000, idempotencyKey: '48', amount: 500.01 }); // Double is wrong
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_1'), { remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE', lastPaymentTransactionId: 'TXN_CASH_48', remainingBalance: 499.99, paidAmount: 500.01 }); // Doubles are slightly off
    await assertSucceeds(batch.commit());
  });

  it('Test 49: Cash audit log delete by dealership staff: DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/audit_logs/log_del_1').set({ dealershipId: 'sharan_motors', action: 'LOGIN' });
    });
    const db = testEnv.authenticatedContext('dealership_admin_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/audit_logs/log_del_1').delete());
  });

  it('Test 50: Replay an existing CASH transaction -> DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      const db = c.firestore();
      await db.doc('dealerships/sharan_motors/loans/loan_replay').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_replay',
        remainingBalancePaise: 100000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
        lastPaymentTransactionId: 'TXN_CASH_original'
      });
      await db.doc('dealerships/sharan_motors/transactions/TXN_CASH_original').set({
        dealershipId: 'sharan_motors', loanId: 'loan_replay', customerId: 'cust_1',
        paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user',
        amountPaise: 50000, idempotencyKey: 'original'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_replay'), {
      remainingBalancePaise: 50000,
      paidAmountPaise: 100000,
      loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_original'
    });
    await assertFails(batch.commit());
  });

  it('Test 51: Mismatched deterministic ID -> DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_mismatch').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_mismatch',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_WRONG_ID'), {
      dealershipId: 'sharan_motors', loanId: 'loan_mismatch', customerId: 'cust_1',
      paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user',
      amountPaise: 50000, idempotencyKey: 'mismatch'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_mismatch'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_WRONG_ID'
    });
    await assertFails(batch.commit());
  });

  it('Test 52: Fresh valid atomic cash -> ALLOW', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_fresh').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_fresh',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
    });
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_fresh'), {
      dealershipId: 'sharan_motors', loanId: 'loan_fresh', customerId: 'cust_1',
      paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user',
      amountPaise: 50000, idempotencyKey: 'fresh'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_fresh'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_fresh'
    });
    await assertSucceeds(batch.commit());
  });

  // 7. Dedicated 20-Scenario Coverage Tests
  it('Test 53 (Scenario 3): Replay transaction after original payment already applied -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      const db = c.firestore();
      await db.doc('dealerships/sharan_motors/loans/loan_applied').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_applied',
        remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
        lastPaymentTransactionId: 'TXN_CASH_applied'
      });
      await db.doc('dealerships/sharan_motors/transactions/TXN_CASH_applied').set({
        dealershipId: 'sharan_motors', loanId: 'loan_applied', customerId: 'cust_1',
        paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user',
        amountPaise: 50000, idempotencyKey: 'applied'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_applied'), {
      remainingBalancePaise: 0,
      paidAmountPaise: 100000,
      loanStatus: 'SETTLED',
      lastPaymentTransactionId: 'TXN_CASH_applied'
    });
    await assertFails(batch.commit());
  });

  it('Test 54 (Scenario 4): Same lastPaymentTransactionId reused -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_reused_txn').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_reused_txn',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE',
        lastPaymentTransactionId: 'TXN_CASH_reused_id'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_reused_txn'), {
      remainingBalancePaise: 50000,
      paidAmountPaise: 50000,
      loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_reused_id'
    });
    await assertFails(batch.commit());
  });

  it('Test 55 (Scenario 6): CASH transaction paired with wrong loan -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_sc6').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_sc6',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_sc6'), {
      dealershipId: 'sharan_motors', loanId: 'wrong_loan_id', customerId: 'cust_1',
      paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user',
      amountPaise: 50000, idempotencyKey: 'sc6'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_sc6'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_sc6'
    });
    await assertFails(batch.commit());
  });

  it('Test 56 (Scenario 7): CASH transaction paired with wrong customer -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_sc7').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_sc7',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_sc7'), {
      dealershipId: 'sharan_motors', loanId: 'loan_sc7', customerId: 'wrong_cust_id',
      paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user',
      amountPaise: 50000, idempotencyKey: 'sc7'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_sc7'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_sc7'
    });
    await assertFails(batch.commit());
  });

  it('Test 57 (Scenario 8): CASH transaction paired with wrong dealership -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_sc8').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_sc8',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_sc8'), {
      dealershipId: 'other_motors', loanId: 'loan_sc8', customerId: 'cust_1',
      paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user',
      amountPaise: 50000, idempotencyKey: 'sc8'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_sc8'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_sc8'
    });
    await assertFails(batch.commit());
  });

  it('Test 58 (Scenario 11): Duplicate transaction document ID -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      const adminDb = c.firestore();
      await adminDb.doc('dealerships/sharan_motors/loans/loan_sc11').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_sc11',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
      await adminDb.doc('dealerships/sharan_motors/transactions/TXN_CASH_sc11').set({
        dealershipId: 'sharan_motors', loanId: 'loan_sc11', customerId: 'cust_1',
        paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user',
        amountPaise: 50000, idempotencyKey: 'sc11'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_sc11'), {
      dealershipId: 'sharan_motors', loanId: 'loan_sc11', customerId: 'cust_1',
      paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user',
      amountPaise: 50000, idempotencyKey: 'sc11'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_sc11'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_sc11'
    });
    await assertFails(batch.commit());
  });

  it('Test 59 (Scenario 12): Duplicate/reused idempotency key -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      const adminDb = c.firestore();
      await adminDb.doc('dealerships/sharan_motors/loans/loan_sc12').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_sc12',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
      await adminDb.doc('dealerships/sharan_motors/transactions/TXN_CASH_reused_key').set({
        dealershipId: 'sharan_motors', loanId: 'loan_sc12', customerId: 'cust_1',
        paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user',
        amountPaise: 50000, idempotencyKey: 'reused_key'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_reused_key'), {
      dealershipId: 'sharan_motors', loanId: 'loan_sc12', customerId: 'cust_1',
      paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user',
      amountPaise: 50000, idempotencyKey: 'reused_key'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_sc12'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_reused_key'
    });
    await assertFails(batch.commit());
  });

  it('Test 60 (Scenario 13): Existing legacy transaction used to authorize another loan reduction -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      const adminDb = c.firestore();
      await adminDb.doc('dealerships/sharan_motors/loans/loan_sc13').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_sc13',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
      await adminDb.doc('dealerships/sharan_motors/transactions/TXN_CASH_legacy_txn').set({
        dealershipId: 'sharan_motors', loanId: 'other_loan', customerId: 'cust_1',
        paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'finance_user',
        amountPaise: 50000, idempotencyKey: 'legacy_txn'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_sc13'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_legacy_txn'
    });
    await assertFails(batch.commit());
  });

  it('Test 61 (Scenario 16): SUPER_ADMIN CASH -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_sc16').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_sc16',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('super_admin_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_sc16'), {
      dealershipId: 'sharan_motors', loanId: 'loan_sc16', customerId: 'cust_1',
      paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'super_admin_user',
      amountPaise: 50000, idempotencyKey: 'sc16'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_sc16'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_sc16'
    });
    await assertFails(batch.commit());
  });

  it('Test 62 (Scenario 17): STATE_MANAGER CASH -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_sc17').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_sc17',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('state_manager_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_sc17'), {
      dealershipId: 'sharan_motors', loanId: 'loan_sc17', customerId: 'cust_1',
      paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'state_manager_user',
      amountPaise: 50000, idempotencyKey: 'sc17'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_sc17'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_sc17'
    });
    await assertFails(batch.commit());
  });

  it('Test 63 (Scenario 18): SALES_USER CASH -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_sc18').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_sc18',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('sales_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_sc18'), {
      dealershipId: 'sharan_motors', loanId: 'loan_sc18', customerId: 'cust_1',
      paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'sales_user',
      amountPaise: 50000, idempotencyKey: 'sc18'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_sc18'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_sc18'
    });
    await assertFails(batch.commit());
  });

  it('Test 64 (Scenario 19): CUSTOMER CASH -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_sc19').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_sc19',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
    });

    const db = testEnv.authenticatedContext('customer_user').firestore();
    const batch = db.batch();
    batch.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_sc19'), {
      dealershipId: 'sharan_motors', loanId: 'loan_sc19', customerId: 'cust_1',
      paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'customer_user',
      amountPaise: 50000, idempotencyKey: 'sc19'
    });
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_sc19'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_sc19'
    });
    await assertFails(batch.commit());
  });

  it('Test 65 (Scenario 20): Cross-tenant transaction replay -> EXPECT DENY', async () => {
    await testEnv.withSecurityRulesDisabled(async (c) => {
      const adminDb = c.firestore();
      await adminDb.doc('dealerships/sharan_motors/loans/loan_sc20').set({
        dealershipId: 'sharan_motors', customerId: 'cust_1', loanId: 'loan_sc20',
        remainingBalancePaise: 100000, paidAmountPaise: 0, loanStatus: 'ACTIVE'
      });
      await adminDb.doc('dealerships/other_motors/transactions/TXN_CASH_other_tenant').set({
        dealershipId: 'other_motors', loanId: 'loan_other', customerId: 'cust_other',
        paymentMethod: 'CASH', status: 'SUCCESS', collectedByUserId: 'other_tenant_user',
        amountPaise: 50000, idempotencyKey: 'other_tenant'
      });
    });

    const db = testEnv.authenticatedContext('finance_user').firestore(); // sharan_motors
    const batch = db.batch();
    batch.update(db.doc('dealerships/sharan_motors/loans/loan_sc20'), {
      remainingBalancePaise: 50000, paidAmountPaise: 50000, loanStatus: 'ACTIVE',
      lastPaymentTransactionId: 'TXN_CASH_other_tenant'
    });
    await assertFails(batch.commit());
  });

  it('Test 66: Full-object set() payload -> DENY for Scenario A (prohibited financial field change) and ALLOW for Scenario B (identical unchanged extra fields)', async () => {
    // Scenario A: Attempt to update loan while changing a prohibited financial field (e.g. principalAmountPaise from 9089700 to 8000000) -> EXPECT DENY
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_repro_a').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_repro_a',
        remainingBalancePaise: 8362616,
        remainingBalance: 83626.16,
        paidAmountPaise: 2323500,
        paidAmount: 23235.0,
        principalAmountPaise: 9089700,
        downPaymentPaise: 0,
        loanAmountPaise: 9089700,
        emiAmountPaise: 363588,
        processingFeePaise: 0,
        principalAmount: 90897.0,
        downPayment: 0.0,
        loanAmount: 90897.0,
        emiAmount: 3635.88,
        processingFee: 0.0,
        loanStatus: 'ACTIVE',
        nextEmiDate: 1789848496000,
        lastPaymentTransactionId: ''
      });
    });

    const db = testEnv.authenticatedContext('dealership_admin_user').firestore();
    const batchA = db.batch();
    batchA.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_repro_A_set'), {
      dealershipId: 'sharan_motors',
      customerId: 'cust_1',
      loanId: 'loan_repro_a',
      paymentMethod: 'CASH',
      status: 'SUCCESS',
      amountPaise: 363588,
      collectedByUserId: 'dealership_admin_user',
      idempotencyKey: 'repro_A_set'
    });
    batchA.set(db.doc('dealerships/sharan_motors/audit_logs/AUDIT_CASH_repro_A_set'), {
      auditLogId: 'AUDIT_CASH_repro_A_set',
      dealershipId: 'sharan_motors',
      userId: 'dealership_admin_user',
      action: 'RECORD_CASH_PAYMENT',
      entityType: 'Loan',
      entityId: 'loan_repro_a',
      timestamp: Date.now()
    });
    batchA.set(db.doc('dealerships/sharan_motors/loans/loan_repro_a'), {
      dealershipId: 'sharan_motors',
      customerId: 'cust_1',
      loanId: 'loan_repro_a',
      remainingBalancePaise: 7999028,
      remainingBalance: 79990.28,
      paidAmountPaise: 2687088,
      paidAmount: 26870.88,
      principalAmountPaise: 8000000, // Changing prohibited financial field from 9089700 to 8000000
      downPaymentPaise: 0,
      loanAmountPaise: 9089700,
      emiAmountPaise: 363588,
      processingFeePaise: 0,
      principalAmount: 90897.0,
      downPayment: 0.0,
      loanAmount: 90897.0,
      emiAmount: 3635.88,
      processingFee: 0.0,
      loanStatus: 'ACTIVE',
      nextEmiDate: 1792440496000,
      lastPaymentTransactionId: 'TXN_CASH_repro_A_set'
    });
    await assertFails(batchA.commit());

    // Scenario B: Full-document set containing identical unchanged extra fields -> EXPECT ALLOW
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_repro_b').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_repro_b',
        remainingBalancePaise: 8362616,
        remainingBalance: 83626.16,
        paidAmountPaise: 2323500,
        paidAmount: 23235.0,
        principalAmountPaise: 9089700,
        principalAmount: 90897.0,
        downPaymentPaise: 0,
        downPayment: 0.0,
        loanAmountPaise: 9089700,
        loanAmount: 90897.0,
        emiAmountPaise: 363588,
        emiAmount: 3635.88,
        processingFeePaise: 0,
        processingFee: 0.0,
        loanStatus: 'ACTIVE',
        nextEmiDate: 1789848496000,
        lastPaymentTransactionId: ''
      });
    });

    const batchB = db.batch();
    batchB.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_repro_B_set'), {
      dealershipId: 'sharan_motors',
      customerId: 'cust_1',
      loanId: 'loan_repro_b',
      paymentMethod: 'CASH',
      status: 'SUCCESS',
      amountPaise: 363588,
      collectedByUserId: 'dealership_admin_user',
      idempotencyKey: 'repro_B_set'
    });
    batchB.set(db.doc('dealerships/sharan_motors/audit_logs/AUDIT_CASH_repro_B_set'), {
      auditLogId: 'AUDIT_CASH_repro_B_set',
      dealershipId: 'sharan_motors',
      userId: 'dealership_admin_user',
      action: 'RECORD_CASH_PAYMENT',
      entityType: 'Loan',
      entityId: 'loan_repro_b',
      timestamp: Date.now()
    });
    batchB.set(db.doc('dealerships/sharan_motors/loans/loan_repro_b'), {
      dealershipId: 'sharan_motors',
      customerId: 'cust_1',
      loanId: 'loan_repro_b',
      remainingBalancePaise: 7999028,
      remainingBalance: 79990.28,
      paidAmountPaise: 2687088,
      paidAmount: 26870.88,
      principalAmountPaise: 9089700,
      downPaymentPaise: 0,
      loanAmountPaise: 9089700,
      emiAmountPaise: 363588,
      processingFeePaise: 0,
      principalAmount: 90897.0,
      downPayment: 0.0,
      loanAmount: 90897.0,
      emiAmount: 3635.88,
      processingFee: 0.0,
      loanStatus: 'ACTIVE',
      nextEmiDate: 1792440496000,
      lastPaymentTransactionId: 'TXN_CASH_repro_B_set'
    });
    await assertSucceeds(batchB.commit());
  });

  it('Test 67: Tight 7-field update() payload -> ALLOW for Scenario A (legacy) and Scenario B (migrated)', async () => {
    // Scenario A: Legacy loan missing optional paise fields
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_repro_a').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_repro_a',
        remainingBalancePaise: 8362616,
        remainingBalance: 83626.16,
        paidAmountPaise: 2323500,
        paidAmount: 23235.0,
        principalAmount: 90897.0,
        downPayment: 0.0,
        loanAmount: 90897.0,
        emiAmount: 3635.88,
        processingFee: 0.0,
        loanStatus: 'ACTIVE',
        nextEmiDate: 1789848496000,
        lastPaymentTransactionId: ''
      });
    });

    const db = testEnv.authenticatedContext('dealership_admin_user').firestore();
    const batchA = db.batch();
    batchA.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_repro_A_7field'), {
      dealershipId: 'sharan_motors',
      customerId: 'cust_1',
      loanId: 'loan_repro_a',
      paymentMethod: 'CASH',
      status: 'SUCCESS',
      amountPaise: 363588,
      collectedByUserId: 'dealership_admin_user',
      idempotencyKey: 'repro_A_7field'
    });
    batchA.set(db.doc('dealerships/sharan_motors/audit_logs/AUDIT_CASH_repro_A_7field'), {
      auditLogId: 'AUDIT_CASH_repro_A_7field',
      dealershipId: 'sharan_motors',
      userId: 'dealership_admin_user',
      action: 'RECORD_CASH_PAYMENT',
      entityType: 'Loan',
      entityId: 'loan_repro_a',
      timestamp: Date.now()
    });
    batchA.update(db.doc('dealerships/sharan_motors/loans/loan_repro_a'), {
      remainingBalancePaise: 7999028,
      remainingBalance: 79990.28,
      paidAmountPaise: 2687088,
      paidAmount: 26870.88,
      loanStatus: 'ACTIVE',
      nextEmiDate: 1792440496000,
      lastPaymentTransactionId: 'TXN_CASH_repro_A_7field'
    });
    await assertSucceeds(batchA.commit());

    // Scenario B: Fully migrated loan with existing paise fields
    await testEnv.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('dealerships/sharan_motors/loans/loan_repro_b').set({
        dealershipId: 'sharan_motors',
        customerId: 'cust_1',
        loanId: 'loan_repro_b',
        remainingBalancePaise: 8362616,
        remainingBalance: 83626.16,
        paidAmountPaise: 2323500,
        paidAmount: 23235.0,
        principalAmountPaise: 9089700,
        principalAmount: 90897.0,
        downPaymentPaise: 0,
        downPayment: 0.0,
        loanAmountPaise: 9089700,
        loanAmount: 90897.0,
        emiAmountPaise: 363588,
        emiAmount: 3635.88,
        processingFeePaise: 0,
        processingFee: 0.0,
        loanStatus: 'ACTIVE',
        nextEmiDate: 1789848496000,
        lastPaymentTransactionId: ''
      });
    });

    const batchB = db.batch();
    batchB.set(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_repro_B_7field'), {
      dealershipId: 'sharan_motors',
      customerId: 'cust_1',
      loanId: 'loan_repro_b',
      paymentMethod: 'CASH',
      status: 'SUCCESS',
      amountPaise: 363588,
      collectedByUserId: 'dealership_admin_user',
      idempotencyKey: 'repro_B_7field'
    });
    batchB.set(db.doc('dealerships/sharan_motors/audit_logs/AUDIT_CASH_repro_B_7field'), {
      auditLogId: 'AUDIT_CASH_repro_B_7field',
      dealershipId: 'sharan_motors',
      userId: 'dealership_admin_user',
      action: 'RECORD_CASH_PAYMENT',
      entityType: 'Loan',
      entityId: 'loan_repro_b',
      timestamp: Date.now()
    });
    batchB.update(db.doc('dealerships/sharan_motors/loans/loan_repro_b'), {
      remainingBalancePaise: 7999028,
      remainingBalance: 79990.28,
      paidAmountPaise: 2687088,
      paidAmount: 26870.88,
      loanStatus: 'ACTIVE',
      nextEmiDate: 1792440496000,
      lastPaymentTransactionId: 'TXN_CASH_repro_B_7field'
    });
    await assertSucceeds(batchB.commit());
  });

  // 8. Store Expense Management Security Rules Tests (Tests 68-86)
  it('Test 68: Valid DRAFT store expense creation by authorized STORE_MANAGER: ALLOW', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_68').set({
      expenseId: 'exp_68',
      dealershipId: 'sharan_motors',
      storeId: 'TG_Madhapur',
      createdByUserId: 'store_manager_user',
      amountPaise: 500000,
      approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID',
      category: 'STORE_RENT',
      paymentMethod: 'STORE_CASHBOOK',
      createdAt: Date.now(),
      expenseDate: Date.now()
    }));
  });

  it('Test 69: Expense creation by SALES_USER: DENY', async () => {
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_69').set({
      expenseId: 'exp_69',
      dealershipId: 'sharan_motors',
      storeId: 'TG_Madhapur',
      createdByUserId: 'sales_user',
      amountPaise: 500000,
      approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID',
      category: 'STORE_RENT',
      paymentMethod: 'STORE_CASHBOOK',
      createdAt: Date.now(),
      expenseDate: Date.now()
    }));
  });

  it('Test 70: Expense creation with zero amountPaise: DENY', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_70a').set({
      expenseId: 'exp_70a', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
      createdByUserId: 'store_manager_user', amountPaise: 0, approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now(),
      expenseDate: Date.now()
    }));
  });

  it('Test 71: Expense creation with invalid category: DENY', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_71a').set({
      expenseId: 'exp_71a', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
      createdByUserId: 'store_manager_user', amountPaise: 1000, approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID', category: 'INVALID_CAT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now(),
      expenseDate: Date.now()
    }));
  });

  it('Test 72: Cross-dealership expense read and write: DENY', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertFails(db.doc('dealerships/other_motors/stores/TG_Madhapur/expenses/exp_72').get());
    await assertFails(db.doc('dealerships/other_motors/stores/TG_Madhapur/expenses/exp_72_new').set({
      expenseId: 'exp_72_new', dealershipId: 'other_motors', storeId: 'TG_Madhapur',
      createdByUserId: 'store_manager_user', amountPaise: 1000, approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now(),
      expenseDate: Date.now()
    }));
  });

  it('Test 73: Store Manager in Store A attempting to access Store B expenses: DENY', async () => {
    const db = testEnv.authenticatedContext('manager_store_a').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/Store_B/expenses/exp_73').get());
    await assertFails(db.doc('dealerships/sharan_motors/stores/Store_B/expenses/exp_73_new').set({
      expenseId: 'exp_73_new', dealershipId: 'sharan_motors', storeId: 'Store_B',
      createdByUserId: 'manager_store_a', amountPaise: 1000, approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now(),
      expenseDate: Date.now()
    }));
  });

  it('Test 74: DRAFT -> SUBMITTED transition setting submittedByUserId: ALLOW', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_74').update({
      approvalStatus: 'SUBMITTED',
      submittedByUserId: 'store_manager_user',
      updatedAt: Date.now()
    }));
  });

  it('Test 75: SUBMITTED -> APPROVED transition by independent finance manager: ALLOW', async () => {
    const db = testEnv.authenticatedContext('finance_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_75').update({
      approvalStatus: 'APPROVED',
      approvedByUserId: 'finance_user',
      updatedAt: Date.now()
    }));
  });

  it('Test 76: Creator self-approval attempt (approver == creator): DENY', async () => {
    const db = testEnv.authenticatedContext('finance_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_76').update({
      approvalStatus: 'APPROVED',
      approvedByUserId: 'finance_user',
      updatedAt: Date.now()
    }));
  });

  it('Test 77: Submitter self-approval attempt (approver == submitter): DENY', async () => {
    const db = testEnv.authenticatedContext('finance_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_77').update({
      approvalStatus: 'APPROVED',
      approvedByUserId: 'finance_user',
      updatedAt: Date.now()
    }));
  });

  it('Test 78: SUBMITTED -> REJECTED transition reason validation', async () => {
    const db = testEnv.authenticatedContext('finance_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_78').update({
      approvalStatus: 'REJECTED',
      rejectionReason: '',
      updatedAt: Date.now()
    }));
    await assertSucceeds(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_78').update({
      approvalStatus: 'REJECTED',
      rejectionReason: 'Invalid vendor receipt attachment',
      updatedAt: Date.now()
    }));
  });

  it('Test 79: REJECTED -> DRAFT transition for resubmission: ALLOW', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_79').update({
      approvalStatus: 'DRAFT',
      amountPaise: 1200,
      updatedAt: Date.now()
    }));
  });

  it('Test 80: APPROVED + UNPAID -> PAID transition by authorized cash collector: ALLOW', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_80').update({
      paymentStatus: 'PAID',
      paidByUserId: 'store_manager_user',
      paidTimestamp: Date.now(),
      updatedAt: Date.now()
    }));
  });

  it('Test 81: Unapproved expense payment attempt (SUBMITTED -> PAID): DENY', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_81').update({
      paymentStatus: 'PAID',
      paidByUserId: 'store_manager_user',
      paidTimestamp: Date.now(),
      updatedAt: Date.now()
    }));
  });

  it('Test 82: PAID -> REVERSED transition reason validation', async () => {
    const db = testEnv.authenticatedContext('dealership_admin_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_82').update({
      paymentStatus: 'REVERSED',
      reversalReason: '',
      updatedAt: Date.now()
    }));
    await assertSucceeds(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_82').update({
      paymentStatus: 'REVERSED',
      reversalReason: 'Duplicate disbursement reversal',
      updatedAt: Date.now()
    }));
  });

  it('Test 83: Expense document mutation after approval (changing amountPaise): DENY', async () => {
    const db = testEnv.authenticatedContext('finance_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_83').update({
      amountPaise: 999999
    }));
  });

  it('Test 84: Expense document deletion attempt: DENY', async () => {
    const db = testEnv.authenticatedContext('dealership_admin_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_84').delete());
  });

  it('Test 85: SUPER_ADMIN read/write attempt on private dealership expense document: DENY', async () => {
    const db = testEnv.authenticatedContext('super_admin_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_85').get());
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_85_new').set({
      expenseId: 'exp_85_new', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
      createdByUserId: 'super_admin_user', amountPaise: 1000, approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now(),
      expenseDate: Date.now()
    }));
  });

  it('Test 86: STATE_MANAGER read/write attempt on expense document: DENY', async () => {
    const db = testEnv.authenticatedContext('state_manager_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_86').get());
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_86_new').set({
      expenseId: 'exp_86_new', dealershipId: 'sharan_motors', storeId: 'TG_Madhapur',
      createdByUserId: 'state_manager_user', amountPaise: 1000, approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID', category: 'STORE_RENT', paymentMethod: 'STORE_CASHBOOK', createdAt: Date.now(),
      expenseDate: Date.now()
    }));
  });

  // 9. Hardening Update-Denial & Role Bypass Tests (Tests 87-95)
  it('Test 87: SALES_USER attempting update on existing DRAFT expense document: DENY', async () => {
    const db = testEnv.authenticatedContext('sales_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_87').update({
      approvalStatus: 'SUBMITTED',
      submittedByUserId: 'sales_user',
      updatedAt: Date.now()
    }));
  });

  it('Test 88: SALES_USER with storeId == ALL attempting update on existing expense: DENY', async () => {
    const db = testEnv.authenticatedContext('sales_user_all').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_88').update({
      approvalStatus: 'SUBMITTED',
      submittedByUserId: 'sales_user_all',
      updatedAt: Date.now()
    }));
  });

  it('Test 89: STATE_MANAGER attempting update on existing SUBMITTED expense document: DENY', async () => {
    const db = testEnv.authenticatedContext('state_manager_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_89').update({
      approvalStatus: 'APPROVED',
      approvedByUserId: 'state_manager_user',
      updatedAt: Date.now()
    }));
  });

  it('Test 90: STATE_MANAGER with storeId == ALL attempting update on existing expense: DENY', async () => {
    const db = testEnv.authenticatedContext('state_manager_all').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_90').update({
      approvalStatus: 'APPROVED',
      approvedByUserId: 'state_manager_all',
      updatedAt: Date.now()
    }));
  });

  it('Test 91: ADMIN role attempting update on existing expense document: DENY', async () => {
    const db = testEnv.authenticatedContext('admin_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_91').update({
      approvalStatus: 'SUBMITTED',
      submittedByUserId: 'admin_user',
      updatedAt: Date.now()
    }));
  });

  it('Test 92: SUPER_ADMIN attempting update on existing expense document: DENY', async () => {
    const db = testEnv.authenticatedContext('super_admin_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_92').update({
      approvalStatus: 'SUBMITTED',
      submittedByUserId: 'super_admin_user',
      updatedAt: Date.now()
    }));
  });

  it('Test 93: CUSTOMER attempting update on existing expense document: DENY', async () => {
    const db = testEnv.authenticatedContext('customer_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_93').update({
      approvalStatus: 'SUBMITTED',
      submittedByUserId: 'customer_user',
      updatedAt: Date.now()
    }));
  });

  it('Test 94: STORE_MANAGER attempting PAID -> REVERSED transition with reason in assigned store: ALLOW', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_94').update({
      paymentStatus: 'REVERSED',
      reversalReason: 'Disbursement correction by store manager',
      updatedAt: Date.now()
    }));
  });

  it('Test 95: FINANCE_USER attempting PAID -> REVERSED transition with reason: ALLOW', async () => {
    const db = testEnv.authenticatedContext('finance_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_95').update({
      paymentStatus: 'REVERSED',
      reversalReason: 'Finance auditor reversal',
      updatedAt: Date.now()
    }));
  });

  // 10. Authoritative Expense Date Security Rules Tests (Tests 96-102)
  it('Test 96: Valid past expenseDate creation: ALLOW', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_96').set({
      expenseId: 'exp_96',
      dealershipId: 'sharan_motors',
      storeId: 'TG_Madhapur',
      createdByUserId: 'store_manager_user',
      amountPaise: 500000,
      approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID',
      category: 'STORE_RENT',
      paymentMethod: 'STORE_CASHBOOK',
      createdAt: Date.now(),
      expenseDate: Date.now() - 86400000
    }));
  });

  it('Test 97: Valid current expenseDate creation: ALLOW', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertSucceeds(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_97').set({
      expenseId: 'exp_97',
      dealershipId: 'sharan_motors',
      storeId: 'TG_Madhapur',
      createdByUserId: 'store_manager_user',
      amountPaise: 500000,
      approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID',
      category: 'STORE_RENT',
      paymentMethod: 'STORE_CASHBOOK',
      createdAt: Date.now(),
      expenseDate: Date.now()
    }));
  });

  it('Test 98: Zero expenseDate creation: DENY', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_98').set({
      expenseId: 'exp_98',
      dealershipId: 'sharan_motors',
      storeId: 'TG_Madhapur',
      createdByUserId: 'store_manager_user',
      amountPaise: 500000,
      approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID',
      category: 'STORE_RENT',
      paymentMethod: 'STORE_CASHBOOK',
      createdAt: Date.now(),
      expenseDate: 0
    }));
  });

  it('Test 99: Negative expenseDate creation: DENY', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_99').set({
      expenseId: 'exp_99',
      dealershipId: 'sharan_motors',
      storeId: 'TG_Madhapur',
      createdByUserId: 'store_manager_user',
      amountPaise: 500000,
      approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID',
      category: 'STORE_RENT',
      paymentMethod: 'STORE_CASHBOOK',
      createdAt: Date.now(),
      expenseDate: -100000
    }));
  });

  it('Test 100: Missing expenseDate creation: DENY', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_100').set({
      expenseId: 'exp_100',
      dealershipId: 'sharan_motors',
      storeId: 'TG_Madhapur',
      createdByUserId: 'store_manager_user',
      amountPaise: 500000,
      approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID',
      category: 'STORE_RENT',
      paymentMethod: 'STORE_CASHBOOK',
      createdAt: Date.now()
    }));
  });

  it('Test 101: Future expenseDate creation (>24h in future): DENY', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_101').set({
      expenseId: 'exp_101',
      dealershipId: 'sharan_motors',
      storeId: 'TG_Madhapur',
      createdByUserId: 'store_manager_user',
      amountPaise: 500000,
      approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID',
      category: 'STORE_RENT',
      paymentMethod: 'STORE_CASHBOOK',
      createdAt: Date.now(),
      expenseDate: Date.now() + (10 * 86400000)
    }));
  });

  it('Test 102: Non-integer string expenseDate creation: DENY', async () => {
    const db = testEnv.authenticatedContext('store_manager_user').firestore();
    await assertFails(db.doc('dealerships/sharan_motors/stores/TG_Madhapur/expenses/exp_102').set({
      expenseId: 'exp_102',
      dealershipId: 'sharan_motors',
      storeId: 'TG_Madhapur',
      createdByUserId: 'store_manager_user',
      amountPaise: 500000,
      approvalStatus: 'DRAFT',
      paymentStatus: 'UNPAID',
      category: 'STORE_RENT',
      paymentMethod: 'STORE_CASHBOOK',
      createdAt: Date.now(),
      expenseDate: '2026-09-24'
    }));
  });
});

