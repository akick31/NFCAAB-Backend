package com.nfcaab.backend.controllers

import com.nfcaab.backend.model.NewSignup
import com.nfcaab.backend.service.nfcaab.NewSignupService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NewSignupControllerTest {
    private lateinit var newSignupService: NewSignupService
    private lateinit var newSignupController: NewSignupController

    @BeforeEach
    fun setUp() {
        newSignupService = mockk()
        newSignupController = NewSignupController(newSignupService)
    }

    @Test
    fun `getNewSignups should return list of new signups`() {
        val signups = listOf(
            NewSignup().apply {
                id = 1
                username = "user1"
            },
            NewSignup().apply {
                id = 2
                username = "user2"
            },
        )

        every { newSignupService.getNewSignups() } returns signups

        val result = newSignupController.getNewSignups()

        assertEquals(signups, result)
        verify { newSignupService.getNewSignups() }
    }
}

