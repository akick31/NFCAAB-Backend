package com.nfcaab.backend.service.game

import com.nfcaab.backend.dto.GameScenario
import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.InningHalf
import com.nfcaab.backend.model.GameWriteup
import com.nfcaab.backend.repositories.GameWriteupRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameWriteupServiceTest {
    private lateinit var gameWriteupRepository: GameWriteupRepository
    private lateinit var gameWriteupService: GameWriteupService

    @BeforeEach
    fun setUp() {
        gameWriteupRepository = mockk()
        gameWriteupService = GameWriteupService(gameWriteupRepository)
    }

    @Test
    fun `getGameMessageByScenario should return random writeup when found`() {
        val gameScenario = GameScenario(
            result = ActualResult.SINGLE,
            batterOnFirst = true,
            batterOnSecond = false,
            batterOnThird = false,
            runsScored = 0,
            inning = 1,
            inningHalf = InningHalf.TOP,
            outs = 0,
        )
        val writeups = listOf(
            GameWriteup().apply {
                gameWriteup = "John Doe hits a single!"
            },
            GameWriteup().apply {
                gameWriteup = "Base hit for John Doe!"
            }
        )

        every {
            gameWriteupRepository.findByScenario(
                gameScenario.result.description,
                gameScenario.batterOnFirst,
                gameScenario.batterOnSecond,
                gameScenario.batterOnThird,
                gameScenario.runsScored
            )
        } returns writeups

        val result = gameWriteupService.getGameMessageByScenario(gameScenario)

        assertEquals(true, writeups.contains(writeups.first { it.gameWriteup == result }))
        verify {
            gameWriteupRepository.findByScenario(
                gameScenario.result.description,
                gameScenario.batterOnFirst,
                gameScenario.batterOnSecond,
                gameScenario.batterOnThird,
                gameScenario.runsScored
            )
        }
    }

    @Test
    fun `getGameMessageByScenario should return default message when no writeups found`() {
        val gameScenario = GameScenario(
            result = ActualResult.SINGLE,
            batterOnFirst = false,
            batterOnSecond = false,
            batterOnThird = false,
            runsScored = 0,
            inning = 1,
            inningHalf = InningHalf.TOP,
            outs = 0,
        )

        every {
            gameWriteupRepository.findByScenario(
                gameScenario.result.description,
                gameScenario.batterOnFirst,
                gameScenario.batterOnSecond,
                gameScenario.batterOnThird,
                gameScenario.runsScored
            )
        } returns emptyList()

        val result = gameWriteupService.getGameMessageByScenario(gameScenario)

        assertEquals("No message found", result)
        verify {
            gameWriteupRepository.findByScenario(
                gameScenario.result.description,
                gameScenario.batterOnFirst,
                gameScenario.batterOnSecond,
                gameScenario.batterOnThird,
                gameScenario.runsScored
            )
        }
    }
}

