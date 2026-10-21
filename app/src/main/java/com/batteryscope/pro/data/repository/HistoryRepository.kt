package com.batteryscope.pro.data.repository

import com.batteryscope.pro.data.db.AppDatabase
import com.batteryscope.pro.data.model.CapacityEstimate
import com.batteryscope.pro.data.model.ChargingSession
import com.batteryscope.pro.data.model.DischargeLog
import com.batteryscope.pro.domain.estimator.CapacityEstimator
import com.batteryscope.pro.domain.tracker.CycleTracker
import kotlinx.coroutines.flow.Flow

class HistoryRepository(
    private val db: AppDatabase,
    private val capacityEstimator: CapacityEstimator = CapacityEstimator(),
    private val cycleTracker: CycleTracker = CycleTracker()
) {
    private val sessionDao = db.chargingSessionDao()
    private val estimateDao = db.capacityEstimateDao()
    private val dischargeDao = db.dischargeLogDao()

    // Sessions
    fun sessionsFlow(): Flow<List<ChargingSession>> = sessionDao.getAllFlow()
    suspend fun getAllSessions(): List<ChargingSession> = sessionDao.getAll()
    suspend fun getSessionById(id: Long): ChargingSession? = sessionDao.getById(id)
    suspend fun getActiveSession(): ChargingSession? = sessionDao.getActiveSession()
    suspend fun insertSession(session: ChargingSession): Long = sessionDao.insert(session)
    suspend fun updateSession(session: ChargingSession) = sessionDao.update(session)
    suspend fun deleteSession(id: Long) = sessionDao.deleteById(id)
    suspend fun deleteAllSessions() = sessionDao.deleteAll()

    // Capacity estimates
    fun latestEstimateFlow(): Flow<CapacityEstimate?> = estimateDao.getLatestFlow()
    fun allEstimatesFlow(): Flow<List<CapacityEstimate>> = estimateDao.getAllFlow()
    suspend fun getLatestEstimate(): CapacityEstimate? = estimateDao.getLatest()

    suspend fun recomputeCapacityEstimate() {
        val valid = sessionDao.getValidSessionsForEstimate(0.3)
        val estimate = capacityEstimator.estimateFromSessions(valid)
        if (estimate.validSessionCount > 0) {
            estimateDao.insert(estimate)
        }
    }

    // Discharge
    fun totalDischargedFlow(): Flow<Double> = dischargeDao.getTotalDischargedMahFlow()
    suspend fun getTotalDischargedMah(): Double = dischargeDao.getTotalDischargedMah()
    suspend fun insertDischargeLog(log: DischargeLog) = dischargeDao.insert(log)
    suspend fun deleteAllDischargeLogs() = dischargeDao.deleteAll()

    suspend fun getEquivalentCycles(nominalMah: Int?): Double? {
        val total = getTotalDischargedMah()
        return cycleTracker.computeEquivalentCycles(total, nominalMah)
    }

    suspend fun clearAllHistory() {
        sessionDao.deleteAll()
        estimateDao.deleteAll()
        dischargeDao.deleteAll()
    }
}
