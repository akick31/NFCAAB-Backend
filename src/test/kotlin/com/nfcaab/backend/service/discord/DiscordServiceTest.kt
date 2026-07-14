package com.nfcaab.backend.service.discord

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.util.ServerUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class DiscordServiceTest {
    private lateinit var restTemplate: RestTemplate
    private lateinit var serverUtils: ServerUtils
    private lateinit var discordService: DiscordService

    @BeforeEach
    fun setUp() {
        restTemplate = mockk()
        serverUtils = mockk()
        discordService = DiscordService(restTemplate, serverUtils)
    }

    @Test
    fun `startGameThread should return channel and thread IDs when successful`() = runBlocking {
        val game = Game().apply {
            id = 1
            homeTeam = "Home Team"
            awayTeam = "Away Team"
        }
        val responseBody = "channel123,thread456"
        val responseEntity = ResponseEntity.ok(responseBody)

        coEvery {
            serverUtils.retryWithExponentialBackoff<ResponseEntity<String>>(any(), any(), any(), any(), any())
        } returns responseEntity

        val result = discordService.startGameThread(game)

        assert(result != null)
        assert(result?.size == 2)
        coVerify {
            serverUtils.retryWithExponentialBackoff<ResponseEntity<String>>(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `startGameThread should return null on error`() = runBlocking {
        val game = Game().apply {
            id = 1
        }

        coEvery {
            serverUtils.retryWithExponentialBackoff<ResponseEntity<String>>(any(), any(), any(), any(), any())
        } throws Exception("Network error")

        val result = discordService.startGameThread(game)

        assert(result == null)
    }

    @Test
    fun `notifyDelayOfGame should call rest template`() {
        val game = Game().apply {
            id = 1
        }
        val isDelayOfGameOut = true
        val responseEntity = ResponseEntity.ok("Success")

        every {
            restTemplate.postForEntity(any<String>(), any(), String::class.java)
        } returns responseEntity

        discordService.notifyDelayOfGame(game, isDelayOfGameOut)

        verify { restTemplate.postForEntity(any<String>(), any(), String::class.java) }
    }

    @Test
    fun `notifyWarning should call rest template`() {
        val game = Game().apply {
            id = 1
        }
        val responseEntity = ResponseEntity.ok("Success")

        every {
            restTemplate.postForEntity(any<String>(), any(), String::class.java)
        } returns responseEntity

        discordService.notifyWarning(game)

        verify { restTemplate.postForEntity(any<String>(), any(), String::class.java) }
    }
}

