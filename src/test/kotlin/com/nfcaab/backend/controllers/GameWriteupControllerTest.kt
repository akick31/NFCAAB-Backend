package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.GameScenario
import com.nfcaab.backend.service.nfcaab.GameWriteupService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameWriteupControllerTest {
    private lateinit var gameWriteupService: GameWriteupService
    private lateinit var gameWriteupController: GameWriteupController

    @BeforeEach
    fun setUp() {
        gameWriteupService = mockk()
        gameWriteupController = GameWriteupController(gameWriteupService)
    }

    @Test
    fun `getGameMessageByScenario should return game message`() {
        val gameScenario = mockk<GameScenario>()
        val expectedResult = "Game message content"

        every { gameWriteupService.getGameMessageByScenario(gameScenario) } returns expectedResult

        val result = gameWriteupController.getGameMessageByScenario(gameScenario)

        assertEquals(expectedResult, result)
        verify { gameWriteupService.getGameMessageByScenario(gameScenario) }
    }
}

