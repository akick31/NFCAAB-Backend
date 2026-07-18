package com.nfcaab.backend.service.player

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
            this.currentTeam = team
            this.uniformNumber = uniformNumber
            firstName = "John"
            lastName = "Doe"
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

    @Test
    fun `getPlayersByTeam should return active players for the team`() {
        val team = "Team A"
        val players = listOf(
            Player().apply { currentTeam = team; active = true },
            Player().apply { currentTeam = team; active = true },
        )

        every { playerRepository.findByCurrentTeamAndActive(team, true) } returns players

        val result = playerService.getPlayersByTeam(team)

        assertEquals(players, result)
        verify { playerRepository.findByCurrentTeamAndActive(team, true) }
    }

    @Test
    fun `rolloverEligibilityForNewSeason should advance non-senior players by one year`() {
        val sophomore = Player().apply { collegeYear = Player.CollegeYear.SOPHOMORE }

        every { playerRepository.findAll() } returns listOf(sophomore)
        every { playerRepository.save(any()) } answers { firstArg() }

        val graduated = playerService.rolloverEligibilityForNewSeason()

        assertEquals(Player.CollegeYear.JUNIOR, sophomore.collegeYear)
        assertEquals(true, sophomore.active)
        assertEquals(emptyList<Player>(), graduated)
    }

    @Test
    fun `rolloverEligibilityForNewSeason should graduate and deactivate seniors`() {
        val senior = Player().apply {
            collegeYear = Player.CollegeYear.SENIOR
            currentTeam = "Team A"
        }

        every { playerRepository.findAll() } returns listOf(senior)
        every { playerRepository.save(any()) } answers { firstArg() }

        val graduated = playerService.rolloverEligibilityForNewSeason()

        assertEquals(Player.CollegeYear.GRADUATED, senior.collegeYear)
        assertEquals(false, senior.active)
        assertEquals(null, senior.currentTeam)
        assertEquals(listOf(senior), graduated)
    }

    @Test
    fun `rolloverEligibilityForNewSeason should skip inactive players`() {
        val inactivePlayer = Player().apply {
            collegeYear = Player.CollegeYear.JUNIOR
            active = false
        }

        every { playerRepository.findAll() } returns listOf(inactivePlayer)

        val graduated = playerService.rolloverEligibilityForNewSeason()

        assertEquals(Player.CollegeYear.JUNIOR, inactivePlayer.collegeYear)
        assertEquals(emptyList<Player>(), graduated)
        verify(exactly = 0) { playerRepository.save(any()) }
    }
}

