package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.dto.requests.LineupSubmissionRequest
import com.nfcaab.backend.model.GameLineup
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.repositories.GameLineupRepository
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.util.GameNotFoundException
import com.nfcaab.backend.util.InvalidLineupException
import com.nfcaab.backend.util.PlayerNotFoundException
import org.springframework.stereotype.Service

@Service
class LineupService(
    private val gameLineupRepository: GameLineupRepository,
    private val playerService: PlayerService,
    private val gameRepository: GameRepository,
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

    /**
     * Validate and save a lineup for a game
     * @param request Lineup submission request
     * @return List of created GameLineup entries
     */
    fun saveLineup(request: LineupSubmissionRequest): List<GameLineup> {
        // Validate the lineup
        validateLineup(request)

        // Delete existing lineup for this game and team
        deleteExistingLineup(request.gameId, request.team)

        // Create and save lineup entries
        val lineupEntries = mutableListOf<GameLineup>()

        // Save batters (spots 1-9)
        request.batters.forEachIndexed { index, batterUniformNumber ->
            val player = playerService.getPlayerByNumberAndTeam(request.team, batterUniformNumber)
            val lineupEntry = GameLineup().apply {
                gameId = request.gameId.toString()
                team = request.team
                lineupSpot = (index + 1).toString()
                name = "${player.firstName} ${player.lastName}"
                uniformNumber = batterUniformNumber
                position = player.primaryPosition?.description ?: "Unknown"
                archetype = player.archetype?.description ?: "Neutral"
                currentlyPlaying = true
                hasAppeared = false
            }
            lineupEntries.add(gameLineupRepository.save(lineupEntry) ?: throw InvalidLineupException("Failed to save lineup entry"))
        }

        // Save pitcher
        val pitcher = playerService.getPlayerByNumberAndTeam(request.team, request.pitcher)
        val pitcherEntry = GameLineup().apply {
            gameId = request.gameId.toString()
            team = request.team
            lineupSpot = "P"
            name = "${pitcher.firstName} ${pitcher.lastName}"
            uniformNumber = request.pitcher
            position = "Pitcher"
            archetype = pitcher.archetype?.description ?: "Neutral"
            currentlyPlaying = true
            hasAppeared = false
        }
        lineupEntries.add(gameLineupRepository.save(pitcherEntry) ?: throw InvalidLineupException("Failed to save pitcher entry"))

        return lineupEntries
    }

    /**
     * Validate a lineup submission
     * @param request Lineup submission request
     * @throws InvalidLineupException if validation fails
     */
    private fun validateLineup(request: LineupSubmissionRequest) {
        // Validate game exists and team is part of the game
        val game = gameRepository.getGameById(request.gameId)
            ?: throw GameNotFoundException("No game found with ID: ${request.gameId}")
        if (game.homeTeam != request.team && game.awayTeam != request.team) {
            throw InvalidLineupException("Team ${request.team} is not part of game ${request.gameId}")
        }

        // Validate exactly 9 batters
        if (request.batters.size != 9) {
            throw InvalidLineupException("Lineup must have exactly 9 batters, got ${request.batters.size}")
        }

        // Validate no duplicate batters
        val duplicateBatters = request.batters.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicateBatters.isNotEmpty()) {
            throw InvalidLineupException("Duplicate batters found: ${duplicateBatters.keys.joinToString()}")
        }

        // Validate pitcher is not in batting lineup
        if (request.pitcher in request.batters) {
            throw InvalidLineupException("Pitcher cannot be in the batting lineup")
        }

        // Validate all players exist and are on the team
        val allUniformNumbers = request.batters + request.pitcher
        allUniformNumbers.forEach { uniformNumber ->
            try {
                val player = playerService.getPlayerByNumberAndTeam(request.team, uniformNumber)
                if (player.currentTeam != request.team) {
                    throw InvalidLineupException("Player with uniform number $uniformNumber is not on team ${request.team}")
                }
            } catch (e: PlayerNotFoundException) {
                throw InvalidLineupException("Player with uniform number $uniformNumber not found for team ${request.team}")
            }
        }
    }

    /**
     * Delete existing lineup for a game and team
     * @param gameId Game ID
     * @param team Team name
     */
    private fun deleteExistingLineup(gameId: Int, team: String) {
        // Get all existing lineup entries for this game and team
        val existingLineups = gameLineupRepository.getAllLineupsByGameIdAndTeam(gameId, team)
        gameLineupRepository.deleteAll(existingLineups)
    }
}

