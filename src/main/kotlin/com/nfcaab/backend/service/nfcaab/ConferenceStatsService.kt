package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.enums.team.Conference
import com.nfcaab.backend.enums.team.Subdivision
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.GameStats
import com.nfcaab.backend.model.ConferenceStats
import com.nfcaab.backend.repositories.ConferenceStatsRepository
import com.nfcaab.backend.repositories.SeasonStatsRepository
import com.nfcaab.backend.util.Logger
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class ConferenceStatsService(
    private val conferenceStatsRepository: ConferenceStatsRepository,
    private val seasonStatsRepository: SeasonStatsRepository,
) {
    /**
     * Update or create conference stats after a game
     */
    fun updateConferenceStatsAfterGame(
        game: Game,
        gameStats: GameStats,
        conference: Conference,
    ) {
        val season = game.season ?: return
        val subdivision = game.subdivision ?: return
        
        var conferenceStats = conferenceStatsRepository.findBySubdivisionAndConferenceAndSeasonNumber(
            subdivision,
            conference,
            season,
        )
        
        if (conferenceStats == null) {
            conferenceStats = ConferenceStats(
                subdivision = subdivision,
                conference = conference,
                seasonNumber = season,
            )
        }
        
        // Recalculate from all season stats in this conference
        val allSeasonStats = seasonStatsRepository.findBySeasonNumberOrderByTeamAsc(season)
            .filter { it.conference == conference && it.subdivision == subdivision }
        
        recalculateConferenceStats(conferenceStats, allSeasonStats)
        
        conferenceStats.lastModifiedTs = ZonedDateTime.now(ZoneId.of("America/New_York"))
            .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        
        conferenceStatsRepository.save(conferenceStats)
        Logger.info("Updated conference stats for $conference, season $season")
    }

    /**
     * Recalculate conference stats from all season stats
     */
    private fun recalculateConferenceStats(
        conferenceStats: ConferenceStats,
        allSeasonStats: List<com.nfcaab.backend.model.SeasonStats>,
    ) {
        // Reset all counters
        conferenceStats.totalTeams = allSeasonStats.size
        conferenceStats.totalGames = allSeasonStats.sumOf { it.wins + it.losses } / 2 // Each game counted twice
        
        // Aggregate batting stats
        conferenceStats.atBats = allSeasonStats.sumOf { it.atBats }
        conferenceStats.runs = allSeasonStats.sumOf { it.runs }
        conferenceStats.hits = allSeasonStats.sumOf { it.hits }
        conferenceStats.runsBattedIn = allSeasonStats.sumOf { it.runsBattedIn }
        conferenceStats.homeRuns = allSeasonStats.sumOf { it.homeRuns }
        conferenceStats.triples = allSeasonStats.sumOf { it.triples }
        conferenceStats.doubles = allSeasonStats.sumOf { it.doubles }
        conferenceStats.singles = allSeasonStats.sumOf { it.singles }
        conferenceStats.walks = allSeasonStats.sumOf { it.walks }
        conferenceStats.strikeouts = allSeasonStats.sumOf { it.strikeouts }
        conferenceStats.steals = allSeasonStats.sumOf { it.steals }
        conferenceStats.doublePlays = allSeasonStats.sumOf { it.doublePlays }
        
        // Aggregate pitching stats
        val totalInnings = allSeasonStats.sumOf { parseInningsPitched(it.inningsPitched ?: "0.0") }
        conferenceStats.inningsPitched = formatInningsPitched(totalInnings)
        conferenceStats.hitsAllowed = allSeasonStats.sumOf { it.hitsAllowed }
        conferenceStats.runsAllowed = allSeasonStats.sumOf { it.runsAllowed }
        conferenceStats.earnedRunsAllowed = allSeasonStats.sumOf { it.earnedRunsAllowed }
        conferenceStats.walksAllowed = allSeasonStats.sumOf { it.walksAllowed }
        conferenceStats.strikeoutsThrown = allSeasonStats.sumOf { it.strikeoutsThrown }
        conferenceStats.homeRunsAllowed = allSeasonStats.sumOf { it.homeRunsAllowed }
        conferenceStats.triplesAllowed = allSeasonStats.sumOf { it.triplesAllowed }
        conferenceStats.doublesAllowed = allSeasonStats.sumOf { it.doublesAllowed }
        conferenceStats.singlesAllowed = allSeasonStats.sumOf { it.singlesAllowed }
        conferenceStats.stealsAllowed = allSeasonStats.sumOf { it.stealsAllowed }
        conferenceStats.doublePlaysForced = allSeasonStats.sumOf { it.doublePlaysForced }
        
        // Opponent stats
        conferenceStats.opponentAtBats = allSeasonStats.sumOf { it.opponentAtBats }
        conferenceStats.opponentRuns = allSeasonStats.sumOf { it.opponentRuns }
        conferenceStats.opponentHits = allSeasonStats.sumOf { it.opponentHits }
        conferenceStats.opponentRunsBattedIn = allSeasonStats.sumOf { it.opponentRunsBattedIn }
        conferenceStats.opponentHomeRuns = allSeasonStats.sumOf { it.opponentHomeRuns }
        conferenceStats.opponentTriples = allSeasonStats.sumOf { it.opponentTriples }
        conferenceStats.opponentDoubles = allSeasonStats.sumOf { it.opponentDoubles }
        conferenceStats.opponentSingles = allSeasonStats.sumOf { it.opponentSingles }
        conferenceStats.opponentWalks = allSeasonStats.sumOf { it.opponentWalks }
        conferenceStats.opponentStrikeouts = allSeasonStats.sumOf { it.opponentStrikeouts }
        conferenceStats.opponentSteals = allSeasonStats.sumOf { it.opponentSteals }
        conferenceStats.opponentDoublePlays = allSeasonStats.sumOf { it.opponentDoublePlays }
        val totalOppInnings = allSeasonStats.sumOf { parseInningsPitched(it.opponentInningsPitched ?: "0.0") }
        conferenceStats.opponentInningsPitched = formatInningsPitched(totalOppInnings)
        conferenceStats.opponentHitsAllowed = allSeasonStats.sumOf { it.opponentHitsAllowed }
        conferenceStats.opponentRunsAllowed = allSeasonStats.sumOf { it.opponentRunsAllowed }
        conferenceStats.opponentEarnedRunsAllowed = allSeasonStats.sumOf { it.opponentEarnedRunsAllowed }
        conferenceStats.opponentWalksAllowed = allSeasonStats.sumOf { it.opponentWalksAllowed }
        conferenceStats.opponentStrikeoutsThrown = allSeasonStats.sumOf { it.opponentStrikeoutsThrown }
        conferenceStats.opponentHomeRunsAllowed = allSeasonStats.sumOf { it.opponentHomeRunsAllowed }
        conferenceStats.opponentTriplesAllowed = allSeasonStats.sumOf { it.opponentTriplesAllowed }
        conferenceStats.opponentDoublesAllowed = allSeasonStats.sumOf { it.opponentDoublesAllowed }
        conferenceStats.opponentSinglesAllowed = allSeasonStats.sumOf { it.opponentSinglesAllowed }
        conferenceStats.opponentStealsAllowed = allSeasonStats.sumOf { it.opponentStealsAllowed }
        conferenceStats.opponentDoublePlaysForced = allSeasonStats.sumOf { it.opponentDoublePlaysForced }
        
        // Calculate percentages
        conferenceStats.battingAverage = calculateBattingAverage(conferenceStats.hits, conferenceStats.atBats)
        conferenceStats.onBasePercentage = calculateOnBasePercentage(conferenceStats.hits, conferenceStats.walks, conferenceStats.atBats)
        conferenceStats.sluggingPercentage = calculateSluggingPercentage(
            conferenceStats.singles,
            conferenceStats.doubles,
            conferenceStats.triples,
            conferenceStats.homeRuns,
            conferenceStats.atBats,
        )
        conferenceStats.era = calculateERA(conferenceStats.earnedRunsAllowed, totalInnings)
        conferenceStats.whip = calculateWHIP(conferenceStats.walksAllowed, conferenceStats.hitsAllowed, totalInnings)
        
        conferenceStats.opponentBattingAverage = calculateBattingAverage(conferenceStats.opponentHits, conferenceStats.opponentAtBats)
        conferenceStats.opponentOnBasePercentage = calculateOnBasePercentage(conferenceStats.opponentHits, conferenceStats.opponentWalks, conferenceStats.opponentAtBats)
        conferenceStats.opponentSluggingPercentage = calculateSluggingPercentage(
            conferenceStats.opponentSingles,
            conferenceStats.opponentDoubles,
            conferenceStats.opponentTriples,
            conferenceStats.opponentHomeRuns,
            conferenceStats.opponentAtBats,
        )
        conferenceStats.opponentEra = calculateERA(conferenceStats.opponentEarnedRunsAllowed, totalOppInnings)
        conferenceStats.opponentWhip = calculateWHIP(conferenceStats.opponentWalksAllowed, conferenceStats.opponentHitsAllowed, totalOppInnings)
        
        // Game control
        conferenceStats.largestLead = allSeasonStats.maxOfOrNull { it.largestLead } ?: 0
        conferenceStats.largestDeficit = allSeasonStats.maxOfOrNull { it.largestDeficit } ?: 0
        
        // Average response speed
        val responseSpeeds = allSeasonStats.mapNotNull { it.averageResponseSpeed }
        conferenceStats.averageResponseSpeed = if (responseSpeeds.isNotEmpty()) responseSpeeds.average() else null
    }

    private fun parseInningsPitched(innings: String): Double {
        return innings.toDoubleOrNull() ?: 0.0
    }

    private fun formatInningsPitched(innings: Double): String {
        return String.format("%.1f", innings)
    }

    private fun calculateBattingAverage(hits: Int, atBats: Int): Double? {
        return if (atBats > 0) hits.toDouble() / atBats.toDouble() else null
    }

    private fun calculateOnBasePercentage(hits: Int, walks: Int, atBats: Int): Double? {
        val plateAppearances = atBats + walks
        return if (plateAppearances > 0) (hits + walks).toDouble() / plateAppearances.toDouble() else null
    }

    private fun calculateSluggingPercentage(
        singles: Int,
        doubles: Int,
        triples: Int,
        homeRuns: Int,
        atBats: Int,
    ): Double? {
        if (atBats == 0) return null
        val totalBases = singles + (doubles * 2) + (triples * 3) + (homeRuns * 4)
        return totalBases.toDouble() / atBats.toDouble()
    }

    private fun calculateERA(earnedRuns: Int, inningsPitched: Double): Double? {
        return if (inningsPitched > 0) (earnedRuns * 9.0) / inningsPitched else null
    }

    private fun calculateWHIP(walks: Int, hits: Int, inningsPitched: Double): Double? {
        return if (inningsPitched > 0) (walks + hits).toDouble() / inningsPitched else null
    }
}

