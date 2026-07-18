package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.requests.SelfUserUpdateRequest
import com.nfcaab.backend.dto.requests.UserValidationRequest
import com.nfcaab.backend.dto.website.UserDTO
import com.nfcaab.backend.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import com.nfcaab.backend.service.user.UserService

class UserControllerTest {
    private lateinit var userService: UserService
    private lateinit var userController: UserController

    @BeforeEach
    fun setUp() {
        userService = mockk()
        userController = UserController(userService)
    }

    @Test
    fun `getCurrentUser should resolve the authenticated user's own id from the principal`() {
        val authentication = mockk<Authentication>()
        val userDTO =
            mockk<UserDTO> {
                every { id } returns 7L
            }
        every { authentication.name } returns "7"
        every { userService.getUserDTOById(7L) } returns userDTO

        val result = userController.getCurrentUser(authentication)

        assertEquals(userDTO, result)
        verify { userService.getUserDTOById(7L) }
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
    fun `getUserByTeam should return user DTO`() {
        val team = "Team A"
        val userDTO = mockk<UserDTO>()

        every { userService.getUserByTeam(team) } returns userDTO

        val result = userController.getUserByTeam(team)

        assertEquals(userDTO, result)
        verify { userService.getUserByTeam(team) }
    }

    @Test
    fun `getAllUsers should return list of user DTOs`() {
        val userDTOs = listOf(
            mockk<UserDTO>(),
            mockk<UserDTO>(),
        )

        every { userService.getAllUsers() } returns userDTOs

        val result = userController.getAllUsers()

        assertEquals(userDTOs, result)
        verify { userService.getAllUsers() }
    }

    @Test
    fun `getFreeAgents should return list of user DTOs`() {
        val userDTOs = listOf(
            mockk<UserDTO>(),
            mockk<UserDTO>(),
        )

        every { userService.getOpenCoaches() } returns userDTOs

        val result = userController.getFreeAgents()

        assertEquals(userDTOs, result)
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
    fun `updateCurrentUser should resolve the authenticated user's own id and apply the update`() {
        val authentication = mockk<Authentication>()
        val request = SelfUserUpdateRequest(email = "newemail@example.com")
        val userDTO = mockk<UserDTO>()

        every { authentication.name } returns "7"
        every { userService.updateSelf(7L, request) } returns userDTO

        val result = userController.updateCurrentUser(authentication, request)

        assertEquals(userDTO, result)
        verify { userService.updateSelf(7L, request) }
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
    fun `encryptEmails should call service`() {
        every { userService.hashEmails() } returns Unit

        userController.encryptEmails()

        verify { userService.hashEmails() }
    }

    @Test
    fun `validateUser should return validation response`() {
        val request = mockk<UserValidationRequest>()
        val expectedResult = mockk<com.nfcaab.backend.dto.response.UserValidationResponse>()

        every { userService.validateUser(request) } returns expectedResult

        val result = userController.validateUser(request)

        assertEquals(expectedResult, result)
        verify { userService.validateUser(request) }
    }

    @Test
    fun `deleteTeam should return HttpStatus`() {
        val id = 1L
        every { userService.deleteUser(id) } returns org.springframework.http.HttpStatus.OK

        val result = userController.deleteTeam(id)

        assertEquals(org.springframework.http.HttpStatus.OK, result)
        verify { userService.deleteUser(id) }
    }
}

