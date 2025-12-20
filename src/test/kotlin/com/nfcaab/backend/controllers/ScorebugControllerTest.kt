package com.nfcaab.backend.controllers

import com.nfcaab.backend.enums.team.Conference
import com.nfcaab.backend.service.nfcaab.GameSpecificationService.GameCategory
import com.nfcaab.backend.service.nfcaab.GameSpecificationService.GameFilter
import com.nfcaab.backend.service.nfcaab.GameSpecificationService.GameSort
import com.nfcaab.backend.service.nfcaab.ScorebugService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    fun `generateAllScorebugs should return success message`() {
        val expectedResult = "Scorebugs generated successfully"

        every { scorebugService.generateAllScorebugs() } returns expectedResult

        val result = scorebugController.generateAllScorebugs()

        assertEquals(expectedResult, result)
        verify { scorebugService.generateAllScorebugs() }
    }

    @Test
    fun `getScorebugByGameId should return scorebug bytes`() {
        val gameId = 1
        val scorebugBytes = byteArrayOf(1, 2, 3, 4, 5)

        every { scorebugService.getScorebugByGameId(gameId) } returns scorebugBytes

        val result = scorebugController.getScorebugByGameId(gameId)

        assertNotNull(result)
        verify { scorebugService.getScorebugByGameId(gameId) }
    }

    @Test
    fun `getLatestScorebugByGameId should return scorebug bytes`() {
        val gameId = 1
        val scorebugBytes = byteArrayOf(1, 2, 3, 4, 5)

        every { scorebugService.getLatestScorebugByGameId(gameId) } returns scorebugBytes

        val result = scorebugController.getLatestScorebugByGameId(gameId)

        assertNotNull(result)
        verify { scorebugService.getLatestScorebugByGameId(gameId) }
    }

    @Test
    fun `getScorebugsForConference should return list of scorebugs`() {
        val season = 2024
        val week = 1
        val conference = Conference.ACC
        val scorebugs = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))

        every { scorebugService.getScorebugsForConference(season, week, conference) } returns scorebugs

        val result = scorebugController.getScorebugsForConference(season, week, conference)

        assertNotNull(result)
        assertEquals(2, result.size)
        verify { scorebugService.getScorebugsForConference(season, week, conference) }
    }

    @Test
    fun `getFilteredScorebugs should return paginated scorebugs`() {
        val pageable = mockk<Pageable>()
        val scorebugs = listOf(byteArrayOf(1, 2, 3))
        val mockPage = PageImpl(scorebugs)

        every {
            scorebugService.getFilteredScorebugs(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                pageable,
            )
        } returns mockPage

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
                any(),
                pageable,
            )
        }
    }
}

