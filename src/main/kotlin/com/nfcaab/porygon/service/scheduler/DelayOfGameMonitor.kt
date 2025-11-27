package com.nfcaab.porygon.service.scheduler

import com.nfcaab.porygon.enums.play.ActualResult
import com.nfcaab.porygon.enums.play.Scenario
import com.nfcaab.porygon.enums.team.TeamSide
import com.nfcaab.porygon.model.Game
import com.nfcaab.porygon.model.PlateAppearance
import com.nfcaab.porygon.repositories.PlateAppearanceRepository
import com.nfcaab.porygon.service.discord.DiscordService
import com.nfcaab.porygon.service.nfcaab.GameService
import com.nfcaab.porygon.service.nfcaab.PlateAppearanceService
import com.nfcaab.porygon.service.nfcaab.ScorebugService
import com.nfcaab.porygon.service.nfcaab.UserService
import com.nfcaab.porygon.util.Logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DelayOfGameMonitor(
    private val gameService: GameService,
    private val userService: UserService,
    private val plateAppearanceService: PlateAppearanceService,
    private val discordService: DiscordService,
    private val scorebugService: ScorebugService,
    private val plateAppearanceRepository: PlateAppearanceRepository,
) {
    /**
     * Checks for delay of game every minute
     */
    @Scheduled(fixedRate = 60000)
    fun checkForDelayOfGame() {
        val warnedGames = gameService.findGamesToWarn()
        warnedGames.forEach { game ->
            discordService.notifyWarning(game)
            gameService.updateGameAsWarned(game.gameId)
            Logger.info("A delay of game warning for game ${game.id} has been processed")
        }
        val expiredGames = gameService.findExpiredTimers()
        expiredGames.forEach { game ->
            val updatedGame =
                if (game.gameStatus == Game.GameStatus.PREGAME) {
                    applyPregameDelayOfGame(game)
                } else {
                    applyDelayOfGame(game)
                }
            val delayOfGameInstances = getDelayOfGameInstances(updatedGame)
            val isDelayOfGameOut = delayOfGameInstances.first >= 3 || delayOfGameInstances.second >= 3
            if (isDelayOfGameOut) {
                gameService.endDOGOutGame(updatedGame, delayOfGameInstances)
            }
            discordService.notifyDelayOfGame(updatedGame, isDelayOfGameOut)
            Logger.info("A delay of game for game ${game.id} has been processed")
        }
    }

    /**
     * Get the delay of game instances for a given game
     * @return Pair of home and away delay of game instances
     */
    private fun getDelayOfGameInstances(game: Game): Pair<Int, Int> {
        if (game.gameType == Game.GameType.SCRIMMAGE) {
            return Pair(0, 0)
        }
        if (game.waitingOn == TeamSide.HOME) {
            val instances = plateAppearanceService.getHomeDelayOfGameInstances(game.id)
            return Pair(instances, 0)
        } else {
            val instances = plateAppearanceService.getAwayDelayOfGameInstances(game.id)
            return Pair(0, instances)
        }
    }

    /**
     * Apply a delay of game to a game in pregame status
     * @param game
     */
    private fun applyPregameDelayOfGame(game: Game): Game {
        game.gameTimer = gameService.calculateDelayOfGameTimer()
        if (game.waitingOn == TeamSide.HOME) {
            // Put runner on 3rd base for away team
            game.runnerOnThird = game.awayBatterLineupSpot
            if (game.gameType != Game.GameType.SCRIMMAGE) {
                for (coach in game.homeCoachDiscordIds!!) {
                    val user = userService.getUserByDiscordId(coach)
                    user.delayOfGameInstances += 1
                    userService.saveUser(user)
                }
            }
        } else {
            // Put runner on 3rd base for home team
            game.runnerOnThird = game.homeBatterLineupSpot
            if (game.gameType != Game.GameType.SCRIMMAGE) {
                for (coach in game.awayCoachDiscordIds!!) {
                    val user = userService.getUserByDiscordId(coach)
                    user.delayOfGameInstances += 1
                    userService.saveUser(user)
                }
            }
        }

        val savedPlateAppearance = saveDelayOfGameOnOffensePlateAppearance(game)
        game.currentPlateAppearanceId = savedPlateAppearance.id
        gameService.saveGame(game)
        scorebugService.generateScorebug(game)
        return game
    }

    /**
     * Apply a delay of game to a game
     * @param game
     */
    private fun applyDelayOfGame(game: Game): Game {
        game.gameTimer = gameService.calculateDelayOfGameTimer()
        if (game.waitingOn == TeamSide.HOME) {
            // Put runner on 3rd base for away team
            game.runnerOnThird = game.awayBatterLineupSpot

            if (game.gameType != Game.GameType.SCRIMMAGE) {
                for (coach in game.homeCoachDiscordIds!!) {
                    val user = userService.getUserByDiscordId(coach)
                    user.delayOfGameInstances += 1
                    userService.saveUser(user)
                }
            }
        } else {
            // Put runner on 3rd base for home team
            game.runnerOnThird = game.homeBatterLineupSpot

            if (game.gameType != Game.GameType.SCRIMMAGE) {
                for (coach in game.awayCoachDiscordIds!!) {
                    val user = userService.getUserByDiscordId(coach)
                    user.delayOfGameInstances += 1
                    userService.saveUser(user)
                }
            }
        }

        val currentPlateAppearance =
            try {
                plateAppearanceService.getCurrentPlateAppearance(game.id)
            } catch (e: Exception) {
                null
            }

        val savedPlateAppearance =
            if (currentPlateAppearance != null) {
                saveDelayOfGameOnDefensePlateAppearance(game, currentPlateAppearance)
            } else {
                saveDelayOfGameOnOffensePlateAppearance(game)
            }

        game.currentPlateAppearanceId = savedPlateAppearance.id
        game.gameWarned = false
        gameService.saveGame(game)
        scorebugService.generateScorebug(game)
        return game
    }

    /**
     * Save a delay of game on defense plate appearance, as defense has called a number
     */
    private fun saveDelayOfGameOnDefensePlateAppearance(
        game: Game,
        plateAppearance: PlateAppearance,
    ): PlateAppearance {
        plateAppearance.plateAppearanceFinished = true
        plateAppearance.batterNumberSubmission = null
        plateAppearance.pitcherNumberSubmission = null
        plateAppearance.difference = null
        if (game.waitingOn == TeamSide.HOME) {
            plateAppearance.result = Scenario.DELAY_OF_GAME_HOME
            plateAppearance.actualResult = ActualResult.DELAY_OF_GAME
        } else {
            plateAppearance.result = Scenario.DELAY_OF_GAME_AWAY
            plateAppearance.actualResult = ActualResult.DELAY_OF_GAME
        }
        return plateAppearanceRepository.save(plateAppearance)
    }

    /**
     * Save a delay of game on offense plate appearance, as defense hasn't called a number
     * @param game
     */
    private fun saveDelayOfGameOnOffensePlateAppearance(game: Game): PlateAppearance {
        // Create a new plate appearance for delay of game
        val plateAppearance = PlateAppearance()
        plateAppearance.gameId = game.id
        plateAppearance.homeTeam = game.homeTeam
        plateAppearance.awayTeam = game.awayTeam
        plateAppearance.homeScore = game.homeScore
        plateAppearance.awayScore = game.awayScore
        plateAppearance.inningHalf = game.inningHalf
        plateAppearance.inning = game.inning
        plateAppearance.outs = game.outs
        plateAppearance.pitchingTeam = if (game.inningHalf == Game.InningHalf.TOP) game.homeTeam else game.awayTeam
        plateAppearance.battingTeam = if (game.inningHalf == Game.InningHalf.TOP) game.awayTeam else game.homeTeam
        plateAppearance.plateAppearanceFinished = true
        plateAppearance.batterNumberSubmission = null
        plateAppearance.pitcherNumberSubmission = null
        plateAppearance.difference = null
        if (game.waitingOn == TeamSide.HOME) {
            plateAppearance.result = Scenario.DELAY_OF_GAME_HOME
            plateAppearance.actualResult = ActualResult.DELAY_OF_GAME
        } else {
            plateAppearance.result = Scenario.DELAY_OF_GAME_AWAY
            plateAppearance.actualResult = ActualResult.DELAY_OF_GAME
        }
        return plateAppearanceRepository.save(plateAppearance)
    }
}
