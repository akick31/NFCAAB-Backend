package com.nfcaab.backend.service.auth

import com.nfcaab.backend.dto.website.LoginResponse
import com.nfcaab.backend.model.NewSignup
import com.nfcaab.backend.model.User
import com.nfcaab.backend.service.email.EmailService
import com.nfcaab.backend.service.nfcaab.NewSignupService
import com.nfcaab.backend.service.nfcaab.UserService
import com.nfcaab.backend.util.UserUnauthorizedException
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class AuthServiceTest {
    private lateinit var emailService: EmailService
    private lateinit var userService: UserService
    private lateinit var newSignupService: NewSignupService
    private lateinit var sessionService: SessionService
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        emailService = mockk()
        userService = mockk()
        newSignupService = mockk()
        sessionService = mockk()
        passwordEncoder = mockk()
        authService = AuthService(emailService, userService, newSignupService, sessionService, passwordEncoder)
    }

    @Test
    fun `createNewSignup should create signup and send verification email`() {
        val newSignup = NewSignup().apply {
            id = 1
            username = "testuser"
            email = "test@example.com"
            verificationToken = UUID.randomUUID().toString()
        }

        every { newSignupService.createNewSignup(newSignup) } returns newSignup
        every { emailService.sendVerificationEmail(any(), any(), any()) } just Runs

        val result = authService.createNewSignup(newSignup)

        assertEquals(newSignup, result)
        verify { newSignupService.createNewSignup(newSignup) }
        verify { emailService.sendVerificationEmail(newSignup.email, newSignup.id, newSignup.verificationToken ?: "") }
    }

    @Test
    fun `login should return LoginResponse with token when password is correct`() {
        val usernameOrEmail = "testuser"
        val password = "password123"
        val encodedPassword = "encodedPassword"
        val token = "abc123"
        val userRole = com.nfcaab.backend.enums.user.UserRole.USER

        val testUser = User().apply {
            id = 1
            username = usernameOrEmail
            this.password = encodedPassword
            role = userRole
        }

        every { userService.getUserByUsernameOrEmail(usernameOrEmail) } returns testUser
        every { passwordEncoder.matches(password, encodedPassword) } returns true
        every { sessionService.generateToken(testUser.id) } returns token

        val result = authService.login(usernameOrEmail, password)

        assertEquals(LoginResponse(token, testUser.id, userRole), result)
        verify { passwordEncoder.matches(password, encodedPassword) }
        verify { sessionService.generateToken(user.id) }
    }

    @Test
    fun `login should throw exception when password is incorrect`() {
        val usernameOrEmail = "testuser"
        val password = "wrongPassword"
        val encodedPassword = "encodedPassword"

        val user = User().apply {
            id = 1
            username = usernameOrEmail
            this.password = encodedPassword
        }

        every { userService.getUserByUsernameOrEmail(usernameOrEmail) } returns user
        every { passwordEncoder.matches(password, encodedPassword) } returns false

        org.junit.jupiter.api.assertThrows<UserUnauthorizedException> {
            authService.login(usernameOrEmail, password)
        }
        verify { userService.getUserByUsernameOrEmail(usernameOrEmail) }
        verify { passwordEncoder.matches(password, encodedPassword) }
    }

    @Test
    fun `logout should blacklist session`() {
        val token = "abc123"

        every { sessionService.blacklistUserSession(token) } just Runs

        val result = authService.logout(token)

        assertEquals("User logged out successfully", result)
        verify { sessionService.blacklistUserSession(token) }
    }

    @Test
    fun `verifyEmail should approve signup`() {
        val token = "verificationToken"
        val newSignup = NewSignup().apply {
            id = 1
            verificationToken = token
        }

        every { newSignupService.getByVerificationToken(token) } returns newSignup
        every { newSignupService.approveNewSignup(newSignup) } returns true

        val result = authService.verifyEmail(token)

        assertEquals(true, result)
        verify { newSignupService.getByVerificationToken(token) }
        verify { newSignupService.approveNewSignup(newSignup) }
    }

    @Test
    fun `resetVerificationToken should generate new token and send email`() {
        val id = 1L
        val newToken = UUID.randomUUID().toString()
        val newSignup = NewSignup().apply {
            this.id = id
            email = "test@example.com"
            verificationToken = "oldToken"
        }

        every { newSignupService.getNewSignupById(id) } returns newSignup
        every { newSignupService.saveNewSignup(newSignup) } returns newSignup
        every { emailService.sendVerificationEmail(any(), any(), any()) } just Runs

        val result = authService.resetVerificationToken(id)

        assertNotNull(result.verificationToken)
        verify { emailService.sendVerificationEmail(newSignup.email ?: "", newSignup.id, any()) }
    }

    @Test
    fun `forgotPassword should send reset email`() {
        val email = "test@example.com"
        val user = User().apply {
            id = 1
            this.email = email
            resetToken = "resetToken123"
        }

        every { userService.updateResetToken(email) } returns user
        every { emailService.sendPasswordResetEmail(any(), any(), any()) } just Runs

        val result = authService.forgotPassword(email)

        assertEquals(ResponseEntity.ok("Reset email sent"), result)
        verify { emailService.sendPasswordResetEmail(user.email, user.id, user.resetToken ?: "") }
    }
}

