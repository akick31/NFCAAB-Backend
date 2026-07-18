package com.nfcaab.backend.service.auth

import com.nfcaab.backend.dto.website.LoginResponse
import com.nfcaab.backend.model.NewSignup
import com.nfcaab.backend.model.User
import com.nfcaab.backend.service.email.EmailService
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
import javax.servlet.http.HttpServletResponse
import com.nfcaab.backend.service.user.UserService
import com.nfcaab.backend.service.user.NewSignupService

class AuthServiceTest {
    private lateinit var emailService: EmailService
    private lateinit var userService: UserService
    private lateinit var newSignupService: NewSignupService
    private lateinit var sessionService: SessionService
    private lateinit var authCookieService: AuthCookieService
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        emailService = mockk()
        userService = mockk()
        newSignupService = mockk()
        sessionService = mockk()
        authCookieService = mockk()
        passwordEncoder = mockk()
        authService =
            AuthService(emailService, userService, newSignupService, sessionService, authCookieService, passwordEncoder)
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
    fun `login should return LoginResponse and issue an auth cookie when password is correct`() {
        val usernameOrEmail = "testuser"
        val password = "password123"
        val encodedPassword = "encodedPassword"
        val userRole = User.Role.USER
        val response: HttpServletResponse = mockk(relaxed = true)

        val testUser = User().apply {
            id = 1
            username = usernameOrEmail
            this.password = encodedPassword
            role = userRole
        }

        every { userService.getUserByUsernameOrEmail(usernameOrEmail) } returns testUser
        every { passwordEncoder.matches(password, encodedPassword) } returns true
        every { authCookieService.issueAuthCookie(response, testUser.id) } just Runs

        val result = authService.login(usernameOrEmail, password, response)

        assertEquals(LoginResponse(testUser.id, userRole), result)
        verify { passwordEncoder.matches(password, encodedPassword) }
        verify { authCookieService.issueAuthCookie(response, testUser.id) }
    }

    @Test
    fun `login should throw exception when password is incorrect`() {
        val usernameOrEmail = "testuser"
        val password = "wrongPassword"
        val encodedPassword = "encodedPassword"
        val response: HttpServletResponse = mockk()

        val user = User().apply {
            id = 1
            username = usernameOrEmail
            this.password = encodedPassword
        }

        every { userService.getUserByUsernameOrEmail(usernameOrEmail) } returns user
        every { passwordEncoder.matches(password, encodedPassword) } returns false

        org.junit.jupiter.api.assertThrows<UserUnauthorizedException> {
            authService.login(usernameOrEmail, password, response)
        }
        verify { userService.getUserByUsernameOrEmail(usernameOrEmail) }
        verify { passwordEncoder.matches(password, encodedPassword) }
    }

    @Test
    fun `logout should blacklist session and clear the auth cookie`() {
        val token = "abc123"
        val response: HttpServletResponse = mockk(relaxed = true)

        every { sessionService.blacklistUserSession(token) } just Runs
        every { authCookieService.clearAuthCookie(response) } just Runs

        val result = authService.logout(token, response)

        assertEquals("User logged out successfully", result)
        verify { sessionService.blacklistUserSession(token) }
        verify { authCookieService.clearAuthCookie(response) }
    }

    @Test
    fun `logout should clear the auth cookie even when no token was present`() {
        val response: HttpServletResponse = mockk(relaxed = true)

        every { authCookieService.clearAuthCookie(response) } just Runs

        val result = authService.logout(null, response)

        assertEquals("User logged out successfully", result)
        verify { authCookieService.clearAuthCookie(response) }
    }

    @Test
    fun `verifyEmail should mark the signup's email as verified without approving it`() {
        val token = "verificationToken"
        val newSignup = NewSignup().apply {
            id = 1
            verificationToken = token
        }

        every { newSignupService.getByVerificationToken(token) } returns newSignup
        every { newSignupService.saveNewSignup(newSignup) } returns newSignup

        val result = authService.verifyEmail(token)

        assertEquals(true, result)
        assertEquals(true, newSignup.emailVerified)
        verify { newSignupService.getByVerificationToken(token) }
        verify { newSignupService.saveNewSignup(newSignup) }
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

