const fs = require('fs');
let code = fs.readFileSync('rules-tests/test/firestore.rules.spec.js', 'utf8');
code = code.substring(0, code.lastIndexOf('});'));

const additionalTests = `
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
});
`;

fs.writeFileSync('rules-tests/test/firestore.rules.spec.js', code + additionalTests);
