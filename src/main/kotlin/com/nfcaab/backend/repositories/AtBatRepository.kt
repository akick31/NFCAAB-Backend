package com.nfcaab.backend.repositories

import com.nfcaab.backend.model.AtBat
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
interface AtBatRepository : CrudRepository<AtBat?, Int?> {
    @Query(value = "SELECT * FROM at_bats WHERE id =?", nativeQuery = true)
    fun getAtBatById(id: Int): AtBat?

    @Query(value = "SELECT * FROM at_bats WHERE game_id = ? ORDER BY id DESC", nativeQuery = true)
    fun getAllAtBatsByGameId(gameId: Int): List<AtBat>

    @Query(
        value =
            "SELECT ab.* " +
                "FROM at_bats ab " +
                "JOIN game g ON ab.game_id = g.id " +
                "WHERE (batter_submitter = :discordTag OR pitcher_submitter = :discordTag) " +
                "AND g.game_type != 'SCRIMMAGE' " +
                "ORDER BY id DESC;",
        nativeQuery = true,
    )
    fun getAllAtBatsByDiscordTag(discordTag: String): List<AtBat>

    @Query(value = "SELECT * FROM at_bats WHERE game_id = ? AND at_bat_finished = false ORDER BY id DESC LIMIT 1", nativeQuery = true)
    fun getCurrentAtBat(gameId: Int): AtBat?

    @Query(value = "SELECT * FROM at_bats WHERE game_id = ? AND at_bat_finished = true ORDER BY id DESC LIMIT 1", nativeQuery = true)
    fun getPreviousAtBat(gameId: Int): AtBat?

    @Query(
        value = "SELECT COUNT(*) FROM at_bats WHERE game_id = :gameId AND result = 'DELAY OF GAME ON HOME TEAM'",
        nativeQuery = true,
    )
    fun getHomeDelayOfGameInstances(gameId: Int): Int?

    @Query(
        value = "SELECT COUNT(*) FROM at_bats WHERE game_id = :gameId AND result = 'DELAY OF GAME ON AWAY TEAM'",
        nativeQuery = true,
    )
    fun getAwayDelayOfGameInstances(gameId: Int): Int?

    @Query(
        value =
            "SELECT AVG(" +
                "CASE " +
                "WHEN ab.batter_submitter = :discordTag THEN ab.batter_response_speed " +
                "WHEN ab.pitcher_submitter = :discordTag THEN ab.pitcher_response_speed " +
                "END " +
                ") AS avg_response_time " +
                "FROM at_bats ab " +
                "JOIN game g ON ab.game_id = g.game_id " +
                "WHERE (ab.offensive_submitter = :discordTag OR ab.defensive_submitter = :discordTag) " +
                "AND g.season = :season",
        nativeQuery = true,
    )
    fun getUserAverageResponseTime(
        discordTag: String,
        season: Int,
    ): Double?

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM at_bats WHERE game_id =?", nativeQuery = true)
    fun deleteAllAtBatsByGameId(gameId: Int)
}

