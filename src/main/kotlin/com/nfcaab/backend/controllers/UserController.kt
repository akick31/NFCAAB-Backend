package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.website.UserDTO
import com.nfcaab.backend.dto.requests.SelfUserUpdateRequest
import com.nfcaab.backend.dto.requests.UserValidationRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import com.nfcaab.backend.service.user.UserService

@RestController
@RequestMapping("/user")
class UserController(
    private var userService: UserService,
) {
    @GetMapping("/me")
    fun getCurrentUser(authentication: Authentication) = userService.getUserDTOById(authentication.name.toLong())

    @GetMapping("id")
    fun getUserById(
        @RequestParam id: Long,
    ) = userService.getUserById(id)

    @GetMapping("/discord")
    fun getUserDTOByDiscordId(
        @RequestParam id: String,
    ) = userService.getUserDTOByDiscordId(id)

    @GetMapping("/team")
    fun getUserByTeam(
        @RequestParam team: String,
    ) = userService.getUserByTeam(team)

    @GetMapping("")
    fun getAllUsers() = userService.getAllUsers()

    @GetMapping("/open_coaches")
    fun getFreeAgents() = userService.getOpenCoaches()

    @GetMapping("/name")
    fun getUserDTOByName(
        @RequestParam name: String,
    ) = userService.getUserDTOByName(name)

    @PutMapping("/me")
    fun updateCurrentUser(
        authentication: Authentication,
        @RequestBody request: SelfUserUpdateRequest,
    ) = userService.updateSelf(authentication.name.toLong(), request)

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update")
    fun updateUserRole(
        @RequestBody user: UserDTO,
    ) = userService.updateUser(user)

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/hash_emails")
    fun encryptEmails() = userService.hashEmails()

    @PostMapping("/validate")
    fun validateUser(
        @RequestBody userValidationRequest: UserValidationRequest,
    ) = userService.validateUser(userValidationRequest)

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("")
    fun deleteTeam(
        @RequestParam id: Long,
    ) = userService.deleteUser(id)
}
