package com.nfcaab.backend.service.player

import com.nfcaab.backend.model.Player
import com.nfcaab.backend.repositories.PlayerRepository
import com.nfcaab.backend.util.PlayerNotFoundException
import org.springframework.stereotype.Service

@Service
class PlayerService(
    private val playerRepository: PlayerRepository,
) {
    fun rolloverEligibilityForNewSeason(): List<Player> {
        val graduatedPlayers = mutableListOf<Player>()
        playerRepository.findAll()
            .filter { it.active }
            .forEach { player ->
                val nextYear = advanceCollegeYear(player.collegeYear)
                player.collegeYear = nextYear
                if (nextYear == Player.CollegeYear.GRADUATED) {
                    player.active = false
                    player.currentTeam = null
                    graduatedPlayers.add(player)
                }
                playerRepository.save(player)
            }
        return graduatedPlayers
    }

    private fun advanceCollegeYear(currentYear: Player.CollegeYear?): Player.CollegeYear =
        when (currentYear) {
            null, Player.CollegeYear.FRESHMAN -> Player.CollegeYear.SOPHOMORE
            Player.CollegeYear.SOPHOMORE -> Player.CollegeYear.JUNIOR
            Player.CollegeYear.JUNIOR -> Player.CollegeYear.SENIOR
            Player.CollegeYear.SENIOR -> Player.CollegeYear.GRADUATED
            Player.CollegeYear.GRADUATED -> Player.CollegeYear.GRADUATED
        }

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

    fun getPlayersByTeam(team: String): List<Player> = playerRepository.findByCurrentTeamAndActive(team, true)

    fun savePlayer(player: Player): Player = playerRepository.save(player)
}
