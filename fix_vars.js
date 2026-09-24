const fs = require('fs');
let code = fs.readFileSync('firestore.rules', 'utf8');

const newFunc = `
    function isCashPaymentLoanUpdate(dealershipId, loanId) {
      let isOnlyModifyingAllowedFields = request.resource.data.diff(resource.data).affectedKeys().hasOnly(['remainingBalancePaise', 'remainingBalance', 'paidAmountPaise', 'paidAmount', 'loanStatus', 'nextEmiDate', 'lastPaymentTransactionId']);

      let immutableIdsPreserved = request.resource.data.dealershipId == resource.data.dealershipId &&
                                  request.resource.data.customerId == resource.data.customerId &&
                                  request.resource.data.loanId == resource.data.loanId;

      let hasPaiseFields = resource.data.remainingBalancePaise is int &&
                           resource.data.paidAmountPaise is int &&
                           request.resource.data.remainingBalancePaise is int &&
                           request.resource.data.paidAmountPaise is int;

      let txnPath = /databases/$(database)/documents/dealerships/$(dealershipId)/transactions/$(request.resource.data.lastPaymentTransactionId);
      let txn = getAfter(txnPath).data;

      let isTxnValid = request.resource.data.lastPaymentTransactionId is string &&
                       request.resource.data.lastPaymentTransactionId != '' &&
                       txn != null &&
                       !exists(txnPath) &&
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

      return isOnlyModifyingAllowedFields && immutableIdsPreserved && hasPaiseFields && isTxnValid && exactMath && isValidStatus;
    }
`;

// Extract old func bounds
let start = code.indexOf('function isCashPaymentLoanUpdate');
let end = code.indexOf('function isValidCashTransactionCreate');
code = code.substring(0, start) + newFunc + code.substring(end);

// Also remove isFinancialFieldUnchanged to clear the unused warning.
code = code.replace(/function isFinancialFieldUnchanged[\s\S]*?\}/, '');

fs.writeFileSync('firestore.rules', code);
fs.writeFileSync('rules-tests/firestore.rules', code);
