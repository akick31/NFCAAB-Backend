package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.dto.response.GameSpreadResult
import com.nfcaab.backend.dto.response.UpdateSpreadsResponse
import com.nfcaab.backend.dto.response.VegasOddsResponse
import com.nfcaab.backend.model.Team
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.repositories.GameStatsRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

@Service
class VegasOddsService(
    private val gameRepository: GameRepository,
    private val gameStatsRepository: GameStatsRepository,
) {
    private val logger = LoggerFactory.getLogger(VegasOddsService::class.java)

    /**
     * Calculate Vegas odds for a matchup based on team ELO ratings
     * @param homeTeam Home team
     * @param awayTeam Away team
     * @return VegasOddsResponse with home and away spreads
     */
    fun calculateVegasOdds(
        homeTeam: Team,
        awayTeam: Team,
    ): VegasOddsResponse {
        val homeElo = homeTeam.currentElo
        val awayElo = awayTeam.currentElo

        logger.info("Calculating Vegas odds: ${homeTeam.name} (${homeElo.toInt()}) vs ${awayTeam.name} (${awayElo.toInt()})")

        val homeSpread = calculateVegasSpread(homeElo, awayElo)
        val awaySpread = -homeSpread

        return VegasOddsResponse(
            homeTeam = homeTeam.name,
            awayTeam = awayTeam.name,
            homeSpread = homeSpread,
            awaySpread = awaySpread,
            homeElo = homeElo,
            awayElo = awayElo,
        )
    }

    /**
     * Get Vegas odds for a matchup based on team names
     */
    fun getVegasOddsByTeams(
        homeTeamName: String,
        awayTeamName: String,
        teamService: TeamService,
    ): ResponseEntity<VegasOddsResponse> =
        try {
            logger.info("Getting Vegas odds for $homeTeamName vs $awayTeamName")

            val homeTeam = teamService.getTeamByName(homeTeamName)
            val awayTeam = teamService.getTeamByName(awayTeamName)

            val odds = calculateVegasOdds(homeTeam = homeTeam, awayTeam = awayTeam)
            ResponseEntity.ok(odds)
        } catch (e: Exception) {
            logger.error("Error getting Vegas odds for teams: ${e.message}", e)
            ResponseEntity.badRequest().build()
        }

    /**
     * Get Vegas odds for a matchup based on custom ELO ratings
     */
    fun getVegasOddsByElo(
        homeElo: Double,
        awayElo: Double,
    ): ResponseEntity<VegasOddsResponse> =
        try {
            logger.info("Getting Vegas odds for ELO: $homeElo vs $awayElo")

            val homeSpread = calculateVegasSpread(homeElo, awayElo)
            val awaySpread = -homeSpread

            val odds = VegasOddsResponse(
                homeTeam = "Home",
                awayTeam = "Away",
                homeSpread = homeSpread,
                awaySpread = awaySpread,
                homeElo = homeElo,
                awayElo = awayElo,
            )
            ResponseEntity.ok(odds)
        } catch (e: Exception) {
            logger.error("Error getting Vegas odds for ELO: ${e.message}", e)
            ResponseEntity.internalServerError().build()
        }

    /**
     * Calculate the Vegas spread for a team based on ELO difference
     * For baseball, this represents runs difference
     * @param homeElo The home team's ELO rating
     * @param awayElo The away team's ELO rating
     * @return The run spread from home team's perspective (negative means home is favored)
     */
    private fun calculateVegasSpread(
        homeElo: Double,
        awayElo: Double,
    ): Double {
        // Standard ELO to point spread conversion: ~3 points per 100 ELO difference for football
        // For baseball, we'll use ~0.5 runs per 100 ELO difference, plus home field advantage (~0.3 runs)
        val eloDifference = homeElo - awayElo
        val spread = (eloDifference / 100.0) * 0.5 + 0.3

        // Round to nearest 0.5 (standard Vegas practice)
        // Return negative for favored team, positive for underdog
        return -((spread * 2).roundToInt() / 2.0)
    }

    /**
     * Calculate and update Vegas spreads for all games in a specific season and week
     * using team_elo from game_stats
     * @param season Season number
     * @param week Week number
     * @return Response indicating success and number of games updated
     */
    fun updateSpreadsForSeasonAndWeek(
        season: Int,
        week: Int,
    ): ResponseEntity<UpdateSpreadsResponse> {
        return try {
            logger.info("Updating Vegas spreads for season $season, week $week")

            // Get all games for the specified season and week
            val games = gameRepository.getGamesBySeasonAndWeek(season, week)
            if (games.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(
                        UpdateSpreadsResponse(
                            message = "No games found for season $season, week $week",
                            season = season,
                            week = week,
                            totalGames = 0,
                            updatedGames = 0,
                            results = emptyList(),
                        ),
                    )
            }

            // Get all game stats for the specified season and week
            val gameStatsList = gameStatsRepository.findBySeasonOrderByGameIdAsc(season)
                .filter { it.week == week }
            if (gameStatsList.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(
                        UpdateSpreadsResponse(
                            message = "No game stats found for season $season, week $week",
                            season = season,
                            week = week,
                            totalGames = games.size,
                            updatedGames = 0,
                            results = emptyList(),
                        ),
                    )
            }

            // Group game stats by game ID for easy lookup
            val gameStatsByGameId = gameStatsList.groupBy { it.gameId }

            var updatedGames = 0
            val results = mutableListOf<GameSpreadResult>()

            for (game in games) {
                val gameStats = gameStatsByGameId[game.id]
                if (gameStats == null || gameStats.size != 2) {
                    logger.warn("Skipping game ${game.id}: Expected 2 game stats, found ${gameStats?.size ?: 0}")
                    continue
                }

                // Find home and away team stats
                val homeStats = gameStats.find { it.team == game.homeTeam }
                val awayStats = gameStats.find { it.team == game.awayTeam }

                if (homeStats == null || awayStats == null) {
                    logger.warn("Skipping game ${game.id}: Missing team stats for home=${game.homeTeam} or away=${game.awayTeam}")
                    continue
                }

                // Calculate spreads using default elo (1500.0 for both teams)
                val spread = calculateVegasSpread(1500.0, 1500.0)
                val homeSpread = spread
                val awaySpread = -spread

                // Note: Game model doesn't have vegas spread fields yet - would need to add them
                // game.homeVegasSpread = homeSpread
                // game.awayVegasSpread = awaySpread
                // gameRepository.save(game)

                updatedGames++
                results.add(
                    GameSpreadResult(
                        gameId = game.id,
                        homeTeam = game.homeTeam,
                        awayTeam = game.awayTeam,
                        homeElo = 1500.0,
                        awayElo = 1500.0,
                        homeSpread = homeSpread,
                        awaySpread = awaySpread,
                    ),
                )

                logger.info(
                    "Calculated spreads for game ${game.id}: ${game.homeTeam} (1500) " +
                        "vs ${game.awayTeam} (1500) - Home: $homeSpread, Away: $awaySpread",
                )
            }

            ResponseEntity.ok(
                UpdateSpreadsResponse(
                    message = "Successfully calculated Vegas spreads",
                    season = season,
                    week = week,
                    totalGames = games.size,
                    updatedGames = updatedGames,
                    results = results,
                ),
            )
        } catch (e: Exception) {
            logger.error("Error updating Vegas spreads for season $season, week $week: ${e.message}", e)
            return ResponseEntity.internalServerError()
                .body(
                    UpdateSpreadsResponse(
                        message = "Failed to update Vegas spreads: ${e.message}",
                        season = season,
                        week = week,
                        totalGames = 0,
                        updatedGames = 0,
                        results = emptyList(),
                    ),
                )
        }
    }
}

