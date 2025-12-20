package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.Player
import com.nfcaab.backend.repositories.PlayerRepository
import com.nfcaab.backend.util.PlayerNotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlayerServiceTest {
    private lateinit var playerRepository: PlayerRepository
    private lateinit var playerService: PlayerService

    @BeforeEach
    fun setUp() {
        playerRepository = mockk()
        playerService = PlayerService(playerRepository)
    }

    @Test
    fun `getPlayerByNumberAndTeam should return player when found`() {
        val team = "Team A"
        val uniformNumber = 10
        val player = Player().apply {
            this.team = team
            this.uniformNumber = uniformNumber
            name = "John Doe"
        }

        every { playerRepository.getPlayerByNumberAndTeam(team, uniformNumber) } returns player

        val result = playerService.getPlayerByNumberAndTeam(team, uniformNumber)

        assertEquals(player, result)
        verify { playerRepository.getPlayerByNumberAndTeam(team, uniformNumber) }
    }

    @Test
    fun `getPlayerByNumberAndTeam should throw exception when uniform number is null`() {
        val team = "Team A"

        assertThrows(PlayerNotFoundException::class.java) {
            playerService.getPlayerByNumberAndTeam(team, null)
        }
    }

    @Test
    fun `getPlayerByNumberAndTeam should throw exception when player not found`() {
        val team = "Team A"
        val uniformNumber = 10

        every { playerRepository.getPlayerByNumberAndTeam(team, uniformNumber) } returns null

        assertThrows(PlayerNotFoundException::class.java) {
            playerService.getPlayerByNumberAndTeam(team, uniformNumber)
        }
        verify { playerRepository.getPlayerByNumberAndTeam(team, uniformNumber) }
    }
}

