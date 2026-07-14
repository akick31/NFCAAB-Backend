package com.nfcaab.backend.service.elo

import com.nfcaab.backend.dto.response.EloRatingResponse
import com.nfcaab.backend.dto.response.GameWinProbabilitiesResponse
import com.nfcaab.backend.dto.response.PlayWinProbabilityResponse
import com.nfcaab.backend.dto.response.ProcessedGameResult
import com.nfcaab.backend.dto.response.SingleGameWinProbabilitiesResponse
import com.nfcaab.backend.dto.response.SinglePlayWinProbabilityResponse
import com.nfcaab.backend.dto.response.WinProbabilitiesForAllGamesResponse
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.Team
import com.nfcaab.backend.repositories.AtBatRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.pow
import com.nfcaab.backend.service.team.TeamService
import com.nfcaab.backend.service.atbat.AtBatService

@Service
class WinProbabilityService(
    private val atBatRepository: AtBatRepository,
    private val atBatService: AtBatService,
) {
    private val logger = LoggerFactory.getLogger(WinProbabilityService::class.java)

    private val kFactor = 32.0

    fun calculateWinProbability(
        game: Game,
        atBat: AtBat,
        homeElo: Double,
        awayElo: Double,
    ): Double {
        val battingTeamIsHome = atBat.inningHalf != Game.InningHalf.TOP
        val battingElo = if (battingTeamIsHome) homeElo else awayElo
        val defendingElo = if (battingTeamIsHome) awayElo else homeElo
        val scoreDiff = if (battingTeamIsHome) atBat.homeScore - atBat.awayScore else atBat.awayScore - atBat.homeScore
        val inningsRemaining = calculateInningsRemaining(atBat.inning, atBat.inningHalf)
        val outs = atBat.outs
        val bases = calculateBasesOccupied(atBat)

        val eloDiffTime = calculateEloDiffTime(battingElo, defendingElo, inningsRemaining)
        val eloWinProb = 1.0 / (1.0 + 10.0.pow(-eloDiffTime / 400.0))

        val scoreAdjustment = calculateScoreAdjustment(scoreDiff, inningsRemaining)

        val situationAdjustment = calculateSituationAdjustment(outs, bases, inningsRemaining)

        val winProbability = (eloWinProb + scoreAdjustment + situationAdjustment).coerceIn(0.0, 1.0)

        val winProbabilityAdded = calculateWinProbabilityAdded(atBat, winProbability)

        atBat.winProbability = winProbability.toFloat()
        atBat.winProbabilityAdded = winProbabilityAdded.toFloat()

        return winProbability
    }

    private fun calculateInningsRemaining(
        inning: Int,
        inningHalf: Game.InningHalf,
    ): Double {
        val totalHalfInnings = 18.0
        val completedHalfInnings = (inning - 1) * 2.0 + if (inningHalf == Game.InningHalf.BOTTOM) 1.0 else 0.0
        return maxOf(0.0, totalHalfInnings - completedHalfInnings)
    }

    private fun calculateBasesOccupied(atBat: AtBat): Int {
        var bases = 0
        if (atBat.runnerOnFirst != null) bases += 1
        if (atBat.runnerOnSecond != null) bases += 2
        if (atBat.runnerOnThird != null) bases += 4
        return bases
    }

    private fun calculateEloDiffTime(
        offenseElo: Double,
        defenseElo: Double,
        inningsRemaining: Double,
    ): Double {
        val timeFactor = inningsRemaining / 18.0
        return (offenseElo - defenseElo) * kotlin.math.exp(-2.0 * (1.0 - timeFactor))
    }

    private fun calculateScoreAdjustment(
        scoreDiff: Int,
        inningsRemaining: Double,
    ): Double {
        val lateInningFactor = if (inningsRemaining <= 3) 1.5 else 1.0
        return (scoreDiff * 0.04 * lateInningFactor).coerceIn(-0.5, 0.5)
    }

    private fun calculateSituationAdjustment(
        outs: Int,
        bases: Int,
        inningsRemaining: Double,
    ): Double {
        val outsFactor = -outs * 0.02
        val basesFactor = bases * 0.015
        val pressureFactor = if (inningsRemaining <= 3) 0.05 else 0.0
        return (outsFactor + basesFactor + pressureFactor).coerceIn(-0.2, 0.2)
    }

    private fun calculateWinProbabilityAdded(
        atBat: AtBat,
        currentWinProbability: Double,
    ): Double {
        val allAtBats = atBatService.getAllAtBatsByGameId(atBat.gameId)
        val previousAtBat = allAtBats.sortedBy { it.id }.findLast { it.id < atBat.id } ?: return currentWinProbability - 0.5

        val previousHomeWinProb =
            if (previousAtBat.inningHalf == Game.InningHalf.TOP) 1.0 - previousAtBat.winProbability.toDouble() else previousAtBat.winProbability.toDouble()
        val currentHomeWinProb = if (atBat.inningHalf == Game.InningHalf.TOP) 1.0 - currentWinProbability else currentWinProbability

        return currentHomeWinProb - previousHomeWinProb
    }

    fun updateEloRatings(
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ) {
        val homeScore = game.homeScore
        val awayScore = game.awayScore
        val homeWon = homeScore > awayScore

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

    private fun calculateExpectedScore(
        ratingA: Double,
        ratingB: Double,
    ): Double {
        return 1.0 / (1.0 + 10.0.pow((ratingB - ratingA) / 400.0))
    }

    fun getEloRatings(teams: List<Team>): List<EloRatingResponse> =
        teams.map { team ->
            EloRatingResponse(
                teamId = team.id ?: 0,
                teamName = team.name,
                currentElo = team.currentElo,
                overallElo = team.overallElo,
            )
        }.sortedByDescending { it.currentElo }

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

    fun calculateWinProbabilitiesForSingleGame(
        gameId: Int,
        game: Game,
        atBats: List<AtBat>,
        homeTeam: Team,
        awayTeam: Team,
        atBatService: AtBatService,
    ): SingleGameWinProbabilitiesResponse {
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
                    val prevWinProb = if (prev.inningHalf == Game.InningHalf.TOP) 1.0 - prev.winProbability.toDouble() else prev.winProbability.toDouble()
                    val currentWinProb = if (pa.inningHalf == Game.InningHalf.TOP) 1.0 - winProbability else winProbability
                    currentWinProb - prevWinProb
                } ?: 0.0

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

    fun calculateWinProbabilitiesForAllGames(
        games: List<Game>,
        atBatService: AtBatService,
        teamService: TeamService,
    ): WinProbabilitiesForAllGamesResponse {
        var totalGamesProcessed = 0
        var totalAtBatsProcessed = 0
        val processedGames = mutableListOf<ProcessedGameResult>()

        games.forEach { game ->
            try {
                val atBats = atBatService.getAllAtBatsByGameId(game.id)
                if (atBats.isNotEmpty()) {
                    val homeTeam = teamService.getTeamByName(game.homeTeam)
                    val awayTeam = teamService.getTeamByName(game.awayTeam)

                    val singleGameResult =
                        calculateWinProbabilitiesForSingleGame(
                            game.id,
                            game,
                            atBats,
                            homeTeam,
                            awayTeam,
                            atBatService,
                        )

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
