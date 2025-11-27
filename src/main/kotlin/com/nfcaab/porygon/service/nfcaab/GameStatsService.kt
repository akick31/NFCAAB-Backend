package com.nfcaab.porygon.service.nfcaab

import com.nfcaab.porygon.model.Game
import com.nfcaab.porygon.model.Game.ActualResult
import com.nfcaab.porygon.model.Game.GameStatus
import com.nfcaab.porygon.model.Game.InningHalf
import com.nfcaab.porygon.model.Game.Scenario
import com.nfcaab.porygon.model.GameStats
import com.nfcaab.porygon.model.PlateAppearance
import com.nfcaab.porygon.repositories.GameRepository
import com.nfcaab.porygon.repositories.GameStatsRepository
import com.nfcaab.porygon.repositories.PlateAppearanceRepository
import com.nfcaab.porygon.repositories.TeamRepository
import com.nfcaab.porygon.util.GameNotFoundException
import com.nfcaab.porygon.util.GameStatsNotFoundException
import com.nfcaab.porygon.util.Logger
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class GameStatsService(
    private val gameStatsRepository: GameStatsRepository,
    private val gameRepository: GameRepository,
    private val plateAppearanceRepository: PlateAppearanceRepository,
    private val teamRepository: TeamRepository,
) {
    /**
     * Create a game stats entry
     * @param game
     */
    fun createGameStats(game: Game): List<GameStats> {
        val homeTeam =
            teamRepository.getTeamByName(game.homeTeam)
                ?: throw Exception("Could not find home team: ${game.homeTeam}")
        val awayTeam =
            teamRepository.getTeamByName(game.awayTeam)
                ?: throw Exception("Could not find away team: ${game.awayTeam}")

        val homeStats =
            GameStats(
                gameId = game.id,
                team = game.homeTeam,
                season = game.season,
                week = game.week,
                subdivision = game.subdivision,
                gameStatus = game.gameStatus,
                gameType = game.gameType,
            )
        gameStatsRepository.save(homeStats) ?: throw Exception("Could not create game stats for home team")

        val awayStats =
            GameStats(
                gameId = game.id,
                team = game.awayTeam,
                season = game.season,
                week = game.week,
                subdivision = game.subdivision,
                gameStatus = game.gameStatus,
                gameType = game.gameType,
            )
        gameStatsRepository.save(awayStats) ?: throw Exception("Could not create game stats for away team")

        return listOf(homeStats, awayStats)
    }

    /**
     * Generate game stats for all games more recent than the given game ID
     */
    fun generateGameStatsForGamesMoreRecentThanGameId(gameId: Int) {
        try {
            val allGames =
                gameRepository.getAllGamesMoreRecentThanGameId(gameId).ifEmpty {
                    throw GameNotFoundException("Could not find any games more recent than game ID $gameId")
                }

            for (game in allGames) {
                Logger.info("Generating game stats for game ${game.id}")
                generateGameStats(game.id)
            }
        } catch (e: Exception) {
            throw Exception("Could not generate game stats for games more recent than game ID $gameId")
        }
    }

    /**
     * Generate game stats for all games
     */
    fun generateAllGameStats() {
        try {
            val allGames =
                gameRepository.getAllGames().ifEmpty {
                    throw GameNotFoundException("Could not find any games")
                }

            for (game in allGames) {
                Logger.info("Generating game stats for game ${game.id}")
                generateGameStats(game.id)
            }
        } catch (e: Exception) {
            throw Exception("Could not generate game stats")
        }
    }

    /**
     * Generate game stats for a game
     */
    fun generateGameStats(gameId: Int) {
        deleteByGameId(gameId)

        val game =
            gameRepository.getGameById(gameId)
                ?: throw Exception("Could not find game with ID $gameId")
        createGameStats(game)

        val allPlateAppearances = plateAppearanceRepository.getAllPlateAppearancesByGameId(gameId)

        updateGameStats(game, allPlateAppearances)
    }

    /**
     * Update the game stats for the current game
     */
    fun updateGameStats(
        game: Game,
        allPlateAppearances: List<PlateAppearance>,
    ): List<GameStats> {
        var homeStats = getGameStatsByIdAndTeam(game.id, game.homeTeam)
        homeStats = updateStats(allPlateAppearances, game.homeTeam, game, homeStats)

        var awayStats = getGameStatsByIdAndTeam(game.id, game.awayTeam)
        awayStats = updateStats(allPlateAppearances, game.awayTeam, game, awayStats)
        return listOf(homeStats, awayStats)
    }

    /**
     * Save game stats entry
     * @param gameStats
     */
    fun saveGameStats(gameStats: GameStats) = gameStatsRepository.save(gameStats)

    /**
     * Get game stats entry by game ID
     * @param gameId
     */
    fun getGameStatsByIdAndTeam(
        gameId: Int,
        team: String,
    ) = gameStatsRepository.getGameStatsByIdAndTeam(gameId, team)
        ?: throw GameStatsNotFoundException("Could not find game stats for game $gameId and team $team")

    /**
     * Get game stats by game id
     */
    fun getGameStatsById(gameId: Int) = gameStatsRepository.findByGameId(gameId)

    /**
     * Delete game stats entry by game ID
     */
    fun deleteByGameId(gameId: Int) = gameStatsRepository.deleteByGameId(gameId)

    /**
     * Calculate the stats for each team based on plate appearances
     */
    private fun updateStats(
        allPlateAppearances: List<PlateAppearance>,
        team: String,
        game: Game,
        stats: GameStats,
    ): GameStats {
        val battingPlateAppearances = allPlateAppearances.filter { pa -> pa.battingTeam == team }
        val pitchingPlateAppearances = allPlateAppearances.filter { pa -> pa.pitchingTeam == team }

        stats.score = if (team == game.homeTeam) game.homeScore else game.awayScore

        // Batting stats
        stats.atBats = calculateAtBats(battingPlateAppearances)
        stats.runs = battingPlateAppearances.sumOf { it.runsScored }
        stats.hits = calculateHits(battingPlateAppearances)
        stats.runsBattedIn = battingPlateAppearances.sumOf { it.runsScored }
        stats.homeRuns = battingPlateAppearances.count { it.actualResult == ActualResult.HOME_RUN }
        stats.triples = battingPlateAppearances.count { it.actualResult == ActualResult.TRIPLE }
        stats.doubles = battingPlateAppearances.count { it.actualResult == ActualResult.DOUBLE }
        stats.singles = battingPlateAppearances.count { it.actualResult == ActualResult.SINGLE }
        stats.walks = battingPlateAppearances.count { it.actualResult == ActualResult.WALK }
        stats.strikeouts = battingPlateAppearances.count { it.actualResult == ActualResult.STRIKEOUT }
        stats.steals = battingPlateAppearances.count { it.result == Scenario.STEAL_SUCCESS }
        stats.doublePlays = battingPlateAppearances.count { it.actualResult == ActualResult.DOUBLE_PLAY }

        // Pitching stats
        stats.inningsPitched = calculateInningsPitched(pitchingPlateAppearances)
        stats.hitsAllowed = calculateHits(pitchingPlateAppearances)
        stats.runsAllowed = pitchingPlateAppearances.sumOf { it.runsScored }
        stats.earnedRunsAllowed = stats.runsAllowed // TODO: Calculate earned runs properly
        stats.walksAllowed = pitchingPlateAppearances.count { it.actualResult == ActualResult.WALK }
        stats.strikeoutsThrown = pitchingPlateAppearances.count { it.actualResult == ActualResult.STRIKEOUT }
        stats.homeRunsAllowed = pitchingPlateAppearances.count { it.actualResult == ActualResult.HOME_RUN }
        stats.triplesAllowed = pitchingPlateAppearances.count { it.actualResult == ActualResult.TRIPLE }
        stats.doublesAllowed = pitchingPlateAppearances.count { it.actualResult == ActualResult.DOUBLE }
        stats.singlesAllowed = pitchingPlateAppearances.count { it.actualResult == ActualResult.SINGLE }
        stats.stealsAllowed = pitchingPlateAppearances.count { it.result == Scenario.STEAL_SUCCESS }
        stats.doublePlaysForced = pitchingPlateAppearances.count { it.actualResult == ActualResult.DOUBLE_PLAY }

        // Calculated stats
        stats.battingAverage = calculateBattingAverage(stats.hits, stats.atBats)
        stats.onBasePercentage = calculateOnBasePercentage(stats.hits, stats.walks, stats.atBats)
        stats.sluggingPercentage = calculateSluggingPercentage(stats.singles, stats.doubles, stats.triples, stats.homeRuns, stats.atBats)
        stats.era = calculateERA(stats.earnedRunsAllowed, stats.inningsPitched)
        stats.whip = calculateWHIP(stats.walksAllowed, stats.hitsAllowed, stats.inningsPitched)

        // Inning scores
        updateInningScores(allPlateAppearances, team, stats)

        // Game control
        stats.largestLead = calculateLargestLead(allPlateAppearances, team)
        stats.largestDeficit = calculateLargestDeficit(allPlateAppearances, team)

        // Response speed
        stats.averageResponseSpeed = calculateAverageResponseSpeed(allPlateAppearances, team)

        stats.gameStatus = game.gameStatus
        stats.lastModifiedTs =
            ZonedDateTime.now(
                ZoneId.of("America/New_York"),
            ).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        saveGameStats(stats)
        return stats
    }

    private fun calculateAtBats(plateAppearances: List<PlateAppearance>): Int {
        return plateAppearances.count { pa ->
            pa.actualResult != ActualResult.WALK &&
                pa.actualResult != ActualResult.SACRIFICE_FLY
        }
    }

    private fun calculateHits(plateAppearances: List<PlateAppearance>): Int {
        return plateAppearances.count { pa ->
            pa.actualResult == ActualResult.SINGLE ||
                pa.actualResult == ActualResult.DOUBLE ||
                pa.actualResult == ActualResult.TRIPLE ||
                pa.actualResult == ActualResult.HOME_RUN
        }
    }

    private fun calculateInningsPitched(plateAppearances: List<PlateAppearance>): Double {
        // Count outs and divide by 3
        val outs = plateAppearances.count { pa -> pa.actualResult?.let { it in listOf(ActualResult.STRIKEOUT, ActualResult.FLYOUT, ActualResult.LEFT_GROUNDOUT, ActualResult.RIGHT_GROUNDOUT) } ?: false }
        return outs.toDouble() / 3.0
    }

    private fun updateInningScores(
        allPlateAppearances: List<PlateAppearance>,
        team: String,
        stats: GameStats,
    ) {
        val teamPlateAppearances = allPlateAppearances.filter { pa -> pa.battingTeam == team }
        val runsByInning = mutableMapOf<Int, Int>()

        teamPlateAppearances.forEach { pa ->
            val inning = pa.inning
            val currentRuns = runsByInning.getOrDefault(inning, 0)
            runsByInning[inning] = currentRuns + pa.runsScored
        }

        stats.i1Score = runsByInning.getOrDefault(1, 0)
        stats.i2Score = runsByInning.getOrDefault(2, 0)
        stats.i3Score = runsByInning.getOrDefault(3, 0)
        stats.i4Score = runsByInning.getOrDefault(4, 0)
        stats.i5Score = runsByInning.getOrDefault(5, 0)
        stats.i6Score = runsByInning.getOrDefault(6, 0)
        stats.i7Score = runsByInning.getOrDefault(7, 0)
        stats.i8Score = runsByInning.getOrDefault(8, 0)
        stats.i9Score = runsByInning.getOrDefault(9, 0)
        stats.i10Score = runsByInning.getOrDefault(10, 0)
        stats.i11Score = runsByInning.getOrDefault(11, 0)
        stats.i12Score = runsByInning.getOrDefault(12, 0)
        stats.i13Score = runsByInning.getOrDefault(13, 0)
        stats.i14Score = runsByInning.getOrDefault(14, 0)
        stats.i15Score = runsByInning.getOrDefault(15, 0)

        // Extra innings (16+)
        stats.extraInningsScore = runsByInning.filter { it.key > 15 }.values.sum()
    }

    private fun calculateLargestLead(allPlateAppearances: List<PlateAppearance>, team: String): Int {
        return allPlateAppearances.maxOfOrNull { pa ->
            if (team == pa.homeTeam) {
                pa.homeScore - pa.awayScore
            } else {
                pa.awayScore - pa.homeScore
            }
        } ?: 0
    }

    private fun calculateLargestDeficit(allPlateAppearances: List<PlateAppearance>, team: String): Int {
        return allPlateAppearances.maxOfOrNull { pa ->
            if (team == pa.homeTeam) {
                pa.awayScore - pa.homeScore
            } else {
                pa.homeScore - pa.awayScore
            }
        } ?: 0
    }

    private fun calculateAverageResponseSpeed(
        allPlateAppearances: List<PlateAppearance>,
        team: String,
    ): Double {
        val speeds = allPlateAppearances
            .filter { pa -> pa.battingTeam == team || pa.pitchingTeam == team }
            .mapNotNull { pa ->
                if (pa.battingTeam == team) {
                    pa.batterResponseSpeed
                } else {
                    pa.pitcherResponseSpeed
                }
            }
            .map { it.toDouble() }

        return if (speeds.isEmpty()) 0.0 else speeds.average()
    }

    private fun calculateBattingAverage(
        hits: Int,
        atBats: Int,
    ): Double {
        if (atBats == 0) {
            return 0.0
        }
        return hits.toDouble() / atBats.toDouble()
    }

    private fun calculateOnBasePercentage(
        hits: Int,
        walks: Int,
        atBats: Int,
    ): Double {
        val plateAppearances = atBats + walks // Simplified
        if (plateAppearances == 0) {
            return 0.0
        }
        return (hits + walks).toDouble() / plateAppearances.toDouble()
    }

    private fun calculateSluggingPercentage(
        singles: Int,
        doubles: Int,
        triples: Int,
        homeRuns: Int,
        atBats: Int,
    ): Double {
        if (atBats == 0) {
            return 0.0
        }
        val totalBases = singles + (doubles * 2) + (triples * 3) + (homeRuns * 4)
        return totalBases.toDouble() / atBats.toDouble()
    }

    private fun calculateERA(
        earnedRuns: Int,
        inningsPitched: Double,
    ): Double {
        if (inningsPitched == 0.0) {
            return 0.0
        }
        return (earnedRuns.toDouble() / inningsPitched) * 9.0
    }

    private fun calculateWHIP(
        walksAllowed: Int,
        hitsAllowed: Int,
        inningsPitched: Double,
    ): Double {
        if (inningsPitched == 0.0) {
            return 0.0
        }
        return (walksAllowed + hitsAllowed).toDouble() / inningsPitched
    }

    /**
     * Get all game stats for a specific team and season
     * @param team Team name
     * @param season Season number
     * @return List of GameStats for the team in the specified season
     */
    fun getAllGameStatsForTeamAndSeason(
        team: String,
        season: Int,
    ): List<GameStats> {
        return gameStatsRepository.findByTeamAndSeason(team, season)
    }
}

