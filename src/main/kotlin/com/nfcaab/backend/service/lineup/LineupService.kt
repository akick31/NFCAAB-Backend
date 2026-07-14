package com.nfcaab.backend.service.lineup

import com.nfcaab.backend.dto.requests.BatterSubmission
import com.nfcaab.backend.dto.requests.LineupSubmissionRequest
import com.nfcaab.backend.model.GameLineup
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.repositories.GameLineupRepository
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.util.InvalidLineupException
import com.nfcaab.backend.util.PlayerNotFoundException
import org.springframework.stereotype.Service
import com.nfcaab.backend.service.player.PlayerService

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

    fun getCurrentPosition(
        gameId: Int,
        team: String,
        uniformNumber: Int,
    ): Player.Position {
        val entry = gameLineupRepository.getCurrentLineupEntryByUniformNumber(gameId, team, uniformNumber)
            ?: throw PlayerNotFoundException("$uniformNumber is not currently in the lineup for team $team")
        return Player.Position.fromDescription(entry.position ?: "Unknown")
    }

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

        request.batters.forEachIndexed { index, batter ->
            val player = playerService.getPlayerByNumberAndTeam(team, batter.uniformNumber)
            val lineupEntry = GameLineup().apply {
                this.gameId = gameId.toString()
                this.team = team
                lineupSpot = (index + 1).toString()
                name = "${player.firstName} ${player.lastName}"
                uniformNumber = batter.uniformNumber
                position = batter.position.description
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

    fun substituteBatter(
        gameId: Int,
        team: String,
        outgoingUniformNumber: Int,
        incomingUniformNumber: Int,
        incomingPosition: Player.Position,
    ): GameLineup {
        if (incomingPosition !in Player.Position.FIELD_POSITIONS) {
            throw InvalidLineupException("$incomingPosition is not a valid position for a batter")
        }

        val outgoingEntry = gameLineupRepository.getCurrentLineupEntryByUniformNumber(gameId, team, outgoingUniformNumber)
            ?: throw PlayerNotFoundException("$outgoingUniformNumber is not currently in the lineup for team $team")
        val lineupSpot = outgoingEntry.lineupSpot ?: throw InvalidLineupException("Outgoing player has no lineup spot")
        if (lineupSpot == "P") {
            throw InvalidLineupException("Use substitutePitcher to replace a pitcher")
        }

        val incomingPlayer = playerService.getPlayerByNumberAndTeam(team, incomingUniformNumber)
        if (incomingPlayer.currentTeam != team) {
            throw InvalidLineupException("Player with uniform number $incomingUniformNumber is not on team $team")
        }

        val currentlyPlayingNumbers =
            gameLineupRepository.getAllLineupsByGameIdAndTeam(gameId, team)
                .filter { it.currentlyPlaying == true }
                .mapNotNull { it.uniformNumber }
        if (incomingUniformNumber in currentlyPlayingNumbers) {
            throw InvalidLineupException("Player with uniform number $incomingUniformNumber is already in the game")
        }

        outgoingEntry.currentlyPlaying = false
        gameLineupRepository.save(outgoingEntry)

        val incomingEntry = GameLineup().apply {
            this.gameId = gameId.toString()
            this.team = team
            this.lineupSpot = lineupSpot
            name = "${incomingPlayer.firstName} ${incomingPlayer.lastName}"
            uniformNumber = incomingUniformNumber
            position = incomingPosition.description
            archetype = incomingPlayer.batterArchetype?.description ?: "Neutral"
            currentlyPlaying = true
            hasAppeared = false
        }
        return gameLineupRepository.save(incomingEntry) ?: throw InvalidLineupException("Failed to save substitution")
    }

    fun substitutePitcher(
        gameId: Int,
        team: String,
        incomingUniformNumber: Int,
    ): GameLineup {
        val outgoingEntry = gameLineupRepository.getPitcherByTeam(gameId, team)
            ?: throw PlayerNotFoundException("Pitcher not found for team $team")

        val incomingPitcher = playerService.getPlayerByNumberAndTeam(team, incomingUniformNumber)
        if (incomingPitcher.currentTeam != team) {
            throw InvalidLineupException("Player with uniform number $incomingUniformNumber is not on team $team")
        }

        validateRotationRest(incomingPitcher)

        outgoingEntry.currentlyPlaying = false
        gameLineupRepository.save(outgoingEntry)

        val incomingEntry = GameLineup().apply {
            this.gameId = gameId.toString()
            this.team = team
            lineupSpot = "P"
            name = "${incomingPitcher.firstName} ${incomingPitcher.lastName}"
            uniformNumber = incomingUniformNumber
            position = "Pitcher"
            archetype = incomingPitcher.pitcherArchetype?.description ?: "Neutral"
            currentlyPlaying = true
            hasAppeared = false
        }
        return gameLineupRepository.save(incomingEntry) ?: throw InvalidLineupException("Failed to save pitching change")
    }

    private fun validateLineup(
        request: LineupSubmissionRequest,
        gameId: Int,
        team: String,
    ) {
        if (request.batters.size != 9) {
            throw InvalidLineupException("Lineup must have exactly 9 batters, got ${request.batters.size}")
        }

        val duplicateBatters = request.batters.groupingBy { it.uniformNumber }.eachCount().filter { it.value > 1 }
        if (duplicateBatters.isNotEmpty()) {
            throw InvalidLineupException("Duplicate batters found: ${duplicateBatters.keys.joinToString()}")
        }

        val submittedPositions = request.batters.map { it.position }
        val missingPositions = Player.Position.FIELD_POSITIONS - submittedPositions.toSet()
        if (missingPositions.isNotEmpty()) {
            throw InvalidLineupException("Lineup is missing positions: ${missingPositions.joinToString { it.description }}")
        }
        val duplicatePositions = submittedPositions.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicatePositions.isNotEmpty()) {
            throw InvalidLineupException("Duplicate positions found: ${duplicatePositions.keys.joinToString { it.description }}")
        }

        val batterUniformNumbers = request.batters.map { it.uniformNumber }
        if (request.pitcher in batterUniformNumbers) {
            throw InvalidLineupException("Pitcher cannot be in the batting lineup")
        }

        val allUniformNumbers = batterUniformNumbers + request.pitcher
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
        validateRotationRest(startingPitcher)
    }

    private fun validateRotationRest(pitcher: Player) {
        val lastStartGameId = pitcher.lastStartGameId
        if (pitcher.pitcherRole == Player.PitcherRole.STARTER && lastStartGameId != null) {
            val gamesSinceLastStart = gameRepository.countFinishedGamesByTeamSinceGameId(pitcher.currentTeam ?: "", lastStartGameId)
            if (gamesSinceLastStart < MINIMUM_STARTER_REST_GAMES) {
                throw InvalidLineupException(
                    "${pitcher.firstName} ${pitcher.lastName} has only had " +
                        "$gamesSinceLastStart game(s) of rest since their last start " +
                        "(needs $MINIMUM_STARTER_REST_GAMES) and is not eligible to pitch",
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
