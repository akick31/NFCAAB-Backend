package com.nfcaab.backend.service.stats

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.PitcherGameStats
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.repositories.PitcherGameStatsRepository
import org.springframework.stereotype.Service

@Service
class PitcherDecisionService(
    private val atBatRepository: AtBatRepository,
    private val pitcherGameStatsRepository: PitcherGameStatsRepository,
) {
    fun computeDecisions(game: Game) {
        if (game.homeScore == game.awayScore) return

        val atBats = atBatRepository.getAllAtBatsByGameId(game.id).sortedBy { it.id }
        if (atBats.isEmpty()) return

        val homeWins = game.homeScore > game.awayScore
        val winningTeam = if (homeWins) game.homeTeam else game.awayTeam
        val losingTeam = if (homeWins) game.awayTeam else game.homeTeam

        val pitcherGameStats = pitcherGameStatsRepository.findByGameId(game.id)
        fun inningsPitchedFor(
            team: String,
            uniformNumber: Int?,
        ): Double =
            pitcherGameStats
                .firstOrNull { it.team == team && it.uniformNumber == uniformNumber }
                ?.inningsPitched
                ?.toDoubleOrNull() ?: 0.0

        val leadForGoodIndex = findLeadForGoodIndex(atBats, homeWins)
        val leadForGoodPlay = atBats[leadForGoodIndex]

        val losingPitcherUniformNumber = leadForGoodPlay.pitcherUniformNumber

        val winningPitcherPlay =
            atBats.take(leadForGoodIndex + 1).lastOrNull { it.pitchingTeam == winningTeam }
                ?: atBats.firstOrNull { it.pitchingTeam == winningTeam }
        val recordPitcherUniformNumber = winningPitcherPlay?.pitcherUniformNumber

        val starterUniformNumber = atBats.firstOrNull { it.pitchingTeam == winningTeam }?.pitcherUniformNumber
        val recordPitcherIsStarter = recordPitcherUniformNumber != null && recordPitcherUniformNumber == starterUniformNumber
        val recordPitcherInnings = inningsPitchedFor(winningTeam, recordPitcherUniformNumber)

        val winningPitcherUniformNumber =
            if (recordPitcherIsStarter && recordPitcherInnings < 5.0) {
                val winningTeamPitcherOrder =
                    atBats.filter { it.pitchingTeam == winningTeam }.mapNotNull { it.pitcherUniformNumber }.distinct()
                winningTeamPitcherOrder.firstOrNull {
                    it != starterUniformNumber && inningsPitchedFor(winningTeam, it) >= 1.0
                } ?: recordPitcherUniformNumber
            } else {
                recordPitcherUniformNumber
            }

        val finishingPitcherUniformNumber = atBats.lastOrNull { it.pitchingTeam == winningTeam }?.pitcherUniformNumber

        val savePitcherUniformNumber =
            resolveSave(atBats, game, winningTeam, winningPitcherUniformNumber, finishingPitcherUniformNumber) { uniformNumber ->
                inningsPitchedFor(winningTeam, uniformNumber)
            }

        markDecision(pitcherGameStats, winningTeam, winningPitcherUniformNumber) { it.win = true }
        markDecision(pitcherGameStats, losingTeam, losingPitcherUniformNumber) { it.loss = true }
        savePitcherUniformNumber?.let { markDecision(pitcherGameStats, winningTeam, it) { stats -> stats.save = true } }
    }

    private fun findLeadForGoodIndex(
        atBats: List<AtBat>,
        homeWins: Boolean,
    ): Int {
        fun margin(atBat: AtBat): Int = if (homeWins) atBat.homeScore - atBat.awayScore else atBat.awayScore - atBat.homeScore

        for (i in atBats.indices) {
            if (margin(atBats[i]) <= 0) continue
            val holdsForRest = (i until atBats.size).all { j -> margin(atBats[j]) > 0 }
            if (holdsForRest) return i
        }
        return atBats.size - 1
    }

    private fun resolveSave(
        atBats: List<AtBat>,
        game: Game,
        winningTeam: String,
        winningPitcherUniformNumber: Int?,
        finishingPitcherUniformNumber: Int?,
        inningsPitchedFor: (Int?) -> Double,
    ): Int? {
        if (finishingPitcherUniformNumber == null || finishingPitcherUniformNumber == winningPitcherUniformNumber) return null

        val entryIndex =
            atBats.indexOfFirst { it.pitchingTeam == winningTeam && it.pitcherUniformNumber == finishingPitcherUniformNumber }
        if (entryIndex < 0) return null

        val priorPlay = atBats.getOrNull(entryIndex - 1)
        val entryHomeScore = priorPlay?.homeScore ?: 0
        val entryAwayScore = priorPlay?.awayScore ?: 0
        val entryMargin = if (winningTeam == game.homeTeam) entryHomeScore - entryAwayScore else entryAwayScore - entryHomeScore

        val entryPlay = atBats[entryIndex]
        val runnersOnBaseAtEntry =
            listOfNotNull(entryPlay.runnerOnFirst, entryPlay.runnerOnSecond, entryPlay.runnerOnThird).size
        val tyingRunOnBase = entryMargin in 1..runnersOnBaseAtEntry
        val innings = inningsPitchedFor(finishingPitcherUniformNumber)

        val eligible =
            (entryMargin in 1..3 && innings >= 1.0) ||
                tyingRunOnBase ||
                innings >= 3.0

        return if (eligible) finishingPitcherUniformNumber else null
    }

    private fun markDecision(
        pitcherGameStats: List<PitcherGameStats>,
        team: String,
        uniformNumber: Int?,
        apply: (PitcherGameStats) -> Unit,
    ) {
        if (uniformNumber == null) return
        val stats = pitcherGameStats.firstOrNull { it.team == team && it.uniformNumber == uniformNumber } ?: return
        apply(stats)
        pitcherGameStatsRepository.save(stats)
    }
}
