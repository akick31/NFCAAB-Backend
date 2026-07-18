package com.nfcaab.backend.controllers

import com.nfcaab.backend.model.NewSignup
import com.nfcaab.backend.service.auth.AuthCookieService
import com.nfcaab.backend.service.auth.AuthService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthControllerTest {
    private lateinit var authService: AuthService
    private lateinit var authCookieService: AuthCookieService
    private lateinit var authController: AuthController

    @BeforeEach
    fun setUp() {
        authService = mockk()
        authCookieService = mockk()
        authController = AuthController(authService, authCookieService)
    }

    @Test
    fun `registerUser should call authService createNewSignup`() {
        val newSignup = mockk<NewSignup>()
        val expectedResult = mockk<NewSignup>()
        every { authService.createNewSignup(newSignup) } returns expectedResult

        val result = authController.registerUser(newSignup)

        assertEquals(expectedResult, result)
        verify { authService.createNewSignup(newSignup) }
    }

    @Test
    fun `login should call authService login`() {
        val usernameOrEmail = "testuser"
        val password = "password123"
        val response: HttpServletResponse = mockk()
        val expectedResult = mockk<com.nfcaab.backend.dto.website.LoginResponse>()
        every { authService.login(usernameOrEmail, password, response) } returns expectedResult

        val result = authController.login(usernameOrEmail, password, response)

        assertEquals(expectedResult, result)
        verify { authService.login(usernameOrEmail, password, response) }
    }

    @Test
    fun `logout should call authService logout with the cookie's token`() {
        val token = "testToken123"
        val request: HttpServletRequest = mockk()
        val response: HttpServletResponse = mockk()
        val expectedResult = "User logged out successfully"
        every { authCookieService.readAuthCookie(request) } returns token
        every { authService.logout(token, response) } returns expectedResult

        val result = authController.logout(request, response)

        assertEquals(expectedResult, result)
        verify { authService.logout(token, response) }
    }

    @Test
    fun `verifyEmail should call authService verifyEmail`() {
        val token = "verificationToken123"
        every { authService.verifyEmail(token) } returns true

        val result = authController.verifyEmail(token)

        assertEquals(true, result)
        verify { authService.verifyEmail(token) }
    }

    @Test
    fun `resetVerificationToken should call authService resetVerificationToken`() {
        val id = 1L
        val expectedResult = mockk<NewSignup>()
        every { authService.resetVerificationToken(id) } returns expectedResult

        val result = authController.resetVerificationToken(id)

        assertEquals(expectedResult, result)
        verify { authService.resetVerificationToken(id) }
    }

    @Test
    fun `forgotPassword should call authService forgotPassword`() {
        val email = "test@example.com"
        val expectedResult = ResponseEntity.ok("Reset email sent")
        every { authService.forgotPassword(email) } returns expectedResult

        val result = authController.forgotPassword(email)

        assertEquals(expectedResult, result)
        verify { authService.forgotPassword(email) }
    }

    @Test
    fun `resetPassword should call authService resetPassword`() {
        val token = "resetToken123"
        val userId = 1L
        val newPassword = "newPassword123"
        val expectedResult = ResponseEntity.ok("Password updated successfully")
        every { authService.resetPassword(token, userId, newPassword) } returns expectedResult

        val result = authController.resetPassword(token, userId, newPassword)

        assertEquals(expectedResult, result)
        verify { authService.resetPassword(token, userId, newPassword) }
    }
}

