package com.nfcaab.backend.controllers

import com.nfcaab.backend.enums.team.Conference
import com.nfcaab.backend.service.nfcaab.GameSpecificationService.GameCategory
import com.nfcaab.backend.service.nfcaab.GameSpecificationService.GameFilter
import com.nfcaab.backend.service.nfcaab.GameSpecificationService.GameSort
import com.nfcaab.backend.service.nfcaab.ScorebugService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class ScorebugControllerTest {
    private lateinit var scorebugService: ScorebugService
    private lateinit var scorebugController: ScorebugController

    @BeforeEach
    fun setUp() {
        scorebugService = mockk()
        scorebugController = ScorebugController(scorebugService)
    }

    @Test
    fun `generateAllScorebugs should call service`() {
        every { scorebugService.generateAllScorebugs() } returns Unit

        scorebugController.generateAllScorebugs()

        verify { scorebugService.generateAllScorebugs() }
    }

    @Test
    fun `getScorebugByGameId should return scorebug response entity`() {
        val gameId = 1
        val scorebugBytes = byteArrayOf(1, 2, 3, 4, 5)
        val responseEntity = org.springframework.http.ResponseEntity.ok(scorebugBytes)

        every { scorebugService.getScorebugByGameId(gameId) } returns responseEntity

        val result = scorebugController.getScorebugByGameId(gameId)

        assertNotNull(result)
        assertEquals(scorebugBytes, result.body)
        verify { scorebugService.getScorebugByGameId(gameId) }
    }

    @Test
    fun `getLatestScorebugByGameId should return scorebug response entity`() {
        val gameId = 1
        val scorebugBytes = byteArrayOf(1, 2, 3, 4, 5)
        val responseEntity = org.springframework.http.ResponseEntity.ok(scorebugBytes)

        every { scorebugService.getLatestScorebugByGameId(gameId) } returns responseEntity

        val result = scorebugController.getLatestScorebugByGameId(gameId)

        assertNotNull(result)
        assertEquals(scorebugBytes, result.body)
        verify { scorebugService.getLatestScorebugByGameId(gameId) }
    }

    @Test
    fun `getScorebugsForConference should return response entity`() {
        val season = 2024
        val week = 1
        val conference = Conference.ACC
        val scorebugs = listOf(
            mapOf<String, Any>("gameId" to 1, "scorebug" to byteArrayOf(1, 2, 3)),
            mapOf<String, Any>("gameId" to 2, "scorebug" to byteArrayOf(4, 5, 6))
        )
        val responseEntity = org.springframework.http.ResponseEntity.ok(scorebugs)

        every { scorebugService.getScorebugsForConference(season, week, conference) } returns responseEntity

        val result = scorebugController.getScorebugsForConference(season, week, conference)

        assertNotNull(result)
        assertNotNull(result.body)
        verify { scorebugService.getScorebugsForConference(season, week, conference) }
    }

    @Test
    fun `getFilteredScorebugs should return paginated scorebugs`() {
        val pageable = mockk<Pageable>()
        val scorebugResponse = com.nfcaab.backend.dto.response.ScorebugResponse(
            gameId = 1,
            scorebug = byteArrayOf(1, 2, 3),
            homeTeam = "Team A",
            awayTeam = "Team B",
            status = com.nfcaab.backend.model.Game.GameStatus.IN_PROGRESS
        )
        val mockPage = PageImpl(listOf(scorebugResponse))
        val responseEntity = org.springframework.http.ResponseEntity.ok(mockPage)

        every {
            scorebugService.getFilteredScorebugs(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                pageable,
            )
        } returns responseEntity

        val result = scorebugController.getFilteredScorebugs(
            null,
            null,
            GameSort.CLOSEST_TO_END,
            null,
            null,
            null,
            pageable,
        )

        assertNotNull(result)
        verify {
            scorebugService.getFilteredScorebugs(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                pageable,
            )
        }
    }
}

