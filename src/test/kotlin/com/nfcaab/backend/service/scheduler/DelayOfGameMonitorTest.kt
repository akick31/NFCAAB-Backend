package com.nfcaab.backend.service.scheduler

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.GameStatus
import com.nfcaab.backend.model.Game.GameType
import com.nfcaab.backend.enums.team.TeamSide
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.service.discord.DiscordService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.nfcaab.backend.service.user.UserService
import com.nfcaab.backend.service.scorebug.ScorebugService
import com.nfcaab.backend.service.game.GameService
import com.nfcaab.backend.service.game.GameLifecycleService
import com.nfcaab.backend.service.atbat.AtBatService

class DelayOfGameMonitorTest {
    private lateinit var gameService: GameService
    private lateinit var gameLifecycleService: GameLifecycleService
    private lateinit var userService: UserService
    private lateinit var atBatService: AtBatService
    private lateinit var discordService: DiscordService
    private lateinit var scorebugService: ScorebugService
    private lateinit var atBatRepository: AtBatRepository
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
        delayOfGameMonitor = DelayOfGameMonitor(
            gameService,
            gameLifecycleService,
            userService,
            atBatService,
            discordService,
            scorebugService,
            atBatRepository
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
    fun `checkForDelayOfGame should process expired timers`() {
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

        val savedAtBat = com.nfcaab.backend.model.AtBat().apply {
            id = 1
            gameId = 1
        }
        val savedUser = com.nfcaab.backend.model.User().apply {
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
    }
}

