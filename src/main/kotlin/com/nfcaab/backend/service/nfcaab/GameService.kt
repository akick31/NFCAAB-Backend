package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.BaseCondition
import com.nfcaab.backend.model.Game.GameStatus
import com.nfcaab.backend.model.Game.GameType
import com.nfcaab.backend.model.Game.InningHalf
import com.nfcaab.backend.model.Game.InningHalf.BOTTOM
import com.nfcaab.backend.model.Game.InningHalf.TOP
import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.enums.team.Subdivision
import com.nfcaab.backend.enums.team.TeamSide
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.model.Team
import com.nfcaab.backend.model.User
import com.nfcaab.backend.dto.AtBatOutcome
import com.nfcaab.backend.dto.requests.StartRequest
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.service.discord.DiscordService
import com.nfcaab.backend.service.nfcaab.GameSpecificationService.GameCategory
import com.nfcaab.backend.service.nfcaab.GameSpecificationService.GameFilter
import com.nfcaab.backend.service.nfcaab.GameSpecificationService.GameSort
import com.nfcaab.backend.service.nfcaab.ScheduleService
import com.nfcaab.backend.service.nfcaab.SeasonService
import com.nfcaab.backend.service.nfcaab.TeamService
import com.nfcaab.backend.service.nfcaab.UserService
import com.nfcaab.backend.util.GameNotFoundException
import com.nfcaab.backend.util.Logger
import com.nfcaab.backend.util.NoCoachDiscordIdFoundException
import com.nfcaab.backend.util.NoCoachFoundException
import com.nfcaab.backend.util.NoGameFoundException
import com.nfcaab.backend.util.TeamNotFoundException
import com.nfcaab.backend.util.UnableToCreateGameThreadException
import com.nfcaab.backend.util.UnableToDeleteGameException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import kotlinx.coroutines.delay
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Random
import kotlin.math.abs

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val atBatRepository: AtBatRepository,
    private val teamService: TeamService,
    private val discordService: DiscordService,
    private val userService: UserService,
    private val gameStatsService: GameStatsService,
    private val seasonService: SeasonService,
    private val scheduleService: ScheduleService,
    private val gameSpecificationService: GameSpecificationService,
    private val lineupService: LineupService,
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

    /**
     * Start a game
     * @param startRequest
     * @return
     */
    private suspend fun startGame(
        startRequest: StartRequest,
        week: Int?,
    ): Game {
        try {
            val homeTeamData = teamService.getTeamByName(startRequest.homeTeam)
            val awayTeamData = teamService.getTeamByName(startRequest.awayTeam)

            val formattedDateTime = calculateDelayOfGameTimer()

            // Validate request fields
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

            // Create and save the Game object and Stats object
            val newGame =
                saveGame(
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
                        gameStatus = GameStatus.PREGAME
                    ),
                )

            // Create a new Discord thread
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

            // Save the updated entity and create game stats
            saveGame(newGame)
            gameStatsService.createGameStats(newGame)

            Logger.info("Game started: ${newGame.homeTeam} vs ${newGame.awayTeam}")
            return newGame
        } catch (e: Exception) {
            Logger.error("Error starting ${startRequest.homeTeam} vs ${startRequest.awayTeam}: " + e.message!!)
            throw e
        }
    }

    /**
     * Update game information based on the result of a play
     * @param game
     * @param outcome
     */
    fun updateGameValues(
        game: Game,
        outcome: AtBatOutcome
    ): Game {
        // Before updating innings, update the current lineup spot
        if (game.inningHalf == TOP) {
            game.awayBatterLineupSpot = if (game.awayBatterLineupSpot == 9) 1 else game.awayBatterLineupSpot + 1
        } else {
            game.homeBatterLineupSpot = if (game.homeBatterLineupSpot == 9) 1 else game.homeBatterLineupSpot + 1
        }
        if (outcome.outs > 3) {
            val inningHalf = if (game.inningHalf == TOP) BOTTOM else TOP
            val inning = game.inning + if (inningHalf == TOP) 1 else 0

            // Check after every inning in the 9th or later if over
            if (inning > 9 && inningHalf == TOP) {
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
        }

        updateWaitingOn(game)
        updateBattersAndPitchers(game)
        game.homeScore = outcome.homeScore
        game.awayScore = outcome.awayScore
        game.outs = outcome.outs
        game.runnerOnFirst = outcome.runnerOnFirstAfter?.uniformNumber
        game.runnerOnSecond = outcome.runnerOnSecondAfter?.uniformNumber
        game.runnerOnThird = outcome.runnerOnThirdAfter?.uniformNumber
        game.numAtBat += 1
        game.gameWarned = false
        game.gameTimer = calculateDelayOfGameTimer()
        updateCloseGame(game)
        updateUpsetAlert(game)

        if (game.gameStatus == GameStatus.FINAL) {
            endGame(game)
        }

        return game
    }

    /**
     * Update the game information based on a defensive number submission
     * @param game
     * @param atBat
     */
    fun updateWithPitcherNumberSubmission(
        game: Game,
        atBat: AtBat,
    ) {
        game.currentAtBatId = atBat.id
        game.waitingOn = if (game.inningHalf == TOP) TeamSide.AWAY else TeamSide.HOME
        game.gameTimer = calculateDelayOfGameTimer()
        gameRepository.save(game)
    }

    /**
     * Update the game's batters and pitchers
     * @param game
     */
    private fun updateBattersAndPitchers(game: Game) {
        val batter: Player
        val pitcher: Player
        if (game.inningHalf == TOP) {
            batter = lineupService.getBatterByLineupSpot(
                game.id,
                game.awayBatterLineupSpot,
                game.awayTeam,
            )
            pitcher = lineupService.getPitcherByTeam(
                game.id,
                game.homeTeam
            )
        } else {
            batter = lineupService.getBatterByLineupSpot(
                game.id,
                game.homeBatterLineupSpot,
                game.homeTeam,
            )
            pitcher = lineupService.getPitcherByTeam(
                game.id,
                game.awayTeam
            )
        }
        game.batterName = "${batter.firstName} ${batter.lastName}"
        game.batterUniformNumber = batter.uniformNumber
        game.pitcherName = "${pitcher.firstName} ${pitcher.lastName}"
        game.pitcherUniformNumber = pitcher.uniformNumber
    }

    /**
     * Update the request message id
     * @param id
     * @param requestMessageId
     */
    fun updateRequestMessageId(
        id: Int,
        requestMessageId: String,
    ): Game {
        val game = getGameById(id)
        game.requestMessageId = requestMessageId
        return saveGame(game)
    }

    /**
     * Update the last message timestamp
     * @param id
     * @return Game
     */
    fun updateLastMessageTimestamp(id: Int): Game {
        val game = getGameById(id)
        val timestamp = Instant.now()
        game.lastMessageTimestamp = timestamp
        return saveGame(game)
    }

    /**
     * Update the team the game is waiting on
     * @param game
     */
    private fun updateWaitingOn(
        game: Game,
    ) {
        if (game.inningHalf == TOP) {
            game.waitingOn = TeamSide.HOME
        } else {
            game.waitingOn = TeamSide.AWAY
        }
    }

    /**
     * Determines if the game is close
     * @param game the game
     */
    private fun updateCloseGame(
        game: Game,
    ) {
        game.closeGame = abs(game.homeScore - game.awayScore) <= 8 &&
                game.inning >= 8
    }

    /**
     * Determine if there is an upset alert. A game is an upset alert
     * if at least one of the teams is ranked and the game is close
     * @param game the game
     */
    private fun updateUpsetAlert(
        game: Game,
    ) {
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

    /**
     * Rollback the play to the previous play
     * @param game
     * @param previousAtBat
     * @param gamePlay
     */
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

            val newCurrentPlateAppearanec =
                atBatRepository.getCurrentAtBat(game.id)
                    ?: atBatRepository.getPreviousAtBat(game.id)
                    ?: previousAtBat

            game.currentAtBatId = newCurrentPlateAppearanec.id
            game.inning = previousAtBat.inning
            game.outs = previousAtBat.outs
            game.inningHalf = previousAtBat.inningHalf
            game.runnerOnFirst = previousAtBat.runnerOnFirst
            game.runnerOnSecond = previousAtBat.runnerOnSecond
            game.runnerOnThird = previousAtBat.runnerOnThird
            game.waitingOn = if (previousAtBat.inningHalf == TOP) TeamSide.HOME else TeamSide.AWAY
            game.gameTimer = calculateDelayOfGameTimer()
            if (game.inningHalf == TOP) {
                game.awayBatterLineupSpot = if (game.awayBatterLineupSpot == 1) 9 else game.awayBatterLineupSpot - 1
            } else {
                game.homeBatterLineupSpot = if (game.homeBatterLineupSpot == 1) 9 else game.homeBatterLineupSpot - 1
            }
            updateBattersAndPitchers(game)
            gameStatsService.generateGameStats(game.id)
            saveGame(game)
        } catch (e: Exception) {
            Logger.error("There was an error rolling back the plate appearance for game ${game.id}: " + e.message)
            throw e
        }
    }

    /**
     * Start all games for the given week
     * @param season
     * @param week
     * @return
     */
    suspend fun startWeek(
        season: Int,
        week: Int,
    ): List<Game> {
        val gamesToStart =
            scheduleService.getGamesToStartBySeasonAndWeek(season, week) ?: run {
                Logger.error("No games found for season $season week $week")
                throw NoGameFoundException()
            }
        val startedGames = mutableListOf<Game>()
        var count = 0
        for (game in gamesToStart) {
            try {
                if (count >= 25) {
                    delay(300000)
                    count = 0
                    Logger.info("Block of 25 games started, delaying for 5 minutes")
                }
                val startedGame =
                    startGame(
                        StartRequest(
                            game.subdivision,
                            game.homeTeam,
                            game.awayTeam,
                            game.gameType,
                            1
                        ),
                        week,
                    )
                startedGames.add(startedGame)
                scheduleService.markGameAsStarted(game)
                count += 1
            } catch (e: Exception) {
                Logger.error("Error starting ${game.homeTeam} vs ${game.awayTeam}: " + e.message!!)
                continue
            }
        }
        return startedGames
    }

    /**
     * End all ongoing games
     */
    fun endAllGames(): List<Game> {
        val gamesToEnd = getAllOngoingGames()
        val endedGames = mutableListOf<Game>()
        for (game in gamesToEnd) {
            endedGames.add(endGame(game))
        }
        return endedGames
    }

    /**
     * End a game with a delay of game out
     */
    fun endDOGOutGame(
        game: Game,
        delayOfGameInstances: Pair<Int, Int>,
    ): Game {
        // If the home team has 3 delay of game instances, they lose,
        // so put a runner on 3rd base for the away team
        if (delayOfGameInstances.first >= 3) {
            game.runnerOnThird = game.awayBatterLineupSpot
        } else if (delayOfGameInstances.second >= 3) {
            game.runnerOnThird = game.homeBatterLineupSpot
        }
        val updatedGame = saveGame(game)
        endGame(updatedGame)
        return game
    }

    /**
     * End a single game
     * @param channelId
     * @return
     */
    fun endSingleGame(channelId: ULong): Game {
        val game = getGameByPlatformId(channelId)
        return endGame(game)
    }

    /**
     * End a single game by game ID
     * @param gameId
     * @return
     */
    fun endSingleGameByGameId(gameId: Int): Game {
        val game = getGameById(gameId)
        return endGame(game)
    }

    /**
     * End a game
     * @param game
     * @return
     */
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
//            if (game.gameType == GameType.COLLEGE_WORLD_SERIES_CHAMPIONSHIP) {
//                seasonService.endSeason(game)
//            }
            saveGame(game)

            // Update the game stats one last time
            gameStatsService.deleteByGameId(game.id)
            val allPlays = atBatRepository.getAllAtBatsByGameId(game.id)
            gameStatsService.updateGameStats(
                game,
                allPlays,
            )
            val homeStats = gameStatsService.getGameStatsByIdAndTeam(game.id, game.homeTeam)
            val awayStats = gameStatsService.getGameStatsByIdAndTeam(game.id, game.awayTeam)
            homeStats.gameStatus = GameStatus.FINAL
            awayStats.gameStatus = GameStatus.FINAL
            gameStatsService.saveGameStats(homeStats)
            gameStatsService.saveGameStats(awayStats)
            
            // Aggregate stats: Game -> Season -> Conference -> League
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

    /**
     * Restart a game
     * @param channelId
     * @return
     */
    suspend fun restartGame(channelId: ULong): Game {
        val game = getGameByPlatformId(channelId)
        deleteOngoingGame(channelId)
        val startRequest =
            StartRequest(
                game.subdivision ?: Subdivision.NFCAAB,
                game.homeTeam,
                game.awayTeam,
                game.gameType,
                game.seriesGameNumber
            )
        return startGame(startRequest, game.week)
    }

    /**
     * Deletes an ongoing game
     * @param channelId
     * @return
     */
    fun deleteOngoingGame(channelId: ULong): Boolean {
        val game = getGameByPlatformId(channelId)
        val gameId = game.id
        gameRepository.deleteById(gameId)
        gameStatsService.deleteByGameId(gameId)
        atBatRepository.deleteAllAtBatsByGameId(gameId)
        Logger.info("Game $gameId deleted")
        return true
    }

    /**
     * Calculate the delay of game timer
     */
    fun calculateDelayOfGameTimer(): String? {
        // Set the DOG timer
        // Get the current date and time
        val now = ZonedDateTime.now(ZoneId.of("America/New_York"))

        // Add 18 hours to the current date and time
        val futureTime = now.plusHours(18)

        // Define the desired date and time format
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")

        // Format the result and set it on the game
        return futureTime.format(formatter)
    }

    /**
     * Sub a coach in for a team
     * @param id
     */
    fun subCoachIntoGame(
        id: Int,
        team: String,
        discordId: String,
    ): Game {
        val game = getGameById(id)
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
        return saveGame(game)
    }

    /**
     * Get the game by request message id
     * @param requestMessageId
     */
    fun getGameByRequestMessageId(requestMessageId: String) =
        gameRepository.getGameByRequestMessageId(requestMessageId)
            ?: throw GameNotFoundException("Game not found for Request Message ID: $requestMessageId")

    /**
     * Get the game by platform id
     * @param platformId
     */
    fun getGameByPlatformId(platformId: ULong) =
        gameRepository.getGameByPlatformId(platformId)
            ?: throw GameNotFoundException("Game not found for Platform ID: $platformId")

    /**
     * Get an ongoing game by id
     * @param id
     * @return
     */
    fun getGameById(id: Int) = gameRepository.getGameById(id) ?: throw GameNotFoundException("No game found with ID: $id")

    /**
     * Get filtered games
     * @param filters
     * @param sort
     * @param conference
     * @param season
     * @param week
     * @param pageable
     */
    fun getFilteredGames(
        filters: List<GameFilter>,
        category: GameCategory?,
        conference: String?,
        season: Int?,
        week: Int?,
        sort: GameSort,
        pageable: Pageable,
    ): Page<Game> {
        val filterSpec = gameSpecificationService.createSpecification(filters, category, conference, season, week)
        val sortOrders = gameSpecificationService.createSort(sort)
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )

        return gameRepository.findAll(filterSpec, sortedPageable)
            ?: throw GameNotFoundException(
                "No games found for the following filters: " +
                    "filters = $filters, " +
                    "category = $category, " +
                    "conference = $conference, " +
                    "season = $season, " +
                    "week = $week",
            )
    }

    /**
     * Find expired timers
     */
    fun findExpiredTimers() =
        gameRepository.findExpiredTimers().ifEmpty {
            Logger.info("No games found with expired timers")
            emptyList()
        }

    /**
     * Find games to warn
     */
    fun findGamesToWarn() =
        gameRepository.findGamesToWarn().ifEmpty {
            Logger.info("No games found to warn")
            emptyList()
        }

    /**
     * Update a game as warned
     * @param id
     */
    fun updateGameAsWarned(id: Int) = gameRepository.updateGameAsWarned(id)

    /**
     * Mark a game as close game pinged
     * @param id
     */
    fun markCloseGamePinged(id: Int) = gameRepository.markCloseGamePinged(id)

    /**
     * Mark a game as upset alert pinged
     * @param id
     */
    fun markUpsetAlertPinged(id: Int) = gameRepository.markUpsetAlertPinged(id)

    /**
     * Get all games
     */
    fun getAllGames() =
        gameRepository.getAllGames().ifEmpty {
            throw GameNotFoundException("No games found when getting all games")
        }

    /**
     * Get all ongoing games
     */
    fun getAllOngoingGames() =
        gameRepository.getAllOngoingGames().ifEmpty {
            emptyList()
        }

    /**
     * Get all games with the teams in it for the requested week
     * @param teams
     * @param season
     * @param week
     */
    fun getGamesWithTeams(
        teams: List<Team>,
        season: Int,
        week: Int,
    ): List<Game> {
        val games = mutableListOf<Game>()
        for (team in teams) {
            val game = gameRepository.getGamesByTeamSeasonAndWeek(team.name ?: "", season, week)
            if (game != null) {
                games.add(game)
            } else {
                throw GameNotFoundException("No games found for ${team.name} in season $season week $week")
            }
        }
        return games
    }

    /**
     * Returns the difference between the batter and pitcher numbers.
     * @param batterNumber
     * @param pitcherNumber
     * @return
     */
    fun getDifference(
        batterNumber: Int,
        pitcherNumber: Int,
    ): Int {
        var difference = abs(pitcherNumber - batterNumber)
        if (difference > 500) {
            difference = 1000 - difference
        }
        return difference
    }

    /**
     * Get the base condition based on the runners on base
     * @param runnerOnFirst
     * @param runnerOnSecond
     * @param runnerOnThird
     * @return
     */
    fun getBaseCondition(
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
    ): BaseCondition {
        return when {
            runnerOnFirst != null && runnerOnSecond != null && runnerOnThird != null -> BaseCondition.BASED_LOADED
            runnerOnFirst != null && runnerOnSecond != null -> BaseCondition.FIRST_SECOND
            runnerOnFirst != null && runnerOnThird != null -> BaseCondition.FIRST_THIRD
            runnerOnSecond != null && runnerOnThird != null -> BaseCondition.SECOND_THIRD
            runnerOnFirst != null -> BaseCondition.FIRST
            runnerOnSecond != null -> BaseCondition.SECOND
            runnerOnThird != null -> BaseCondition.THIRD
            else -> BaseCondition.EMPTY
        }
    }

    /**
     * Save a game
     * @param game
     * @return
     */
    fun saveGame(game: Game) = gameRepository.save(game)
        ?: throw GameNotFoundException("Unable to save game: ${game.id} - ${game.homeTeam} vs ${game.awayTeam}")

    /**
     * Update a game
     * @param game
     * @return
     */
    fun updateGame(game: Game): Game {
        return saveGame(game)
    }
}
