package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.website.NewSignupDTO
import com.nfcaab.backend.model.NewSignup
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.nfcaab.backend.service.user.NewSignupService

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
        val signupDTOs = listOf(
            NewSignupDTO(
                id = 1,
                username = "user1",
                coachName = "Coach 1",
                discordTag = "user1#1234",
                discordId = "123",
                teamChoiceOne = "Team A",
                teamChoiceTwo = "Team B",
                teamChoiceThree = "Team C",
                approved = false,
                emailVerified = true,
            ),
            NewSignupDTO(
                id = 2,
                username = "user2",
                coachName = "Coach 2",
                discordTag = "user2#5678",
                discordId = "456",
                teamChoiceOne = "Team D",
                teamChoiceTwo = "Team E",
                teamChoiceThree = "Team F",
                approved = false,
                emailVerified = false,
            ),
        )

        every { newSignupService.getNewSignups() } returns signupDTOs

        val result = newSignupController.getNewSignups()

        assertEquals(signupDTOs, result)
        verify { newSignupService.getNewSignups() }
    }

    @Test
    fun `approveNewSignup should look up the signup and approve it`() {
        val id = 1L
        val newSignup = NewSignup().apply { this.id = id }

        every { newSignupService.getNewSignupById(id) } returns newSignup
        every { newSignupService.approveNewSignup(newSignup) } returns true

        val result = newSignupController.approveNewSignup(id)

        assertEquals(true, result)
        verify { newSignupService.approveNewSignup(newSignup) }
    }

    @Test
    fun `rejectNewSignup should look up the signup and delete it`() {
        val id = 1L
        val newSignup = NewSignup().apply { this.id = id }

        every { newSignupService.getNewSignupById(id) } returns newSignup
        every { newSignupService.deleteNewSignup(newSignup) } returns Unit

        newSignupController.rejectNewSignup(id)

        verify { newSignupService.deleteNewSignup(newSignup) }
    }
}
