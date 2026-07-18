package com.nfcaab.backend.service.auth

import com.nfcaab.backend.model.NewSignup
import com.nfcaab.backend.dto.website.LoginResponse
import com.nfcaab.backend.service.email.EmailService
import com.nfcaab.backend.service.auth.SessionService
import com.nfcaab.backend.util.Logger
import com.nfcaab.backend.util.UserUnauthorizedException
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID
import javax.servlet.http.HttpServletResponse
import com.nfcaab.backend.service.user.UserService
import com.nfcaab.backend.service.user.NewSignupService

@Component
class AuthService(
    private val emailService: EmailService,
    private val userService: UserService,
    private val newSignupService: NewSignupService,
    private val sessionService: SessionService,
    private val authCookieService: AuthCookieService,
    private val passwordEncoder: PasswordEncoder,
) {
    /**
     * Create a new user
     * @param newSignup
     * @return
     */
    fun createNewSignup(newSignup: NewSignup): NewSignup {
        try {
            val signup = newSignupService.createNewSignup(newSignup)
            emailService.sendVerificationEmail(signup.email, signup.id, signup.verificationToken ?: "")
            Logger.info("User ${signup.username} registered successfully. Verification email sent.")
            return signup
        } catch (e: Exception) {
            Logger.error("Error creating new sign up for ${newSignup.username}", e)
            throw e
        }
    }

    /**
     * Login a user
     * @param usernameOrEmail
     * @param password
     * @return
     */
    fun login(
        usernameOrEmail: String,
        password: String,
        response: HttpServletResponse,
    ): LoginResponse {
        val user = userService.getUserByUsernameOrEmail(usernameOrEmail)
        if (!passwordEncoder.matches(password, user.password)) {
            throw UserUnauthorizedException()
        }
        authCookieService.issueAuthCookie(response, user.id)
        return LoginResponse(user.id, user.role)
    }

    /**
     * Logout a user
     * @param token
     * @param response
     * @return
     */
    fun logout(
        token: String?,
        response: HttpServletResponse,
    ): String {
        if (token != null) {
            sessionService.blacklistUserSession(token)
        }
        authCookieService.clearAuthCookie(response)
        return "User logged out successfully"
    }

    /**
     * Verify user email
     * @param token
     * @return
     */
    fun verifyEmail(token: String): Boolean {
        val newSignup = newSignupService.getByVerificationToken(token)
        newSignup.emailVerified = true
        newSignupService.saveNewSignup(newSignup)
        return true
    }

    /**
     * Reset verification token
     * @param id
     * @return
     */
    fun resetVerificationToken(id: Long): NewSignup {
        val newSignup = newSignupService.getNewSignupById(id)
        val verificationToken = UUID.randomUUID().toString()
        newSignup.verificationToken = verificationToken
        newSignupService.saveNewSignup(newSignup)
        emailService.sendVerificationEmail(newSignup.email, newSignup.id, verificationToken)
        return newSignup
    }

    /**
     * Send password reset email
     * @param email
     * @return
     */
    fun forgotPassword(email: String): ResponseEntity<String> {
        val user = userService.updateResetToken(email)

        emailService.sendPasswordResetEmail(user.email, user.id, user.resetToken ?: "")
        return ResponseEntity.ok("Reset email sent")
    }

    /**
     * Reset user password
     * @param token
     * @param userId
     * @param newPassword
     * @return
     */
    fun resetPassword(
        token: String,
        userId: Long,
        newPassword: String,
    ): ResponseEntity<String> {
        val user = userService.getUserById(userId)

        if (user.resetToken != token ||
            user.resetTokenExpiration?.let { LocalDateTime.parse(it).isBefore(LocalDateTime.now()) } == true
        ) {
            return ResponseEntity.badRequest().body("Invalid or expired token")
        }

        userService.updateUserPassword(user.id, newPassword)
        return ResponseEntity.ok("Password updated successfully")
    }
}
