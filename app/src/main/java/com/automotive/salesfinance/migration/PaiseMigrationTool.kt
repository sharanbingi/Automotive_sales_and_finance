package com.automotive.salesfinance.migration

import com.automotive.salesfinance.model.Loan
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class MigrationStatus {
    DRY_RUN, MIGRATED, SKIPPED_ALREADY_MIGRATED, CONFLICT, FAILED
}

data class MigrationResult(
    val dealershipId: String,
    val loanId: String,
    val status: MigrationStatus,
    val fieldsInitialized: List<String>,
    val conflicts: List<String>,
    val errorMessage: String? = null
)

class PaiseMigrationTool(private val firestore: FirebaseFirestore) {

    suspend fun migrateDealershipLoans(dealershipId: String, dryRun: Boolean = true): List<MigrationResult> {
        return try {
            val snapshot = firestore.collection("dealerships")
                .document(dealershipId)
                .collection("loans")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val loanId = doc.id
                migrateLoanPaise(dealershipId, loanId, dryRun)
            }
        } catch (e: Exception) {
            listOf(MigrationResult(dealershipId, "ALL", MigrationStatus.FAILED, emptyList(), emptyList(), e.message))
        }
    }

    suspend fun migrateLoanPaise(dealershipId: String, loanId: String, dryRun: Boolean = true): MigrationResult {
        val loanRef = firestore.collection("dealerships")
            .document(dealershipId)
            .collection("loans")
            .document(loanId)

        var finalFieldsInitialized = listOf<String>()
        var finalConflicts = listOf<String>()
        var finalStatus = MigrationStatus.DRY_RUN

        try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(loanRef)
                if (!snapshot.exists()) {
                    throw Exception("Loan document not found")
                }

                val loan = snapshot.toObject(Loan::class.java) ?: throw Exception("Failed to deserialize Loan")

                val resultData = PaiseMigrationLogic.calculateUpdates(loan)
                
                finalFieldsInitialized = resultData.fieldsInitialized
                finalConflicts = resultData.conflicts

                if (resultData.conflicts.isNotEmpty()) {
                    finalStatus = MigrationStatus.CONFLICT
                    return@runTransaction
                }

                if (resultData.fieldsInitialized.isEmpty()) {
                    finalStatus = MigrationStatus.SKIPPED_ALREADY_MIGRATED
                    return@runTransaction
                }

                if (dryRun) {
                    finalStatus = MigrationStatus.DRY_RUN
                    return@runTransaction
                }

                if (resultData.updates.isNotEmpty()) {
                    transaction.update(loanRef, resultData.updates)
                    finalStatus = MigrationStatus.MIGRATED
                }
            }.await()

            return MigrationResult(
                dealershipId = dealershipId,
                loanId = loanId,
                status = finalStatus,
                fieldsInitialized = finalFieldsInitialized,
                conflicts = finalConflicts,
                errorMessage = null
            )
        } catch (e: Exception) {
            return MigrationResult(
                dealershipId = dealershipId,
                loanId = loanId,
                status = MigrationStatus.FAILED,
                fieldsInitialized = finalFieldsInitialized,
                conflicts = finalConflicts,
                errorMessage = e.message
            )
        }
    }
}
