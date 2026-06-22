package com.nichihab.app.util

import com.nichihab.app.data.local.db.entity.SessionEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object TimeUtil {
    
    /**
     * Determines the logical date for the current time.
     * If the session is an overnight session (startTime > endTime) and the current time is 
     * before the endTime, it means we are technically still in the "tail end" of yesterday's session.
     * Therefore, the logical date is Yesterday. Otherwise, it is Today.
     * 
     * @param latestSession The template session to determine if the schedule is overnight
     * @return ISO_LOCAL_DATE string for the logical date
     */
    fun getLogicalDate(latestSession: SessionEntity?): String {
        val today = LocalDate.now()
        val nowSec = LocalTime.now().toSecondOfDay()
        
        if (latestSession != null) {
            val startSec = latestSession.startTimeHour * 3600 + latestSession.startTimeMinute * 60
            val endSec = latestSession.endTimeHour * 3600 + latestSession.endTimeMinute * 60
            
            // If it's a cross-midnight session and we are currently before the end time
            if (startSec > endSec && nowSec < endSec) {
                return today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            }
        }
        
        return today.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * Determines the logical date for the current time.
     * @param latestSession The template session (domain model)
     * @return ISO_LOCAL_DATE string for the logical date
     */
    fun getLogicalDateDomain(latestSession: com.nichihab.app.domain.model.Session?): String {
        val today = LocalDate.now()
        val nowSec = LocalTime.now().toSecondOfDay()
        
        if (latestSession != null) {
            val startSec = latestSession.startTimeHour * 3600 + latestSession.startTimeMinute * 60
            val endSec = latestSession.endTimeHour * 3600 + latestSession.endTimeMinute * 60
            
            if (startSec > endSec && nowSec < endSec) {
                return today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            }
        }
        
        return today.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}
