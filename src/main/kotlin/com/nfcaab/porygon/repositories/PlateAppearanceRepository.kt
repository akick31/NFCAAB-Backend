package com.nfcaab.porygon.repositories

import com.nfcaab.porygon.model.PlateAppearance
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
interface PlateAppearanceRepository : CrudRepository<PlateAppearance?, Int?> {
    @Query(value = "SELECT * FROM plate_appearances WHERE id =?", nativeQuery = true)
    fun getPlateAppearanceById(id: Int): PlateAppearance?

    @Query(value = "SELECT * FROM plate_appearances WHERE game_id = ? ORDER BY id DESC", nativeQuery = true)
    fun getAllPlateAppearancesByGameId(gameId: Int): List<PlateAppearance>

    @Query(
        value =
            "SELECT pa.* " +
                "FROM plate_appearances pa " +
                "JOIN game g ON pa.game_id = g.id " +
                "WHERE (batter_submitter = :discordTag OR pitcher_submitter = :discordTag) " +
                "AND g.game_type != 'SCRIMMAGE' " +
                "ORDER BY id DESC;",
        nativeQuery = true,
    )
    fun getAllPlateAppearancesByDiscordTag(discordTag: String): List<PlateAppearance>

    @Query(value = "SELECT * FROM plate_appearances WHERE game_id = ? AND plate_appearance_finished = false ORDER BY id DESC LIMIT 1", nativeQuery = true)
    fun getCurrentPlateAppearance(gameId: Int): PlateAppearance?

    @Query(value = "SELECT * FROM plate_appearances WHERE game_id = ? AND plate_appearance_finished = true ORDER BY id DESC LIMIT 1", nativeQuery = true)
    fun getPreviousPlateAppearance(gameId: Int): PlateAppearance?

    @Query(
        value = "SELECT COUNT(*) FROM plate_appearances WHERE game_id = :gameId AND result = 'DELAY OF GAME ON HOME TEAM'",
        nativeQuery = true,
    )
    fun getHomeDelayOfGameInstances(gameId: Int): Int?

    @Query(
        value = "SELECT COUNT(*) FROM plate_appearances WHERE game_id = :gameId AND result = 'DELAY OF GAME ON AWAY TEAM'",
        nativeQuery = true,
    )
    fun getAwayDelayOfGameInstances(gameId: Int): Int?

    @Query(
        value =
            "SELECT AVG(" +
                "CASE " +
                "WHEN pa.batter_submitter = :discordTag THEN pa.batter_response_speed " +
                "WHEN pa.pitcher_submitter = :discordTag THEN pa.pitcher_response_speed " +
                "END " +
                ") AS avg_response_time " +
                "FROM plate_appearances pa " +
                "JOIN game g ON pa.game_id = g.game_id " +
                "WHERE (pa.offensive_submitter = :discordTag OR pa.defensive_submitter = :discordTag) " +
                "AND g.season = :season",
        nativeQuery = true,
    )
    fun getUserAverageResponseTime(
        discordTag: String,
        season: Int,
    ): Double?

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM plate_appearances WHERE game_id =?", nativeQuery = true)
    fun deleteAllPlateAppearancesByGameId(gameId: Int)
}
