package com.nichihab.app.ui.commercial

object CommercialTierManager {
    const val FREE_TIER_DAILY_TASK_LIMIT = 3

    // Actualizado por BillingManager cuando se detecta suscripción activa
    var isProUser = true

    // Puente Comercial: Todas las funciones de gating centralizadas aquí.
    // Para desactivar cualquier restricción, solo cambia el return a true.

    fun canAddTask(currentTasksToday: Int): Boolean {
        return isProUser || currentTasksToday < FREE_TIER_DAILY_TASK_LIMIT
    }

    fun canUseHardMode(): Boolean {
        return isProUser // Premium only
    }

    fun canUseWeeklyCalendar(): Boolean {
        return isProUser // Premium only
    }

    fun canScheduleForLater(): Boolean {
        return isProUser // Premium only
    }

    fun canViewFullHistory(): Boolean {
        return isProUser
    }

    fun remainingTasks(currentTasksToday: Int): Int {
        if (isProUser) return 9999
        return (FREE_TIER_DAILY_TASK_LIMIT - currentTasksToday).coerceAtLeast(0)
    }
}
