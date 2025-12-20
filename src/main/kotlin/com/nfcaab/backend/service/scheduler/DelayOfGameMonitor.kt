package com.nfcaab.backend.service.scheduler

import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.enums.team.TeamSide
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.service.discord.DiscordService
import com.nfcaab.backend.service.nfcaab.GameService
import com.nfcaab.backend.service.nfcaab.AtBatService
import com.nfcaab.backend.service.nfcaab.ScorebugService
import com.nfcaab.backend.service.nfcaab.UserService
import com.nfcaab.backend.util.Logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DelayOfGameMonitor(
    private val gameService: GameService,
    private val userService: UserService,
    private val atBatService: AtBatService,
    private val discordService: DiscordService,
    private val scorebugService: ScorebugService,
    private val atBatRepository: AtBatRepository,
) {
    /**
     * Checks for delay of game every minute
     */
    @Scheduled(fixedRate = 60000)
    fun checkForDelayOfGame() {
        val warnedGames = gameService.findGamesToWarn()
        warnedGames.forEach { game ->
            discordService.notifyWarning(game)
            gameService.updateGameAsWarned(game.id)
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
            val instances = atBatService.getHomeDelayOfGameInstances(game.id)
            return Pair(instances, 0)
        } else {
            val instances = atBatService.getAwayDelayOfGameInstances(game.id)
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
                val user = userService.getUserByDiscordId(game.homeCoachDiscordId)
                user.delayOfGameInstances += 1
                userService.saveUser(user)
            }
        } else {
            // Put runner on 3rd base for home team
            game.runnerOnThird = game.homeBatterLineupSpot
            if (game.gameType != Game.GameType.SCRIMMAGE) {
                val user = userService.getUserByDiscordId(game.awayCoachDiscordId)
                user.delayOfGameInstances += 1
                userService.saveUser(user)
            }
        }

        val savedAtBat = saveDelayOfGameOnOffenseAtBat(game)
        game.currentAtBatId = savedAtBat.id
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
                val user = userService.getUserByDiscordId(game.homeCoachDiscordId)
                user.delayOfGameInstances += 1
                userService.saveUser(user)
            }
        } else {
            // Put runner on 3rd base for home team
            game.runnerOnThird = game.homeBatterLineupSpot

            if (game.gameType != Game.GameType.SCRIMMAGE) {
                val user = userService.getUserByDiscordId(game.awayCoachDiscordId)
                user.delayOfGameInstances += 1
                userService.saveUser(user)
            }
        }

        val currentAtBat =
            try {
                atBatService.getCurrentAtBat(game.id)
            } catch (e: Exception) {
                null
            }

        val savedAtBat =
            if (currentAtBat != null) {
                saveDelayOfGameOnDefenseAtBat(game, currentAtBat)
            } else {
                saveDelayOfGameOnOffenseAtBat(game)
            }

        game.currentAtBatId = savedAtBat.id
        game.gameWarned = false
        gameService.saveGame(game)
        scorebugService.generateScorebug(game)
        return game
    }

    /**
     * Save a delay of game on defense plate appearance, as defense has called a number
     */
    private fun saveDelayOfGameOnDefenseAtBat(
        game: Game,
        atBat: AtBat,
    ): AtBat {
        atBat.atBatFinished = true
        atBat.batterNumberSubmission = null
        atBat.pitcherNumberSubmission = null
        atBat.difference = null
        if (game.waitingOn == TeamSide.HOME) {
            atBat.result = Scenario.DELAY_OF_GAME_HOME
            atBat.actualResult = ActualResult.DELAY_OF_GAME
        } else {
            atBat.result = Scenario.DELAY_OF_GAME_AWAY
            atBat.actualResult = ActualResult.DELAY_OF_GAME
        }
        return atBatRepository.save(atBat)
    }

    /**
     * Save a delay of game on offense plate appearance, as defense hasn't called a number
     * @param game
     */
    private fun saveDelayOfGameOnOffenseAtBat(game: Game): AtBat {
        // Create a new plate appearance for delay of game
        val atBat = AtBat()
        atBat.gameId = game.id
        atBat.homeTeam = game.homeTeam
        atBat.awayTeam = game.awayTeam
        atBat.homeScore = game.homeScore
        atBat.awayScore = game.awayScore
        atBat.inningHalf = game.inningHalf
        atBat.inning = game.inning
        atBat.outs = game.outs
        atBat.pitchingTeam = if (game.inningHalf == Game.InningHalf.TOP) game.homeTeam else game.awayTeam
        atBat.battingTeam = if (game.inningHalf == Game.InningHalf.TOP) game.awayTeam else game.homeTeam
        atBat.atBatFinished = true
        atBat.batterNumberSubmission = null
        atBat.pitcherNumberSubmission = null
        atBat.difference = null
        if (game.waitingOn == TeamSide.HOME) {
            atBat.result = Scenario.DELAY_OF_GAME_HOME
            atBat.actualResult = ActualResult.DELAY_OF_GAME
        } else {
            atBat.result = Scenario.DELAY_OF_GAME_AWAY
            atBat.actualResult = ActualResult.DELAY_OF_GAME
        }
        return atBatRepository.save(atBat)
    }
}
