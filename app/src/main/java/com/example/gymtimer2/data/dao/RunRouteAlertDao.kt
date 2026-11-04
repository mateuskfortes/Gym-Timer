package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface RunRouteAlertDao {
    /** Silencia os avisos de afastamento somente para a sessão indicada. */
    @Query("UPDATE runs SET route_alerts_muted = 1 WHERE id = :runId AND status != 'COMPLETED'")
    suspend fun muteRouteAlerts(runId: Long)
}
