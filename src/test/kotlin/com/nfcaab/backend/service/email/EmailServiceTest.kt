package com.nfcaab.backend.service.email

import com.nfcaab.backend.util.EncryptionUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

class EmailServiceTest {
    private lateinit var encryptionUtils: EncryptionUtils
    private lateinit var mailSender: JavaMailSender
    private lateinit var emailService: EmailService

    @BeforeEach
    fun setUp() {
        encryptionUtils = mockk()
        mailSender = mockk(relaxed = true)
        emailService = EmailService(encryptionUtils, mailSender, "https://fakecollegebaseball.com")
    }

    @Test
    fun `sendVerificationEmail should send email with correct content`() {
        val email = "encrypted-email"
        val userId = 123L
        val verificationToken = "token-123"
        val decryptedEmail = "test@example.com"

        every { encryptionUtils.decrypt(email) } returns decryptedEmail
        every { mailSender.send(any<SimpleMailMessage>()) } returns Unit

        emailService.sendVerificationEmail(email, userId, verificationToken)

        verify { encryptionUtils.decrypt(email) }
        verify { mailSender.send(any<SimpleMailMessage>()) }
    }

    @Test
    fun `sendPasswordResetEmail should send email with correct content`() {
        val email = "encrypted-email"
        val userId = 123L
        val resetToken = "reset-token-123"
        val decryptedEmail = "test@example.com"

        every { encryptionUtils.decrypt(email) } returns decryptedEmail
        every { mailSender.send(any<SimpleMailMessage>()) } returns Unit

        emailService.sendPasswordResetEmail(email, userId, resetToken)

        verify { encryptionUtils.decrypt(email) }
        verify { mailSender.send(any<SimpleMailMessage>()) }
    }
}

