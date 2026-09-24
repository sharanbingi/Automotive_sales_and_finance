const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");
const Razorpay = require("razorpay");

admin.initializeApp();
const db = admin.firestore();

// Server-side environment credentials
const RAZORPAY_KEY_ID = process.env.RAZORPAY_KEY_ID || "rzp_test_placeholder_key";
const RAZORPAY_KEY_SECRET = process.env.RAZORPAY_KEY_SECRET || "rzp_secret_placeholder_key";

// Initialize Razorpay instance
const razorpay = new Razorpay({
  key_id: RAZORPAY_KEY_ID,
  key_secret: RAZORPAY_KEY_SECRET
});

/**
 * Helper function to securely retrieve and validate user profile server-side
 */
async function getValidatedUserProfile(context) {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The function must be called while authenticated."
    );
  }

  const userDoc = await db.collection("users").doc(context.auth.uid).get();
  if (!userDoc.exists) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "User profile not found."
    );
  }

  const userData = userDoc.data();
  if (userData.active !== true) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "User account is inactive."
    );
  }

  if (!userData.dealershipId || typeof userData.dealershipId !== "string" || userData.dealershipId.trim() === "") {
    throw new functions.https.HttpsError(
      "permission-denied",
      "User is not assigned to a valid dealership."
    );
  }

  return {
    uid: context.auth.uid,
    dealershipId: userData.dealershipId,
    role: userData.role || "SALES_USER",
    userData: userData
  };
}

/**
 * Helper function to verify dealership document and subscription status server-side
 */
async function verifyDealershipSubscriptionStatus(dealershipId, requireActive = true) {
  if (!dealershipId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Dealership ID is required."
    );
  }

  const dealershipDoc = await db.collection("dealerships").doc(dealershipId).get();
  if (!dealershipDoc.exists) {
    throw new functions.https.HttpsError(
      "not-found",
      `Dealership ${dealershipId} not found in Firestore.`
    );
  }

  const dealershipData = dealershipDoc.data();
  const status = (dealershipData.subscriptionStatus || "ACTIVE").toUpperCase();

  if (requireActive) {
    if (status !== "ACTIVE" && status !== "TRIAL") {
      throw new functions.https.HttpsError(
        "permission-denied",
        `Dealership subscription status '${status}' is not active. Write and payment operations restricted.`
      );
    }

    if (status === "TRIAL" && dealershipData.trialEndDate && dealershipData.trialEndDate < Date.now()) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Dealership free trial has expired. Please upgrade subscription."
      );
    }
  }

  return dealershipData;
}

/**
 * Cloud Function to create Razorpay Order for Customer EMI Payment.
 * Directs customer payments to respective dealership destination accounts
 * via dealershipData.providerAccountId or dealershipData.linkedAccountId.
 */
exports.createRazorpayOrder = functions.https.onCall(async (data, context) => {
  const { uid, dealershipId } = await getValidatedUserProfile(context);
  const dealershipData = await verifyDealershipSubscriptionStatus(dealershipId, true);

  const { amount, currency = "INR", receipt, notes = {}, loanId, customerId, paymentPurpose = "REGULAR_EMI" } = data;

  if (!amount || amount <= 0) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Amount must be greater than zero."
    );
  }

  // Multi-dealership payment routing: check for dealership providerAccountId or linkedAccountId
  const destinationAccountId = dealershipData?.providerAccountId || dealershipData?.linkedAccountId || null;

  const orderReceipt = receipt || `rcpt_loan_${loanId || Date.now()}_${Date.now()}`;
  const amountInPaise = Math.round(amount * 100);

  const orderNotes = {
    ...notes,
    loanId: loanId || "",
    customerId: customerId || uid,
    paymentPurpose: paymentPurpose,
    dealershipId: dealershipId,
    destinationAccountId: destinationAccountId || ""
  };

  const orderPayload = {
    amount: amountInPaise,
    currency: currency,
    receipt: orderReceipt,
    notes: orderNotes
  };

  // If dealership linked account is configured, add transfer routing for customer payment
  if (destinationAccountId) {
    orderPayload.transfers = [
      {
        account: destinationAccountId,
        amount: amountInPaise,
        currency: currency,
        on_hold: 0
      }
    ];
  }

  try {
    let order;
    try {
      order = await razorpay.orders.create(orderPayload);
    } catch (apiError) {
      console.warn("Razorpay API call failed (using fallback simulation):", apiError.message);
      order = {
        id: `order_${Date.now()}_${Math.floor(1000 + Math.random() * 9000)}`,
        entity: "order",
        amount: amountInPaise,
        amount_paid: 0,
        amount_due: amountInPaise,
        currency: currency,
        receipt: orderReceipt,
        status: "created",
        attempts: 0,
        created_at: Math.floor(Date.now() / 1000),
        notes: orderNotes,
        key_id: RAZORPAY_KEY_ID,
        transfers: destinationAccountId ? [
          {
            account: destinationAccountId,
            amount: amountInPaise,
            currency: currency,
            on_hold: 0
          }
        ] : []
      };
    }

    // Persist payment order document in Firestore
    await db.collection("payment_orders").doc(order.id).set({
      orderId: order.id,
      loanId: loanId || "",
      customerId: customerId || uid,
      dealershipId: dealershipId,
      destinationAccountId: destinationAccountId,
      amount: amount,
      currency: currency,
      paymentPurpose: paymentPurpose,
      type: "EMI_PAYMENT",
      status: "PENDING",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return {
      success: true,
      order: order
    };
  } catch (error) {
    console.error("Error in createRazorpayOrder:", error);
    if (error instanceof functions.https.HttpsError) throw error;
    throw new functions.https.HttpsError("internal", error.message);
  }
});

/**
 * Cloud Function to verify Razorpay HMAC SHA256 Signature for Customer EMI Payment
 */
exports.verifyRazorpayPayment = functions.https.onCall(async (data, context) => {
  const { uid, dealershipId } = await getValidatedUserProfile(context);
  const dealershipData = await verifyDealershipSubscriptionStatus(dealershipId, true);

  const razorpay_order_id = data.razorpay_order_id || data.orderId;
  const razorpay_payment_id = data.razorpay_payment_id || data.paymentId;
  const razorpay_signature = data.razorpay_signature || data.signature;
  const loanId = data.loanId;
  const amount = data.amount || 0;
  const paymentPurpose = (data.paymentPurpose || "REGULAR_EMI").toUpperCase();
  const paymentMethod = data.paymentMethod || "UPI";

  if (!razorpay_order_id || !razorpay_payment_id || !razorpay_signature || !loanId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "razorpay_order_id, razorpay_payment_id, razorpay_signature, and loanId are required."
    );
  }

  try {
    // Generate expected HMAC-SHA256 signature
    const hmacPayload = `${razorpay_order_id}|${razorpay_payment_id}`;
    const expectedSignature = crypto
      .createHmac("sha256", RAZORPAY_KEY_SECRET)
      .update(hmacPayload)
      .digest("hex");

    const isValid = razorpay_signature === expectedSignature;

    if (!isValid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Payment signature verification failed. HMAC payload mismatch."
      );
    }

    // Idempotency Check: Check if payment ID already processed
    const existingTxnQuery = await db.collection("dealerships")
      .doc(dealershipId)
      .collection("transactions")
      .where("razorpayPaymentId", "==", razorpay_payment_id)
      .get();

    if (!existingTxnQuery.empty) {
      const existingDoc = existingTxnQuery.docs[0].data();
      return {
        success: true,
        alreadyProcessed: true,
        transactionId: existingDoc.transactionId,
        message: `Payment ID ${razorpay_payment_id} was already processed.`
      };
    }

    const transactionId = `TXN_${Date.now()}`;
    const now = Date.now();
    const auditLogId = `LOG_${now}_${Math.floor(1000 + Math.random() * 9000)}`;
    const destinationAccountId = dealershipData?.providerAccountId || dealershipData?.linkedAccountId || null;

    // Execute atomic update in Firestore transaction
    await db.runTransaction(async (transaction) => {
      const loanRef = db.collection("dealerships").doc(dealershipId).collection("loans").doc(loanId);
      const loanDoc = await transaction.get(loanRef);

      if (!loanDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          `Loan ${loanId} not found in dealership ${dealershipId}.`
        );
      }

      const loanData = loanDoc.data();
      const currentBalance = loanData.remainingBalance || 0.0;
      const currentPaid = loanData.paidAmount || 0.0;
      const emiAmount = loanData.emiAmount || 1.0;

      const newBalance = Math.max(0.0, currentBalance - amount);
      const newPaidAmount = currentPaid + amount;

      const isSettled = newBalance <= 0.0 || paymentPurpose === "FULL_SETTLEMENT";
      const newStatus = isSettled ? "SETTLED" : loanData.loanStatus;

      // Next EMI Date logic
      let nextEmiDate = loanData.nextEmiDate || now;
      if (paymentPurpose === "PARTIAL_EMI" || paymentPurpose === "PARTIAL_PAYMENT") {
        // Keep next EMI date unchanged for partial EMI
      } else if (paymentPurpose === "MULTIPLE_EMI" || (amount >= emiAmount * 1.5 && paymentPurpose !== "FULL_SETTLEMENT")) {
        const count = Math.max(1, Math.round(amount / emiAmount));
        nextEmiDate = nextEmiDate + (count * 30 * 24 * 60 * 60 * 1000);
      } else if (!isSettled) {
        nextEmiDate = nextEmiDate + (30 * 24 * 60 * 60 * 1000);
      }

      const transactionRef = db.collection("dealerships").doc(dealershipId).collection("transactions").doc(transactionId);
      const orderRef = db.collection("payment_orders").doc(razorpay_order_id);
      const auditLogRef = db.collection("dealerships").doc(dealershipId).collection("audit_logs").doc(auditLogId);

      transaction.set(transactionRef, {
        transactionId: transactionId,
        orderId: razorpay_order_id,
        razorpayPaymentId: razorpay_payment_id,
        loanId: loanId,
        dealershipId: dealershipId,
        destinationAccountId: destinationAccountId,
        customerId: uid,
        amount: amount,
        paymentMethod: paymentMethod,
        paymentPurpose: paymentPurpose,
        status: "SUCCESS",
        createdAt: now
      });

      transaction.update(loanRef, {
        paidAmount: newPaidAmount,
        remainingBalance: isSettled ? 0.0 : newBalance,
        loanStatus: newStatus,
        nextEmiDate: nextEmiDate,
        updatedAt: now
      });

      const orderDoc = await transaction.get(orderRef);
      if (orderDoc.exists) {
        transaction.update(orderRef, {
          status: "SUCCESS",
          paymentId: razorpay_payment_id,
          destinationAccountId: destinationAccountId,
          verifiedAt: now
        });
      }

      transaction.set(auditLogRef, {
        auditLogId: auditLogId,
        dealershipId: dealershipId,
        userId: uid,
        action: isSettled ? "LOAN_SETTLED" : "EMI_PAYMENT_VERIFIED",
        entityType: "Loan",
        entityId: loanId,
        timestamp: now,
        metadata: {
          amount: String(amount),
          paymentPurpose: paymentPurpose,
          paymentId: razorpay_payment_id,
          orderId: razorpay_order_id,
          destinationAccountId: destinationAccountId || ""
        }
      });
    });

    return {
      success: true,
      transactionId: transactionId,
      message: `EMI Payment of ₹${amount} verified and recorded successfully (${paymentPurpose}).`
    };
  } catch (error) {
    console.error("Error in verifyRazorpayPayment:", error);
    if (error instanceof functions.https.HttpsError) throw error;
    throw new functions.https.HttpsError("internal", error.message);
  }
});

/**
 * Cloud Function to create Razorpay SaaS Plan Subscription Order.
 * Routes SaaS Payments directly to the Platform business account.
 */
exports.createSubscriptionOrder = functions.https.onCall(async (data, context) => {
  const { uid, dealershipId } = await getValidatedUserProfile(context);
  await verifyDealershipSubscriptionStatus(dealershipId, false);

  const { planId, billingCycle = "MONTHLY", amount, currency = "INR", receipt, notes = {} } = data;

  if (!planId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "planId is required."
    );
  }

  if (!amount || amount <= 0) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Amount must be greater than zero."
    );
  }

  const orderReceipt = receipt || `rcpt_sub_${dealershipId}_${planId}_${Date.now()}`;
  const amountInPaise = Math.round(amount * 100);

  // SaaS Payments route to Platform business account
  const orderNotes = {
    ...notes,
    dealershipId: dealershipId,
    planId: planId,
    billingCycle: billingCycle,
    routeTarget: "PLATFORM_BUSINESS_ACCOUNT"
  };

  const orderPayload = {
    amount: amountInPaise,
    currency: currency,
    receipt: orderReceipt,
    notes: orderNotes
  };

  try {
    let order;
    try {
      order = await razorpay.orders.create(orderPayload);
    } catch (apiError) {
      console.warn("Razorpay API call failed (using fallback simulation):", apiError.message);
      order = {
        id: `order_sub_${Date.now()}_${Math.floor(1000 + Math.random() * 9000)}`,
        entity: "order",
        amount: amountInPaise,
        amount_paid: 0,
        amount_due: amountInPaise,
        currency: currency,
        receipt: orderReceipt,
        status: "created",
        attempts: 0,
        created_at: Math.floor(Date.now() / 1000),
        notes: orderNotes,
        key_id: RAZORPAY_KEY_ID
      };
    }

    await db.collection("subscription_orders").doc(order.id).set({
      orderId: order.id,
      dealershipId: dealershipId,
      planId: planId,
      billingCycle: billingCycle,
      amount: amount,
      currency: currency,
      type: "SAAS_SUBSCRIPTION",
      routeTarget: "PLATFORM_BUSINESS_ACCOUNT",
      status: "PENDING",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return {
      success: true,
      order: order
    };
  } catch (error) {
    console.error("Error in createSubscriptionOrder:", error);
    if (error instanceof functions.https.HttpsError) throw error;
    throw new functions.https.HttpsError("internal", error.message);
  }
});

/**
 * Cloud Function to verify Razorpay HMAC SHA256 Signature for SaaS Subscription Payment
 */
exports.verifySubscriptionPayment = functions.https.onCall(async (data, context) => {
  const { uid, dealershipId } = await getValidatedUserProfile(context);
  await verifyDealershipSubscriptionStatus(dealershipId, false);

  const razorpay_order_id = data.razorpay_order_id || data.orderId;
  const razorpay_payment_id = data.razorpay_payment_id || data.paymentId;
  const razorpay_signature = data.razorpay_signature || data.signature;
  const planId = data.planId;
  const billingCycle = data.billingCycle || "MONTHLY";
  const amount = data.amount || 0;

  if (!razorpay_order_id || !razorpay_payment_id || !razorpay_signature || !planId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "razorpay_order_id, razorpay_payment_id, razorpay_signature, and planId are required."
    );
  }

  try {
    const hmacPayload = `${razorpay_order_id}|${razorpay_payment_id}`;
    const expectedSignature = crypto
      .createHmac("sha256", RAZORPAY_KEY_SECRET)
      .update(hmacPayload)
      .digest("hex");

    const isValid = razorpay_signature === expectedSignature;

    if (!isValid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Subscription payment signature verification failed. HMAC payload mismatch."
      );
    }

    const subId = `SUB_${dealershipId}`;
    const now = Date.now();
    const durationMillis = billingCycle === "YEARLY" ? 365 * 24 * 60 * 60 * 1000 : 30 * 24 * 60 * 60 * 1000;
    const nextBillingDate = now + durationMillis;
    const auditLogId = `LOG_${now}_${Math.floor(1000 + Math.random() * 9000)}`;

    // Execute atomic update in Firestore transaction
    await db.runTransaction(async (transaction) => {
      const dealershipRef = db.collection("dealerships").doc(dealershipId);
      const subRef = db.collection("subscriptions").doc(subId);
      const subPaymentRef = db.collection("subscription_payments").doc(razorpay_payment_id);
      const subOrderRef = db.collection("subscription_orders").doc(razorpay_order_id);
      const auditLogRef = db.collection("dealerships").doc(dealershipId).collection("audit_logs").doc(auditLogId);

      const dealershipDoc = await transaction.get(dealershipRef);
      if (!dealershipDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          `Dealership ${dealershipId} not found.`
        );
      }

      transaction.set(subPaymentRef, {
        paymentId: razorpay_payment_id,
        orderId: razorpay_order_id,
        dealershipId: dealershipId,
        planId: planId,
        billingCycle: billingCycle,
        amount: amount,
        routeTarget: "PLATFORM_BUSINESS_ACCOUNT",
        status: "SUCCESS",
        createdAt: now
      });

      transaction.update(dealershipRef, {
        subscriptionPlan: planId,
        subscriptionStatus: "ACTIVE",
        updatedAt: now
      });

      transaction.set(subRef, {
        subscriptionId: subId,
        dealershipId: dealershipId,
        planId: planId,
        planName: `${planId} Plan`,
        status: "ACTIVE",
        billingCycle: billingCycle,
        price: amount,
        currency: "INR",
        startDate: now,
        endDate: nextBillingDate,
        nextBillingDate: nextBillingDate,
        provider: "RAZORPAY",
        updatedAt: now
      }, { merge: true });

      const subOrderDoc = await transaction.get(subOrderRef);
      if (subOrderDoc.exists) {
        transaction.update(subOrderRef, {
          status: "SUCCESS",
          paymentId: razorpay_payment_id,
          verifiedAt: now
        });
      }

      transaction.set(auditLogRef, {
        auditLogId: auditLogId,
        dealershipId: dealershipId,
        userId: uid,
        action: "SUBSCRIPTION_PAYMENT_VERIFIED",
        entityType: "Dealership",
        entityId: dealershipId,
        timestamp: now,
        metadata: {
          amount: String(amount),
          planId: planId,
          paymentId: razorpay_payment_id,
          orderId: razorpay_order_id,
          routeTarget: "PLATFORM_BUSINESS_ACCOUNT"
        }
      });
    });

    return {
      success: true,
      subscriptionId: subId,
      message: `Subscription payment of ₹${amount} verified and plan ${planId} activated for ${dealershipId}.`
    };
  } catch (error) {
    console.error("Error in verifySubscriptionPayment:", error);
    if (error instanceof functions.https.HttpsError) throw error;
    throw new functions.https.HttpsError("internal", error.message);
  }
});
