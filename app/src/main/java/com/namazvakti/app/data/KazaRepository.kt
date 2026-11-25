package com.namazvakti.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

enum class KazaPrayerType(val displayName: String, val rekats: String) {
    SABAH("Sabah", "2 rekât farz"),
    OGLE("Öğle", "4 rekât farz"),
    IKINDI("İkindi", "4 rekât farz"),
    AKSAM("Akşam", "3 rekât farz"),
    YATSI("Yatsı", "4 rekât farz"),
    VITIR("Vitir", "3 rekât vacip")
}

data class KazaLogEntry(
    val timestampMillis: Long,
    val title: String,
    val badge: String,
    val isFulfillment: Boolean
)

data class KazaTrackerState(
    val counts: Map<KazaPrayerType, Int> = KazaPrayerType.entries.associateWith { 0 },
    val fastingDays: Int = 0,
    val completedFastingDays: Int = 0,
    val initialTotalPrayers: Int = 0,
    val completedPrayers: Int = 0,
    val todayDateIso: String = "",
    val todayCompletedPrayers: Int = 0,
    val todayCompletedFasts: Int = 0,
    val history: List<KazaLogEntry> = emptyList(),
    val canUndo: Boolean = false,
    val tesbihIndex: Int = 0,
    val tesbihCount: Int = 0,
    val tesbihTotal: Int = 0
) {
    val totalRemainingPrayers: Int
        get() = counts.values.sum()

    val prayerCompletionFraction: Float
        get() {
            val denom = (completedPrayers + totalRemainingPrayers).coerceAtLeast(1)
            return if (totalRemainingPrayers == 0 && completedPrayers == 0) 0f
            else (completedPrayers.toFloat() / denom.toFloat()).coerceIn(0f, 1f)
        }

    val fastingCompletionFraction: Float
        get() {
            val denom = (completedFastingDays + fastingDays).coerceAtLeast(1)
            return if (fastingDays == 0 && completedFastingDays == 0) 0f
            else (completedFastingDays.toFloat() / denom.toFloat()).coerceIn(0f, 1f)
        }
}

/**
 * Pure state-transition functions for Kaza prayer & fasting tracking (100% unit-testable).
 */
object KazaStateReducer {
    private const val MAX_HISTORY = 25

    fun normalizeForDate(state: KazaTrackerState, todayIso: String): KazaTrackerState {
        if (state.todayDateIso == todayIso) return state
        return state.copy(
            todayDateIso = todayIso,
            todayCompletedPrayers = 0,
            todayCompletedFasts = 0
        )
    }

    fun addPrayerDaysToAll(
        state: KazaTrackerState,
        days: Int,
        replaceExisting: Boolean,
        todayIso: String,
        nowMillis: Long
    ): KazaTrackerState {
        if (days < 0) return state
        val base = normalizeForDate(state, todayIso)
        val nextCounts = KazaPrayerType.entries.associateWith { type ->
            if (replaceExisting) days else (base.counts[type] ?: 0) + days
        }
        val entry = KazaLogEntry(
            timestampMillis = nowMillis,
            title = if (replaceExisting) "Kaza namazı borcu $days gün olarak ayarlandı"
            else "Kaza namazına $days gün borç eklendi",
            badge = "+${days * KazaPrayerType.entries.size}",
            isFulfillment = false
        )
        return base.copy(
            counts = nextCounts,
            initialTotalPrayers = if (replaceExisting) nextCounts.values.sum()
            else base.initialTotalPrayers + (days * KazaPrayerType.entries.size),
            completedPrayers = if (replaceExisting) 0 else base.completedPrayers,
            history = prependHistory(base.history, entry)
        )
    }

    fun fulfillPrayer(
        state: KazaTrackerState,
        type: KazaPrayerType,
        count: Int = 1,
        todayIso: String,
        nowMillis: Long
    ): KazaTrackerState {
        val base = normalizeForDate(state, todayIso)
        val current = base.counts[type] ?: 0
        if (current <= 0 || count <= 0) return base
        val deducted = count.coerceAtMost(current)
        val nextCounts = base.counts.toMutableMap().apply { put(type, current - deducted) }
        val entry = KazaLogEntry(
            timestampMillis = nowMillis,
            title = "${type.displayName} kazası kılındı",
            badge = "-$deducted",
            isFulfillment = true
        )
        return base.copy(
            counts = nextCounts,
            completedPrayers = base.completedPrayers + deducted,
            todayCompletedPrayers = base.todayCompletedPrayers + deducted,
            history = prependHistory(base.history, entry)
        )
    }

    fun setPrayerDebt(
        state: KazaTrackerState,
        type: KazaPrayerType,
        newCount: Int,
        todayIso: String,
        nowMillis: Long
    ): KazaTrackerState {
        val safe = newCount.coerceAtLeast(0)
        val base = normalizeForDate(state, todayIso)
        val old = base.counts[type] ?: 0
        if (safe == old) return base
        val nextCounts = base.counts.toMutableMap().apply { put(type, safe) }
        val delta = safe - old
        val badge = if (delta > 0) "+$delta" else "$delta"
        val entry = KazaLogEntry(
            timestampMillis = nowMillis,
            title = "${type.displayName} kaza borcu güncellendi ($safe)",
            badge = badge,
            isFulfillment = false
        )
        return base.copy(
            counts = nextCounts,
            history = prependHistory(base.history, entry)
        )
    }

    fun fulfillOneFullDay(
        state: KazaTrackerState,
        todayIso: String,
        nowMillis: Long
    ): KazaTrackerState {
        val base = normalizeForDate(state, todayIso)
        var completedNow = 0
        val nextCounts = KazaPrayerType.entries.associateWith { type ->
            val v = base.counts[type] ?: 0
            if (v > 0) {
                completedNow++
                v - 1
            } else 0
        }
        if (completedNow == 0) return base
        val entry = KazaLogEntry(
            timestampMillis = nowMillis,
            title = "1 günlük kaza namazı kılındı",
            badge = "-$completedNow",
            isFulfillment = true
        )
        return base.copy(
            counts = nextCounts,
            completedPrayers = base.completedPrayers + completedNow,
            todayCompletedPrayers = base.todayCompletedPrayers + completedNow,
            history = prependHistory(base.history, entry)
        )
    }

    fun addFastingDebt(
        state: KazaTrackerState,
        days: Int,
        replaceExisting: Boolean,
        todayIso: String,
        nowMillis: Long
    ): KazaTrackerState {
        val safe = days.coerceAtLeast(0)
        val base = normalizeForDate(state, todayIso)
        val nextDays = if (replaceExisting) safe else base.fastingDays + safe
        if (nextDays == base.fastingDays && !replaceExisting) return base
        val entry = KazaLogEntry(
            timestampMillis = nowMillis,
            title = if (replaceExisting) "Kaza orucu borcu $safe gün olarak ayarlandı"
            else "Kaza orucuna $safe gün borç eklendi",
            badge = "+$safe gün",
            isFulfillment = false
        )
        return base.copy(
            fastingDays = nextDays,
            completedFastingDays = if (replaceExisting) 0 else base.completedFastingDays,
            history = prependHistory(base.history, entry)
        )
    }

    fun fulfillFastingDay(
        state: KazaTrackerState,
        days: Int = 1,
        todayIso: String,
        nowMillis: Long
    ): KazaTrackerState {
        val base = normalizeForDate(state, todayIso)
        if (base.fastingDays <= 0 || days <= 0) return base
        val deducted = days.coerceAtMost(base.fastingDays)
        val entry = KazaLogEntry(
            timestampMillis = nowMillis,
            title = "$deducted gün kaza orucu tutuldu",
            badge = "-$deducted gün",
            isFulfillment = true
        )
        return base.copy(
            fastingDays = base.fastingDays - deducted,
            completedFastingDays = base.completedFastingDays + deducted,
            todayCompletedFasts = base.todayCompletedFasts + deducted,
            history = prependHistory(base.history, entry)
        )
    }

    private fun prependHistory(existing: List<KazaLogEntry>, item: KazaLogEntry): List<KazaLogEntry> =
        (listOf(item) + existing).take(MAX_HISTORY)
}

class KazaRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var undoSnapshot: KazaTrackerState? = null
    private val _state = MutableStateFlow(load())
    val state: StateFlow<KazaTrackerState> = _state.asStateFlow()

    fun addDaysToAllPrayers(days: Int, replaceExisting: Boolean) {
        mutateWithUndo { cur ->
            KazaStateReducer.addPrayerDaysToAll(
                state = cur,
                days = days,
                replaceExisting = replaceExisting,
                todayIso = LocalDate.now().toString(),
                nowMillis = System.currentTimeMillis()
            )
        }
    }

    fun fulfillPrayer(type: KazaPrayerType, count: Int = 1) {
        mutateWithUndo { cur ->
            KazaStateReducer.fulfillPrayer(
                state = cur,
                type = type,
                count = count,
                todayIso = LocalDate.now().toString(),
                nowMillis = System.currentTimeMillis()
            )
        }
    }

    fun setPrayerDebt(type: KazaPrayerType, newCount: Int) {
        mutateWithUndo { cur ->
            KazaStateReducer.setPrayerDebt(
                state = cur,
                type = type,
                newCount = newCount,
                todayIso = LocalDate.now().toString(),
                nowMillis = System.currentTimeMillis()
            )
        }
    }

    fun addPrayerDebt(type: KazaPrayerType, delta: Int) {
        val current = _state.value.counts[type] ?: 0
        setPrayerDebt(type, (current + delta).coerceAtLeast(0))
    }

    fun completeOneFullDay() {
        mutateWithUndo { cur ->
            KazaStateReducer.fulfillOneFullDay(
                state = cur,
                todayIso = LocalDate.now().toString(),
                nowMillis = System.currentTimeMillis()
            )
        }
    }

    fun addFastingDebt(days: Int, replaceExisting: Boolean = false) {
        mutateWithUndo { cur ->
            KazaStateReducer.addFastingDebt(
                state = cur,
                days = days,
                replaceExisting = replaceExisting,
                todayIso = LocalDate.now().toString(),
                nowMillis = System.currentTimeMillis()
            )
        }
    }

    fun fulfillFastingDay(days: Int = 1) {
        mutateWithUndo { cur ->
            KazaStateReducer.fulfillFastingDay(
                state = cur,
                days = days,
                todayIso = LocalDate.now().toString(),
                nowMillis = System.currentTimeMillis()
            )
        }
    }

    fun undoLastAction() {
        val snap = undoSnapshot ?: return
        undoSnapshot = null
        val restored = snap.copy(canUndo = false)
        save(restored)
        _state.value = restored
    }

    fun resetAllKaza() {
        mutateWithUndo { cur ->
            cur.copy(
                counts = KazaPrayerType.entries.associateWith { 0 },
                fastingDays = 0,
                completedFastingDays = 0,
                initialTotalPrayers = 0,
                completedPrayers = 0,
                todayDateIso = LocalDate.now().toString(),
                todayCompletedPrayers = 0,
                todayCompletedFasts = 0,
                history = emptyList()
            )
        }
    }

    fun updateTesbih(index: Int, count: Int, total: Int) {
        val next = _state.value.copy(
            tesbihIndex = index,
            tesbihCount = count.coerceAtLeast(0),
            tesbihTotal = total.coerceAtLeast(0)
        )
        save(next)
        _state.value = next
    }

    private fun mutateWithUndo(transform: (KazaTrackerState) -> KazaTrackerState) {
        val before = _state.value
        val after = transform(before)
        if (after == before) return
        undoSnapshot = before
        val next = after.copy(canUndo = true)
        save(next)
        _state.value = next
    }

    private fun load(): KazaTrackerState {
        val counts = KazaPrayerType.entries.associateWith { type ->
            prefs.getInt("kaza_${type.name}", 0).coerceAtLeast(0)
        }
        val todayIso = LocalDate.now().toString()
        val storedDate = prefs.getString(KEY_TODAY_DATE, "") ?: ""
        val raw = KazaTrackerState(
            counts = counts,
            fastingDays = prefs.getInt(KEY_FASTING, 0).coerceAtLeast(0),
            completedFastingDays = prefs.getInt(KEY_FASTING_COMPLETED, 0).coerceAtLeast(0),
            initialTotalPrayers = prefs.getInt(KEY_INITIAL, 0).coerceAtLeast(0),
            completedPrayers = prefs.getInt(KEY_COMPLETED, 0).coerceAtLeast(0),
            todayDateIso = storedDate,
            todayCompletedPrayers = prefs.getInt(KEY_TODAY_PRAYERS, 0).coerceAtLeast(0),
            todayCompletedFasts = prefs.getInt(KEY_TODAY_FASTS, 0).coerceAtLeast(0),
            history = decodeHistory(prefs.getString(KEY_HISTORY_JSON, null)),
            canUndo = false,
            tesbihIndex = prefs.getInt(KEY_TESBIH_IDX, 0).coerceAtLeast(0),
            tesbihCount = prefs.getInt(KEY_TESBIH_CNT, 0).coerceAtLeast(0),
            tesbihTotal = prefs.getInt(KEY_TESBIH_TOT, 0).coerceAtLeast(0)
        )
        return KazaStateReducer.normalizeForDate(raw, todayIso)
    }

    private fun save(s: KazaTrackerState) {
        prefs.edit().apply {
            s.counts.forEach { (type, value) -> putInt("kaza_${type.name}", value) }
            putInt(KEY_FASTING, s.fastingDays)
            putInt(KEY_FASTING_COMPLETED, s.completedFastingDays)
            putInt(KEY_INITIAL, s.initialTotalPrayers)
            putInt(KEY_COMPLETED, s.completedPrayers)
            putString(KEY_TODAY_DATE, s.todayDateIso)
            putInt(KEY_TODAY_PRAYERS, s.todayCompletedPrayers)
            putInt(KEY_TODAY_FASTS, s.todayCompletedFasts)
            putString(KEY_HISTORY_JSON, encodeHistory(s.history))
            putInt(KEY_TESBIH_IDX, s.tesbihIndex)
            putInt(KEY_TESBIH_CNT, s.tesbihCount)
            putInt(KEY_TESBIH_TOT, s.tesbihTotal)
        }.apply()
    }

    private fun encodeHistory(list: List<KazaLogEntry>): String {
        val arr = JSONArray()
        for (item in list) {
            arr.put(
                JSONObject().apply {
                    put("ts", item.timestampMillis)
                    put("t", item.title)
                    put("b", item.badge)
                    put("f", item.isFulfillment)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeHistory(raw: String?): List<KazaLogEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        KazaLogEntry(
                            timestampMillis = o.optLong("ts", 0L),
                            title = o.optString("t", ""),
                            badge = o.optString("b", ""),
                            isFulfillment = o.optBoolean("f", true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val PREFS_NAME = "namaz_kaza_tracker"
        private const val KEY_FASTING = "kaza_fasting"
        private const val KEY_FASTING_COMPLETED = "kaza_fasting_completed"
        private const val KEY_INITIAL = "kaza_initial"
        private const val KEY_COMPLETED = "kaza_completed"
        private const val KEY_TODAY_DATE = "kaza_today_date"
        private const val KEY_TODAY_PRAYERS = "kaza_today_prayers"
        private const val KEY_TODAY_FASTS = "kaza_today_fasts"
        private const val KEY_HISTORY_JSON = "kaza_history_json"
        private const val KEY_TESBIH_IDX = "tesbih_idx"
        private const val KEY_TESBIH_CNT = "tesbih_cnt"
        private const val KEY_TESBIH_TOT = "tesbih_tot"
    }
}
