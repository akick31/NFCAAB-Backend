package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.GameStats
import com.nfcaab.backend.model.LeagueStats
import com.nfcaab.backend.repositories.LeagueStatsRepository
import com.nfcaab.backend.repositories.SeasonStatsRepository
import com.nfcaab.backend.util.Logger
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class LeagueStatsService(
    private val leagueStatsRepository: LeagueStatsRepository,
    private val seasonStatsRepository: SeasonStatsRepository,
) {
    /**
     * Update or create league stats after a game
     */
    fun updateLeagueStatsAfterGame(
        game: Game,
        gameStats: GameStats,
    ) {
        val season = game.season ?: return
        val subdivision = game.subdivision ?: return
        
        var leagueStats = leagueStatsRepository.findBySubdivisionAndSeasonNumber(subdivision, season)
        
        if (leagueStats == null) {
            leagueStats = LeagueStats(
                subdivision = subdivision,
                seasonNumber = season,
            )
        }
        
        // Recalculate from all season stats in this subdivision
        val allSeasonStats = seasonStatsRepository.findBySeasonNumberOrderByTeamAsc(season)
            .filter { it.subdivision == subdivision }
        
        recalculateLeagueStats(leagueStats, allSeasonStats)
        
        leagueStats.lastModifiedTs = ZonedDateTime.now(ZoneId.of("America/New_York"))
            .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        
        leagueStatsRepository.save(leagueStats)
        Logger.info("Updated league stats for $subdivision, season $season")
    }

    /**
     * Recalculate league stats from all season stats
     */
    private fun recalculateLeagueStats(
        leagueStats: LeagueStats,
        allSeasonStats: List<com.nfcaab.backend.model.SeasonStats>,
    ) {
        // Reset all counters
        leagueStats.totalTeams = allSeasonStats.distinctBy { it.team }.size
        leagueStats.totalGames = allSeasonStats.sumOf { it.wins + it.losses } / 2 // Each game counted twice
        
        // Aggregate batting stats
        leagueStats.atBats = allSeasonStats.sumOf { it.atBats }
        leagueStats.runs = allSeasonStats.sumOf { it.runs }
        leagueStats.hits = allSeasonStats.sumOf { it.hits }
        leagueStats.runsBattedIn = allSeasonStats.sumOf { it.runsBattedIn }
        leagueStats.homeRuns = allSeasonStats.sumOf { it.homeRuns }
        leagueStats.triples = allSeasonStats.sumOf { it.triples }
        leagueStats.doubles = allSeasonStats.sumOf { it.doubles }
        leagueStats.singles = allSeasonStats.sumOf { it.singles }
        leagueStats.walks = allSeasonStats.sumOf { it.walks }
        leagueStats.strikeouts = allSeasonStats.sumOf { it.strikeouts }
        leagueStats.steals = allSeasonStats.sumOf { it.steals }
        leagueStats.doublePlays = allSeasonStats.sumOf { it.doublePlays }
        
        // Aggregate pitching stats
        val totalInnings = allSeasonStats.sumOf { parseInningsPitched(it.inningsPitched ?: "0.0") }
        leagueStats.inningsPitched = formatInningsPitched(totalInnings)
        leagueStats.hitsAllowed = allSeasonStats.sumOf { it.hitsAllowed }
        leagueStats.runsAllowed = allSeasonStats.sumOf { it.runsAllowed }
        leagueStats.earnedRunsAllowed = allSeasonStats.sumOf { it.earnedRunsAllowed }
        leagueStats.walksAllowed = allSeasonStats.sumOf { it.walksAllowed }
        leagueStats.strikeoutsThrown = allSeasonStats.sumOf { it.strikeoutsThrown }
        leagueStats.homeRunsAllowed = allSeasonStats.sumOf { it.homeRunsAllowed }
        leagueStats.triplesAllowed = allSeasonStats.sumOf { it.triplesAllowed }
        leagueStats.doublesAllowed = allSeasonStats.sumOf { it.doublesAllowed }
        leagueStats.singlesAllowed = allSeasonStats.sumOf { it.singlesAllowed }
        leagueStats.stealsAllowed = allSeasonStats.sumOf { it.stealsAllowed }
        leagueStats.doublePlaysForced = allSeasonStats.sumOf { it.doublePlaysForced }
        
        // Calculate percentages
        leagueStats.battingAverage = calculateBattingAverage(leagueStats.hits, leagueStats.atBats)
        leagueStats.onBasePercentage = calculateOnBasePercentage(leagueStats.hits, leagueStats.walks, leagueStats.atBats)
        leagueStats.sluggingPercentage = calculateSluggingPercentage(
            leagueStats.singles,
            leagueStats.doubles,
            leagueStats.triples,
            leagueStats.homeRuns,
            leagueStats.atBats,
        )
        leagueStats.era = calculateERA(leagueStats.earnedRunsAllowed, totalInnings)
        leagueStats.whip = calculateWHIP(leagueStats.walksAllowed, leagueStats.hitsAllowed, totalInnings)
        
        // Average response speed
        val responseSpeeds = allSeasonStats.mapNotNull { it.averageResponseSpeed }
        leagueStats.averageResponseSpeed = if (responseSpeeds.isNotEmpty()) responseSpeeds.average() else 0.0
    }

    private fun parseInningsPitched(innings: String): Double {
        return innings.toDoubleOrNull() ?: 0.0
    }

    private fun formatInningsPitched(innings: Double): String {
        return String.format("%.1f", innings)
    }

    private fun calculateBattingAverage(hits: Int, atBats: Int): Double {
        return if (atBats > 0) hits.toDouble() / atBats.toDouble() else 0.0
    }

    private fun calculateOnBasePercentage(hits: Int, walks: Int, atBats: Int): Double {
        val plateAppearances = atBats + walks
        return if (plateAppearances > 0) (hits + walks).toDouble() / plateAppearances.toDouble() else 0.0
    }

    private fun calculateSluggingPercentage(
        singles: Int,
        doubles: Int,
        triples: Int,
        homeRuns: Int,
        atBats: Int,
    ): Double {
        if (atBats == 0) return 0.0
        val totalBases = singles + (doubles * 2) + (triples * 3) + (homeRuns * 4)
        return totalBases.toDouble() / atBats.toDouble()
    }

    private fun calculateERA(earnedRuns: Int, inningsPitched: Double): Double {
        return if (inningsPitched > 0) (earnedRuns * 9.0) / inningsPitched else 0.0
    }

    private fun calculateWHIP(walks: Int, hits: Int, inningsPitched: Double): Double {
        return if (inningsPitched > 0) (walks + hits).toDouble() / inningsPitched else 0.0
    }
}

