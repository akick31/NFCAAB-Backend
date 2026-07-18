package com.nfcaab.backend.controllers

import com.nfcaab.backend.model.NewSignup
import com.nfcaab.backend.service.auth.AuthCookieService
import com.nfcaab.backend.service.auth.AuthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val authCookieService: AuthCookieService,
) {
    @PostMapping("/register")
    fun registerUser(
        @RequestBody newSignup: NewSignup,
    ) = authService.createNewSignup(newSignup)

    @PostMapping("/login")
    fun login(
        @RequestParam("usernameOrEmail") usernameOrEmail: String,
        @RequestParam("password") password: String,
        response: HttpServletResponse,
    ) = authService.login(usernameOrEmail, password, response)

    @PostMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) = authService.logout(authCookieService.readAuthCookie(request), response)

    @GetMapping("/verify")
    fun verifyEmail(
        @RequestParam("token") token: String,
    ) = authService.verifyEmail(token)

    @PutMapping("/resend-verification-email")
    fun resetVerificationToken(
        @RequestParam("id") id: Long,
    ) = authService.resetVerificationToken(id)

    @PostMapping("/forgot-password")
    fun forgotPassword(
        @RequestParam email: String,
    ) = authService.forgotPassword(email)

    @PostMapping("/reset-password")
    fun resetPassword(
        @RequestParam token: String,
        @RequestParam userId: Long,
        @RequestParam newPassword: String,
    ) = authService.resetPassword(token, userId, newPassword)
}
