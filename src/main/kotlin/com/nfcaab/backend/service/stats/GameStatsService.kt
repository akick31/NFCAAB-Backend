package com.nfcaab.backend.service.stats

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.model.GameStats
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.repositories.GameStatsRepository
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.repositories.TeamRepository
import com.nfcaab.backend.util.GameNotFoundException
import com.nfcaab.backend.util.GameStatsNotFoundException
import com.nfcaab.backend.util.Logger
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class GameStatsService(
    private val gameStatsRepository: GameStatsRepository,
    private val gameRepository: GameRepository,
    private val atBatRepository: AtBatRepository,
    private val teamRepository: TeamRepository,
    private val seasonStatsService: SeasonStatsService,
    private val conferenceStatsService: ConferenceStatsService,
    private val leagueStatsService: LeagueStatsService,
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

        val allAtBats = atBatRepository.getAllAtBatsByGameId(gameId)

        updateGameStats(game, allAtBats)
    }

    /**
     * Update the game stats for the current game
     */
    fun updateGameStats(
        game: Game,
        allAtBats: List<AtBat>,
    ): List<GameStats> {
        var homeStats = getGameStatsByIdAndTeam(game.id, game.homeTeam)
        homeStats = updateStats(allAtBats, game.homeTeam, game, homeStats)

        var awayStats = getGameStatsByIdAndTeam(game.id, game.awayTeam)
        awayStats = updateStats(allAtBats, game.awayTeam, game, awayStats)
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
        allAtBats: List<AtBat>,
        team: String,
        game: Game,
        stats: GameStats,
    ): GameStats {
        val battingAtBats = allAtBats.filter { pa -> pa.battingTeam == team }
        val pitchingAtBats = allAtBats.filter { pa -> pa.pitchingTeam == team }

        stats.score = if (team == game.homeTeam) game.homeScore else game.awayScore

        // Batting stats
        stats.atBats = calculateAtBats(battingAtBats)
        stats.runs = battingAtBats.sumOf { it.runsScored }
        stats.hits = calculateHits(battingAtBats)
        stats.runsBattedIn = battingAtBats.sumOf { it.runsScored }
        stats.homeRuns = battingAtBats.count { it.actualResult == ActualResult.HOME_RUN }
        stats.triples = battingAtBats.count { it.actualResult == ActualResult.TRIPLE }
        stats.doubles = battingAtBats.count { it.actualResult == ActualResult.DOUBLE }
        stats.singles = battingAtBats.count { it.actualResult == ActualResult.SINGLE }
        stats.walks = battingAtBats.count { it.actualResult == ActualResult.WALK }
        stats.strikeouts = battingAtBats.count { it.actualResult == ActualResult.STRIKEOUT }
        stats.steals = battingAtBats.count { it.result == Scenario.STEAL_SUCCESS }
        stats.doublePlays = battingAtBats.count { it.actualResult == ActualResult.DOUBLE_PLAY }

        // Pitching stats
        stats.inningsPitched = calculateInningsPitched(pitchingAtBats)
        stats.hitsAllowed = calculateHits(pitchingAtBats)
        stats.runsAllowed = pitchingAtBats.sumOf { it.runsScored }
        stats.earnedRunsAllowed = stats.runsAllowed // TODO: Calculate earned runs properly
        stats.walksAllowed = pitchingAtBats.count { it.actualResult == ActualResult.WALK }
        stats.strikeoutsThrown = pitchingAtBats.count { it.actualResult == ActualResult.STRIKEOUT }
        stats.homeRunsAllowed = pitchingAtBats.count { it.actualResult == ActualResult.HOME_RUN }
        stats.triplesAllowed = pitchingAtBats.count { it.actualResult == ActualResult.TRIPLE }
        stats.doublesAllowed = pitchingAtBats.count { it.actualResult == ActualResult.DOUBLE }
        stats.singlesAllowed = pitchingAtBats.count { it.actualResult == ActualResult.SINGLE }
        stats.stealsAllowed = pitchingAtBats.count { it.result == Scenario.STEAL_SUCCESS }
        stats.doublePlaysForced = pitchingAtBats.count { it.actualResult == ActualResult.DOUBLE_PLAY }

        // Calculated stats
        stats.battingAverage = calculateBattingAverage(stats.hits, stats.atBats)
        stats.onBasePercentage = calculateOnBasePercentage(stats.hits, stats.walks, stats.atBats)
        stats.sluggingPercentage = calculateSluggingPercentage(stats.singles, stats.doubles, stats.triples, stats.homeRuns, stats.atBats)
        stats.era = calculateERA(stats.earnedRunsAllowed, stats.inningsPitched)
        stats.whip = calculateWHIP(stats.walksAllowed, stats.hitsAllowed, stats.inningsPitched)

        // Inning scores
        updateInningScores(allAtBats, team, stats)

        // Game control
        stats.largestLead = calculateLargestLead(allAtBats, team)
        stats.largestDeficit = calculateLargestDeficit(allAtBats, team)

        // Response speed
        stats.averageResponseSpeed = calculateAverageResponseSpeed(allAtBats, team)

        stats.gameStatus = game.gameStatus
        stats.lastModifiedTs =
            ZonedDateTime.now(
                ZoneId.of("America/New_York"),
            ).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        saveGameStats(stats)
        return stats
    }

    private fun calculateAtBats(atBats: List<AtBat>): Int {
        return atBats.count { pa ->
            pa.actualResult != ActualResult.WALK &&
                pa.actualResult != ActualResult.SACRIFICE_FLY
        }
    }

    private fun calculateHits(atBats: List<AtBat>): Int {
        return atBats.count { pa ->
            pa.actualResult == ActualResult.SINGLE ||
                pa.actualResult == ActualResult.DOUBLE ||
                pa.actualResult == ActualResult.TRIPLE ||
                pa.actualResult == ActualResult.HOME_RUN
        }
    }

    private fun calculateInningsPitched(atBats: List<AtBat>): Double {
        // Count outs and divide by 3
        val outs = atBats.count { pa -> pa.actualResult?.let { it in listOf(ActualResult.STRIKEOUT, ActualResult.FLYOUT, ActualResult.GROUNDOUT) } ?: false }
        return outs.toDouble() / 3.0
    }

    private fun updateInningScores(
        allAtBats: List<AtBat>,
        team: String,
        stats: GameStats,
    ) {
        val teamAtBats = allAtBats.filter { pa -> pa.battingTeam == team }
        val runsByInning = mutableMapOf<Int, Int>()

        teamAtBats.forEach { pa ->
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

    private fun calculateLargestLead(allAtBats: List<AtBat>, team: String): Int {
        return allAtBats.maxOfOrNull { pa ->
            if (team == pa.homeTeam) {
                pa.homeScore - pa.awayScore
            } else {
                pa.awayScore - pa.homeScore
            }
        } ?: 0
    }

    private fun calculateLargestDeficit(allAtBats: List<AtBat>, team: String): Int {
        return allAtBats.maxOfOrNull { pa ->
            if (team == pa.homeTeam) {
                pa.awayScore - pa.homeScore
            } else {
                pa.homeScore - pa.awayScore
            }
        } ?: 0
    }

    private fun calculateAverageResponseSpeed(
        allAtBats: List<AtBat>,
        team: String,
    ): Double {
        val speeds = allAtBats
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
        val atBats = atBats + walks // Simplified
        if (atBats == 0) {
            return 0.0
        }
        return (hits + walks).toDouble() / atBats.toDouble()
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

    /**
     * Aggregate stats after a game ends: Game -> Season -> Conference -> League
     * @param game The finished game
     */
    fun aggregateStatsAfterGame(game: Game) {
        if (game.season == null) return
        
        val homeGameStats = getGameStatsByIdAndTeam(game.id, game.homeTeam)
        val awayGameStats = getGameStatsByIdAndTeam(game.id, game.awayTeam)
        
        // Update season stats for both teams
        seasonStatsService.updateSeasonStatsAfterGame(game, homeGameStats)
        seasonStatsService.updateSeasonStatsAfterGame(game, awayGameStats)
        
        // Update conference stats if teams have conferences
        val homeTeam = teamRepository.getTeamByName(game.homeTeam)
        val awayTeam = teamRepository.getTeamByName(game.awayTeam)
        
        homeTeam?.conference?.let { conference ->
            conferenceStatsService.updateConferenceStatsAfterGame(game, homeGameStats, conference)
        }
        awayTeam?.conference?.let { conference ->
            conferenceStatsService.updateConferenceStatsAfterGame(game, awayGameStats, conference)
        }
        
        // Update league stats
        leagueStatsService.updateLeagueStatsAfterGame(game, homeGameStats)
        leagueStatsService.updateLeagueStatsAfterGame(game, awayGameStats)
    }
}

