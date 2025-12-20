package com.nfcaab.backend.controllers

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils

class HealthControllerTest {
    @Test
    fun `healthCheck should return ok when status is UP`() {
        val healthEndpoint = mockk<HealthEndpoint>()
        val healthController = HealthController()
        ReflectionTestUtils.setField(healthController, "healthEndpoint", healthEndpoint)

        val health = Health.up().build()
        every { healthEndpoint.health() } returns health

        val result = healthController.healthCheck()

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("Application is healthy", result.body)
    }

    @Test
    fun `healthCheck should return internal server error when status is DOWN`() {
        val healthEndpoint = mockk<HealthEndpoint>()
        val healthController = HealthController()
        ReflectionTestUtils.setField(healthController, "healthEndpoint", healthEndpoint)

        val health = Health.down().build()
        every { healthEndpoint.health() } returns health

        val result = healthController.healthCheck()

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.statusCode)
        assertEquals("Application is unhealthy", result.body)
    }
}

