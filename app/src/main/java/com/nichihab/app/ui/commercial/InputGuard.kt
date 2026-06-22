package com.nichihab.app.ui.commercial

object InputGuard {
    fun sanitizeTaskDescription(text: String): String {
        if (!CommercialSecurityConfig.ENABLE_INPUT_GUARD) return text
        var sanitized = text.trim()
        if (sanitized.length > CommercialSecurityConfig.MAX_TASK_CHARS) {
            sanitized = sanitized.substring(0, CommercialSecurityConfig.MAX_TASK_CHARS)
        }
        return sanitized
    }

    fun containsInjectionPattern(text: String): Boolean {
        if (!CommercialSecurityConfig.ENABLE_INPUT_GUARD) return false
        val lower = text.lowercase()
        return lower.contains("ignore previous") || 
               lower.contains("you are now") || 
               lower.contains("system:") ||
               lower.contains("new rules:")
    }
}
