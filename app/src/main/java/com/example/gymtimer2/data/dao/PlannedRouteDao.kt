package com.example.gymtimer2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.gymtimer2.data.entity.PlannedRouteEntity
import com.example.gymtimer2.data.entity.PlannedRoutePointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedRouteDao {
    @Insert
    suspend fun insertRoute(route: PlannedRouteEntity): Long

    @Insert
    suspend fun insertPoints(points: List<PlannedRoutePointEntity>)

    @Transaction
    suspend fun insertImportedRoute(
        route: PlannedRouteEntity,
        points: List<PlannedRoutePointEntity>
    ): Long {
        val routeId = insertRoute(route)
        insertPoints(points.map { it.copy(routeId = routeId) })
        return routeId
    }

    @Query("SELECT * FROM planned_routes ORDER BY is_favorite DESC, importedAtEpochMs DESC")
    fun observeRoutes(): Flow<List<PlannedRouteEntity>>

    @Query("UPDATE planned_routes SET is_favorite = :isFavorite WHERE id = :routeId")
    suspend fun setFavorite(routeId: Long, isFavorite: Boolean)

    @Query("SELECT * FROM planned_route_points WHERE route_id = :routeId ORDER BY point_index")
    fun observePoints(routeId: Long): Flow<List<PlannedRoutePointEntity>>

    @Query(
        "SELECT routes.id AS routeId, routes.name, routes.distanceMeters, " +
            "points.latitude, points.longitude " +
            "FROM planned_routes AS routes " +
            "INNER JOIN planned_route_points AS points ON points.route_id = routes.id " +
            "WHERE routes.is_local_segment = 1 AND points.point_index = 0 " +
            "ORDER BY routes.importedAtEpochMs DESC"
    )
    fun observeLocalSegmentStarts(): Flow<List<LocalSegmentStartRow>>
}

data class LocalSegmentStartRow(
    val routeId: Long,
    val name: String,
    val distanceMeters: Double,
    val latitude: Double,
    val longitude: Double
)
