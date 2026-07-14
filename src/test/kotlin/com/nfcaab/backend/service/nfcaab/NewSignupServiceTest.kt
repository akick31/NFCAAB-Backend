package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.NewSignup
import com.nfcaab.backend.model.User
import com.nfcaab.backend.repositories.NewSignupRepository
import com.nfcaab.backend.repositories.UserRepository
import com.nfcaab.backend.util.EmailNotFoundException
import com.nfcaab.backend.util.EncryptionUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NewSignupServiceTest {
    private lateinit var dtoConverter: com.nfcaab.backend.converter.DTOConverter
    private lateinit var encryptionUtils: EncryptionUtils
    private lateinit var userService: UserService
    private lateinit var newSignupRepository: NewSignupRepository
    private lateinit var userRepository: UserRepository
    private lateinit var newSignupService: NewSignupService

    @BeforeEach
    fun setUp() {
        dtoConverter = mockk()
        encryptionUtils = mockk()
        userService = mockk()
        newSignupRepository = mockk()
        userRepository = mockk()
        newSignupService = NewSignupService(
            dtoConverter,
            encryptionUtils,
            userService,
            newSignupRepository,
            userRepository
        )
    }

    @Test
    fun `createNewSignup should create signup when email does not exist`() {
        val newSignup = NewSignup().apply {
            username = "testuser"
            email = "test@example.com"
            password = "password123"
            coachName = "Test Coach"
            discordTag = "test#1234"
            discordId = "123456"
        }
        val hashedEmail = "hashed-email"
        val encryptedEmail = "encrypted-email"

        every { encryptionUtils.hash(newSignup.email!!) } returns hashedEmail
        every { encryptionUtils.encrypt(newSignup.email!!) } returns encryptedEmail
        every { userRepository.getUserByEmail(hashedEmail) } returns null
        every { newSignupRepository.save(any()) } returns mockk()

        val result = newSignupService.createNewSignup(newSignup)

        assertNotNull(result)
        verify { encryptionUtils.hash(newSignup.email!!) }
        verify { encryptionUtils.encrypt(newSignup.email!!) }
        verify { userRepository.getUserByEmail(hashedEmail) }
        verify { newSignupRepository.save(any()) }
    }

    @Test
    fun `createNewSignup should throw exception when email is null`() {
        val newSignup = NewSignup().apply {
            username = "testuser"
            email = ""
            password = "password123"
        }

        assertThrows(EmailNotFoundException::class.java) {
            newSignupService.createNewSignup(newSignup)
        }
    }

    @Test
    fun `createNewSignup should throw exception when email already exists`() {
        val newSignup = NewSignup().apply {
            username = "testuser"
            email = "test@example.com"
            password = "password123"
        }
        val hashedEmail = "hashed-email"
        val existingUser = User().apply {
            email = "encrypted-email"
        }

        every { encryptionUtils.hash(newSignup.email!!) } returns hashedEmail
        every { userRepository.getUserByEmail(hashedEmail) } returns existingUser

        assertThrows(EmailNotFoundException::class.java) {
            newSignupService.createNewSignup(newSignup)
        }
    }

    @Test
    fun `approveNewSignup should create user and return true`() {
        val newSignup = NewSignup().apply {
            id = 1L
            username = "testuser"
            email = "encrypted-email"
            hashedEmail = "hashed-email"
            password = "hashed-password"
            coachName = "Test Coach"
            discordTag = "test#1234"
            discordId = "123456"
            approved = false
        }

        every { newSignupRepository.save(any()) } returns newSignup
        every { userService.saveUser(any()) } returns mockk()

        val result = newSignupService.approveNewSignup(newSignup)

        assertEquals(true, result)
        verify { newSignupRepository.save(any()) }
        verify { userService.saveUser(any()) }
    }

    @Test
    fun `getNewSignupById should return signup`() {
        val id = 1L
        val newSignup = NewSignup().apply {
            this.id = id
        }

        every { newSignupRepository.getById(id) } returns newSignup

        val result = newSignupService.getNewSignupById(id)

        assertEquals(newSignup, result)
        verify { newSignupRepository.getById(id) }
    }

    @Test
    fun `getNewSignupByDiscordId should return signup`() {
        val discordId = "123456"
        val newSignup = NewSignup().apply {
            this.discordId = discordId
        }

        every { newSignupRepository.getByDiscordId(discordId) } returns newSignup

        val result = newSignupService.getNewSignupByDiscordId(discordId)

        assertEquals(newSignup, result)
        verify { newSignupRepository.getByDiscordId(discordId) }
    }

    @Test
    fun `getByVerificationToken should return signup`() {
        val token = "verification-token"
        val newSignup = NewSignup().apply {
            verificationToken = token
        }

        every { newSignupRepository.getByVerificationToken(token) } returns newSignup

        val result = newSignupService.getByVerificationToken(token)

        assertEquals(newSignup, result)
        verify { newSignupRepository.getByVerificationToken(token) }
    }

    @Test
    fun `getNewSignups should return list of DTOs`() {
        val signups = listOf(
            NewSignup().apply { username = "user1" },
            NewSignup().apply { username = "user2" }
        )
        val dto1 = mockk<com.nfcaab.backend.dto.website.NewSignupDTO>()
        val dto2 = mockk<com.nfcaab.backend.dto.website.NewSignupDTO>()

        every { newSignupRepository.getNewSignups() } returns signups
        every { dtoConverter.convertToNewSignupDTO(signups[0]) } returns dto1
        every { dtoConverter.convertToNewSignupDTO(signups[1]) } returns dto2

        val result = newSignupService.getNewSignups()

        assertEquals(2, result.size)
        verify { newSignupRepository.getNewSignups() }
    }

    @Test
    fun `saveNewSignup should call repository`() {
        val newSignup = NewSignup().apply { username = "testuser" }

        every { newSignupRepository.save(newSignup) } returns newSignup

        val result = newSignupService.saveNewSignup(newSignup)

        assertEquals(newSignup, result)
        verify { newSignupRepository.save(newSignup) }
    }

    @Test
    fun `deleteNewSignup should call repository`() {
        val newSignup = NewSignup().apply { username = "testuser" }

        every { newSignupRepository.delete(newSignup) } returns Unit

        newSignupService.deleteNewSignup(newSignup)

        verify { newSignupRepository.delete(newSignup) }
    }
}

