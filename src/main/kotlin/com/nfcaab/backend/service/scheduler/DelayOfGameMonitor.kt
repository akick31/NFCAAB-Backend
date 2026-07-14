package com.nfcaab.backend.service.scheduler

import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.enums.team.TeamSide
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.service.discord.DiscordService
import com.nfcaab.backend.util.Logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import com.nfcaab.backend.service.user.UserService
import com.nfcaab.backend.service.scorebug.ScorebugService
import com.nfcaab.backend.service.game.GameLifecycleService
import com.nfcaab.backend.service.game.GameService
import com.nfcaab.backend.service.atbat.AtBatService

@Service
class DelayOfGameMonitor(
    private val gameService: GameService,
    private val gameLifecycleService: GameLifecycleService,
    private val userService: UserService,
    private val atBatService: AtBatService,
    private val discordService: DiscordService,
    private val scorebugService: ScorebugService,
    private val atBatRepository: AtBatRepository,
) {
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
                gameLifecycleService.endDOGOutGame(updatedGame, delayOfGameInstances)
            }
            discordService.notifyDelayOfGame(updatedGame, isDelayOfGameOut)
            Logger.info("A delay of game for game ${game.id} has been processed")
        }
    }

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

    private fun applyDelayOfGameHomeRun(game: Game): Int {
        val runsScored =
            1 +
                listOfNotNull(game.runnerOnFirst, game.runnerOnSecond, game.runnerOnThird).size
        game.runnerOnFirst = null
        game.runnerOnSecond = null
        game.runnerOnThird = null
        if (game.inningHalf == Game.InningHalf.TOP) {
            game.awayScore += runsScored
        } else {
            game.homeScore += runsScored
        }
        return runsScored
    }

    private fun applyPregameDelayOfGame(game: Game): Game {
        game.gameTimer = gameService.calculateDelayOfGameTimer()
        if (game.waitingOn == TeamSide.HOME) {
            if (game.gameType != Game.GameType.SCRIMMAGE) {
                val user = userService.getUserByDiscordId(game.homeCoachDiscordId)
                user.delayOfGameInstances += 1
                userService.saveUser(user)
            }
        } else {
            if (game.gameType != Game.GameType.SCRIMMAGE) {
                val user = userService.getUserByDiscordId(game.awayCoachDiscordId)
                user.delayOfGameInstances += 1
                userService.saveUser(user)
            }
        }
        val runsScored = applyDelayOfGameHomeRun(game)

        val savedAtBat = saveDelayOfGameOnOffenseAtBat(game, runsScored)
        game.currentAtBatId = savedAtBat.id
        gameService.saveGame(game)
        scorebugService.generateScorebug(game)
        return game
    }

    private fun applyDelayOfGame(game: Game): Game {
        game.gameTimer = gameService.calculateDelayOfGameTimer()
        if (game.waitingOn == TeamSide.HOME) {
            if (game.gameType != Game.GameType.SCRIMMAGE) {
                val user = userService.getUserByDiscordId(game.homeCoachDiscordId)
                user.delayOfGameInstances += 1
                userService.saveUser(user)
            }
        } else {
            if (game.gameType != Game.GameType.SCRIMMAGE) {
                val user = userService.getUserByDiscordId(game.awayCoachDiscordId)
                user.delayOfGameInstances += 1
                userService.saveUser(user)
            }
        }
        val runsScored = applyDelayOfGameHomeRun(game)

        val currentAtBat =
            try {
                atBatService.getCurrentAtBat(game.id)
            } catch (e: Exception) {
                null
            }

        val savedAtBat =
            if (currentAtBat != null) {
                saveDelayOfGameOnDefenseAtBat(game, currentAtBat, runsScored)
            } else {
                saveDelayOfGameOnOffenseAtBat(game, runsScored)
            }

        game.currentAtBatId = savedAtBat.id
        game.gameWarned = false
        gameService.saveGame(game)
        scorebugService.generateScorebug(game)
        return game
    }

    private fun saveDelayOfGameOnDefenseAtBat(
        game: Game,
        atBat: AtBat,
        runsScored: Int,
    ): AtBat {
        atBat.atBatFinished = true
        atBat.batterNumberSubmission = null
        atBat.pitcherNumberSubmission = null
        atBat.difference = null
        atBat.runsScored = runsScored
        atBat.homeScore = game.homeScore
        atBat.awayScore = game.awayScore
        if (game.waitingOn == TeamSide.HOME) {
            atBat.result = Scenario.DELAY_OF_GAME_HOME
            atBat.actualResult = ActualResult.DELAY_OF_GAME
        } else {
            atBat.result = Scenario.DELAY_OF_GAME_AWAY
            atBat.actualResult = ActualResult.DELAY_OF_GAME
        }
        return atBatRepository.save(atBat)
    }

    private fun saveDelayOfGameOnOffenseAtBat(
        game: Game,
        runsScored: Int,
    ): AtBat {
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
        atBat.runsScored = runsScored
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
