package com.nichihab.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichihab.app.domain.model.SessionStatus
import com.nichihab.app.domain.model.Verification
import com.nichihab.app.domain.repository.SessionRepository
import com.nichihab.app.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class StatsUiState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val completionRate: Float = 0f,
    val totalCompleted: Int = 0,
    val totalSessions: Int = 0,
    val estimatedTimeSavedMinutes: Long = 0,
    val heatmapData: Map<String, Int> = emptyMap(), // date -> completion level (0-3)
    val recentVerifications: List<Verification> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val verificationRepository: VerificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.observeAllSessions().collect {
                loadStats()
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val latestSession = sessionRepository.getLatestSession()
            val logicalTodayStr = com.nichihab.app.util.TimeUtil.getLogicalDateDomain(latestSession)
            val logicalToday = LocalDate.parse(logicalTodayStr)
            
            val thirtyDaysAgo = logicalToday.minusDays(29)
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE

            val sessions = sessionRepository.getSessionsInRange(
                thirtyDaysAgo.format(formatter),
                logicalToday.format(formatter)
            )

            // Calculate streaks
            val allSessions = sessions.sortedByDescending { it.date }
            var currentStreak = 0
            var longestStreak = 0
            var tempStreak = 0
            var lastDate: LocalDate? = null

            for (session in allSessions) {
                val sessionDate = LocalDate.parse(session.date)
                if (session.status == SessionStatus.VERIFIED) {
                    if (lastDate == null || ChronoUnit.DAYS.between(sessionDate, lastDate) == 1L) {
                        tempStreak++
                    } else {
                        longestStreak = maxOf(longestStreak, tempStreak)
                        tempStreak = 1
                    }
                    lastDate = sessionDate
                } else {
                    longestStreak = maxOf(longestStreak, tempStreak)
                    tempStreak = 0
                    lastDate = null
                }
            }
            longestStreak = maxOf(longestStreak, tempStreak)

            // Current streak (must include today or yesterday)
            currentStreak = 0
            var checkDate = logicalToday
            for (i in 0 until sessions.size) {
                val session = sessions.find { it.date == checkDate.format(formatter) }
                if (session?.status == SessionStatus.VERIFIED) {
                    currentStreak++
                    checkDate = checkDate.minusDays(1)
                } else if (i == 0 && session == null) {
                    // Today might not have a session yet, check yesterday
                    checkDate = checkDate.minusDays(1)
                    continue
                } else {
                    break
                }
            }

            // Completion rate
            val verifiedCount = sessions.count { it.status == SessionStatus.VERIFIED }
            val completionRate = if (sessions.isNotEmpty()) {
                verifiedCount.toFloat() / sessions.size
            } else 0f

            // Time saved estimate (average 2 hours per blocked session)
            val timeSaved = verifiedCount * 120L

            // Heatmap data
            val heatmap = mutableMapOf<String, Int>()
            for (i in 0L..29L) {
                val date = thirtyDaysAgo.plusDays(i).format(formatter)
                val session = sessions.find { it.date == date }
                heatmap[date] = when {
                    session == null -> 0
                    session.status == SessionStatus.VERIFIED -> 3
                    session.status == SessionStatus.LOCKED -> 1
                    else -> 0
                }
            }

            // Recent verifications
            val recentVerifications = try {
                verificationRepository.getForSession(
                    sessions.firstOrNull()?.id ?: 0
                ).take(20)
            } catch (e: Exception) {
                emptyList()
            }

            _uiState.update {
                it.copy(
                    currentStreak = currentStreak,
                    longestStreak = longestStreak,
                    completionRate = completionRate,
                    totalCompleted = verifiedCount,
                    totalSessions = sessions.size,
                    estimatedTimeSavedMinutes = timeSaved,
                    heatmapData = heatmap,
                    recentVerifications = recentVerifications,
                    isLoading = false
                )
            }
        }
    }
}
