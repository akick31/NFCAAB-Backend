package com.nfcaab.porygon.service.nfcaab

import com.nfcaab.porygon.dto.response.EloRatingResponse
import com.nfcaab.porygon.dto.response.GameWinProbabilitiesResponse
import com.nfcaab.porygon.dto.response.PlayWinProbabilityResponse
import com.nfcaab.porygon.dto.response.ProcessedGameResult
import com.nfcaab.porygon.dto.response.SingleGameWinProbabilitiesResponse
import com.nfcaab.porygon.dto.response.SinglePlayWinProbabilityResponse
import com.nfcaab.porygon.dto.response.WinProbabilitiesForAllGamesResponse
import com.nfcaab.porygon.enums.team.TeamSide
import com.nfcaab.porygon.model.Game
import com.nfcaab.porygon.model.PlateAppearance
import com.nfcaab.porygon.model.Team
import com.nfcaab.porygon.repositories.PlateAppearanceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.pow

@Service
class WinProbabilityService(
    private val plateAppearanceRepository: PlateAppearanceRepository,
    private val plateAppearanceService: PlateAppearanceService,
    private val gameStatsService: GameStatsService,
) {
    private val logger = LoggerFactory.getLogger(WinProbabilityService::class.java)

    // ELO parameters
    private val kFactor = 32.0

    /**
     * Calculate win probability for a plate appearance based on game state
     * Simplified version for baseball using basic probabilities
     */
    fun calculateWinProbability(
        game: Game,
        plateAppearance: PlateAppearance,
        homeElo: Double,
        awayElo: Double,
    ): Double {
        try {
            val scoreDiff = plateAppearance.homeScore - plateAppearance.awayScore
            val inningsRemaining = calculateInningsRemaining(plateAppearance.inning, plateAppearance.inningHalf)
            val outs = plateAppearance.outs
            val bases = calculateBasesOccupied(plateAppearance)

            // Calculate ELO difference with time decay
            val eloDiffTime = calculateEloDiffTime(homeElo, awayElo, inningsRemaining)

            // Base win probability from ELO ratings
            val eloWinProb = calculateExpectedScore(homeElo, awayElo)

            // Adjust for score difference (more important in late innings)
            val scoreAdjustment = calculateScoreAdjustment(scoreDiff, inningsRemaining)

            // Adjust for game situation (outs, bases, inning)
            val situationAdjustment = calculateSituationAdjustment(outs, bases, inningsRemaining)

            // Combine adjustments
            val winProbability = (eloWinProb + scoreAdjustment + situationAdjustment).coerceIn(0.0, 1.0)

            // Calculate win probability added
            val winProbabilityAdded = calculateWinProbabilityAdded(game, plateAppearance, winProbability)

            // Set the win probability and change on the plate appearance
            plateAppearance.winProbability = winProbability.toFloat()
            plateAppearance.winProbabilityAdded = winProbabilityAdded.toFloat()

            return winProbability
        } catch (e: Exception) {
            logger.error("Error calculating win probability: ${e.message}", e)
            val defaultProbability = 0.5
            plateAppearance.winProbability = defaultProbability.toFloat()
            plateAppearance.winProbabilityAdded = 0.0F
            return defaultProbability
        }
    }

    /**
     * Calculate innings remaining (0-9, where 9 means bottom 9th, 0 means extra innings)
     */
    private fun calculateInningsRemaining(
        inning: Int,
        inningHalf: Game.InningHalf,
    ): Double {
        // Calculate how many half-innings remain
        val totalHalfInnings = 18.0 // 9 innings * 2 half-innings
        val completedHalfInnings = (inning - 1) * 2.0 + if (inningHalf == Game.InningHalf.TOP) 1.0 else 0.0
        return maxOf(0.0, totalHalfInnings - completedHalfInnings)
    }

    /**
     * Calculate bases occupied as a numeric value (0-7)
     */
    private fun calculateBasesOccupied(plateAppearance: PlateAppearance): Int {
        var bases = 0
        if (plateAppearance.runnerOnFirst != null) bases += 1
        if (plateAppearance.runnerOnSecond != null) bases += 2
        if (plateAppearance.runnerOnThird != null) bases += 4
        return bases
    }

    /**
     * Calculate ELO difference with time decay
     */
    private fun calculateEloDiffTime(
        offenseElo: Double,
        defenseElo: Double,
        inningsRemaining: Double,
    ): Double {
        // Time decay: less important as game progresses
        val timeFactor = inningsRemaining / 18.0 // Normalize to 0-1
        return (offenseElo - defenseElo) * kotlin.math.exp(-2.0 * (1.0 - timeFactor))
    }

    /**
     * Calculate score adjustment based on lead and innings remaining
     */
    private fun calculateScoreAdjustment(
        scoreDiff: Int,
        inningsRemaining: Double,
    ): Double {
        // Late innings: score matters more
        val lateInningFactor = if (inningsRemaining <= 3) 1.5 else 1.0
        // Each run is worth approximately 0.04 win probability (adjusted by inning)
        return (scoreDiff * 0.04 * lateInningFactor).coerceIn(-0.5, 0.5)
    }

    /**
     * Calculate situation adjustment (outs, bases, inning pressure)
     */
    private fun calculateSituationAdjustment(
        outs: Int,
        bases: Int,
        inningsRemaining: Double,
    ): Double {
        // More outs = less likely to score
        val outsFactor = -outs * 0.02
        // More runners on base = more likely to score
        val basesFactor = bases * 0.015
        // Late innings = more pressure
        val pressureFactor = if (inningsRemaining <= 3) 0.05 else 0.0
        return (outsFactor + basesFactor + pressureFactor).coerceIn(-0.2, 0.2)
    }

    /**
     * Calculate win probability added for a plate appearance
     */
    private fun calculateWinProbabilityAdded(
        game: Game,
        plateAppearance: PlateAppearance,
        currentWinProbability: Double,
    ): Double {
        // Get the previous plate appearance for proper WPA calculation
                val previousPlateAppearance =
            try {
                val allPlateAppearances = plateAppearanceService.getAllPlateAppearancesByGameId(plateAppearance.gameId)
                allPlateAppearances.sortedBy { it.id }.findLast { it.id < plateAppearance.id }
            } catch (e: Exception) {
                null
            }

        val winProbabilityAdded =
            if (previousPlateAppearance != null) {
                val currentWinProb = plateAppearance.winProbability.toDouble()
                val previousWinProb = previousPlateAppearance.winProbability.toDouble()

                // For baseball, we need to account for which team is batting
                val currentBattingTeam = if (plateAppearance.inningHalf == Game.InningHalf.TOP) TeamSide.AWAY else TeamSide.HOME
                val previousBattingTeam = if (previousPlateAppearance.inningHalf == Game.InningHalf.TOP) TeamSide.AWAY else TeamSide.HOME

                // If batting team changed (new half-inning), perspective flips
                if (currentBattingTeam != previousBattingTeam) {
                    val previousHomeWinProb =
                        if (previousBattingTeam == TeamSide.HOME) {
                            previousWinProb
                        } else {
                            1.0 - previousWinProb
                        }

                    val currentHomeWinProb =
                        if (currentBattingTeam == TeamSide.HOME) {
                            currentWinProb
                        } else {
                            1.0 - currentWinProb
                        }

                    currentHomeWinProb - previousHomeWinProb
                } else {
                    // Same team batting, direct difference
                    currentWinProb - previousWinProb
                }
            } else {
                // Fallback to simple difference for first plate appearance
                val gameWinProb = game.winProbability?.toDouble() ?: 0.5
                currentWinProbability - gameWinProb
            }
        return winProbabilityAdded
    }

    /**
     * Update ELO ratings after a game
     */
    fun updateEloRatings(
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ) {
        try {
            val homeScore = game.homeScore
            val awayScore = game.awayScore
            val homeWon = homeScore > awayScore

            // Use K-factor from model parameters
            val expectedHome = calculateExpectedScore(homeTeam.currentElo, awayTeam.currentElo)
            val expectedAway = 1.0 - expectedHome

            val actualHome = if (homeWon) 1.0 else 0.0
            val actualAway = 1.0 - actualHome

            val newHomeElo = homeTeam.currentElo + kFactor * (actualHome - expectedHome)
            val newAwayElo = awayTeam.currentElo + kFactor * (actualAway - expectedAway)

            homeTeam.currentElo = newHomeElo
            awayTeam.currentElo = newAwayElo

            logger.info("Updated ELO ratings - ${game.homeTeam}: ${newHomeElo.toInt()}, ${game.awayTeam}: ${newAwayElo.toInt()}")
        } catch (e: Exception) {
            logger.error("Error updating ELO ratings: ${e.message}", e)
        }
    }

    /**
     * Calculate expected score for ELO
     */
    private fun calculateExpectedScore(
        ratingA: Double,
        ratingB: Double,
    ): Double {
        return 1.0 / (1.0 + 10.0.pow((ratingB - ratingA) / 400.0))
    }

    /**
     * Get ELO ratings for all teams
     */
    fun getEloRatings(teams: List<Team>): List<EloRatingResponse> =
        try {
            teams.map { team ->
                EloRatingResponse(
                    teamId = team.id ?: 0,
                    teamName = team.name,
                    currentElo = team.currentElo,
                    overallElo = team.overallElo,
                )
            }.sortedByDescending { it.currentElo }
        } catch (e: Exception) {
            logger.error("Error getting ELO ratings response: ${e.message}", e)
            throw e
        }

    /**
     * Get win probability for each team for all plate appearances in a game
     */
    fun getWinProbabilitiesForGame(
        gameId: Int,
        plateAppearances: List<PlateAppearance>,
    ): GameWinProbabilitiesResponse =
        try {
            val results =
                plateAppearances.sortedBy { it.id }.map { pa ->
                    val homeTeamWinProbability =
                        if (pa.inningHalf == Game.InningHalf.TOP) {
                            1.0 - (pa.winProbability.toDouble())
                        } else {
                            pa.winProbability.toDouble()
                        }
                    val awayTeamWinProbability = 1.0 - homeTeamWinProbability

                    PlayWinProbabilityResponse(
                        playNumber = pa.id,
                        inning = pa.inning,
                        inningHalf = pa.inningHalf.name,
                        homeScore = pa.homeScore,
                        awayScore = pa.awayScore,
                        homeTeamWinProbability = homeTeamWinProbability,
                        awayTeamWinProbability = awayTeamWinProbability,
                    )
                }

            GameWinProbabilitiesResponse(
                gameId = gameId,
                totalPlays = plateAppearances.size,
                plays = results,
            )
        } catch (e: Exception) {
            logger.error("Error getting team win probabilities response: ${e.message}", e)
            throw e
        }

    /**
     * Calculate win probability for all plate appearances in a specific game
     */
    fun calculateWinProbabilitiesForSingleGame(
        gameId: Int,
        game: Game,
        plateAppearances: List<PlateAppearance>,
        homeTeam: Team,
        awayTeam: Team,
        plateAppearanceService: PlateAppearanceService,
    ): SingleGameWinProbabilitiesResponse {
        try {
            val gameStats = gameStatsService.getGameStatsById(gameId)
            val statsMap = gameStats.associateBy { it.team }

            val currentHomeElo = statsMap[game.homeTeam]?.teamElo?.toDouble() ?: homeTeam.currentElo
            val currentAwayElo = statsMap[game.awayTeam]?.teamElo?.toDouble() ?: awayTeam.currentElo

            var previousPlateAppearance: PlateAppearance? = null
            val processedPlateAppearances = mutableListOf<SinglePlayWinProbabilityResponse>()

            val sortedPlateAppearances = plateAppearances.sortedBy { it.id }

            for (pa in sortedPlateAppearances) {
                val homeElo = statsMap[game.homeTeam]?.teamElo?.toDouble() ?: homeTeam.currentElo
                val awayElo = statsMap[game.awayTeam]?.teamElo?.toDouble() ?: awayTeam.currentElo

                val winProbability = calculateWinProbability(game, pa, homeElo, awayElo)

                val winProbabilityAdded =
                    previousPlateAppearance?.let { prev ->
                        val prevWinProb =
                            if (prev.inningHalf == Game.InningHalf.TOP) {
                                1.0 - (prev.winProbability.toDouble())
                            } else {
                                prev.winProbability.toDouble()
                            }
                        val currentWinProb =
                            if (pa.inningHalf == Game.InningHalf.TOP) {
                                1.0 - winProbability
                            } else {
                                winProbability
                            }

                        if (pa.inningHalf != prev.inningHalf) {
                            currentWinProb - prevWinProb
                        } else {
                            currentWinProb - prevWinProb
                        }
                    } ?: 0.0

                // Save plate appearance with updated win probability
                plateAppearanceRepository.save(pa)

                processedPlateAppearances.add(
                    SinglePlayWinProbabilityResponse(
                        playId = pa.id,
                        playNumber = pa.id,
                        inning = pa.inning,
                        inningHalf = pa.inningHalf.name,
                        homeScore = pa.homeScore,
                        awayScore = pa.awayScore,
                        winProbability = winProbability,
                        winProbabilityAdded = winProbabilityAdded,
                        possession = pa.inningHalf.name,
                        possessionTeam = if (pa.inningHalf == Game.InningHalf.TOP) game.awayTeam else game.homeTeam,
                        homeElo = currentHomeElo,
                        awayElo = currentAwayElo,
                    ),
                )

                previousPlateAppearance = pa
            }

            return SingleGameWinProbabilitiesResponse(
                gameId = gameId,
                homeTeam = game.homeTeam,
                awayTeam = game.awayTeam,
                totalPlays = plateAppearances.size,
                processedPlays = processedPlateAppearances.size,
                plays = processedPlateAppearances,
            )
        } catch (e: Exception) {
            logger.error("Error calculating win probability for game response: ${e.message}", e)
            throw e
        }
    }

    /**
     * Calculate win probability for ALL games in the database
     */
    fun calculateWinProbabilitiesForAllGames(
        games: List<Game>,
        plateAppearanceService: PlateAppearanceService,
        teamService: TeamService,
    ): WinProbabilitiesForAllGamesResponse =
        try {
            var totalGamesProcessed = 0
            var totalPlateAppearancesProcessed = 0
            val processedGames = mutableListOf<ProcessedGameResult>()

            games.forEach { game ->
                try {
                    val plateAppearances = plateAppearanceService.getAllPlateAppearancesByGameId(game.id)
                    if (plateAppearances.isNotEmpty()) {
                        val homeTeam = teamService.getTeamByName(game.homeTeam)
                        val awayTeam = teamService.getTeamByName(game.awayTeam)

                        // Use the single game method to calculate win probabilities
                        val singleGameResult =
                            calculateWinProbabilitiesForSingleGame(
                                game.id,
                                game,
                                plateAppearances,
                                homeTeam,
                                awayTeam,
                                plateAppearanceService,
                            )

                        // Add to our results
                        processedGames.add(
                            ProcessedGameResult(
                                gameId = game.id,
                                homeTeam = game.homeTeam,
                                awayTeam = game.awayTeam,
                                playsProcessed = singleGameResult.processedPlays,
                            ),
                        )

                        totalPlateAppearancesProcessed += singleGameResult.processedPlays
                        totalGamesProcessed++
                    }
                } catch (e: Exception) {
                    // Log error but continue with other games
                    logger.error("Error processing game ${game.id}: ${e.message}")
                }
            }

            WinProbabilitiesForAllGamesResponse(
                totalGames = games.size,
                gamesProcessed = totalGamesProcessed,
                totalPlaysProcessed = totalPlateAppearancesProcessed,
                processedGames = processedGames,
            )
        } catch (e: Exception) {
            logger.error("Error calculating win probability for all games response: ${e.message}", e)
            throw e
        }
}

