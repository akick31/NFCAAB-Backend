package com.nfcaab.backend.service.stats

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.BatterGameStats
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.model.PitcherGameStats
import com.nfcaab.backend.repositories.BatterGameStatsRepository
import com.nfcaab.backend.repositories.PitcherGameStatsRepository
import com.nfcaab.backend.repositories.RunEventRepository
import com.nfcaab.backend.service.player.PlayerService
import com.nfcaab.backend.util.PlayerNotFoundException
import org.springframework.stereotype.Service

@Service
class PlayerGameStatsService(
    private val batterGameStatsRepository: BatterGameStatsRepository,
    private val pitcherGameStatsRepository: PitcherGameStatsRepository,
    private val runEventRepository: RunEventRepository,
    private val playerService: PlayerService,
) {
    fun updatePlayerGameStats(
        game: Game,
        allAtBats: List<AtBat>,
    ) {
        allAtBats
            .groupBy { it.battingTeam to it.batterUniformNumber }
            .forEach { (key, atBats) ->
                val (team, uniformNumber) = key
                if (team != null && uniformNumber != null) {
                    updateBatterStats(game, team, uniformNumber, atBats)
                }
            }

        allAtBats
            .groupBy { it.pitchingTeam to it.pitcherUniformNumber }
            .forEach { (key, atBats) ->
                val (team, uniformNumber) = key
                if (team != null && uniformNumber != null) {
                    updatePitcherStats(game, team, uniformNumber, atBats)
                }
            }
    }

    private fun updateBatterStats(
        game: Game,
        team: String,
        uniformNumber: Int,
        atBats: List<AtBat>,
    ) {
        val stats =
            batterGameStatsRepository.findByGameIdAndUniformNumberAndTeam(game.id, uniformNumber, team)
                ?: BatterGameStats()
        val player = resolvePlayerSafely(team, uniformNumber)

        stats.gameId = game.id
        stats.gameNumber = game.seriesGameNumber
        stats.playerId = player?.id?.toString()
        stats.team = team
        stats.opponentTeam = if (team == game.homeTeam) game.awayTeam else game.homeTeam
        stats.lineupSpot = atBats.first().lineupSpot.toString()
        stats.firstName = player?.firstName
        stats.lastName = player?.lastName
        stats.uniformNumber = uniformNumber
        stats.position = player?.primaryPosition?.description
        stats.archetype = player?.batterArchetype?.description

        stats.atBats = calculateAtBats(atBats)
        stats.runs = runEventRepository.countByGameIdAndScoringPlayerUniformNumberAndScoringTeam(game.id, uniformNumber, team)
        stats.hits = calculateHits(atBats)
        stats.runsBattedIn = atBats.sumOf { it.runsScored }
        stats.homeRuns = atBats.count { it.actualResult == ActualResult.HOME_RUN }
        stats.triples = atBats.count { it.actualResult == ActualResult.TRIPLE }
        stats.doubles = atBats.count { it.actualResult == ActualResult.DOUBLE }
        stats.singles = atBats.count { it.actualResult == ActualResult.SINGLE }
        stats.walks = atBats.count { it.actualResult == ActualResult.WALK }
        stats.strikeouts = atBats.count { it.actualResult == ActualResult.STRIKEOUT }
        stats.steals = atBats.count { it.result == Scenario.STEAL_SUCCESS }
        stats.doublePlays = atBats.count { it.actualResult == ActualResult.DOUBLE_PLAY }

        batterGameStatsRepository.save(stats)
    }

    private fun updatePitcherStats(
        game: Game,
        team: String,
        uniformNumber: Int,
        atBats: List<AtBat>,
    ) {
        val stats =
            pitcherGameStatsRepository.findByGameIdAndUniformNumberAndTeam(game.id, uniformNumber, team)
                ?: PitcherGameStats()
        val player = resolvePlayerSafely(team, uniformNumber)

        stats.gameId = game.id
        stats.gameNumber = game.seriesGameNumber
        stats.playerId = player?.id?.toString()
        stats.team = team
        stats.opponentTeam = if (team == game.homeTeam) game.awayTeam else game.homeTeam
        stats.firstName = player?.firstName
        stats.lastName = player?.lastName
        stats.uniformNumber = uniformNumber
        stats.position = Player.Position.PITCHER.description
        stats.archetype = player?.pitcherArchetype?.description

        stats.pitchesThrown = atBats.size
        stats.inningsPitched = calculateInningsPitched(atBats).toString()
        stats.hits = calculateHits(atBats)
        stats.runs = runEventRepository.countByGameIdAndChargedPitcherUniformNumberAndChargedPitcherTeam(game.id, uniformNumber, team)
        stats.earnedRuns = stats.runs
        stats.walks = atBats.count { it.actualResult == ActualResult.WALK }
        stats.strikeouts = atBats.count { it.actualResult == ActualResult.STRIKEOUT }
        stats.homeRuns = atBats.count { it.actualResult == ActualResult.HOME_RUN }
        stats.triples = atBats.count { it.actualResult == ActualResult.TRIPLE }
        stats.doubles = atBats.count { it.actualResult == ActualResult.DOUBLE }
        stats.singles = atBats.count { it.actualResult == ActualResult.SINGLE }
        stats.stealsAllowed = atBats.count { it.result == Scenario.STEAL_SUCCESS }
        stats.doublePlaysForced = atBats.count { it.actualResult == ActualResult.DOUBLE_PLAY }

        pitcherGameStatsRepository.save(stats)
    }

    private fun resolvePlayerSafely(
        team: String,
        uniformNumber: Int,
    ): Player? =
        try {
            playerService.getPlayerByNumberAndTeam(team, uniformNumber)
        } catch (e: PlayerNotFoundException) {
            null
        }

    private fun calculateAtBats(atBats: List<AtBat>): Int =
        atBats.count { pa ->
            pa.actualResult != ActualResult.WALK &&
                pa.actualResult != ActualResult.SACRIFICE_FLY &&
                pa.actualResult != ActualResult.SACRIFICE_BUNT &&
                pa.actualResult != ActualResult.STOLEN_BASE &&
                pa.actualResult != ActualResult.CAUGHT_STEALING
        }

    private fun calculateHits(atBats: List<AtBat>): Int =
        atBats.count { pa ->
            pa.actualResult == ActualResult.SINGLE ||
                pa.actualResult == ActualResult.DOUBLE ||
                pa.actualResult == ActualResult.TRIPLE ||
                pa.actualResult == ActualResult.HOME_RUN
        }

    private fun calculateInningsPitched(atBats: List<AtBat>): Double {
        val singleOutResults =
            setOf(
                ActualResult.STRIKEOUT,
                ActualResult.FLYOUT,
                ActualResult.GROUNDOUT,
                ActualResult.SACRIFICE_FLY,
                ActualResult.SACRIFICE_BUNT,
                ActualResult.FIELDERS_CHOICE,
                ActualResult.CAUGHT_STEALING,
            )
        val outs =
            atBats.sumOf { pa ->
                val outsRecorded: Int =
                    when (pa.actualResult) {
                        ActualResult.DOUBLE_PLAY -> 2
                        in singleOutResults -> 1
                        else -> 0
                    }
                outsRecorded
            }
        return outs.toDouble() / 3.0
    }
}
