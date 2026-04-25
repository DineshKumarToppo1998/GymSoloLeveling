package com.hunterxdk.gymsololeveling.feature.weight.data

import com.hunterxdk.gymsololeveling.core.data.local.dao.WeightEntryDao
import com.hunterxdk.gymsololeveling.core.data.local.entity.WeightEntryEntity
import com.hunterxdk.gymsololeveling.core.domain.SessionManager
import com.hunterxdk.gymsololeveling.core.domain.model.effectiveUserId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepository @Inject constructor(
    private val dao: WeightEntryDao,
    private val sessionManager: SessionManager,
) {
    fun getEntries(): Flow<List<WeightEntryEntity>> {
        val uid = sessionManager.session.value.effectiveUserId
        return dao.getAll(uid)
    }

    suspend fun logWeight(weightKg: Double, notes: String?) {
        val uid = sessionManager.session.value.effectiveUserId
        dao.insert(WeightEntryEntity(
            id = java.util.UUID.randomUUID().toString(),
            uid = uid,
            weightKg = weightKg,
            loggedAt = System.currentTimeMillis(),
            notes = notes ?: "",
        ))
    }

    suspend fun deleteEntry(entry: WeightEntryEntity) = dao.delete(entry)
}