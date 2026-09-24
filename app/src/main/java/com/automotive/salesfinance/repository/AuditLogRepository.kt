package com.automotive.salesfinance.repository

import com.automotive.salesfinance.data.DemoData
import com.automotive.salesfinance.model.AuditLog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuditLogRepository(private val authRepository: AuthRepository) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _auditLogsFlow = MutableStateFlow<List<AuditLog>>(
        if (authRepository.isDemoMode.value) DemoData.auditLogs else emptyList()
    )
    val auditLogsFlow: StateFlow<List<AuditLog>> = _auditLogsFlow.asStateFlow()

    fun clearInMemoryState() {
        _auditLogsFlow.value = emptyList()
    }

    init {
        authRepository.registerClearListener {
            clearInMemoryState()
        }
        CoroutineScope(Dispatchers.Unconfined + SupervisorJob()).launch {
            authRepository.isDemoMode.collect { isDemo ->
                if (!isDemo) {
                    _auditLogsFlow.value = _auditLogsFlow.value.filterNot { a ->
                        DemoData.auditLogs.any { it.auditLogId == a.auditLogId }
                    }
                } else if (_auditLogsFlow.value.isEmpty()) {
                    _auditLogsFlow.value = DemoData.auditLogs
                }
            }
        }
    }

    fun getAuditLogsForDealership(dealershipId: String): List<AuditLog> {
        if (dealershipId.isBlank()) {
            return if (authRepository.isDemoMode.value) {
                _auditLogsFlow.value.filter { it.dealershipId == DemoData.DEMO_DEALERSHIP_ID }
            } else {
                emptyList()
            }
        }
        return _auditLogsFlow.value.filter { it.dealershipId == dealershipId }
    }

    fun getAllAuditLogs(): List<AuditLog> = _auditLogsFlow.value

    suspend fun fetchAuditLogsFromFirestore(dealershipId: String = ""): List<AuditLog> {
        if (authRepository.isDemoMode.value) {
            return if (dealershipId.isBlank()) _auditLogsFlow.value else getAuditLogsForDealership(dealershipId)
        }
        if (firestore == null) {
            _auditLogsFlow.value = _auditLogsFlow.value.filterNot { a -> DemoData.auditLogs.any { it.auditLogId == a.auditLogId } }
            return if (dealershipId.isBlank()) _auditLogsFlow.value else getAuditLogsForDealership(dealershipId)
        }
        return try {
            val fetched = if (dealershipId.isBlank()) {
                val snapshot = firestore?.collectionGroup("audit_logs")?.limit(200)?.get()?.await()
                snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toObject(AuditLog::class.java) } catch (_: Exception) { null }
                } ?: emptyList()
            } else {
                val snapshot = firestore?.collection("dealerships")
                    ?.document(dealershipId)
                    ?.collection("audit_logs")
                    ?.limit(100)
                    ?.get()
                    ?.await()
                snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toObject(AuditLog::class.java) } catch (_: Exception) { null }
                } ?: emptyList()
            }

            if (dealershipId.isBlank()) {
                _auditLogsFlow.value = fetched
            } else {
                val current = _auditLogsFlow.value.filterNot { it.dealershipId == dealershipId || DemoData.auditLogs.any { demo -> demo.auditLogId == it.auditLogId } }.toMutableList()
                current.addAll(fetched)
                _auditLogsFlow.value = current
            }
            fetched
        } catch (e: Exception) {
            val liveOnly = _auditLogsFlow.value.filterNot { a -> DemoData.auditLogs.any { it.auditLogId == a.auditLogId } }
            _auditLogsFlow.value = liveOnly
            if (dealershipId.isBlank()) liveOnly else liveOnly.filter { it.dealershipId == dealershipId }
        }
    }

    suspend fun logEvent(
        dealershipId: String,
        userId: String,
        action: String,
        entityType: String,
        entityId: String,
        metadata: Map<String, String> = emptyMap()
    ): Result<AuditLog> {
        if (dealershipId.isBlank() && !authRepository.isDemoMode.value) {
            return Result.failure(IllegalArgumentException("Missing or blank dealershipId"))
        }
        val targetId = if (dealershipId.isBlank()) DemoData.DEMO_DEALERSHIP_ID else dealershipId
        val log = AuditLog(
            auditLogId = "LOG_" + System.currentTimeMillis() + "_" + (1000..9999).random(),
            dealershipId = targetId,
            userId = userId,
            action = action,
            entityType = entityType,
            entityId = entityId,
            timestamp = System.currentTimeMillis(),
            metadata = metadata
        )

        val currentList = _auditLogsFlow.value.toMutableList()
        currentList.add(0, log)
        _auditLogsFlow.value = currentList

        if (!authRepository.isDemoMode.value && firestore != null) {
            try {
                firestore?.collection("dealerships")
                    ?.document(log.dealershipId)
                    ?.collection("audit_logs")
                    ?.document(log.auditLogId)
                    ?.set(log)
                    ?.await()
            } catch (_: Exception) {}
        }
        return Result.success(log)
    }

    suspend fun saveAuditLog(auditLog: AuditLog): Result<Unit> {
        val currentList = _auditLogsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.auditLogId == auditLog.auditLogId }
        if (index >= 0) {
            currentList[index] = auditLog
        } else {
            currentList.add(0, auditLog)
        }
        _auditLogsFlow.value = currentList

        if (!authRepository.isDemoMode.value && firestore != null) {
            try {
                firestore?.collection("dealerships")
                    ?.document(auditLog.dealershipId)
                    ?.collection("audit_logs")
                    ?.document(auditLog.auditLogId)
                    ?.set(auditLog)
                    ?.await()
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }
}
