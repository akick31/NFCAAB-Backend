package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.LineupToken
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.repositories.LineupTokenRepository
import com.nfcaab.backend.util.GameNotFoundException
import com.nfcaab.backend.util.InvalidLineupTokenException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class LineupTokenServiceTest {
    private lateinit var lineupTokenRepository: LineupTokenRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var lineupTokenService: LineupTokenService

    @BeforeEach
    fun setUp() {
        lineupTokenRepository = mockk()
        gameRepository = mockk()
        lineupTokenService = LineupTokenService(lineupTokenRepository, gameRepository)
    }

    @Test
    fun `generateToken should create a token when the team is part of the game`() {
        val game = Game().apply {
            id = 1
            homeTeam = "Team A"
            awayTeam = "Team B"
        }
        val savedTokenSlot = slot<LineupToken>()

        every { gameRepository.getGameById(1) } returns game
        every { lineupTokenRepository.save(capture(savedTokenSlot)) } answers { savedTokenSlot.captured }

        val result = lineupTokenService.generateToken(1, "Team A")

        assertEquals(1, result.gameId)
        assertEquals("Team A", result.team)
        assertNotNull(result.token)
        assertEquals(false, result.used)
    }

    @Test
    fun `generateToken should throw when the game does not exist`() {
        every { gameRepository.getGameById(1) } returns null

        assertThrows<GameNotFoundException> {
            lineupTokenService.generateToken(1, "Team A")
        }
    }

    @Test
    fun `generateToken should throw when the team is not part of the game`() {
        val game = Game().apply {
            id = 1
            homeTeam = "Team A"
            awayTeam = "Team B"
        }
        every { gameRepository.getGameById(1) } returns game

        assertThrows<InvalidLineupTokenException> {
            lineupTokenService.generateToken(1, "Team C")
        }
    }

    @Test
    fun `validateToken should return the token when valid`() {
        val lineupToken = LineupToken().apply {
            token = "test-token"
            used = false
            expiresAt = LocalDateTime.now().plusHours(1)
        }
        every { lineupTokenRepository.getByToken("test-token") } returns lineupToken

        val result = lineupTokenService.validateToken("test-token")

        assertEquals(lineupToken, result)
    }

    @Test
    fun `validateToken should throw when the token does not exist`() {
        every { lineupTokenRepository.getByToken("missing") } returns null

        assertThrows<InvalidLineupTokenException> {
            lineupTokenService.validateToken("missing")
        }
    }

    @Test
    fun `validateToken should throw when the token was already used`() {
        val lineupToken = LineupToken().apply {
            token = "test-token"
            used = true
            expiresAt = LocalDateTime.now().plusHours(1)
        }
        every { lineupTokenRepository.getByToken("test-token") } returns lineupToken

        assertThrows<InvalidLineupTokenException> {
            lineupTokenService.validateToken("test-token")
        }
    }

    @Test
    fun `validateToken should throw when the token has expired`() {
        val lineupToken = LineupToken().apply {
            token = "test-token"
            used = false
            expiresAt = LocalDateTime.now().minusHours(1)
        }
        every { lineupTokenRepository.getByToken("test-token") } returns lineupToken

        assertThrows<InvalidLineupTokenException> {
            lineupTokenService.validateToken("test-token")
        }
    }

    @Test
    fun `consumeToken should mark the token as used`() {
        val lineupToken = LineupToken().apply {
            token = "test-token"
            used = false
        }
        every { lineupTokenRepository.save(lineupToken) } returns lineupToken

        lineupTokenService.consumeToken(lineupToken)

        assertEquals(true, lineupToken.used)
        verify { lineupTokenRepository.save(lineupToken) }
    }
}
