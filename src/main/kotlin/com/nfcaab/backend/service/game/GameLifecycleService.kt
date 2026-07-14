package com.nfcaab.backend.service.game

import com.nfcaab.backend.dto.AtBatOutcome
import com.nfcaab.backend.dto.requests.StartRequest
import com.nfcaab.backend.enums.team.Subdivision
import com.nfcaab.backend.enums.team.TeamSide
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.GameStatus
import com.nfcaab.backend.model.Game.GameType
import com.nfcaab.backend.model.Game.InningHalf.BOTTOM
import com.nfcaab.backend.model.Game.InningHalf.TOP
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.service.discord.DiscordService
import com.nfcaab.backend.service.lineup.LineupService
import com.nfcaab.backend.service.schedule.ScheduleService
import com.nfcaab.backend.service.schedule.SeasonService
import com.nfcaab.backend.service.stats.GameStatsService
import com.nfcaab.backend.service.team.TeamService
import com.nfcaab.backend.service.user.UserService
import com.nfcaab.backend.util.Logger
import com.nfcaab.backend.util.NoCoachDiscordIdFoundException
import com.nfcaab.backend.util.NoCoachFoundException
import com.nfcaab.backend.util.PlayerNotFoundException
import com.nfcaab.backend.util.TeamNotFoundException
import com.nfcaab.backend.util.UnableToCreateGameThreadException
import com.nfcaab.backend.util.UnableToDeleteGameException
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.math.abs

@Service
class GameLifecycleService(
    private val gameRepository: GameRepository,
    private val atBatRepository: AtBatRepository,
    private val teamService: TeamService,
    private val discordService: DiscordService,
    private val userService: UserService,
    private val gameStatsService: GameStatsService,
    private val seasonService: SeasonService,
    private val scheduleService: ScheduleService,
    private val lineupService: LineupService,
    private val gameService: GameService,
) {
    suspend fun startSingleGame(
        startRequest: StartRequest,
        week: Int?,
    ): Game {
        val game = startGame(startRequest, week)
        if (startRequest.gameType != GameType.SCRIMMAGE) {
            scheduleService.markManuallyStartedGameAsStarted(game)
        }
        return game
    }

    suspend fun startGame(
        startRequest: StartRequest,
        week: Int?,
    ): Game {
        try {
            val homeTeamData = teamService.getTeamByName(startRequest.homeTeam)
            val awayTeamData = teamService.getTeamByName(startRequest.awayTeam)

            val formattedDateTime = gameService.calculateDelayOfGameTimer()

            val homeTeam = homeTeamData.name
            val awayTeam = awayTeamData.name

            val homeCoachUsername = homeTeamData.coachUsername ?: throw NoCoachFoundException()
            val awayCoachUsername = awayTeamData.coachUsername ?: throw NoCoachFoundException()
            val homeCoachDiscordId = homeTeamData.coachDiscordId ?: throw NoCoachDiscordIdFoundException()
            val awayCoachDiscordId = awayTeamData.coachDiscordId ?: throw NoCoachDiscordIdFoundException()

            val subdivision = startRequest.subdivision

            var (season, currentWeek) =
                if (startRequest.gameType != GameType.SCRIMMAGE) {
                    seasonService.getCurrentSeason().seasonNumber to seasonService.getCurrentSeason().currentWeek
                } else {
                    null to null
                }

            if (week != null) {
                currentWeek = week
            }

            val newGame =
                gameService.saveGame(
                    Game(
                        gameThreadId = null,
                        requestMessageId = null,
                        subdivision = subdivision,
                        season = season,
                        week = currentWeek,
                        seriesGameNumber = startRequest.seriesGameNumber,
                        gameType = startRequest.gameType,
                        homeTeam = homeTeam,
                        awayTeam = awayTeam,
                        homeWins = homeTeamData.currentWins,
                        homeLosses = homeTeamData.currentLosses,
                        awayWins = awayTeamData.currentWins,
                        awayLosses = awayTeamData.currentLosses,
                        homeTeamRank = homeTeamData.ranking,
                        awayTeamRank = awayTeamData.ranking,
                        homeCoach = homeCoachUsername,
                        awayCoach = awayCoachUsername,
                        homeCoachDiscordId = homeCoachDiscordId,
                        awayCoachDiscordId = awayCoachDiscordId,
                        homeScore = 0,
                        awayScore = 0,
                        waitingOn = TeamSide.HOME,
                        inningHalf = TOP,
                        inning = 1,
                        outs = 0,
                        runnerOnFirst = null,
                        runnerOnSecond = null,
                        runnerOnThird = null,
                        homeBatterLineupSpot = 1,
                        awayBatterLineupSpot = 1,
                        batterName = null,
                        batterUniformNumber = null,
                        pitcherName = null,
                        pitcherUniformNumber = null,
                        numAtBat = 1,
                        currentAtBatId = null,
                        gameWarned = false,
                        gameTimer = formattedDateTime,
                        timestamp = Instant.now(),
                        lastMessageTimestamp = Instant.now(),
                        closeGame = false,
                        closeGamePinged = false,
                        upsetAlert = false,
                        upsetAlertPinged = false,
                        gameStatus = GameStatus.PREGAME,
                    ),
                )

            val discordData =
                discordService.startGameThread(newGame)
                    ?: run {
                        deleteOngoingGame(
                            newGame.gameThreadId?.toULong()
                                ?: throw UnableToDeleteGameException(),
                        )
                        throw UnableToCreateGameThreadException()
                    }

            if (discordData[0] == "null") {
                deleteOngoingGame(
                    newGame.gameThreadId?.toULong()
                        ?: throw UnableToDeleteGameException(),
                )
                throw UnableToCreateGameThreadException()
            }

            newGame.gameThreadId = discordData[0]
            newGame.requestMessageId = discordData[1]

            gameService.saveGame(newGame)
            gameStatsService.createGameStats(newGame)

            Logger.info("Game started: ${newGame.homeTeam} vs ${newGame.awayTeam}")
            return newGame
        } catch (e: Exception) {
            Logger.error("Error starting ${startRequest.homeTeam} vs ${startRequest.awayTeam}: " + e.message!!)
            throw e
        }
    }

    fun updateGameValues(
        game: Game,
        outcome: AtBatOutcome,
        advanceLineupSpot: Boolean = true,
    ): Game {
        if (advanceLineupSpot) {
            if (game.inningHalf == TOP) {
                game.awayBatterLineupSpot = if (game.awayBatterLineupSpot == 9) 1 else game.awayBatterLineupSpot + 1
            } else {
                game.homeBatterLineupSpot = if (game.homeBatterLineupSpot == 9) 1 else game.homeBatterLineupSpot + 1
            }
        }
        if (outcome.outs >= 3) {
            val inningHalf = if (game.inningHalf == TOP) BOTTOM else TOP
            val inning = game.inning + if (inningHalf == TOP) 1 else 0

            if (game.inning >= 9 && inningHalf == BOTTOM && outcome.homeScore > outcome.awayScore) {
                game.gameStatus = GameStatus.FINAL
            } else if (inning > 9 && inningHalf == TOP) {
                if (outcome.homeScore > outcome.awayScore || outcome.awayScore > outcome.homeScore) {
                    game.gameStatus = GameStatus.FINAL
                } else {
                    game.gameStatus = GameStatus.EXTRA_INNINGS
                }
            }

            game.inning = inning
            game.inningHalf = inningHalf
            game.outs = 0
            game.runnerOnFirst = null
            game.runnerOnSecond = null
            game.runnerOnThird = null
        } else {
            game.outs = outcome.outs
            game.runnerOnFirst = outcome.runnerOnFirstAfter?.uniformNumber
            game.runnerOnSecond = outcome.runnerOnSecondAfter?.uniformNumber
            game.runnerOnThird = outcome.runnerOnThirdAfter?.uniformNumber
        }

        updateWaitingOn(game)
        updateBattersAndPitchers(game)
        game.homeScore = outcome.homeScore
        game.awayScore = outcome.awayScore
        game.numAtBat += 1
        game.gameWarned = false
        game.gameTimer = gameService.calculateDelayOfGameTimer()
        updateCloseGame(game)
        updateUpsetAlert(game)

        if (game.gameStatus != GameStatus.FINAL &&
            game.inningHalf == BOTTOM &&
            game.inning >= 9 &&
            game.homeScore > game.awayScore
        ) {
            game.gameStatus = GameStatus.FINAL
        }

        if (game.gameStatus == GameStatus.FINAL) {
            endGame(game)
        }

        return game
    }

    fun updateWithPitcherNumberSubmission(
        game: Game,
        atBat: AtBat,
    ) {
        game.currentAtBatId = atBat.id
        game.waitingOn = if (game.inningHalf == TOP) TeamSide.AWAY else TeamSide.HOME
        game.gameTimer = gameService.calculateDelayOfGameTimer()
        gameRepository.save(game)
    }

    private fun updateBattersAndPitchers(game: Game) {
        val batter: Player
        val pitcher: Player
        if (game.inningHalf == TOP) {
            batter = lineupService.getBatterByLineupSpot(game.id, game.awayBatterLineupSpot, game.awayTeam)
            pitcher = lineupService.getPitcherByTeam(game.id, game.homeTeam)
        } else {
            batter = lineupService.getBatterByLineupSpot(game.id, game.homeBatterLineupSpot, game.homeTeam)
            pitcher = lineupService.getPitcherByTeam(game.id, game.awayTeam)
        }
        game.batterName = "${batter.firstName} ${batter.lastName}"
        game.batterUniformNumber = batter.uniformNumber
        game.pitcherName = "${pitcher.firstName} ${pitcher.lastName}"
        game.pitcherUniformNumber = pitcher.uniformNumber
    }

    private fun updateWaitingOn(game: Game) {
        game.waitingOn = if (game.inningHalf == TOP) TeamSide.HOME else TeamSide.AWAY
    }

    private fun updateCloseGame(game: Game) {
        game.closeGame = abs(game.homeScore - game.awayScore) <= 8 && game.inning >= 8
    }

    private fun updateUpsetAlert(game: Game) {
        val homeTeam = teamService.getTeamByName(game.homeTeam)
        val awayTeam = teamService.getTeamByName(game.awayTeam)

        val homeTeamRanking = homeTeam.ranking ?: 100
        val awayTeamRanking = awayTeam.ranking ?: 100

        if ((
                (game.homeScore <= game.awayScore && homeTeamRanking < awayTeamRanking) ||
                    (game.awayScore <= game.homeScore && awayTeamRanking < homeTeamRanking)
            ) &&
            game.inning >= 8
        ) {
            game.upsetAlert = true
        }
        if ((
                (abs(game.homeScore - game.awayScore) <= 8 && homeTeamRanking < awayTeamRanking) ||
                    (abs(game.awayScore - game.homeScore) <= 8 && awayTeamRanking < homeTeamRanking)
            ) &&
            game.inning >= 8
        ) {
            game.upsetAlert = true
        }
        game.upsetAlert = false
    }

    fun rollbackAtBat(
        game: Game,
        previousAtBat: AtBat,
        gamePlay: AtBat,
    ) {
        try {
            if (gamePlay.actualResult == ActualResult.DELAY_OF_GAME) {
                if (game.waitingOn == TeamSide.HOME) {
                    game.awayScore -= previousAtBat.runsScored
                    if (game.gameType != Game.GameType.SCRIMMAGE) {
                        val user = userService.getUserByDiscordId(game.homeCoachDiscordId)
                        user.delayOfGameInstances -= 1
                        userService.saveUser(user)
                    }
                } else {
                    game.homeScore -= previousAtBat.runsScored
                    if (game.gameType != Game.GameType.SCRIMMAGE) {
                        val user = userService.getUserByDiscordId(game.awayCoachDiscordId)
                        user.delayOfGameInstances -= 1
                        userService.saveUser(user)
                    }
                }
            }
            if (game.gameStatus == GameStatus.FINAL) {
                if (game.inning <= 9) {
                    game.gameStatus = GameStatus.IN_PROGRESS
                } else {
                    game.gameStatus = GameStatus.EXTRA_INNINGS
                }
            }

            val newCurrentPlateAppearance =
                atBatRepository.getCurrentAtBat(game.id)
                    ?: atBatRepository.getPreviousAtBat(game.id)
                    ?: previousAtBat

            game.currentAtBatId = newCurrentPlateAppearance.id
            game.inning = previousAtBat.inning
            game.outs = previousAtBat.outs
            game.inningHalf = previousAtBat.inningHalf
            game.runnerOnFirst = previousAtBat.runnerOnFirst
            game.runnerOnSecond = previousAtBat.runnerOnSecond
            game.runnerOnThird = previousAtBat.runnerOnThird
            game.waitingOn = if (previousAtBat.inningHalf == TOP) TeamSide.HOME else TeamSide.AWAY
            game.gameTimer = gameService.calculateDelayOfGameTimer()
            if (game.inningHalf == TOP) {
                game.awayBatterLineupSpot = if (game.awayBatterLineupSpot == 1) 9 else game.awayBatterLineupSpot - 1
            } else {
                game.homeBatterLineupSpot = if (game.homeBatterLineupSpot == 1) 9 else game.homeBatterLineupSpot - 1
            }
            updateBattersAndPitchers(game)
            gameStatsService.generateGameStats(game.id)
            gameService.saveGame(game)
        } catch (e: Exception) {
            Logger.error("There was an error rolling back the plate appearance for game ${game.id}: " + e.message)
            throw e
        }
    }

    fun endAllGames(): List<Game> {
        val gamesToEnd = gameService.getAllOngoingGames()
        val endedGames = mutableListOf<Game>()
        for (game in gamesToEnd) {
            endedGames.add(endGame(game))
        }
        return endedGames
    }

    fun endDOGOutGame(
        game: Game,
        delayOfGameInstances: Pair<Int, Int>,
    ): Game {
        if (delayOfGameInstances.first >= 3) {
            game.runnerOnThird = game.awayBatterLineupSpot
        } else if (delayOfGameInstances.second >= 3) {
            game.runnerOnThird = game.homeBatterLineupSpot
        }
        val updatedGame = gameService.saveGame(game)
        endGame(updatedGame)
        return game
    }

    fun endSingleGame(channelId: ULong): Game {
        val game = gameService.getGameByPlatformId(channelId)
        return endGame(game)
    }

    fun endSingleGameByGameId(gameId: Int): Game {
        val game = gameService.getGameById(gameId)
        return endGame(game)
    }

    private fun endGame(game: Game): Game {
        try {
            game.gameStatus = GameStatus.FINAL
            if (game.gameType != GameType.SCRIMMAGE) {
                teamService.updateTeamWinsAndLosses(game)
                userService.updateUserWinsAndLosses(game)

                val homeUser = userService.getUserByDiscordId(game.homeCoachDiscordId)
                val awayUser = userService.getUserByDiscordId(game.awayCoachDiscordId)
                val userList = listOf(homeUser, awayUser)
                for (user in userList) {
                    val responseTime =
                        atBatRepository.getUserAverageResponseTime(
                            user.discordTag,
                            seasonService.getCurrentSeason().seasonNumber,
                        ) ?: throw Exception("Could not get average response time for user ${user.username}")
                    userService.updateUserAverageResponseTime(user.id, responseTime)
                }

                scheduleService.markGameAsFinished(game)
            }
            gameService.saveGame(game)

            gameStatsService.deleteByGameId(game.id)
            val allPlays = atBatRepository.getAllAtBatsByGameId(game.id)
            gameStatsService.updateGameStats(game, allPlays)
            val homeStats = gameStatsService.getGameStatsByIdAndTeam(game.id, game.homeTeam)
            val awayStats = gameStatsService.getGameStatsByIdAndTeam(game.id, game.awayTeam)
            homeStats.gameStatus = GameStatus.FINAL
            awayStats.gameStatus = GameStatus.FINAL
            gameStatsService.saveGameStats(homeStats)
            gameStatsService.saveGameStats(awayStats)

            if (game.gameType != GameType.SCRIMMAGE && game.season != null) {
                gameStatsService.aggregateStatsAfterGame(game)
            }

            Logger.info("Game ${game.id} ended")
            return game
        } catch (e: Exception) {
            Logger.error("Error in ${game.id}: " + e.message!!)
            throw e
        }
    }

    suspend fun restartGame(channelId: ULong): Game {
        val game = gameService.getGameByPlatformId(channelId)
        deleteOngoingGame(channelId)
        val startRequest =
            StartRequest(
                game.subdivision ?: Subdivision.NFCAAB,
                game.homeTeam,
                game.awayTeam,
                game.gameType,
                game.seriesGameNumber,
            )
        return startGame(startRequest, game.week)
    }

    fun deleteOngoingGame(channelId: ULong): Boolean {
        val game = gameService.getGameByPlatformId(channelId)
        val gameId = game.id
        gameRepository.deleteById(gameId)
        gameStatsService.deleteByGameId(gameId)
        atBatRepository.deleteAllAtBatsByGameId(gameId)
        Logger.info("Game $gameId deleted")
        return true
    }

    fun subCoachIntoGame(
        id: Int,
        team: String,
        discordId: String,
    ): Game {
        val game = gameService.getGameById(id)
        val userData = userService.getUserDTOByDiscordId(discordId)
        val coach = userData.discordTag

        when (team.lowercase()) {
            game.homeTeam.lowercase() -> {
                game.homeCoach = coach
                game.homeCoachDiscordId = userData.discordId
            }
            game.awayTeam.lowercase() -> {
                game.awayCoach = coach
                game.awayCoachDiscordId = userData.discordId
            }
            else -> {
                throw TeamNotFoundException("$team not found in game $id")
            }
        }
        return gameService.saveGame(game)
    }

    fun pinchRun(
        gameId: Int,
        team: String,
        base: Game.Base,
        incomingUniformNumber: Int,
    ): Game {
        val game = gameService.getGameById(gameId)
        val outgoingUniformNumber =
            when (base) {
                Game.Base.FIRST -> game.runnerOnFirst
                Game.Base.SECOND -> game.runnerOnSecond
                Game.Base.THIRD -> game.runnerOnThird
            } ?: throw PlayerNotFoundException("No runner on $base for game $gameId")

        val position = lineupService.getCurrentPosition(gameId, team, outgoingUniformNumber)
        lineupService.substituteBatter(gameId, team, outgoingUniformNumber, incomingUniformNumber, position)

        when (base) {
            Game.Base.FIRST -> game.runnerOnFirst = incomingUniformNumber
            Game.Base.SECOND -> game.runnerOnSecond = incomingUniformNumber
            Game.Base.THIRD -> game.runnerOnThird = incomingUniformNumber
        }

        return gameService.saveGame(game)
    }
}
