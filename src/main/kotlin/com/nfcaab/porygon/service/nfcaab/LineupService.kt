package com.nfcaab.porygon.service.nfcaab

import com.nfcaab.porygon.model.GameLineup
import com.nfcaab.porygon.model.Player
import com.nfcaab.porygon.repositories.GameLineupRepository
import com.nfcaab.porygon.util.PlayerNotFoundException
import org.springframework.stereotype.Service

@Service
class LineupService(
    private val gameLineupRepository: GameLineupRepository,
    private val playerService: PlayerService,
) {
    /**
     * Get a batter by lineup spot and team
     * @param gameId Game ID
     * @param team Team name
     * @param lineupSpot Lineup spot (1-9)
     * @return Player
     */
    fun getBatterByLineupSpotAndTeam(
        gameId: Int,
        team: String,
        lineupSpot: Int,
    ): Player {
        val gameLineup = gameLineupRepository.getBatterByLineupSpotAndTeam(gameId, team, lineupSpot)
            ?: throw PlayerNotFoundException("Batter not found in lineup spot $lineupSpot for team $team")
        return playerService.getPlayerByNumberAndTeam(team, gameLineup.uniformNumber)
    }

    /**
     * Get a batter by lineup spot and team (alternate signature for GameService compatibility)
     * @param gameId Game ID
     * @param lineupSpot Lineup spot (1-9)
     * @param team Team name
     * @return Player
     */
    fun getBatterByLineupSpot(
        gameId: Int,
        lineupSpot: Int,
        team: String,
    ): Player = getBatterByLineupSpotAndTeam(gameId, team, lineupSpot)

    /**
     * Get the current pitcher for a team
     * @param gameId Game ID
     * @param team Team name
     * @return Player
     */
    fun getPitcherByTeam(
        gameId: Int,
        team: String,
    ): Player {
        val gameLineup = gameLineupRepository.getPitcherByTeam(gameId, team)
            ?: throw PlayerNotFoundException("Pitcher not found for team $team")
        return playerService.getPlayerByNumberAndTeam(team, gameLineup.uniformNumber)
    }
}

