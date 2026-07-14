package com.nfcaab.backend.service.scheduler

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.GameStatus
import com.nfcaab.backend.model.Game.GameType
import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.model.User
import com.nfcaab.backend.enums.team.TeamSide
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.service.discord.DiscordService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.nfcaab.backend.service.user.UserService
import com.nfcaab.backend.service.scorebug.ScorebugService
import com.nfcaab.backend.service.game.GameService
import com.nfcaab.backend.service.game.GameLifecycleService
import com.nfcaab.backend.service.atbat.AtBatService
import com.nfcaab.backend.service.lineup.LineupService

class DelayOfGameMonitorTest {
    private lateinit var gameService: GameService
    private lateinit var gameLifecycleService: GameLifecycleService
    private lateinit var userService: UserService
    private lateinit var atBatService: AtBatService
    private lateinit var discordService: DiscordService
    private lateinit var scorebugService: ScorebugService
    private lateinit var atBatRepository: AtBatRepository
    private lateinit var lineupService: LineupService
    private lateinit var delayOfGameMonitor: DelayOfGameMonitor

    @BeforeEach
    fun setUp() {
        gameService = mockk()
        gameLifecycleService = mockk()
        userService = mockk()
        atBatService = mockk()
        discordService = mockk()
        scorebugService = mockk()
        atBatRepository = mockk()
        lineupService = mockk()
        delayOfGameMonitor = DelayOfGameMonitor(
            gameService,
            gameLifecycleService,
            userService,
            atBatService,
            discordService,
            scorebugService,
            atBatRepository,
            lineupService,
        )
    }

    @Test
    fun `checkForDelayOfGame should process warned games`() {
        val warnedGames = listOf(
            Game().apply {
                id = 1
                gameStatus = GameStatus.IN_PROGRESS
            }
        )

        every { gameService.findGamesToWarn() } returns warnedGames
        every { discordService.notifyWarning(any()) } returns Unit
        every { gameService.updateGameAsWarned(any()) } returns Unit
        every { gameService.findExpiredTimers() } returns emptyList()

        delayOfGameMonitor.checkForDelayOfGame()

        verify { gameService.findGamesToWarn() }
        verify { discordService.notifyWarning(any()) }
        verify { gameService.updateGameAsWarned(any()) }
    }

    @Test
    fun `checkForDelayOfGame should apply a home run when the pitcher delays`() {
        val expiredGame = Game().apply {
            id = 1
            gameStatus = GameStatus.IN_PROGRESS
            waitingOn = TeamSide.HOME
            gameType = GameType.OUT_OF_CONFERENCE
            homeTeam = "Home Team"
            awayTeam = "Away Team"
            homeCoachDiscordId = "home123"
            awayCoachDiscordId = "away123"
            homeBatterLineupSpot = 1
            awayBatterLineupSpot = 2
            inningHalf = Game.InningHalf.TOP
        }

        val savedAtBat = AtBat().apply {
            id = 1
            gameId = 1
        }
        val savedUser = User().apply {
            id = 1
        }

        every { gameService.findGamesToWarn() } returns emptyList()
        every { gameService.findExpiredTimers() } returns listOf(expiredGame)
        every { gameService.calculateDelayOfGameTimer() } returns "2024-01-01 12:00:00"
        every { atBatService.getHomeDelayOfGameInstances(any()) } returns 1
        every { atBatService.getCurrentAtBat(any()) } throws Exception("No current at bat")
        every { userService.getUserByDiscordId(any()) } returns savedUser
        every { userService.saveUser(any()) } returns savedUser
        every { gameService.saveGame(any()) } returns expiredGame
        every { scorebugService.generateScorebug(any()) } returns mockk()
        every { atBatRepository.save(any()) } returns savedAtBat
        every { gameLifecycleService.endDOGOutGame(any(), any()) } returns expiredGame
        every { discordService.notifyDelayOfGame(any(), any()) } returns Unit

        delayOfGameMonitor.checkForDelayOfGame()

        verify { gameService.findExpiredTimers() }
        verify { discordService.notifyDelayOfGame(any(), any()) }
        assertEquals(1, expiredGame.awayScore)
    }

    private fun pendingBatterAtBat() =
        AtBat().apply {
            id = 5
            gameId = 1
        }

    private fun batter() = Player().apply { firstName = "First"; lastName = "Last"; uniformNumber = 7 }

    private fun pitcher() = Player().apply { firstName = "Pitch"; lastName = "Er"; uniformNumber = 21 }

    private fun batterDelayGame(outs: Int) =
        Game().apply {
            id = 1
            gameStatus = GameStatus.IN_PROGRESS
            waitingOn = TeamSide.AWAY
            gameType = GameType.OUT_OF_CONFERENCE
            homeTeam = "Home Team"
            awayTeam = "Away Team"
            homeCoachDiscordId = "home123"
            awayCoachDiscordId = "away123"
            homeBatterLineupSpot = 1
            awayBatterLineupSpot = 3
            inningHalf = Game.InningHalf.TOP
            inning = 5
            this.outs = outs
            homeScore = 2
            awayScore = 1
        }

    @Test
    fun `checkForDelayOfGame should charge two outs and skip two batters when the batter delays with zero outs`() {
        val expiredGame = batterDelayGame(outs = 0)
        val pendingAtBat = pendingBatterAtBat()

        every { gameService.findGamesToWarn() } returns emptyList()
        every { gameService.findExpiredTimers() } returns listOf(expiredGame)
        every { gameService.calculateDelayOfGameTimer() } returns "2024-01-01 12:00:00"
        every { atBatService.getAwayDelayOfGameInstances(any()) } returns 1
        every { atBatService.getCurrentAtBat(any()) } returns pendingAtBat
        every { userService.getUserByDiscordId(any()) } returns User().apply { id = 1 }
        every { userService.saveUser(any()) } returns User().apply { id = 1 }
        every { lineupService.getBatterByLineupSpot(any(), any(), any()) } returns batter()
        every { lineupService.getPitcherByTeam(any(), any()) } returns pitcher()
        val savedAtBatSlot = slot<AtBat>()
        every { atBatRepository.save(capture(savedAtBatSlot)) } answers { savedAtBatSlot.captured }
        every { gameService.saveGame(any()) } returns expiredGame
        every { scorebugService.generateScorebug(any()) } returns mockk()
        every { discordService.notifyDelayOfGame(any(), any()) } returns Unit

        delayOfGameMonitor.checkForDelayOfGame()

        assertEquals(2, expiredGame.outs)
        assertEquals(5, expiredGame.awayBatterLineupSpot)
        assertEquals(Game.InningHalf.TOP, expiredGame.inningHalf)
        assertEquals(5, expiredGame.inning)
        assertEquals(TeamSide.HOME, expiredGame.waitingOn)
        assertEquals(ActualResult.STRIKEOUT, pendingAtBat.actualResult)
        assertEquals(Scenario.DELAY_OF_GAME_AWAY, pendingAtBat.result)
    }

    @Test
    fun `checkForDelayOfGame should end the half inning when the batter delays with one out`() {
        val expiredGame = batterDelayGame(outs = 1)
        val pendingAtBat = pendingBatterAtBat()

        every { gameService.findGamesToWarn() } returns emptyList()
        every { gameService.findExpiredTimers() } returns listOf(expiredGame)
        every { gameService.calculateDelayOfGameTimer() } returns "2024-01-01 12:00:00"
        every { atBatService.getAwayDelayOfGameInstances(any()) } returns 1
        every { atBatService.getCurrentAtBat(any()) } returns pendingAtBat
        every { userService.getUserByDiscordId(any()) } returns User().apply { id = 1 }
        every { userService.saveUser(any()) } returns User().apply { id = 1 }
        every { lineupService.getBatterByLineupSpot(any(), any(), any()) } returns batter()
        every { lineupService.getPitcherByTeam(any(), any()) } returns pitcher()
        val savedAtBatSlot = slot<AtBat>()
        every { atBatRepository.save(capture(savedAtBatSlot)) } answers { savedAtBatSlot.captured }
        every { gameService.saveGame(any()) } returns expiredGame
        every { scorebugService.generateScorebug(any()) } returns mockk()
        every { discordService.notifyDelayOfGame(any(), any()) } returns Unit

        delayOfGameMonitor.checkForDelayOfGame()

        assertEquals(0, expiredGame.outs)
        assertEquals(Game.InningHalf.BOTTOM, expiredGame.inningHalf)
        assertEquals(5, expiredGame.inning)
        assertEquals(TeamSide.AWAY, expiredGame.waitingOn)
        assertEquals(null, expiredGame.runnerOnFirst)
    }

    @Test
    fun `checkForDelayOfGame should charge only the current batter when the batter delays with two outs`() {
        val expiredGame = batterDelayGame(outs = 2)
        val pendingAtBat = pendingBatterAtBat()

        every { gameService.findGamesToWarn() } returns emptyList()
        every { gameService.findExpiredTimers() } returns listOf(expiredGame)
        every { gameService.calculateDelayOfGameTimer() } returns "2024-01-01 12:00:00"
        every { atBatService.getAwayDelayOfGameInstances(any()) } returns 1
        every { atBatService.getCurrentAtBat(any()) } returns pendingAtBat
        every { userService.getUserByDiscordId(any()) } returns User().apply { id = 1 }
        every { userService.saveUser(any()) } returns User().apply { id = 1 }
        every { atBatRepository.save(any()) } answers { firstArg() }
        every { gameService.saveGame(any()) } returns expiredGame
        every { scorebugService.generateScorebug(any()) } returns mockk()
        every { discordService.notifyDelayOfGame(any(), any()) } returns Unit

        delayOfGameMonitor.checkForDelayOfGame()

        assertEquals(0, expiredGame.outs)
        assertEquals(Game.InningHalf.BOTTOM, expiredGame.inningHalf)
        assertEquals(ActualResult.STRIKEOUT, pendingAtBat.actualResult)
        verify(exactly = 0) { lineupService.getBatterByLineupSpot(any(), any(), any()) }
    }

    @Test
    fun `checkForDelayOfGame should end the game without playing the bottom of the ninth when home already leads`() {
        val expiredGame = batterDelayGame(outs = 2).apply { inning = 9; homeScore = 5; awayScore = 2 }
        val pendingAtBat = pendingBatterAtBat()

        every { gameService.findGamesToWarn() } returns emptyList()
        every { gameService.findExpiredTimers() } returns listOf(expiredGame)
        every { gameService.calculateDelayOfGameTimer() } returns "2024-01-01 12:00:00"
        every { atBatService.getAwayDelayOfGameInstances(any()) } returns 1
        every { atBatService.getCurrentAtBat(any()) } returns pendingAtBat
        every { userService.getUserByDiscordId(any()) } returns User().apply { id = 1 }
        every { userService.saveUser(any()) } returns User().apply { id = 1 }
        every { atBatRepository.save(any()) } answers { firstArg() }
        every { gameService.saveGame(any()) } returns expiredGame
        every { gameLifecycleService.endSingleGameByGameId(any()) } returns expiredGame
        every { scorebugService.generateScorebug(any()) } returns mockk()
        every { discordService.notifyDelayOfGame(any(), any()) } returns Unit

        delayOfGameMonitor.checkForDelayOfGame()

        assertEquals(GameStatus.FINAL, expiredGame.gameStatus)
        assertEquals(9, expiredGame.inning)
        assertEquals(Game.InningHalf.BOTTOM, expiredGame.inningHalf)
        verify { gameLifecycleService.endSingleGameByGameId(1) }
    }
}
