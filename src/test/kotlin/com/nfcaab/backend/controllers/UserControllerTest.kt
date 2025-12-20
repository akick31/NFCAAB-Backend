package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.requests.UserValidationRequest
import com.nfcaab.backend.dto.website.UserDTO
import com.nfcaab.backend.model.User
import com.nfcaab.backend.service.nfcaab.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserControllerTest {
    private lateinit var userService: UserService
    private lateinit var userController: UserController

    @BeforeEach
    fun setUp() {
        userService = mockk()
        userController = UserController(userService)
    }

    @Test
    fun `getUserById should return user`() {
        val id = 1L
        val user = User().apply {
            this.id = id
            username = "testuser"
        }

        every { userService.getUserById(id) } returns user

        val result = userController.getUserById(id)

        assertEquals(user, result)
        verify { userService.getUserById(id) }
    }

    @Test
    fun `getUserDTOByDiscordId should return user DTO`() {
        val discordId = "discord123"
        val userDTO = mockk<UserDTO>()

        every { userService.getUserDTOByDiscordId(discordId) } returns userDTO

        val result = userController.getUserDTOByDiscordId(discordId)

        assertEquals(userDTO, result)
        verify { userService.getUserDTOByDiscordId(discordId) }
    }

    @Test
    fun `getUserByTeam should return user`() {
        val team = "Team A"
        val user = User().apply {
            this.team = team
        }

        every { userService.getUserByTeam(team) } returns user

        val result = userController.getUserByTeam(team)

        assertEquals(user, result)
        verify { userService.getUserByTeam(team) }
    }

    @Test
    fun `getAllUsers should return list of users`() {
        val users = listOf(
            User().apply { username = "user1" },
            User().apply { username = "user2" },
        )

        every { userService.getAllUsers() } returns users

        val result = userController.getAllUsers()

        assertEquals(users, result)
        verify { userService.getAllUsers() }
    }

    @Test
    fun `getFreeAgents should return list of users`() {
        val users = listOf(
            User().apply { username = "freeagent1" },
            User().apply { username = "freeagent2" },
        )

        every { userService.getOpenCoaches() } returns users

        val result = userController.getFreeAgents()

        assertEquals(users, result)
        verify { userService.getOpenCoaches() }
    }

    @Test
    fun `getUserDTOByName should return user DTO`() {
        val name = "testuser"
        val userDTO = mockk<UserDTO>()

        every { userService.getUserDTOByName(name) } returns userDTO

        val result = userController.getUserDTOByName(name)

        assertEquals(userDTO, result)
        verify { userService.getUserDTOByName(name) }
    }

    @Test
    fun `updateUserEmail should return success message`() {
        val id = 1L
        val newEmail = "newemail@example.com"
        val expectedResult = "Email updated successfully"

        every { userService.updateEmail(id, newEmail) } returns expectedResult

        val result = userController.updateUserEmail(id, newEmail)

        assertEquals(expectedResult, result)
        verify { userService.updateEmail(id, newEmail) }
    }

    @Test
    fun `updateUserRole should return updated user DTO`() {
        val userDTO = mockk<UserDTO>()

        every { userService.updateUser(userDTO) } returns userDTO

        val result = userController.updateUserRole(userDTO)

        assertEquals(userDTO, result)
        verify { userService.updateUser(userDTO) }
    }

    @Test
    fun `encryptEmails should return success message`() {
        val expectedResult = "Emails encrypted successfully"

        every { userService.hashEmails() } returns expectedResult

        val result = userController.encryptEmails()

        assertEquals(expectedResult, result)
        verify { userService.hashEmails() }
    }

    @Test
    fun `validateUser should return validation result`() {
        val request = mockk<UserValidationRequest>()
        val expectedResult = "User validated successfully"

        every { userService.validateUser(request) } returns expectedResult

        val result = userController.validateUser(request)

        assertEquals(expectedResult, result)
        verify { userService.validateUser(request) }
    }

    @Test
    fun `deleteTeam should return success`() {
        val id = 1L
        every { userService.deleteUser(id) } returns true

        val result = userController.deleteTeam(id)

        assertEquals(true, result)
        verify { userService.deleteUser(id) }
    }
}

