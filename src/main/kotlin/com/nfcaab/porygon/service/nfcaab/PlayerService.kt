package com.nfcaab.porygon.service.nfcaab

import com.nfcaab.porygon.model.Player
import com.nfcaab.porygon.repositories.PlayerRepository
import com.nfcaab.porygon.util.PlayerNotFoundException
import org.springframework.stereotype.Service

@Service
class PlayerService(
    private val playerRepository: PlayerRepository,
) {
    /**
     * Get a player by uniform number and team
     * @param team Team name
     * @param uniformNumber Player's uniform number
     * @return Player
     */
    fun getPlayerByNumberAndTeam(
        team: String,
        uniformNumber: Int?,
    ): Player {
        if (uniformNumber == null) {
            throw PlayerNotFoundException("Uniform number is null for team $team")
        }
        return playerRepository.getPlayerByNumberAndTeam(team, uniformNumber)
            ?: throw PlayerNotFoundException("Player with uniform number $uniformNumber not found for team $team")
    }
}

