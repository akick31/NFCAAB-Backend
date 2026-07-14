package com.nfcaab.backend.service.stats

import com.nfcaab.backend.model.Player
import com.nfcaab.backend.repositories.BatterGameStatsRepository
import com.nfcaab.backend.repositories.PitcherGameStatsRepository
import com.nfcaab.backend.repositories.SeasonStatLineBatterRepository
import com.nfcaab.backend.repositories.SeasonStatLinePitcherRepository
import org.springframework.stereotype.Service

@Service
class PlayerStatLineService(
    private val seasonStatLineBatterRepository: SeasonStatLineBatterRepository,
    private val seasonStatLinePitcherRepository: SeasonStatLinePitcherRepository,
    private val batterGameStatsRepository: BatterGameStatsRepository,
    private val pitcherGameStatsRepository: PitcherGameStatsRepository,
) {
    fun pitcherLine(
        player: Player?,
        season: Int?,
    ): String? {
        val playerId = player?.id ?: return null
        if (season == null) return null
        val line = seasonStatLinePitcherRepository.findByPlayerIdAndSeason(playerId.toString(), season) ?: return null
        val era = line.earnedRunAverage ?: return null
        return "%.2f ERA, %d-%d, %d K".format(era, line.wins ?: 0, line.losses ?: 0, line.strikeouts ?: 0)
    }

    fun batterLine(
        player: Player?,
        season: Int?,
    ): String? {
        val playerId = player?.id ?: return null
        if (season == null) return null
        val line = seasonStatLineBatterRepository.findByPlayerIdAndSeason(playerId.toString(), season) ?: return null
        val average = line.battingAverage ?: return null
        return "${formatAverage(average)} AVG, ${line.homeRuns ?: 0} HR, ${line.runsBattedIn ?: 0} RBI"
    }

    fun pitcherGameLine(
        team: String?,
        uniformNumber: Int?,
        gameId: Int,
    ): String? {
        if (team == null || uniformNumber == null) return null
        val stats = pitcherGameStatsRepository.findByGameIdAndUniformNumberAndTeam(gameId, uniformNumber, team) ?: return null
        return "${stats.inningsPitched ?: "0.0"} IP, ${stats.hits ?: 0} H, ${stats.walks ?: 0} BB, ${stats.strikeouts ?: 0} K"
    }

    fun batterGameLine(
        team: String?,
        uniformNumber: Int?,
        gameId: Int,
    ): String? {
        if (team == null || uniformNumber == null) return null
        val stats = batterGameStatsRepository.findByGameIdAndUniformNumberAndTeam(gameId, uniformNumber, team) ?: return null
        val base = "${stats.hits ?: 0}-${stats.atBats ?: 0}"
        val extras = mutableListOf<String>()
        when {
            (stats.homeRuns ?: 0) > 0 -> extras.add("HR")
            (stats.triples ?: 0) > 0 -> extras.add("3B")
            (stats.doubles ?: 0) > 0 -> extras.add("2B")
        }
        if ((stats.runsBattedIn ?: 0) > 0) extras.add("${stats.runsBattedIn} RBI")
        return if (extras.isEmpty()) base else "$base, ${extras.joinToString(", ")}"
    }

    private fun formatAverage(average: Float): String {
        val formatted = "%.3f".format(average)
        return if (formatted.startsWith("0.")) formatted.substring(1) else formatted
    }
}
