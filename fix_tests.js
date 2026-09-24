const fs = require('fs');
let code = fs.readFileSync('rules-tests/test/firestore.rules.spec.js', 'utf8');

// For every batch.set(db.doc('.../transactions/txn_cash_...'), { ..., idempotencyKey: 'idem_...' })
// we need to make sure the transaction doc ID is exactly 'TXN_CASH_' + idempotencyKey.

// Let's just use regex to match all transaction IDs and update their idempotencyKey to match the ID's suffix,
// and change the ID to TXN_CASH_something.

code = code.replace(/txn_cash_(\w+)/g, 'TXN_CASH_$1');
code = code.replace(/txn_new_(\w+)/g, 'TXN_CASH_new_$1');

// Now, update idempotencyKey to match the ID's suffix.
// The IDs now look like TXN_CASH_1, TXN_CASH_2, TXN_CASH_new_1, etc.
// We want idempotencyKey: '1', idempotencyKey: '2', idempotencyKey: 'new_1' etc.

// Let's replace idempotencyKey: 'idem_X' with the right one based on the context.
// Actually, it's easier to just do:
code = code.replace(/idempotencyKey:\s*'[^']+'/g, function(match, offset, string) {
    // find the nearest previous TXN_CASH_...
    let substr = string.substring(0, offset);
    let lastTxnMatch = substr.match(/TXN_CASH_(\w+)'\)/g);
    if (lastTxnMatch) {
        let lastTxn = lastTxnMatch[lastTxnMatch.length - 1];
        let key = lastTxn.replace("')", "").replace("TXN_CASH_", "");
        return `idempotencyKey: '${key}'`;
    }
    return match;
});

// Test 26 currently creates a standalone transaction:
// it('Test 26: Authorized Cash Collector can create a valid CASH transaction with paise'
// We need to change it to fail, because standalone is not allowed now.
code = code.replace(
    "it('Test 26: Authorized Cash Collector can create a valid CASH transaction with paise', async () => {\n    const db = testEnv.authenticatedContext('finance_user').firestore();\n    await assertSucceeds(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_new_1').set({\n      dealershipId: 'sharan_motors',\n      loanId: 'loan_1',\n      customerId: 'cust_1',\n      paymentMethod: 'CASH',\n      status: 'SUCCESS',\n      collectedByUserId: 'finance_user',\n      amountPaise: 300000,\n      idempotencyKey: 'new_1'\n    }));\n  });",
    "it('Test 26: Authorized Cash Collector CANNOT create a standalone CASH transaction (Atomicity)', async () => {\n    const db = testEnv.authenticatedContext('finance_user').firestore();\n    await assertFails(db.doc('dealerships/sharan_motors/transactions/TXN_CASH_new_1').set({\n      dealershipId: 'sharan_motors',\n      loanId: 'loan_1',\n      customerId: 'cust_1',\n      paymentMethod: 'CASH',\n      status: 'SUCCESS',\n      collectedByUserId: 'finance_user',\n      amountPaise: 300000,\n      idempotencyKey: 'new_1'\n    }));\n  });"
);

// We should also append the replay test and the atomicity tests explicitly.
const additionalTests = `
  it('Test 50: Replay an existing CASH transaction -> DENY', async () => {
    // 1. Create a loan and a successful transaction first (as super_admin or bypassing rules)
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

    // 2. Try to replay it in a new batch to reduce balance further
    const db = testEnv.authenticatedContext('finance_user').firestore();
    const batch = db.batch();
    // We don't write the transaction because it already exists, or we try to overwrite it
    // Wait, the test specifies "by sending a loan update pointing to an already existing transaction via lastPaymentTransactionId"
    // Let's just update the loan. The transaction already exists, so txnExistsBefore will be true.
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
    // ID doesn't match TXN_CASH_ + idempotencyKey
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
`;

code = code.replace("});\n", "});\n" + additionalTests);

fs.writeFileSync('rules-tests/test/firestore.rules.spec.js', code);
