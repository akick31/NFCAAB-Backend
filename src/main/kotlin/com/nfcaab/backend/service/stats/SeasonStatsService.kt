package com.nfcaab.backend.service.stats

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.GameStats
import com.nfcaab.backend.model.SeasonStats
import com.nfcaab.backend.repositories.GameStatsRepository
import com.nfcaab.backend.repositories.SeasonStatsRepository
import com.nfcaab.backend.repositories.TeamRepository
import com.nfcaab.backend.util.Logger
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class SeasonStatsService(
    private val seasonStatsRepository: SeasonStatsRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val teamRepository: TeamRepository,
) {
    /**
     * Update or create season stats for a team after a game
     */
    fun updateSeasonStatsAfterGame(game: Game, gameStats: GameStats) {
        val season = game.season ?: return
        val team = gameStats.team
        
        val teamName = team ?: return
        var seasonStats = seasonStatsRepository.findByTeamAndSeasonNumber(teamName, season)
        
        if (seasonStats == null) {
            val teamData = teamRepository.getTeamByName(teamName)
                ?: throw Exception("Team not found: $teamName")
            
            seasonStats = SeasonStats(
                team = teamName,
                seasonNumber = season,
                subdivision = game.subdivision ?: throw Exception("Game subdivision is null"),
                conference = teamData.conference,
            )
        }
        
        // Recalculate all season stats from all game stats for this team
        val allGameStats = gameStatsRepository.findByTeamAndSeason(teamName, season)
        recalculateSeasonStats(seasonStats, allGameStats)
        
        seasonStats.lastModifiedTs = ZonedDateTime.now(ZoneId.of("America/New_York"))
            .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        
        seasonStatsRepository.save(seasonStats)
        Logger.info("Updated season stats for team $teamName, season $season")
    }

    /**
     * Recalculate season stats from all game stats
     */
    private fun recalculateSeasonStats(
        seasonStats: SeasonStats,
        allGameStats: List<GameStats>,
    ) {
        // Reset counters
        seasonStats.wins = 0
        seasonStats.losses = 0
        seasonStats.atBats = 0
        seasonStats.runs = 0
        seasonStats.hits = 0
        seasonStats.runsBattedIn = 0
        seasonStats.homeRuns = 0
        seasonStats.triples = 0
        seasonStats.doubles = 0
        seasonStats.singles = 0
        seasonStats.walks = 0
        seasonStats.strikeouts = 0
        seasonStats.steals = 0
        seasonStats.doublePlays = 0
        seasonStats.inningsPitched = "0.0"
        seasonStats.hitsAllowed = 0
        seasonStats.runsAllowed = 0
        seasonStats.earnedRunsAllowed = 0
        seasonStats.walksAllowed = 0
        seasonStats.strikeoutsThrown = 0
        seasonStats.homeRunsAllowed = 0
        seasonStats.triplesAllowed = 0
        seasonStats.doublesAllowed = 0
        seasonStats.singlesAllowed = 0
        seasonStats.stealsAllowed = 0
        seasonStats.doublePlaysForced = 0
        seasonStats.largestLead = 0
        seasonStats.largestDeficit = 0
        
        // Opponent stats
        seasonStats.opponentAtBats = 0
        seasonStats.opponentRuns = 0
        seasonStats.opponentHits = 0
        seasonStats.opponentRunsBattedIn = 0
        seasonStats.opponentHomeRuns = 0
        seasonStats.opponentTriples = 0
        seasonStats.opponentDoubles = 0
        seasonStats.opponentSingles = 0
        seasonStats.opponentWalks = 0
        seasonStats.opponentStrikeouts = 0
        seasonStats.opponentSteals = 0
        seasonStats.opponentDoublePlays = 0
        seasonStats.opponentInningsPitched = "0.0"
        seasonStats.opponentHitsAllowed = 0
        seasonStats.opponentRunsAllowed = 0
        seasonStats.opponentEarnedRunsAllowed = 0
        seasonStats.opponentWalksAllowed = 0
        seasonStats.opponentStrikeoutsThrown = 0
        seasonStats.opponentHomeRunsAllowed = 0
        seasonStats.opponentTriplesAllowed = 0
        seasonStats.opponentDoublesAllowed = 0
        seasonStats.opponentSinglesAllowed = 0
        seasonStats.opponentStealsAllowed = 0
        seasonStats.opponentDoublePlaysForced = 0
        
        var totalResponseSpeed = 0.0
        var responseSpeedCount = 0
        
        // Aggregate from all game stats
        for (gameStat in allGameStats) {
            if (gameStat.gameStatus == Game.GameStatus.FINAL) {
                // Determine win/loss
                val opponentStats = gameStatsRepository.findByGameId(gameStat.gameId)
                    .firstOrNull { it.team != gameStat.team }
                
                if (opponentStats != null) {
                    if (gameStat.score > opponentStats.score) {
                        seasonStats.wins++
                    } else if (gameStat.score < opponentStats.score) {
                        seasonStats.losses++
                    }
                }
            }
            
            // Batting stats
            seasonStats.atBats += gameStat.atBats
            seasonStats.runs += gameStat.runs
            seasonStats.hits += gameStat.hits
            seasonStats.runsBattedIn += gameStat.runsBattedIn
            seasonStats.homeRuns += gameStat.homeRuns
            seasonStats.triples += gameStat.triples
            seasonStats.doubles += gameStat.doubles
            seasonStats.singles += gameStat.singles
            seasonStats.walks += gameStat.walks
            seasonStats.strikeouts += gameStat.strikeouts
            seasonStats.steals += gameStat.steals
            seasonStats.doublePlays += gameStat.doublePlays
            
            // Pitching stats
            val inningsPitched = gameStat.inningsPitched
            val currentInnings = parseInningsPitched(seasonStats.inningsPitched ?: "0.0")
            seasonStats.inningsPitched = formatInningsPitched(currentInnings + inningsPitched)
            seasonStats.hitsAllowed += gameStat.hitsAllowed
            seasonStats.runsAllowed += gameStat.runsAllowed
            seasonStats.earnedRunsAllowed += gameStat.earnedRunsAllowed
            seasonStats.walksAllowed += gameStat.walksAllowed
            seasonStats.strikeoutsThrown += gameStat.strikeoutsThrown
            seasonStats.homeRunsAllowed += gameStat.homeRunsAllowed
            seasonStats.triplesAllowed += gameStat.triplesAllowed
            seasonStats.doublesAllowed += gameStat.doublesAllowed
            seasonStats.singlesAllowed += gameStat.singlesAllowed
            seasonStats.stealsAllowed += gameStat.stealsAllowed
            seasonStats.doublePlaysForced += gameStat.doublePlaysForced
            
            // Game control
            if (gameStat.largestLead > seasonStats.largestLead) {
                seasonStats.largestLead = gameStat.largestLead
            }
            if (gameStat.largestDeficit > seasonStats.largestDeficit) {
                seasonStats.largestDeficit = gameStat.largestDeficit
            }
            
            // Response speed
            if (gameStat.averageResponseSpeed > 0) {
                totalResponseSpeed += gameStat.averageResponseSpeed
                responseSpeedCount++
            }
            
            // Opponent stats (from opponent's game stats)
            val opponentGameStats = gameStatsRepository.findByGameId(gameStat.gameId)
                .firstOrNull { it.team != gameStat.team }
            
            if (opponentGameStats != null) {
                seasonStats.opponentAtBats += opponentGameStats.atBats
                seasonStats.opponentRuns += opponentGameStats.runs
                seasonStats.opponentHits += opponentGameStats.hits
                seasonStats.opponentRunsBattedIn += opponentGameStats.runsBattedIn
                seasonStats.opponentHomeRuns += opponentGameStats.homeRuns
                seasonStats.opponentTriples += opponentGameStats.triples
                seasonStats.opponentDoubles += opponentGameStats.doubles
                seasonStats.opponentSingles += opponentGameStats.singles
                seasonStats.opponentWalks += opponentGameStats.walks
                seasonStats.opponentStrikeouts += opponentGameStats.strikeouts
                seasonStats.opponentSteals += opponentGameStats.steals
                seasonStats.opponentDoublePlays += opponentGameStats.doublePlays
                
                val oppInnings = opponentGameStats.inningsPitched
                val currentOppInnings = parseInningsPitched(seasonStats.opponentInningsPitched ?: "0.0")
                seasonStats.opponentInningsPitched = formatInningsPitched(currentOppInnings + oppInnings)
                seasonStats.opponentHitsAllowed += opponentGameStats.hitsAllowed
                seasonStats.opponentRunsAllowed += opponentGameStats.runsAllowed
                seasonStats.opponentEarnedRunsAllowed += opponentGameStats.earnedRunsAllowed
                seasonStats.opponentWalksAllowed += opponentGameStats.walksAllowed
                seasonStats.opponentStrikeoutsThrown += opponentGameStats.strikeoutsThrown
                seasonStats.opponentHomeRunsAllowed += opponentGameStats.homeRunsAllowed
                seasonStats.opponentTriplesAllowed += opponentGameStats.triplesAllowed
                seasonStats.opponentDoublesAllowed += opponentGameStats.doublesAllowed
                seasonStats.opponentSinglesAllowed += opponentGameStats.singlesAllowed
                seasonStats.opponentStealsAllowed += opponentGameStats.stealsAllowed
                seasonStats.opponentDoublePlaysForced += opponentGameStats.doublePlaysForced
            }
        }
        
        // Calculate percentages
        seasonStats.battingAverage = calculateBattingAverage(seasonStats.hits, seasonStats.atBats)
        seasonStats.onBasePercentage = calculateOnBasePercentage(seasonStats.hits, seasonStats.walks, seasonStats.atBats)
        seasonStats.sluggingPercentage = calculateSluggingPercentage(
            seasonStats.singles,
            seasonStats.doubles,
            seasonStats.triples,
            seasonStats.homeRuns,
            seasonStats.atBats,
        )
        seasonStats.era = calculateERA(seasonStats.earnedRunsAllowed, parseInningsPitched(seasonStats.inningsPitched ?: "0.0"))
        seasonStats.whip = calculateWHIP(
            seasonStats.walksAllowed,
            seasonStats.hitsAllowed,
            parseInningsPitched(seasonStats.inningsPitched ?: "0.0"),
        )
        
        seasonStats.opponentBattingAverage = calculateBattingAverage(seasonStats.opponentHits, seasonStats.opponentAtBats)
        seasonStats.opponentOnBasePercentage = calculateOnBasePercentage(seasonStats.opponentHits, seasonStats.opponentWalks, seasonStats.opponentAtBats)
        seasonStats.opponentSluggingPercentage = calculateSluggingPercentage(
            seasonStats.opponentSingles,
            seasonStats.opponentDoubles,
            seasonStats.opponentTriples,
            seasonStats.opponentHomeRuns,
            seasonStats.opponentAtBats,
        )
        seasonStats.opponentEra = calculateERA(seasonStats.opponentEarnedRunsAllowed, parseInningsPitched(seasonStats.opponentInningsPitched ?: "0.0"))
        seasonStats.opponentWhip = calculateWHIP(
            seasonStats.opponentWalksAllowed,
            seasonStats.opponentHitsAllowed,
            parseInningsPitched(seasonStats.opponentInningsPitched ?: "0.0"),
        )
        
        // Average response speed
        seasonStats.averageResponseSpeed = if (responseSpeedCount > 0) totalResponseSpeed / responseSpeedCount else null
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

