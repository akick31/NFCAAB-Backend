package com.nfcaab.backend.repositories

import com.nfcaab.backend.model.Game
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
interface GameRepository : CrudRepository<Game, Int>, JpaSpecificationExecutor<Game> {
    @Query(value = "SELECT * FROM game WHERE game_id =?", nativeQuery = true)
    fun getGameById(gameId: Int): Game?

    @Query(value = "SELECT * FROM game WHERE JSON_CONTAINS(request_message_id, ?, '\$')", nativeQuery = true)
    fun getGameByRequestMessageId(requestMessageId: String): Game?

    @Query(value = "SELECT * FROM game WHERE platform_id = :platformId", nativeQuery = true)
    fun getGameByPlatformId(platformId: String): Game?

    @Query(value = "SELECT * FROM game WHERE game_type != 'SCRIMMAGE'", nativeQuery = true)
    fun getAllGames(): List<Game>

    @Query(value = "SELECT * FROM game WHERE game_type != 'SCRIMMAGE' AND game_id >= :gameId", nativeQuery = true)
    fun getAllGamesMoreRecentThanGameId(gameId: Int): List<Game>

    @Query(value = "SELECT * FROM game WHERE game_status != 'FINAL' AND game_type != 'SCRIMMAGE'", nativeQuery = true)
    fun getAllOngoingGames(): List<Game>

    @Query(
        "SELECT DISTINCT * FROM game WHERE (home_team_rank BETWEEN 1 AND 25 OR away_team_rank BETWEEN 1 AND 25)",
        nativeQuery = true,
    )
    fun getRankedGames(): List<Game>

    @Query(
        value =
            "SELECT * FROM game " +
                "WHERE (home_team = :team OR away_team = :team) " +
                "AND season = :season " +
                "AND week = :week " +
                "AND game_type != 'SCRIMMAGE'",
        nativeQuery = true,
    )
    fun getGamesByTeamSeasonAndWeek(
        team: String,
        season: Int,
        week: Int,
    ): Game?

    @Query(
        value =
            "SELECT * FROM game " +
                "WHERE season = :season " +
                "AND week = :week " +
                "AND game_type != 'SCRIMMAGE'",
        nativeQuery = true,
    )
    fun getGamesBySeasonAndWeek(
        season: Int,
        week: Int,
    ): List<Game>

    @Query(
        value =
            "SELECT * FROM game " +
                "WHERE STR_TO_DATE(game_timer, '%m/%d/%Y %H:%i:%s') <= CONVERT_TZ(NOW(), 'UTC', 'America/New_York') " +
                "AND game_status != 'FINAL' " +
                "AND game_warned = True",
        nativeQuery = true,
    )
    fun findExpiredTimers(): List<Game>

    @Query(
        value =
            "SELECT * FROM game " +
                "WHERE STR_TO_DATE(game_timer, '%m/%d/%Y %H:%i:%s') " +
                "BETWEEN CONVERT_TZ(NOW(), 'UTC', 'America/New_York') " +
                "AND DATE_ADD(CONVERT_TZ(NOW(), 'UTC', 'America/New_York'), INTERVAL 6 HOUR) " +
                "AND game_status != 'FINAL' " +
                "AND game_warned = False",
        nativeQuery = true,
    )
    fun findGamesToWarn(): List<Game>

    @Transactional
    @Modifying
    @Query(value = "UPDATE game SET game_warned = True WHERE game_id = ?", nativeQuery = true)
    fun updateGameAsWarned(gameId: Int)

    @Transactional
    @Modifying
    @Query(value = "UPDATE game SET close_game_pinged = true WHERE game_id = ?", nativeQuery = true)
    fun markCloseGamePinged(gameId: Int)

    @Transactional
    @Modifying
    @Query(value = "UPDATE game SET upset_alert_pinged = true WHERE game_id = ?", nativeQuery = true)
    fun markUpsetAlertPinged(gameId: Int)

    @Query(
        value =
            "SELECT COUNT(*) FROM game " +
                "WHERE (home_team = :team OR away_team = :team) " +
                "AND game_id > :gameId " +
                "AND game_status = 'FINAL'",
        nativeQuery = true,
    )
    fun countFinishedGamesByTeamSinceGameId(
        team: String,
        gameId: Int,
    ): Int
}
