package com.nfcaab.backend.service.user

import com.nfcaab.backend.converter.DTOConverter
import com.nfcaab.backend.dto.website.UserDTO
import com.nfcaab.backend.model.User
import com.nfcaab.backend.repositories.UserRepository
import com.nfcaab.backend.util.EncryptionUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var encryptionUtils: EncryptionUtils
    private lateinit var dtoConverter: DTOConverter
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        encryptionUtils = mockk()
        dtoConverter = mockk()
        userService = UserService(userRepository, encryptionUtils, dtoConverter)
    }

    @Test
    fun `getUserById should return user`() {
        val id = 1L
        val user = User().apply {
            this.id = id
            username = "testuser"
        }

        every { userRepository.getById(id) } returns user

        val result = userService.getUserById(id)

        assertEquals(user, result)
        verify { userRepository.getById(id) }
    }

    @Test
    fun `getUserByUsernameOrEmail should return user when found by email`() {
        val usernameOrEmail = "test@example.com"
        val hashedEmail = "hashed-email"
        val user = User().apply {
            this.username = "testuser"
            this.email = "encrypted-email"
        }

        every { encryptionUtils.hash(usernameOrEmail) } returns hashedEmail
        every { userRepository.getUserByEmail(hashedEmail) } returns user

        val result = userService.getUserByUsernameOrEmail(usernameOrEmail)

        assertEquals(user, result)
        verify { encryptionUtils.hash(usernameOrEmail) }
        verify { userRepository.getUserByEmail(hashedEmail) }
    }

    @Test
    fun `getUserByTeam should return user DTO`() {
        val team = "Team A"
        val user = User().apply {
            this.team = team
        }
        val userDTO = mockk<UserDTO>()

        every { userRepository.getByTeam(team) } returns user
        every { dtoConverter.convertToUserDTO(user) } returns userDTO

        val result = userService.getUserByTeam(team)

        assertEquals(userDTO, result)
        verify { userRepository.getByTeam(team) }
        verify { dtoConverter.convertToUserDTO(user) }
    }

    @Test
    fun `getAllUsers should return list of users`() {
        val users = listOf(
            User().apply { username = "user1" },
            User().apply { username = "user2" },
        )
        val userDTOs = listOf(mockk<UserDTO>(), mockk<UserDTO>())

        every { userRepository.findAll() } returns users
        every { dtoConverter.convertToUserDTO(users[0]) } returns userDTOs[0]
        every { dtoConverter.convertToUserDTO(users[1]) } returns userDTOs[1]

        val result = userService.getAllUsers()

        assertEquals(userDTOs, result)
        verify { userRepository.findAll() }
    }

    @Test
    fun `getOpenCoaches should return list of users without teams`() {
        val users = listOf(
            User().apply {
                username = "coach1"
                team = null
            },
        )
        val userDTO = mockk<UserDTO>()

        every { userRepository.getOpenCoaches() } returns users
        every { dtoConverter.convertToUserDTO(users[0]) } returns userDTO

        val result = userService.getOpenCoaches()

        assertEquals(listOf(userDTO), result)
        verify { userRepository.getOpenCoaches() }
    }
}

