package com.nfcaab.backend.service.game

import com.nfcaab.backend.dto.AtBatOutcome
import com.nfcaab.backend.dto.requests.StartRequest
import com.nfcaab.backend.dto.website.UserDTO
import com.nfcaab.backend.enums.team.Subdivision
import com.nfcaab.backend.enums.team.TeamSide
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.GameStatus
import com.nfcaab.backend.model.Game.GameType
import com.nfcaab.backend.model.Game.InningHalf.TOP
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.model.Season
import com.nfcaab.backend.model.Team
import com.nfcaab.backend.model.User
import com.nfcaab.backend.model.User.Role
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.service.discord.DiscordService
import com.nfcaab.backend.service.lineup.LineupService
import com.nfcaab.backend.service.schedule.ScheduleService
import com.nfcaab.backend.service.schedule.SeasonService
import com.nfcaab.backend.service.stats.GameStatsService
import com.nfcaab.backend.service.stats.PitcherDecisionService
import com.nfcaab.backend.service.team.TeamService
import com.nfcaab.backend.service.user.UserService
import com.nfcaab.backend.util.TeamNotFoundException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameLifecycleServiceTest {
    private lateinit var gameRepository: GameRepository
    private lateinit var atBatRepository: AtBatRepository
    private lateinit var teamService: TeamService
    private lateinit var discordService: DiscordService
    private lateinit var userService: UserService
    private lateinit var gameStatsService: GameStatsService
    private lateinit var pitcherDecisionService: PitcherDecisionService
    private lateinit var seasonService: SeasonService
    private lateinit var scheduleService: ScheduleService
    private lateinit var lineupService: LineupService
    private lateinit var gameService: GameService
    private lateinit var gameLifecycleService: GameLifecycleService

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        atBatRepository = mockk()
        teamService = mockk()
        discordService = mockk()
        userService = mockk()
        gameStatsService = mockk()
        pitcherDecisionService = mockk()
        seasonService = mockk()
        scheduleService = mockk()
        lineupService = mockk()
        gameService = mockk()
        gameLifecycleService =
            GameLifecycleService(
                gameRepository,
                atBatRepository,
                teamService,
                discordService,
                userService,
                gameStatsService,
                pitcherDecisionService,
                seasonService,
                scheduleService,
                lineupService,
                gameService,
            )
    }

    private fun testUserDTO() =
        UserDTO(
            id = 9,
            username = "newCoach",
            coachName = "New Coach",
            discordTag = "newCoach#1",
            discordId = "discord9",
            role = Role.USER,
            team = null,
            delayOfGameInstances = 0,
            wins = 0,
            losses = 0,
            winPercentage = 0.0F,
            conferenceWins = 0,
            conferenceLosses = 0,
            conferenceWinPercentage = 0.0F,
            seriesWins = 0,
            seriesLosses = 0,
            seriesPushes = 0,
            conferenceChampionships = 0,
            tournamentAppearances = 0,
            superRegionalAppearances = 0,
            collegeWorldSeriesAppearances = 0,
            championships = 0,
            averageResponseTime = 0.0,
        )

    private fun scrimmageGame(id: Int = 1) =
        Game().apply {
            this.id = id
            homeTeam = "Home Team"
            awayTeam = "Away Team"
            homeCoachDiscordId = "home123"
            awayCoachDiscordId = "away123"
            gameType = GameType.SCRIMMAGE
            gameStatus = GameStatus.IN_PROGRESS
            inning = 5
            season = null
        }

    @Test
    fun `startSingleGame should not mark schedule game started for scrimmages`() =
        runBlocking {
            val homeTeam = Team().apply { name = "Home Team"; coachUsername = "homeCoach"; coachDiscordId = "home123"; currentWins = 1; currentLosses = 0; ranking = null }
            val awayTeam = Team().apply { name = "Away Team"; coachUsername = "awayCoach"; coachDiscordId = "away123"; currentWins = 0; currentLosses = 1; ranking = null }
            val startRequest = StartRequest(Subdivision.NFCAAB, "Home Team", "Away Team", GameType.SCRIMMAGE, 1)
            val savedGame = scrimmageGame()

            every { teamService.getTeamByName("Home Team") } returns homeTeam
            every { teamService.getTeamByName("Away Team") } returns awayTeam
            every { gameService.calculateDelayOfGameTimer() } returns "07/13/2026 12:00:00"
            every { gameService.saveGame(any()) } returns savedGame
            coEvery { discordService.startGameThread(any()) } returns listOf("thread1", "message1")
            every { gameStatsService.createGameStats(any()) } returns emptyList()

            val result = gameLifecycleService.startSingleGame(startRequest, null)

            assertEquals(savedGame, result)
            verify(exactly = 0) { scheduleService.markManuallyStartedGameAsStarted(any()) }
        }

    @Test
    fun `startSingleGame should mark schedule game started for non-scrimmage games`() =
        runBlocking {
            val homeTeam = Team().apply { name = "Home Team"; coachUsername = "homeCoach"; coachDiscordId = "home123"; currentWins = 1; currentLosses = 0; ranking = 5 }
            val awayTeam = Team().apply { name = "Away Team"; coachUsername = "awayCoach"; coachDiscordId = "away123"; currentWins = 0; currentLosses = 1; ranking = 10 }
            val startRequest = StartRequest(Subdivision.NFCAAB, "Home Team", "Away Team", GameType.OUT_OF_CONFERENCE, 1)
            val season = Season().apply { seasonNumber = 2026; currentWeek = 3 }
            val savedGame = scrimmageGame().apply { gameType = GameType.OUT_OF_CONFERENCE }

            every { teamService.getTeamByName("Home Team") } returns homeTeam
            every { teamService.getTeamByName("Away Team") } returns awayTeam
            every { seasonService.getCurrentSeason() } returns season
            every { gameService.calculateDelayOfGameTimer() } returns "07/13/2026 12:00:00"
            every { gameService.saveGame(any()) } returns savedGame
            coEvery { discordService.startGameThread(any()) } returns listOf("thread1", "message1")
            every { gameStatsService.createGameStats(any()) } returns emptyList()
            every { scheduleService.markManuallyStartedGameAsStarted(any()) } returns Unit

            gameLifecycleService.startSingleGame(startRequest, null)

            verify(exactly = 1) { scheduleService.markManuallyStartedGameAsStarted(savedGame) }
        }

    @Test
    fun `updateGameValues should advance to a new inning when three outs are recorded`() {
        val game = scrimmageGame().apply { inningHalf = TOP; inning = 5; outs = 2; homeBatterLineupSpot = 1; awayBatterLineupSpot = 9 }
        val batter = Player().apply { firstName = "First"; lastName = "Last"; uniformNumber = 12 }
        val pitcher = Player().apply { firstName = "Pitch"; lastName = "Er"; uniformNumber = 21 }
        val outcome =
            AtBatOutcome(
                actualResult = ActualResult.GROUNDOUT,
                outs = 3,
                runsScored = 0,
                homeScore = 0,
                awayScore = 0,
                runnerOnFirstAfter = null,
                runnerOnSecondAfter = null,
                runnerOnThirdAfter = null,
                baseConditionAfter = Game.BaseCondition.EMPTY,
            )

        every { lineupService.getBatterByLineupSpot(any(), any(), any()) } returns batter
        every { lineupService.getPitcherByTeam(any(), any()) } returns pitcher
        every { teamService.getTeamByName(any()) } returns Team().apply { name = "Home Team"; ranking = 1 }
        every { gameService.calculateDelayOfGameTimer() } returns "07/13/2026 12:00:00"

        val result = gameLifecycleService.updateGameValues(game, outcome)

        assertEquals(Game.InningHalf.BOTTOM, result.inningHalf)
        assertEquals(5, result.inning)
        assertEquals(0, result.outs)
        assertEquals(TeamSide.AWAY, result.waitingOn)
    }

    @Test
    fun `updateGameValues should end the game after the top of the ninth when the home team is already leading`() {
        val game = scrimmageGame().apply { inningHalf = TOP; inning = 9; outs = 2; homeBatterLineupSpot = 1; awayBatterLineupSpot = 9 }
        val batter = Player().apply { firstName = "First"; lastName = "Last"; uniformNumber = 12 }
        val pitcher = Player().apply { firstName = "Pitch"; lastName = "Er"; uniformNumber = 21 }
        val outcome =
            AtBatOutcome(
                actualResult = ActualResult.GROUNDOUT,
                outs = 3,
                runsScored = 0,
                homeScore = 5,
                awayScore = 2,
                runnerOnFirstAfter = null,
                runnerOnSecondAfter = null,
                runnerOnThirdAfter = null,
                baseConditionAfter = Game.BaseCondition.EMPTY,
            )

        every { lineupService.getBatterByLineupSpot(any(), any(), any()) } returns batter
        every { lineupService.getPitcherByTeam(any(), any()) } returns pitcher
        every { teamService.getTeamByName(any()) } returns Team().apply { name = "Home Team"; ranking = 1 }
        every { gameService.calculateDelayOfGameTimer() } returns "07/13/2026 12:00:00"
        every { gameService.saveGame(any()) } returns game
        every { gameStatsService.deleteByGameId(any()) } returns Unit
        every { atBatRepository.getAllAtBatsByGameId(any()) } returns emptyList()
        every { gameStatsService.updateGameStats(any(), any()) } returns emptyList()
        every { gameStatsService.getGameStatsByIdAndTeam(any(), any()) } returns mockk(relaxed = true)
        every { gameStatsService.saveGameStats(any()) } returns mockk()
        every { pitcherDecisionService.computeDecisions(any()) } returns Unit

        val result = gameLifecycleService.updateGameValues(game, outcome)

        assertEquals(GameStatus.FINAL, result.gameStatus)
        assertEquals(9, result.inning)
        assertEquals(Game.InningHalf.BOTTOM, result.inningHalf)
        verify(exactly = 0) { gameStatsService.aggregateStatsAfterGame(any()) }
    }

    @Test
    fun `updateGameValues should end the game immediately on a walk off in the bottom of the ninth`() {
        val game = scrimmageGame().apply { inningHalf = Game.InningHalf.BOTTOM; inning = 9; outs = 1; homeScore = 2; awayScore = 3 }
        val batter = Player().apply { firstName = "First"; lastName = "Last"; uniformNumber = 12 }
        val pitcher = Player().apply { firstName = "Pitch"; lastName = "Er"; uniformNumber = 21 }
        val outcome =
            AtBatOutcome(
                actualResult = ActualResult.HOME_RUN,
                outs = 1,
                runsScored = 2,
                homeScore = 4,
                awayScore = 3,
                runnerOnFirstAfter = null,
                runnerOnSecondAfter = null,
                runnerOnThirdAfter = null,
                baseConditionAfter = Game.BaseCondition.EMPTY,
            )

        every { lineupService.getBatterByLineupSpot(any(), any(), any()) } returns batter
        every { lineupService.getPitcherByTeam(any(), any()) } returns pitcher
        every { teamService.getTeamByName(any()) } returns Team().apply { name = "Home Team"; ranking = 1 }
        every { gameService.calculateDelayOfGameTimer() } returns "07/13/2026 12:00:00"
        every { gameService.saveGame(any()) } returns game
        every { gameStatsService.deleteByGameId(any()) } returns Unit
        every { atBatRepository.getAllAtBatsByGameId(any()) } returns emptyList()
        every { gameStatsService.updateGameStats(any(), any()) } returns emptyList()
        every { gameStatsService.getGameStatsByIdAndTeam(any(), any()) } returns mockk(relaxed = true)
        every { gameStatsService.saveGameStats(any()) } returns mockk()
        every { pitcherDecisionService.computeDecisions(any()) } returns Unit

        val result = gameLifecycleService.updateGameValues(game, outcome)

        assertEquals(GameStatus.FINAL, result.gameStatus)
        assertEquals(9, result.inning)
        assertEquals(Game.InningHalf.BOTTOM, result.inningHalf)
    }

    @Test
    fun `updateGameValues should not advance the lineup spot when applying a steal`() {
        val game = scrimmageGame().apply { inningHalf = TOP; inning = 5; outs = 1; awayBatterLineupSpot = 4 }
        val batter = Player().apply { firstName = "First"; lastName = "Last"; uniformNumber = 12 }
        val pitcher = Player().apply { firstName = "Pitch"; lastName = "Er"; uniformNumber = 21 }
        val outcome =
            AtBatOutcome(
                actualResult = ActualResult.STOLEN_BASE,
                outs = 1,
                runsScored = 0,
                homeScore = 0,
                awayScore = 0,
                runnerOnFirstAfter = null,
                runnerOnSecondAfter = batter,
                runnerOnThirdAfter = null,
                baseConditionAfter = Game.BaseCondition.SECOND,
            )

        every { lineupService.getBatterByLineupSpot(any(), any(), any()) } returns batter
        every { lineupService.getPitcherByTeam(any(), any()) } returns pitcher
        every { teamService.getTeamByName(any()) } returns Team().apply { name = "Home Team"; ranking = 1 }
        every { gameService.calculateDelayOfGameTimer() } returns "07/13/2026 12:00:00"

        val result = gameLifecycleService.updateGameValues(game, outcome, false)

        assertEquals(4, result.awayBatterLineupSpot)
        assertEquals(TeamSide.HOME, result.waitingOn)
    }

    @Test
    fun `updateGameValues should still play the bottom of the ninth when the home team is not leading`() {
        val game = scrimmageGame().apply { inningHalf = TOP; inning = 9; outs = 2; homeBatterLineupSpot = 1; awayBatterLineupSpot = 9 }
        val batter = Player().apply { firstName = "First"; lastName = "Last"; uniformNumber = 12 }
        val pitcher = Player().apply { firstName = "Pitch"; lastName = "Er"; uniformNumber = 21 }
        val outcome =
            AtBatOutcome(
                actualResult = ActualResult.GROUNDOUT,
                outs = 3,
                runsScored = 0,
                homeScore = 2,
                awayScore = 2,
                runnerOnFirstAfter = null,
                runnerOnSecondAfter = null,
                runnerOnThirdAfter = null,
                baseConditionAfter = Game.BaseCondition.EMPTY,
            )

        every { lineupService.getBatterByLineupSpot(any(), any(), any()) } returns batter
        every { lineupService.getPitcherByTeam(any(), any()) } returns pitcher
        every { teamService.getTeamByName(any()) } returns Team().apply { name = "Home Team"; ranking = 1 }
        every { gameService.calculateDelayOfGameTimer() } returns "07/13/2026 12:00:00"

        val result = gameLifecycleService.updateGameValues(game, outcome)

        assertEquals(GameStatus.IN_PROGRESS, result.gameStatus)
        assertEquals(9, result.inning)
        assertEquals(Game.InningHalf.BOTTOM, result.inningHalf)
    }

    @Test
    fun `updateWithPitcherNumberSubmission should set the current at bat and flip waitingOn`() {
        val game = scrimmageGame().apply { inningHalf = TOP }
        val atBat = AtBat().apply { id = 42; gameId = game.id }

        every { gameService.calculateDelayOfGameTimer() } returns "07/13/2026 12:00:00"
        every { gameRepository.save(any()) } returns game

        gameLifecycleService.updateWithPitcherNumberSubmission(game, atBat)

        assertEquals(42, game.currentAtBatId)
        assertEquals(TeamSide.AWAY, game.waitingOn)
        verify { gameRepository.save(game) }
    }

    @Test
    fun `rollbackAtBat should restore game state from the previous at bat`() {
        val game = scrimmageGame().apply { inningHalf = TOP; inning = 5; homeBatterLineupSpot = 1; awayBatterLineupSpot = 1; gameStatus = GameStatus.IN_PROGRESS }
        val previousAtBat = AtBat().apply { id = 10; gameId = game.id; inning = 4; outs = 1; inningHalf = TOP; runnerOnFirst = 5 }
        val currentAtBat = AtBat().apply { id = 11; gameId = game.id; actualResult = null }
        val batter = Player().apply { firstName = "First"; lastName = "Last"; uniformNumber = 12 }
        val pitcher = Player().apply { firstName = "Pitch"; lastName = "Er"; uniformNumber = 21 }

        every { atBatRepository.getCurrentAtBat(game.id) } returns null
        every { atBatRepository.getPreviousAtBat(game.id) } returns previousAtBat
        every { gameService.calculateDelayOfGameTimer() } returns "07/13/2026 12:00:00"
        every { lineupService.getBatterByLineupSpot(any(), any(), any()) } returns batter
        every { lineupService.getPitcherByTeam(any(), any()) } returns pitcher
        every { gameStatsService.generateGameStats(game.id) } returns Unit
        every { gameService.saveGame(game) } returns game

        gameLifecycleService.rollbackAtBat(game, previousAtBat, currentAtBat)

        assertEquals(4, game.inning)
        assertEquals(1, game.outs)
        assertEquals(5, game.runnerOnFirst)
        verify { gameService.saveGame(game) }
    }

    @Test
    fun `endDOGOutGame should place a runner on third for the offended side and end the game`() {
        val game = scrimmageGame().apply { awayBatterLineupSpot = 4 }

        every { gameService.saveGame(game) } returns game
        every { teamService.updateTeamWinsAndLosses(any()) } returns Unit
        every { userService.updateUserWinsAndLosses(any()) } returns Unit
        every { userService.getUserByDiscordId(any()) } returns User().apply { id = 1; discordTag = "tag#1" }
        every { atBatRepository.getUserAverageResponseTime(any(), any()) } returns 5.0
        every { userService.updateUserAverageResponseTime(any(), any()) } returns Unit
        every { scheduleService.markGameAsFinished(any()) } returns Unit
        every { gameStatsService.deleteByGameId(any()) } returns Unit
        every { atBatRepository.getAllAtBatsByGameId(any()) } returns emptyList()
        every { gameStatsService.updateGameStats(any(), any()) } returns emptyList()
        every { gameStatsService.getGameStatsByIdAndTeam(any(), any()) } returns mockk(relaxed = true)
        every { gameStatsService.saveGameStats(any()) } returns mockk()
        every { pitcherDecisionService.computeDecisions(any()) } returns Unit

        val result = gameLifecycleService.endDOGOutGame(game, Pair(3, 0))

        assertEquals(4, result.runnerOnThird)
        verify { gameService.saveGame(game) }
    }

    @Test
    fun `endSingleGame should end a non-scrimmage game and aggregate stats`() {
        val game = scrimmageGame().apply { gameType = GameType.OUT_OF_CONFERENCE; season = 2026 }
        val homeUser = User().apply { id = 1; discordTag = "home#1" }
        val awayUser = User().apply { id = 2; discordTag = "away#1" }
        val season = Season().apply { seasonNumber = 2026; currentWeek = 3 }

        every { gameService.getGameByPlatformId(any()) } returns game
        every { teamService.updateTeamWinsAndLosses(game) } returns Unit
        every { userService.updateUserWinsAndLosses(game) } returns Unit
        every { userService.getUserByDiscordId("home123") } returns homeUser
        every { userService.getUserByDiscordId("away123") } returns awayUser
        every { seasonService.getCurrentSeason() } returns season
        every { atBatRepository.getUserAverageResponseTime(any(), any()) } returns 5.0
        every { userService.updateUserAverageResponseTime(any(), any()) } returns Unit
        every { scheduleService.markGameAsFinished(game) } returns Unit
        every { gameService.saveGame(game) } returns game
        every { gameStatsService.deleteByGameId(game.id) } returns Unit
        every { atBatRepository.getAllAtBatsByGameId(game.id) } returns emptyList()
        every { gameStatsService.updateGameStats(game, any()) } returns emptyList()
        every { gameStatsService.getGameStatsByIdAndTeam(any(), any()) } returns mockk(relaxed = true)
        every { gameStatsService.saveGameStats(any()) } returns mockk()
        every { gameStatsService.aggregateStatsAfterGame(game) } returns Unit
        every { pitcherDecisionService.computeDecisions(any()) } returns Unit

        val result = gameLifecycleService.endSingleGame(1UL)

        assertEquals(GameStatus.FINAL, result.gameStatus)
        verify { gameStatsService.aggregateStatsAfterGame(game) }
    }

    @Test
    fun `endAllGames should end every ongoing scrimmage game`() {
        val game = scrimmageGame()

        every { gameService.getAllOngoingGames() } returns listOf(game)
        every { teamService.updateTeamWinsAndLosses(any()) } returns Unit
        every { userService.updateUserWinsAndLosses(any()) } returns Unit
        every { gameService.saveGame(game) } returns game
        every { gameStatsService.deleteByGameId(game.id) } returns Unit
        every { atBatRepository.getAllAtBatsByGameId(game.id) } returns emptyList()
        every { gameStatsService.updateGameStats(game, any()) } returns emptyList()
        every { gameStatsService.getGameStatsByIdAndTeam(any(), any()) } returns mockk(relaxed = true)
        every { gameStatsService.saveGameStats(any()) } returns mockk()
        every { pitcherDecisionService.computeDecisions(any()) } returns Unit

        val result = gameLifecycleService.endAllGames()

        assertEquals(1, result.size)
        assertEquals(GameStatus.FINAL, result[0].gameStatus)
    }

    @Test
    fun `deleteOngoingGame should delete the game, stats, and at bats`() {
        val game = scrimmageGame()

        every { gameService.getGameByPlatformId(1UL) } returns game
        every { gameRepository.deleteById(game.id) } returns Unit
        every { gameStatsService.deleteByGameId(game.id) } returns Unit
        every { atBatRepository.deleteAllAtBatsByGameId(game.id) } returns Unit

        val result = gameLifecycleService.deleteOngoingGame(1UL)

        assertEquals(true, result)
        verify { gameRepository.deleteById(game.id) }
    }

    @Test
    fun `subCoachIntoGame should replace the home coach when team matches`() {
        val game = scrimmageGame()
        val userDTO = testUserDTO()

        every { gameService.getGameById(game.id) } returns game
        every { userService.getUserDTOByDiscordId("discord9") } returns userDTO
        every { gameService.saveGame(any()) } returns game

        val result = gameLifecycleService.subCoachIntoGame(game.id, "Home Team", "discord9")

        assertEquals("newCoach#1", result.homeCoach)
        assertEquals("discord9", result.homeCoachDiscordId)
    }

    @Test
    fun `subCoachIntoGame should throw when the team does not match either side`() {
        val game = scrimmageGame()

        every { gameService.getGameById(game.id) } returns game
        every { userService.getUserDTOByDiscordId(any()) } returns testUserDTO()

        assertThrows(TeamNotFoundException::class.java) {
            gameLifecycleService.subCoachIntoGame(game.id, "Unknown Team", "discord9")
        }
    }

    @Test
    fun `pinchRun should replace the runner on the given base and update the lineup`() {
        val game = scrimmageGame().apply { runnerOnSecond = 5 }

        every { gameService.getGameById(game.id) } returns game
        every { lineupService.getCurrentPosition(game.id, "Away Team", 5) } returns Player.Position.SECOND_BASE
        every {
            lineupService.substituteBatter(game.id, "Away Team", 5, 15, Player.Position.SECOND_BASE)
        } returns mockk(relaxed = true)
        every { gameService.saveGame(any()) } returns game

        val result = gameLifecycleService.pinchRun(game.id, "Away Team", Game.Base.SECOND, 15)

        assertEquals(15, result.runnerOnSecond)
        verify { lineupService.substituteBatter(game.id, "Away Team", 5, 15, Player.Position.SECOND_BASE) }
    }

    @Test
    fun `pinchRun should throw when there is no runner on the requested base`() {
        val game = scrimmageGame().apply { runnerOnFirst = null }

        every { gameService.getGameById(game.id) } returns game

        assertThrows(com.nfcaab.backend.util.PlayerNotFoundException::class.java) {
            gameLifecycleService.pinchRun(game.id, "Away Team", Game.Base.FIRST, 15)
        }
    }

    @Test
    fun `restartGame should delete and restart the game with the same matchup`() =
        runBlocking {
            val game = scrimmageGame().apply { subdivision = Subdivision.NFCAAB; seriesGameNumber = 1; week = 3 }
            val homeTeam = Team().apply { name = "Home Team"; coachUsername = "homeCoach"; coachDiscordId = "home123"; currentWins = 1; currentLosses = 0; ranking = null }
            val awayTeam = Team().apply { name = "Away Team"; coachUsername = "awayCoach"; coachDiscordId = "away123"; currentWins = 0; currentLosses = 1; ranking = null }
            val restartedGame = scrimmageGame(id = 2)

            every { gameService.getGameByPlatformId(1UL) } returns game
            every { gameRepository.deleteById(game.id) } returns Unit
            every { gameStatsService.deleteByGameId(game.id) } returns Unit
            every { atBatRepository.deleteAllAtBatsByGameId(game.id) } returns Unit
            every { teamService.getTeamByName("Home Team") } returns homeTeam
            every { teamService.getTeamByName("Away Team") } returns awayTeam
            every { gameService.calculateDelayOfGameTimer() } returns "07/13/2026 12:00:00"
            every { gameService.saveGame(any()) } returns restartedGame
            coEvery { discordService.startGameThread(any()) } returns listOf("thread2", "message2")
            every { gameStatsService.createGameStats(any()) } returns emptyList()

            val result = gameLifecycleService.restartGame(1UL)

            assertEquals(restartedGame, result)
        }
}
