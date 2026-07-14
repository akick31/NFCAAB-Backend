package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.dto.response.EloRatingResponse
import com.nfcaab.backend.dto.response.GameWinProbabilitiesResponse
import com.nfcaab.backend.dto.response.PlayWinProbabilityResponse
import com.nfcaab.backend.dto.response.ProcessedGameResult
import com.nfcaab.backend.dto.response.SingleGameWinProbabilitiesResponse
import com.nfcaab.backend.dto.response.SinglePlayWinProbabilityResponse
import com.nfcaab.backend.dto.response.WinProbabilitiesForAllGamesResponse
import com.nfcaab.backend.enums.team.TeamSide
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.Team
import com.nfcaab.backend.repositories.AtBatRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.pow

@Service
class WinProbabilityService(
    private val atBatRepository: AtBatRepository,
    private val atBatService: AtBatService,
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
        atBat: AtBat,
        homeElo: Double,
        awayElo: Double,
    ): Double {
        val scoreDiff = atBat.homeScore - atBat.awayScore
        val inningsRemaining = calculateInningsRemaining(atBat.inning, atBat.inningHalf)
        val outs = atBat.outs
        val bases = calculateBasesOccupied(atBat)

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
        val winProbabilityAdded = calculateWinProbabilityAdded(game, atBat, winProbability)

        // Set the win probability and change on the plate appearance
        atBat.winProbability = winProbability.toFloat()
        atBat.winProbabilityAdded = winProbabilityAdded.toFloat()

        return winProbability
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
    private fun calculateBasesOccupied(atBat: AtBat): Int {
        var bases = 0
        if (atBat.runnerOnFirst != null) bases += 1
        if (atBat.runnerOnSecond != null) bases += 2
        if (atBat.runnerOnThird != null) bases += 4
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
        atBat: AtBat,
        currentWinProbability: Double,
    ): Double {
        // Get the previous plate appearance for proper WPA calculation
        val allAtBats = atBatService.getAllAtBatsByGameId(atBat.gameId)
        val previousAtBat = allAtBats.sortedBy { it.id }.findLast { it.id < atBat.id }

        val winProbabilityAdded =
            if (previousAtBat != null) {
                val currentWinProb = atBat.winProbability.toDouble()
                val previousWinProb = previousAtBat.winProbability.toDouble()

                // For baseball, we need to account for which team is batting
                val currentBattingTeam = if (atBat.inningHalf == Game.InningHalf.TOP) TeamSide.AWAY else TeamSide.HOME
                val previousBattingTeam = if (previousAtBat.inningHalf == Game.InningHalf.TOP) TeamSide.AWAY else TeamSide.HOME

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
                // Fallback to simple difference for first plate appearance (start at 0.5)
                currentWinProbability - 0.5
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
        teams.map { team ->
            EloRatingResponse(
                teamId = team.id ?: 0,
                teamName = team.name,
                currentElo = team.currentElo,
                overallElo = team.overallElo,
            )
        }.sortedByDescending { it.currentElo }

    /**
     * Get win probability for each team for all plate appearances in a game
     */
    fun getWinProbabilitiesForGame(
        gameId: Int,
        atBats: List<AtBat>,
    ): GameWinProbabilitiesResponse {
        val results =
            atBats.sortedBy { it.id }.map { pa ->
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

        return GameWinProbabilitiesResponse(
            gameId = gameId,
            totalPlays = atBats.size,
            plays = results,
        )
    }

    /**
     * Calculate win probability for all plate appearances in a specific game
     */
    fun calculateWinProbabilitiesForSingleGame(
        gameId: Int,
        game: Game,
        atBats: List<AtBat>,
        homeTeam: Team,
        awayTeam: Team,
        atBatService: AtBatService,
    ): SingleGameWinProbabilitiesResponse {
        val gameStats = gameStatsService.getGameStatsById(gameId)
        val statsMap = gameStats.associateBy { it.team }

        val currentHomeElo = homeTeam.currentElo
        val currentAwayElo = awayTeam.currentElo

        var previousAtBat: AtBat? = null
        val processedAtBats = mutableListOf<SinglePlayWinProbabilityResponse>()

        val sortedAtBats = atBats.sortedBy { it.id }

        for (pa in sortedAtBats) {
            val homeElo = homeTeam.currentElo
            val awayElo = awayTeam.currentElo

            val winProbability = calculateWinProbability(game, pa, homeElo, awayElo)

            val winProbabilityAdded =
                previousAtBat?.let { prev ->
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
            atBatRepository.save(pa)

            processedAtBats.add(
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

            previousAtBat = pa
        }

        return SingleGameWinProbabilitiesResponse(
            gameId = gameId,
            homeTeam = game.homeTeam,
            awayTeam = game.awayTeam,
            totalPlays = atBats.size,
            processedPlays = processedAtBats.size,
            plays = processedAtBats,
        )
    }

    /**
     * Calculate win probability for ALL games in the database
     */
    fun calculateWinProbabilitiesForAllGames(
        games: List<Game>,
        atBatService: AtBatService,
        teamService: TeamService,
    ): WinProbabilitiesForAllGamesResponse {
        var totalGamesProcessed = 0
        var totalAtBatsProcessed = 0
        val processedGames = mutableListOf<ProcessedGameResult>()

        // Each game is isolated: one bad game shouldn't stop the rest of the batch from being processed.
        games.forEach { game ->
            try {
                val atBats = atBatService.getAllAtBatsByGameId(game.id)
                if (atBats.isNotEmpty()) {
                    val homeTeam = teamService.getTeamByName(game.homeTeam)
                    val awayTeam = teamService.getTeamByName(game.awayTeam)

                    // Use the single game method to calculate win probabilities
                    val singleGameResult =
                        calculateWinProbabilitiesForSingleGame(
                            game.id,
                            game,
                            atBats,
                            homeTeam,
                            awayTeam,
                            atBatService,
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

                    totalAtBatsProcessed += singleGameResult.processedPlays
                    totalGamesProcessed++
                }
            } catch (e: Exception) {
                logger.error("Error processing game ${game.id}, skipping it and continuing the batch: ${e.message}", e)
            }
        }

        return WinProbabilitiesForAllGamesResponse(
            totalGames = games.size,
            gamesProcessed = totalGamesProcessed,
            totalPlaysProcessed = totalAtBatsProcessed,
            processedGames = processedGames,
        )
    }
}
