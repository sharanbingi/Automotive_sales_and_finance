import re

with open('firestore.rules', 'r') as f:
    content = f.read()

# Update isCashPaymentLoanUpdate
old_is_cash = """    function isCashPaymentLoanUpdate(dealershipId, loanId) {
      let allowedKeys = ['remainingBalancePaise', 'remainingBalance', 'paidAmountPaise', 'paidAmount', 'loanStatus', 'nextEmiDate', 'lastPaymentTransactionId'];
      let isOnlyModifyingAllowedFields = request.resource.data.diff(resource.data).affectedKeys().hasOnly(allowedKeys);

      let immutableIdsPreserved = request.resource.data.dealershipId == resource.data.dealershipId &&
                                  request.resource.data.customerId == resource.data.customerId &&
                                  request.resource.data.loanId == resource.data.loanId;

      let hasPaiseFields = resource.data.remainingBalancePaise is int &&
                           resource.data.paidAmountPaise is int &&
                           request.resource.data.remainingBalancePaise is int &&
                           request.resource.data.paidAmountPaise is int;

      let txnId = request.resource.data.lastPaymentTransactionId;
      let hasTxnId = txnId is string && txnId != '';
      let txn = getAfter(/databases/$(database)/documents/dealerships/$(dealershipId)/transactions/$(txnId)).data;

      let isTxnValid = txn != null &&
                       txn.dealershipId == dealershipId &&
                       txn.loanId == loanId &&
                       txn.customerId == resource.data.customerId &&
                       txn.paymentMethod == 'CASH' &&
                       txn.status == 'SUCCESS' &&
                       txn.collectedByUserId == request.auth.uid &&
                       txn.amountPaise is int &&
                       txn.amountPaise > 0;

      let exactMath = (resource.data.remainingBalancePaise - request.resource.data.remainingBalancePaise == txn.amountPaise) &&
                      (request.resource.data.paidAmountPaise - resource.data.paidAmountPaise == txn.amountPaise);

      let isValidStatus = (request.resource.data.loanStatus == 'SETTLED' && request.resource.data.remainingBalancePaise == 0 && resource.data.remainingBalancePaise == txn.amountPaise) ||
                          (request.resource.data.loanStatus == resource.data.loanStatus && request.resource.data.remainingBalancePaise > 0);

      return isOnlyModifyingAllowedFields && immutableIdsPreserved && hasPaiseFields && hasTxnId && isTxnValid && exactMath && isValidStatus;
    }"""

new_is_cash = """    function isCashPaymentLoanUpdate(dealershipId, loanId) {
      let allowedKeys = ['remainingBalancePaise', 'remainingBalance', 'paidAmountPaise', 'paidAmount', 'loanStatus', 'nextEmiDate', 'lastPaymentTransactionId'];
      let isOnlyModifyingAllowedFields = request.resource.data.diff(resource.data).affectedKeys().hasOnly(allowedKeys);

      let immutableIdsPreserved = request.resource.data.dealershipId == resource.data.dealershipId &&
                                  request.resource.data.customerId == resource.data.customerId &&
                                  request.resource.data.loanId == resource.data.loanId;

      let hasPaiseFields = resource.data.remainingBalancePaise is int &&
                           resource.data.paidAmountPaise is int &&
                           request.resource.data.remainingBalancePaise is int &&
                           request.resource.data.paidAmountPaise is int;

      let txnId = request.resource.data.lastPaymentTransactionId;
      let hasTxnId = txnId is string && txnId != '';
      let txnPath = /databases/$(database)/documents/dealerships/$(dealershipId)/transactions/$(txnId);
      let txnExistsBefore = exists(txnPath);
      let txn = getAfter(txnPath).data;

      let isTxnValid = txn != null &&
                       !txnExistsBefore &&
                       request.resource.data.lastPaymentTransactionId != resource.data.lastPaymentTransactionId &&
                       txn.dealershipId == dealershipId &&
                       txn.loanId == loanId &&
                       txn.customerId == resource.data.customerId &&
                       txn.paymentMethod == 'CASH' &&
                       txn.status == 'SUCCESS' &&
                       txn.collectedByUserId == request.auth.uid &&
                       txn.amountPaise is int &&
                       txn.amountPaise > 0;

      let exactMath = (resource.data.remainingBalancePaise - request.resource.data.remainingBalancePaise == txn.amountPaise) &&
                      (request.resource.data.paidAmountPaise - resource.data.paidAmountPaise == txn.amountPaise);

      let isValidStatus = (request.resource.data.loanStatus == 'SETTLED' && request.resource.data.remainingBalancePaise == 0 && resource.data.remainingBalancePaise == txn.amountPaise) ||
                          (request.resource.data.loanStatus == resource.data.loanStatus && request.resource.data.remainingBalancePaise > 0);

      return isOnlyModifyingAllowedFields && immutableIdsPreserved && hasPaiseFields && hasTxnId && isTxnValid && exactMath && isValidStatus;
    }"""

old_is_valid = """    function isValidCashTransactionCreate(dealershipId) {
      return request.resource.data.dealershipId == dealershipId &&
             request.resource.data.paymentMethod == 'CASH' &&
             request.resource.data.status == 'SUCCESS' &&
             request.resource.data.amountPaise is int &&
             request.resource.data.amountPaise > 0 &&
             request.resource.data.loanId is string && request.resource.data.loanId != '' &&
             request.resource.data.customerId is string && request.resource.data.customerId != '' &&
             request.resource.data.collectedByUserId == request.auth.uid &&
             request.resource.data.idempotencyKey is string && request.resource.data.idempotencyKey != '';
    }"""

new_is_valid = """    function isValidCashTransactionCreate(dealershipId, transactionId) {
      let loanId = request.resource.data.loanId;
      let loanPath = /databases/$(database)/documents/dealerships/$(dealershipId)/loans/$(loanId);
      let loanExistsAfter = existsAfter(loanPath);
      let loanAfter = getAfter(loanPath).data;
      let isAtomicallyPaired = loanExistsAfter && loanAfter.lastPaymentTransactionId == transactionId;
      let deterministicId = 'TXN_CASH_' + request.resource.data.idempotencyKey;

      return request.resource.data.dealershipId == dealershipId &&
             request.resource.data.paymentMethod == 'CASH' &&
             request.resource.data.status == 'SUCCESS' &&
             request.resource.data.amountPaise is int &&
             request.resource.data.amountPaise > 0 &&
             request.resource.data.loanId is string && request.resource.data.loanId != '' &&
             request.resource.data.customerId is string && request.resource.data.customerId != '' &&
             request.resource.data.collectedByUserId == request.auth.uid &&
             request.resource.data.idempotencyKey is string && request.resource.data.idempotencyKey != '' &&
             transactionId == deterministicId &&
             isAtomicallyPaired;
    }"""

old_txn_match = """      match /transactions/{transactionId} {
        allow read: if isAuthenticated() && (isSuperAdmin() || isTenantUser(dealershipId));
        allow create: if (isSuperAdmin() && request.resource.data.paymentMethod != 'CASH') ||
                      (isAuthorizedCashCollector(dealershipId) && isValidCashTransactionCreate(dealershipId)) ||
                      (isStaff(dealershipId) && request.resource.data.status != 'SUCCESS'); // Protect digital SUCCESS
        allow update, delete: if isSuperAdmin(); // Staff cannot mutate ledger
      }"""

new_txn_match = """      match /transactions/{transactionId} {
        allow read: if isAuthenticated() && (isSuperAdmin() || isTenantUser(dealershipId));
        allow create: if (isSuperAdmin() && request.resource.data.paymentMethod != 'CASH') ||
                      (isAuthorizedCashCollector(dealershipId) && isValidCashTransactionCreate(dealershipId, transactionId)) ||
                      (isStaff(dealershipId) && request.resource.data.status != 'SUCCESS'); // Protect digital SUCCESS
        allow update, delete: if isSuperAdmin(); // Staff cannot mutate ledger
      }"""

content = content.replace(old_is_cash, new_is_cash)
content = content.replace(old_is_valid, new_is_valid)
content = content.replace(old_txn_match, new_txn_match)

with open('firestore.rules', 'w') as f:
    f.write(content)
