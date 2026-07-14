package com.nfcaab.backend.service.lineup

import com.nfcaab.backend.dto.requests.BatterSubmission
import com.nfcaab.backend.dto.requests.LineupSubmissionRequest
import com.nfcaab.backend.model.GameLineup
import com.nfcaab.backend.model.LineupToken
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.repositories.GameLineupRepository
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.util.InvalidLineupException
import com.nfcaab.backend.util.PlayerNotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.nfcaab.backend.service.player.PlayerService

class LineupServiceTest {
    private lateinit var gameLineupRepository: GameLineupRepository
    private lateinit var playerService: PlayerService
    private lateinit var gameRepository: GameRepository
    private lateinit var lineupTokenService: LineupTokenService
    private lateinit var lineupService: LineupService

    @BeforeEach
    fun setUp() {
        gameLineupRepository = mockk()
        playerService = mockk()
        gameRepository = mockk()
        lineupTokenService = mockk()
        lineupService = LineupService(gameLineupRepository, playerService, gameRepository, lineupTokenService)
    }

    private fun tokenFor(
        gameId: Int,
        team: String,
    ) = LineupToken().apply {
        this.token = "test-token"
        this.gameId = gameId
        this.team = team
    }

    private fun fullLineupBatters(pitcherNumberAlias: Int? = null) =
        listOf(
            BatterSubmission(1, Player.Position.CATCHER),
            BatterSubmission(2, Player.Position.FIRST_BASE),
            BatterSubmission(3, Player.Position.SECOND_BASE),
            BatterSubmission(4, Player.Position.THIRD_BASE),
            BatterSubmission(5, Player.Position.SHORTSTOP),
            BatterSubmission(6, Player.Position.LEFT_FIELD),
            BatterSubmission(7, Player.Position.CENTER_FIELD),
            BatterSubmission(8, Player.Position.RIGHT_FIELD),
            BatterSubmission(pitcherNumberAlias ?: 9, Player.Position.DESIGNATED_HITTER),
        )

    @Test
    fun `saveLineup should save valid lineup`() {
        val request =
            LineupSubmissionRequest(
                token = "test-token",
                batters = fullLineupBatters(),
                pitcher = 10,
            )
        val lineupToken = tokenFor(1, "Team A")

        val player = Player().apply {
            firstName = "John"
            lastName = "Doe"
            uniformNumber = 1
            currentTeam = "Team A"
        }

        every { lineupTokenService.validateToken("test-token") } returns lineupToken
        every { gameLineupRepository.getAllLineupsByGameIdAndTeam(1, "Team A") } returns emptyList()
        every { gameLineupRepository.deleteAll(any()) } returns Unit
        every { playerService.getPlayerByNumberAndTeam(any(), any()) } returns player
        every { gameLineupRepository.save(any()) } returns mockk<GameLineup>()
        every { lineupTokenService.consumeToken(lineupToken) } returns lineupToken

        val result = lineupService.saveLineup(request)

        assertEquals(10, result.size) // 9 batters + 1 pitcher
        verify { lineupTokenService.validateToken("test-token") }
        verify { lineupTokenService.consumeToken(lineupToken) }
        verify { gameLineupRepository.save(any()) }
    }

    @Test
    fun `saveLineup should throw exception when lineup has wrong number of batters`() {
        val request =
            LineupSubmissionRequest(
                token = "test-token",
                batters = fullLineupBatters().dropLast(1), // Only 8 batters
                pitcher = 10,
            )
        val lineupToken = tokenFor(1, "Team A")

        every { lineupTokenService.validateToken("test-token") } returns lineupToken

        assertThrows<InvalidLineupException> {
            lineupService.saveLineup(request)
        }
    }

    @Test
    fun `saveLineup should throw exception when a position is missing`() {
        val request =
            LineupSubmissionRequest(
                token = "test-token",
                batters = fullLineupBatters().dropLast(1) + BatterSubmission(9, Player.Position.CATCHER),
                pitcher = 10,
            )
        val lineupToken = tokenFor(1, "Team A")

        every { lineupTokenService.validateToken("test-token") } returns lineupToken

        assertThrows<InvalidLineupException> {
            lineupService.saveLineup(request)
        }
    }

    @Test
    fun `saveLineup should throw exception when pitcher is also in the batting lineup`() {
        val request =
            LineupSubmissionRequest(
                token = "test-token",
                batters = fullLineupBatters(pitcherNumberAlias = 10),
                pitcher = 10,
            )
        val lineupToken = tokenFor(1, "Team A")

        every { lineupTokenService.validateToken("test-token") } returns lineupToken

        assertThrows<InvalidLineupException> {
            lineupService.saveLineup(request)
        }
    }

    @Test
    fun `saveLineup should reject a starting pitcher who has not rested enough games`() {
        val request =
            LineupSubmissionRequest(
                token = "test-token",
                batters = fullLineupBatters(),
                pitcher = 10,
            )
        val lineupToken = tokenFor(5, "Team A")

        val batter = Player().apply {
            firstName = "John"
            lastName = "Doe"
            currentTeam = "Team A"
        }
        val restingStarter = Player().apply {
            firstName = "Jane"
            lastName = "Smith"
            currentTeam = "Team A"
            pitcherRole = Player.PitcherRole.STARTER
            lastStartGameId = 3
        }

        every { lineupTokenService.validateToken("test-token") } returns lineupToken
        every { playerService.getPlayerByNumberAndTeam("Team A", 10) } returns restingStarter
        every { playerService.getPlayerByNumberAndTeam("Team A", match { it != 10 }) } returns batter
        every { gameRepository.countFinishedGamesByTeamSinceGameId("Team A", 3) } returns 2

        assertThrows<InvalidLineupException> {
            lineupService.saveLineup(request)
        }
    }

    @Test
    fun `substituteBatter should replace the outgoing player at the same lineup spot`() {
        val outgoingEntry =
            GameLineup().apply {
                id = 1
                gameId = "1"
                team = "Team A"
                lineupSpot = "3"
                uniformNumber = 5
                position = "Second Base"
                currentlyPlaying = true
            }
        val incomingPlayer = Player().apply {
            firstName = "New"
            lastName = "Guy"
            uniformNumber = 15
            currentTeam = "Team A"
        }

        every { gameLineupRepository.getCurrentLineupEntryByUniformNumber(1, "Team A", 5) } returns outgoingEntry
        every { playerService.getPlayerByNumberAndTeam("Team A", 15) } returns incomingPlayer
        every { gameLineupRepository.getAllLineupsByGameIdAndTeam(1, "Team A") } returns listOf(outgoingEntry)
        every { gameLineupRepository.save(any()) } answers { firstArg() }

        val result = lineupService.substituteBatter(1, "Team A", 5, 15, Player.Position.SECOND_BASE)

        assertEquals("3", result.lineupSpot)
        assertEquals(15, result.uniformNumber)
        assertEquals(false, outgoingEntry.currentlyPlaying)
    }

    @Test
    fun `substituteBatter should reject an incoming player already in the game`() {
        val outgoingEntry =
            GameLineup().apply {
                lineupSpot = "3"
                uniformNumber = 5
                position = "Second Base"
                currentlyPlaying = true
            }
        val alreadyPlaying =
            GameLineup().apply {
                lineupSpot = "4"
                uniformNumber = 15
                currentlyPlaying = true
            }
        val incomingPlayer = Player().apply { currentTeam = "Team A"; uniformNumber = 15 }

        every { gameLineupRepository.getCurrentLineupEntryByUniformNumber(1, "Team A", 5) } returns outgoingEntry
        every { playerService.getPlayerByNumberAndTeam("Team A", 15) } returns incomingPlayer
        every { gameLineupRepository.getAllLineupsByGameIdAndTeam(1, "Team A") } returns listOf(outgoingEntry, alreadyPlaying)

        assertThrows<InvalidLineupException> {
            lineupService.substituteBatter(1, "Team A", 5, 15, Player.Position.SECOND_BASE)
        }
    }

    @Test
    fun `substituteBatter should reject the pitcher lineup spot`() {
        val outgoingEntry =
            GameLineup().apply {
                lineupSpot = "P"
                uniformNumber = 20
                position = "Pitcher"
                currentlyPlaying = true
            }

        every { gameLineupRepository.getCurrentLineupEntryByUniformNumber(1, "Team A", 20) } returns outgoingEntry

        assertThrows<InvalidLineupException> {
            lineupService.substituteBatter(1, "Team A", 20, 15, Player.Position.SECOND_BASE)
        }
    }

    @Test
    fun `substituteBatter should throw when the outgoing player is not currently in the lineup`() {
        every { gameLineupRepository.getCurrentLineupEntryByUniformNumber(1, "Team A", 5) } returns null

        assertThrows<PlayerNotFoundException> {
            lineupService.substituteBatter(1, "Team A", 5, 15, Player.Position.SECOND_BASE)
        }
    }

    @Test
    fun `substitutePitcher should replace the current pitcher and respect the rest rule`() {
        val outgoingPitcherEntry =
            GameLineup().apply {
                lineupSpot = "P"
                uniformNumber = 20
                position = "Pitcher"
                currentlyPlaying = true
            }
        val incomingPitcher = Player().apply {
            firstName = "Relief"
            lastName = "Guy"
            uniformNumber = 21
            currentTeam = "Team A"
            pitcherRole = Player.PitcherRole.RELIEVER
        }

        every { gameLineupRepository.getPitcherByTeam(1, "Team A") } returns outgoingPitcherEntry
        every { playerService.getPlayerByNumberAndTeam("Team A", 21) } returns incomingPitcher
        every { gameLineupRepository.save(any()) } answers { firstArg() }

        val result = lineupService.substitutePitcher(1, "Team A", 21)

        assertEquals("P", result.lineupSpot)
        assertEquals(21, result.uniformNumber)
        assertEquals(false, outgoingPitcherEntry.currentlyPlaying)
    }

    @Test
    fun `substitutePitcher should reject a resting starter`() {
        val outgoingPitcherEntry =
            GameLineup().apply {
                lineupSpot = "P"
                uniformNumber = 20
                position = "Pitcher"
                currentlyPlaying = true
            }
        val restingStarter = Player().apply {
            firstName = "Ace"
            lastName = "Starter"
            uniformNumber = 22
            currentTeam = "Team A"
            pitcherRole = Player.PitcherRole.STARTER
            lastStartGameId = 3
        }

        every { gameLineupRepository.getPitcherByTeam(1, "Team A") } returns outgoingPitcherEntry
        every { playerService.getPlayerByNumberAndTeam("Team A", 22) } returns restingStarter
        every { gameRepository.countFinishedGamesByTeamSinceGameId("Team A", 3) } returns 1

        assertThrows<InvalidLineupException> {
            lineupService.substitutePitcher(1, "Team A", 22)
        }
    }

    @Test
    fun `getCurrentPosition should return the position from the current lineup entry`() {
        val entry =
            GameLineup().apply {
                uniformNumber = 5
                position = "Second Base"
                currentlyPlaying = true
            }

        every { gameLineupRepository.getCurrentLineupEntryByUniformNumber(1, "Team A", 5) } returns entry

        val result = lineupService.getCurrentPosition(1, "Team A", 5)

        assertEquals(Player.Position.SECOND_BASE, result)
    }
}
