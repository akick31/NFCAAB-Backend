package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.dto.requests.LineupSubmissionRequest
import com.nfcaab.backend.model.GameLineup
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.repositories.GameLineupRepository
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.util.InvalidLineupException
import com.nfcaab.backend.util.PlayerNotFoundException
import org.springframework.stereotype.Service

@Service
class LineupService(
    private val gameLineupRepository: GameLineupRepository,
    private val playerService: PlayerService,
    private val gameRepository: GameRepository,
    private val lineupTokenService: LineupTokenService,
) {
    companion object {
        private const val MINIMUM_STARTER_REST_GAMES = 4
    }

    fun getBatterByLineupSpotAndTeam(
        gameId: Int,
        team: String,
        lineupSpot: Int,
    ): Player {
        val gameLineup = gameLineupRepository.getBatterByLineupSpotAndTeam(gameId, team, lineupSpot)
            ?: throw PlayerNotFoundException("Batter not found in lineup spot $lineupSpot for team $team")
        return playerService.getPlayerByNumberAndTeam(team, gameLineup.uniformNumber)
    }

    fun getBatterByLineupSpot(
        gameId: Int,
        lineupSpot: Int,
        team: String,
    ): Player = getBatterByLineupSpotAndTeam(gameId, team, lineupSpot)

    fun getPitcherByTeam(
        gameId: Int,
        team: String,
    ): Player {
        val gameLineup = gameLineupRepository.getPitcherByTeam(gameId, team)
            ?: throw PlayerNotFoundException("Pitcher not found for team $team")
        return playerService.getPlayerByNumberAndTeam(team, gameLineup.uniformNumber)
    }

    fun saveLineup(request: LineupSubmissionRequest): List<GameLineup> {
        val lineupToken = lineupTokenService.validateToken(request.token)
        val gameId = lineupToken.gameId ?: throw InvalidLineupException("Lineup token is missing a game id")
        val team = lineupToken.team ?: throw InvalidLineupException("Lineup token is missing a team")

        validateLineup(request, gameId, team)

        deleteExistingLineup(gameId, team)

        val lineupEntries = mutableListOf<GameLineup>()

        request.batters.forEachIndexed { index, batterUniformNumber ->
            val player = playerService.getPlayerByNumberAndTeam(team, batterUniformNumber)
            val lineupEntry = GameLineup().apply {
                this.gameId = gameId.toString()
                this.team = team
                lineupSpot = (index + 1).toString()
                name = "${player.firstName} ${player.lastName}"
                uniformNumber = batterUniformNumber
                position = player.primaryPosition?.description ?: "Unknown"
                archetype = player.batterArchetype?.description ?: "Neutral"
                currentlyPlaying = true
                hasAppeared = false
            }
            lineupEntries.add(gameLineupRepository.save(lineupEntry) ?: throw InvalidLineupException("Failed to save lineup entry"))
        }

        val pitcher = playerService.getPlayerByNumberAndTeam(team, request.pitcher)
        if (pitcher.pitcherRole == Player.PitcherRole.STARTER) {
            pitcher.lastStartGameId = gameId
            playerService.savePlayer(pitcher)
        }
        val pitcherEntry = GameLineup().apply {
            this.gameId = gameId.toString()
            this.team = team
            lineupSpot = "P"
            name = "${pitcher.firstName} ${pitcher.lastName}"
            uniformNumber = request.pitcher
            position = "Pitcher"
            archetype = pitcher.pitcherArchetype?.description ?: "Neutral"
            currentlyPlaying = true
            hasAppeared = false
        }
        lineupEntries.add(gameLineupRepository.save(pitcherEntry) ?: throw InvalidLineupException("Failed to save pitcher entry"))

        lineupTokenService.consumeToken(lineupToken)

        return lineupEntries
    }

    private fun validateLineup(
        request: LineupSubmissionRequest,
        gameId: Int,
        team: String,
    ) {
        if (request.batters.size != 9) {
            throw InvalidLineupException("Lineup must have exactly 9 batters, got ${request.batters.size}")
        }

        val duplicateBatters = request.batters.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicateBatters.isNotEmpty()) {
            throw InvalidLineupException("Duplicate batters found: ${duplicateBatters.keys.joinToString()}")
        }

        if (request.pitcher in request.batters) {
            throw InvalidLineupException("Pitcher cannot be in the batting lineup")
        }

        val allUniformNumbers = request.batters + request.pitcher
        allUniformNumbers.forEach { uniformNumber ->
            try {
                val player = playerService.getPlayerByNumberAndTeam(team, uniformNumber)
                if (player.currentTeam != team) {
                    throw InvalidLineupException("Player with uniform number $uniformNumber is not on team $team")
                }
            } catch (e: PlayerNotFoundException) {
                throw InvalidLineupException("Player with uniform number $uniformNumber not found for team $team")
            }
        }

        val startingPitcher = playerService.getPlayerByNumberAndTeam(team, request.pitcher)
        val lastStartGameId = startingPitcher.lastStartGameId
        if (startingPitcher.pitcherRole == Player.PitcherRole.STARTER && lastStartGameId != null) {
            val gamesSinceLastStart = gameRepository.countFinishedGamesByTeamSinceGameId(team, lastStartGameId)
            if (gamesSinceLastStart < MINIMUM_STARTER_REST_GAMES) {
                throw InvalidLineupException(
                    "${startingPitcher.firstName} ${startingPitcher.lastName} has only had " +
                        "$gamesSinceLastStart game(s) of rest since their last start " +
                        "(needs $MINIMUM_STARTER_REST_GAMES) and is not eligible to start",
                )
            }
        }
    }

    private fun deleteExistingLineup(
        gameId: Int,
        team: String,
    ) {
        val existingLineups = gameLineupRepository.getAllLineupsByGameIdAndTeam(gameId, team)
        gameLineupRepository.deleteAll(existingLineups)
    }
}
