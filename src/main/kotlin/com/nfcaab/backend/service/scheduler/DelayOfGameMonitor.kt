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
import com.nfcaab.backend.service.lineup.LineupService

@Service
class DelayOfGameMonitor(
    private val gameService: GameService,
    private val gameLifecycleService: GameLifecycleService,
    private val userService: UserService,
    private val atBatService: AtBatService,
    private val discordService: DiscordService,
    private val scorebugService: ScorebugService,
    private val atBatRepository: AtBatRepository,
    private val lineupService: LineupService,
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
            val delayingSide = game.waitingOn
            val updatedGame =
                if (game.gameStatus == Game.GameStatus.PREGAME) {
                    applyPregameDelayOfGame(game)
                } else {
                    applyDelayOfGame(game)
                }
            val delayOfGameInstances = getDelayOfGameInstances(updatedGame, delayingSide)
            val isDelayOfGameOut = delayOfGameInstances.first >= 3 || delayOfGameInstances.second >= 3
            if (isDelayOfGameOut) {
                gameLifecycleService.endDOGOutGame(updatedGame, delayOfGameInstances)
            }
            discordService.notifyDelayOfGame(updatedGame, isDelayOfGameOut)
            Logger.info("A delay of game for game ${game.id} has been processed")
        }
    }

    private fun getDelayOfGameInstances(
        game: Game,
        delayingSide: TeamSide?,
    ): Pair<Int, Int> {
        if (game.gameType == Game.GameType.SCRIMMAGE) {
            return Pair(0, 0)
        }
        if (delayingSide == TeamSide.HOME) {
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

        val currentAtBat = atBatService.getCurrentAtBatOrNull(game.id)

        val savedAtBat =
            if (currentAtBat != null) {
                applyBatterDelayOfGame(game, currentAtBat)
            } else {
                val runsScored = applyDelayOfGameHomeRun(game)
                if (game.inningHalf == Game.InningHalf.BOTTOM &&
                    game.inning >= 9 &&
                    game.homeScore > game.awayScore
                ) {
                    game.gameStatus = Game.GameStatus.FINAL
                }
                saveDelayOfGameOnOffenseAtBat(game, runsScored)
            }

        game.currentAtBatId = savedAtBat.id
        game.gameWarned = false
        gameService.saveGame(game)

        if (game.gameStatus == Game.GameStatus.FINAL) {
            gameLifecycleService.endSingleGameByGameId(game.id)
        }

        scorebugService.generateScorebug(game)
        return game
    }

    private fun applyBatterDelayOfGame(
        game: Game,
        pendingAtBat: AtBat,
    ): AtBat {
        val outsToCharge = minOf(2, 3 - game.outs)

        var savedAtBat = finalizeDelayOfGameStrikeout(game, pendingAtBat)
        game.outs += 1
        advanceBattingLineupSpot(game)

        if (outsToCharge == 2) {
            savedAtBat = atBatRepository.save(buildDelayOfGameStrikeoutAtBat(game))
            game.outs += 1
            advanceBattingLineupSpot(game)
        }

        if (game.outs >= 3) {
            advanceHalfInning(game)
        } else {
            game.waitingOn = if (game.inningHalf == Game.InningHalf.TOP) TeamSide.HOME else TeamSide.AWAY
        }

        return savedAtBat
    }

    private fun advanceBattingLineupSpot(game: Game) {
        if (game.inningHalf == Game.InningHalf.TOP) {
            game.awayBatterLineupSpot = if (game.awayBatterLineupSpot == 9) 1 else game.awayBatterLineupSpot + 1
        } else {
            game.homeBatterLineupSpot = if (game.homeBatterLineupSpot == 9) 1 else game.homeBatterLineupSpot + 1
        }
    }

    private fun advanceHalfInning(game: Game) {
        val inningHalf = if (game.inningHalf == Game.InningHalf.TOP) Game.InningHalf.BOTTOM else Game.InningHalf.TOP
        val inning = game.inning + if (inningHalf == Game.InningHalf.TOP) 1 else 0

        if (game.inning >= 9 && inningHalf == Game.InningHalf.BOTTOM && game.homeScore > game.awayScore) {
            game.gameStatus = Game.GameStatus.FINAL
        } else if (inning > 9 && inningHalf == Game.InningHalf.TOP) {
            game.gameStatus =
                if (game.homeScore != game.awayScore) {
                    Game.GameStatus.FINAL
                } else {
                    Game.GameStatus.EXTRA_INNINGS
                }
        }

        game.inning = inning
        game.inningHalf = inningHalf
        game.outs = 0
        game.runnerOnFirst = null
        game.runnerOnSecond = null
        game.runnerOnThird = null
        game.waitingOn = if (inningHalf == Game.InningHalf.TOP) TeamSide.HOME else TeamSide.AWAY
    }

    private fun finalizeDelayOfGameStrikeout(
        game: Game,
        atBat: AtBat,
    ): AtBat {
        atBat.atBatFinished = true
        atBat.batterNumberSubmission = null
        atBat.pitcherNumberSubmission = null
        atBat.difference = null
        atBat.runsScored = 0
        atBat.homeScore = game.homeScore
        atBat.awayScore = game.awayScore
        atBat.actualResult = ActualResult.STRIKEOUT
        if (game.waitingOn == TeamSide.HOME) {
            atBat.result = Scenario.DELAY_OF_GAME_HOME
        } else {
            atBat.result = Scenario.DELAY_OF_GAME_AWAY
        }
        return atBatRepository.save(atBat)
    }

    private fun buildDelayOfGameStrikeoutAtBat(game: Game): AtBat {
        val battingTeam = if (game.inningHalf == Game.InningHalf.TOP) game.awayTeam else game.homeTeam
        val pitchingTeam = if (game.inningHalf == Game.InningHalf.TOP) game.homeTeam else game.awayTeam
        val lineupSpot = if (game.inningHalf == Game.InningHalf.TOP) game.awayBatterLineupSpot else game.homeBatterLineupSpot
        val batter = lineupService.getBatterByLineupSpot(game.id, lineupSpot, battingTeam)
        val pitcher = lineupService.getPitcherByTeam(game.id, pitchingTeam)

        val atBat = AtBat()
        atBat.gameId = game.id
        atBat.homeTeam = game.homeTeam
        atBat.awayTeam = game.awayTeam
        atBat.homeScore = game.homeScore
        atBat.awayScore = game.awayScore
        atBat.inningHalf = game.inningHalf
        atBat.inning = game.inning
        atBat.outs = game.outs
        atBat.pitchingTeam = pitchingTeam
        atBat.battingTeam = battingTeam
        atBat.lineupSpot = lineupSpot
        atBat.batterName = "${batter.firstName} ${batter.lastName}"
        atBat.batterUniformNumber = batter.uniformNumber
        atBat.pitcherName = "${pitcher.firstName} ${pitcher.lastName}"
        atBat.pitcherUniformNumber = pitcher.uniformNumber
        atBat.atBatFinished = true
        atBat.runsScored = 0
        atBat.actualResult = ActualResult.STRIKEOUT
        atBat.result = if (game.waitingOn == TeamSide.HOME) Scenario.DELAY_OF_GAME_HOME else Scenario.DELAY_OF_GAME_AWAY
        return atBat
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
