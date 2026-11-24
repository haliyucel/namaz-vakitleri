package com.namazvakti.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class KazaPrayerType(val displayName: String, val rekats: String) {
    SABAH("Sabah", "2 rekât farz"),
    OGLE("Öğle", "4 rekât farz"),
    IKINDI("İkindi", "4 rekât farz"),
    AKSAM("Akşam", "3 rekât farz"),
    YATSI("Yatsı", "4 rekât farz"),
    VITIR("Vitir", "3 rekât vacip")
}

data class KazaTrackerState(
    val counts: Map<KazaPrayerType, Int> = KazaPrayerType.entries.associateWith { 0 },
    val fastingDays: Int = 0,
    val initialTotalPrayers: Int = 0,
    val completedPrayers: Int = 0,
    val tesbihIndex: Int = 0,
    val tesbihCount: Int = 0,
    val tesbihTotal: Int = 0
) {
    val totalRemainingPrayers: Int
        get() = counts.values.sum()

    val completionFraction: Float
        get() {
            val denom = (completedPrayers + totalRemainingPrayers).coerceAtLeast(1)
            return if (totalRemainingPrayers == 0 && completedPrayers == 0) 0f
            else (completedPrayers.toFloat() / denom.toFloat()).coerceIn(0f, 1f)
        }
}

class KazaRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<KazaTrackerState> = _state.asStateFlow()

    fun addDaysToAllPrayers(days: Int, replaceExisting: Boolean) {
        if (days < 0) return
        update { cur ->
            val nextCounts = KazaPrayerType.entries.associateWith { type ->
                if (replaceExisting) days else (cur.counts[type] ?: 0) + days
            }
            cur.copy(
                counts = nextCounts,
                initialTotalPrayers = if (replaceExisting) nextCounts.values.sum()
                else cur.initialTotalPrayers + (days * KazaPrayerType.entries.size),
                completedPrayers = if (replaceExisting) 0 else cur.completedPrayers
            )
        }
    }

    fun adjustPrayer(type: KazaPrayerType, delta: Int) {
        update { cur ->
            val oldVal = cur.counts[type] ?: 0
            val newVal = (oldVal + delta).coerceAtLeast(0)
            val diff = oldVal - newVal // positive means user completed prayers
            val nextCounts = cur.counts.toMutableMap().apply { put(type, newVal) }
            cur.copy(
                counts = nextCounts,
                completedPrayers = (cur.completedPrayers + diff).coerceAtLeast(0)
            )
        }
    }

    fun completeOneFullDay() {
        update { cur ->
            var completedNow = 0
            val nextCounts = KazaPrayerType.entries.associateWith { type ->
                val v = cur.counts[type] ?: 0
                if (v > 0) {
                    completedNow++
                    v - 1
                } else 0
            }
            cur.copy(
                counts = nextCounts,
                completedPrayers = cur.completedPrayers + completedNow
            )
        }
    }

    fun adjustFastingDays(delta: Int) {
        update { cur ->
            cur.copy(fastingDays = (cur.fastingDays + delta).coerceAtLeast(0))
        }
    }

    fun resetAllKaza() {
        update { cur ->
            cur.copy(
                counts = KazaPrayerType.entries.associateWith { 0 },
                fastingDays = 0,
                initialTotalPrayers = 0,
                completedPrayers = 0
            )
        }
    }

    fun updateTesbih(index: Int, count: Int, total: Int) {
        update { cur ->
            cur.copy(
                tesbihIndex = index,
                tesbihCount = count.coerceAtLeast(0),
                tesbihTotal = total.coerceAtLeast(0)
            )
        }
    }

    private fun update(transform: (KazaTrackerState) -> KazaTrackerState) {
        val next = transform(_state.value)
        save(next)
        _state.value = next
    }

    private fun load(): KazaTrackerState {
        val counts = KazaPrayerType.entries.associateWith { type ->
            prefs.getInt("kaza_${type.name}", 0).coerceAtLeast(0)
        }
        return KazaTrackerState(
            counts = counts,
            fastingDays = prefs.getInt(KEY_FASTING, 0).coerceAtLeast(0),
            initialTotalPrayers = prefs.getInt(KEY_INITIAL, 0).coerceAtLeast(0),
            completedPrayers = prefs.getInt(KEY_COMPLETED, 0).coerceAtLeast(0),
            tesbihIndex = prefs.getInt(KEY_TESBIH_IDX, 0).coerceAtLeast(0),
            tesbihCount = prefs.getInt(KEY_TESBIH_CNT, 0).coerceAtLeast(0),
            tesbihTotal = prefs.getInt(KEY_TESBIH_TOT, 0).coerceAtLeast(0)
        )
    }

    private fun save(s: KazaTrackerState) {
        prefs.edit().apply {
            s.counts.forEach { (type, value) -> putInt("kaza_${type.name}", value) }
            putInt(KEY_FASTING, s.fastingDays)
            putInt(KEY_INITIAL, s.initialTotalPrayers)
            putInt(KEY_COMPLETED, s.completedPrayers)
            putInt(KEY_TESBIH_IDX, s.tesbihIndex)
            putInt(KEY_TESBIH_CNT, s.tesbihCount)
            putInt(KEY_TESBIH_TOT, s.tesbihTotal)
        }.apply()
    }

    companion object {
        private const val PREFS_NAME = "namaz_kaza_tracker"
        private const val KEY_FASTING = "kaza_fasting"
        private const val KEY_INITIAL = "kaza_initial"
        private const val KEY_COMPLETED = "kaza_completed"
        private const val KEY_TESBIH_IDX = "tesbih_idx"
        private const val KEY_TESBIH_CNT = "tesbih_cnt"
        private const val KEY_TESBIH_TOT = "tesbih_tot"
    }
}
