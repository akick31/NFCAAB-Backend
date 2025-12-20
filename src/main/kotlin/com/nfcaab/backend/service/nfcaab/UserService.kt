package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.converter.DTOConverter
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.GameType
import com.nfcaab.backend.model.User
import com.nfcaab.backend.dto.website.UserDTO
import com.nfcaab.backend.dto.requests.UserValidationRequest
import com.nfcaab.backend.dto.response.UserValidationResponse
import com.nfcaab.backend.repositories.UserRepository
import com.nfcaab.backend.util.EncryptionUtils
import com.nfcaab.backend.util.UserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val encryptionUtils: EncryptionUtils,
    private val dtoConverter: DTOConverter,
) {
    /**
     * After a game ends, update the user's wins and losses
     * @param game
     */
    fun updateUserWinsAndLosses(game: Game) {
        val homeUser = getUserByTeam(game.homeTeam)
        val awayUser = getUserByTeam(game.awayTeam)
        val userList = mutableListOf<UserDTO>()
        userList.add(homeUser)
        userList.add(awayUser)

        for (user in userList) {
            val isHomeUser = user.team == game.homeTeam
            val isAwayUser = user.team == game.awayTeam
            if (!isHomeUser && !isAwayUser) {
                continue
            }
            val isWin = if (isHomeUser) game.homeScore > game.awayScore else game.awayScore > game.homeScore
            val gameType = game.gameType

            if (isHomeUser) {
                updateUserRecord(user, gameType, isWin)
            } else {
                updateUserRecord(user, gameType, !isWin)
            }
        }
    }

    /**
     * After a game ends, update the user's average response time
     * @param userId
     * @param responseTime
     */
    fun updateUserAverageResponseTime(
        userId: Long,
        responseTime: Double,
    ) = userRepository.updateAverageResponseTime(userId, responseTime)

    /**
     * Update a user's record after a game
     * @param user
     * @param gameType
     * @param isWin
     */
    private fun updateUserRecord(
        user: UserDTO,
        gameType: GameType,
        isWin: Boolean,
    ) {
        if (isWin) {
            user.wins += 1
            when (gameType) {
                GameType.CONFERENCE_GAME -> user.conferenceWins += 1
                GameType.CONFERENCE_CHAMPIONSHIP -> user.conferenceChampionships += 1
                else -> {}
            }
        } else {
            user.losses += 1
            when (gameType) {
                GameType.CONFERENCE_GAME -> user.conferenceLosses += 1
                else -> {}
            }
        }
        updateUser(user)
    }

    /**
     * Get a user DTO by its ID
     * @param id
     */
    private fun getUserDTOById(id: Long) = dtoConverter.convertToUserDTO(getUserById(id))

    /**
     * Get a user by its ID
     * @param id
     */
    fun getUserById(id: Long) = userRepository.getById(id) ?: throw UserNotFoundException("User not found with id $id")

    /**
     * Get a user by its Discord ID
     * @param discordId
     */
    fun getUserDTOByDiscordId(discordId: String) =
        dtoConverter.convertToUserDTO(
            userRepository.getByDiscordId(discordId)
                ?: throw UserNotFoundException("User not found with Discord ID $discordId"),
        )

    /**
     * Get a user by its team
     * @param team
     */
    fun getUserByTeam(team: String) =
        dtoConverter.convertToUserDTO(
            userRepository.getByTeam(team)
                ?: throw UserNotFoundException("User not found with team $team"),
        )

    /**
     * Get a user by its username or email
     */
    fun getUserByUsernameOrEmail(usernameOrEmail: String) =
        try {
            getUserByEmail(usernameOrEmail)
        } catch (e: Exception) {
            getUserByUsername(usernameOrEmail)
        }

    /**
     * Get a user by its username
     */
    private fun getUserByUsername(username: String) =
        userRepository.getByUsername(username)
            ?: throw UserNotFoundException("User not found with username $username")

    /**
     * Get a user by its email
     */
    private fun getUserByEmail(email: String) =
        userRepository.getUserByEmail(encryptionUtils.hash(email))
            ?: throw UserNotFoundException("User not found with email $email")

    /**
     * Get all users
     * @return List<UserDTO>
     */
    fun getAllUsers(): List<UserDTO> {
        val userData =
            userRepository.findAll().filterNotNull().ifEmpty {
                throw UserNotFoundException("No users found")
            }
        return userData.map { dtoConverter.convertToUserDTO(it) }
    }

    fun getOpenCoaches(): List<UserDTO> {
        val userData =
            userRepository.getOpenCoaches().ifEmpty {
                throw UserNotFoundException("No free agents found")
            }
        return userData.map { dtoConverter.convertToUserDTO(it) }
    }

    /**
     * Get a user by its name
     * @param name
     */
    fun getUserByCoachName(name: String) =
        userRepository.getByCoachName(name)
            ?: throw UserNotFoundException("User not found with coach name $name")

    /**
     * Get a user DTO by its name
     * @param name
     */
    fun getUserDTOByName(name: String) = dtoConverter.convertToUserDTO(getUserByCoachName(name))

    /**
     * Update a user's password
     * @param id
     * @param newPassword
     */
    fun updateUserPassword(
        id: Long,
        newPassword: String,
    ): UserDTO {
        val user = getUserById(id)

        val passwordEncoder = BCryptPasswordEncoder()
        user.password = passwordEncoder.encode(newPassword)
        user.salt = passwordEncoder.encode(newPassword)
        user.resetToken = null
        user.resetTokenExpiration = null

        userRepository.save(user)
        return dtoConverter.convertToUserDTO(user)
    }

    /**
     * Update a user's email
     * @param id
     * @param email
     * @return Boolean
     */
    fun updateEmail(
        id: Long,
        email: String,
    ): UserDTO {
        val user = getUserById(id)
        user.apply {
            this.email = email
        }
        saveUser(user)
        return dtoConverter.convertToUserDTO(user)
    }

    /**
     * Update a user's reset token
     * @param email
     */
    fun updateResetToken(email: String): User {
        val user = getUserByEmail(email)
        val resetToken = UUID.randomUUID().toString()
        user.apply {
            this.resetToken = resetToken
            this.resetTokenExpiration = LocalDateTime.now().plusHours(1).toString()
        }
        saveUser(user)
        return user
    }

    /**
     * Validate a user
     * @param userValidationRequest
     */
    fun validateUser(userValidationRequest: UserValidationRequest): UserValidationResponse {
        val discordIdExists = userRepository.existsByDiscordId(userValidationRequest.discordId)
        val discordTagExists = userRepository.existsByDiscordTag(userValidationRequest.discordTag)
        val usernameExists = userRepository.existsByUsername(userValidationRequest.username)
        val emailExists = userRepository.existsByEmail(userValidationRequest.email)

        return UserValidationResponse(
            discordIdExists,
            discordTagExists,
            usernameExists,
            emailExists,
        )
    }

    /**
     * Encrypt all user emails
     */
    fun hashEmails() {
        val users = userRepository.findAll().filterNotNull()
        users.forEach {
            it.hashedEmail = encryptionUtils.hash(encryptionUtils.decrypt(it.email))
            userRepository.save(it)
        }
    }

    /**
     * Update a user
     * @param user
     * @return UserDTO
     */
    fun updateUser(user: UserDTO): UserDTO {
        val existingUser = getUserDTOById(user.id)

        existingUser.apply {
            username = user.username
            coachName = user.coachName
            discordTag = user.discordTag
            discordId = user.discordId
            role = user.role
            team = user.team
            delayOfGameInstances = user.delayOfGameInstances
            wins = user.wins
            losses = user.losses
            winPercentage = if (user.wins + user.losses > 0) (user.wins / (user.wins + user.losses)).toFloat() else 0.0F
            conferenceWins = user.conferenceWins
            conferenceLosses = user.conferenceLosses
            conferenceWinPercentage =
                if (user.conferenceWins + user.conferenceLosses > 0) {
                    (user.conferenceWins / (user.conferenceWins + user.conferenceLosses).toFloat())
                } else {
                    0.0F
                }
            seriesWins = user.seriesWins
            seriesLosses = user.seriesLosses
            seriesPushes = user.seriesPushes
            conferenceChampionships = user.conferenceChampionships
            tournamentAppearances = user.tournamentAppearances
            superRegionalAppearances = user.superRegionalAppearances
            collegeWorldSeriesAppearances = user.collegeWorldSeriesAppearances
            championships = user.championships
            averageResponseTime = user.averageResponseTime
        }

        return saveUserDTOToUser(user.id, existingUser)
    }

    /**
     * Save a user DTO into a User object in the db
     * @param id
     * @param user
     */
    private fun saveUserDTOToUser(
        id: Long,
        user: UserDTO,
    ): UserDTO {
        val existingUser = getUserById(id)

        existingUser.apply {
            username = user.username
            coachName = user.coachName
            discordTag = user.discordTag
            discordId = user.discordId
            role = user.role
            team = user.team
            delayOfGameInstances = user.delayOfGameInstances
            wins = user.wins
            losses = user.losses
            winPercentage = user.winPercentage
            conferenceWins = user.conferenceWins
            conferenceLosses = user.conferenceLosses
            conferenceWinPercentage = user.conferenceWinPercentage
            seriesWins = user.seriesWins
            seriesLosses = user.seriesLosses
            seriesPushes = user.seriesPushes
            conferenceChampionships = user.conferenceChampionships
            tournamentAppearances = user.tournamentAppearances
            superRegionalAppearances = user.superRegionalAppearances
            collegeWorldSeriesAppearances = user.collegeWorldSeriesAppearances
            championships = user.championships
            averageResponseTime = user.averageResponseTime
        }

        userRepository.save(existingUser)
        return user
    }

    /**
     * Get a user by their Discord id
     */
    fun getUserByDiscordId(id: String) =
        userRepository.getByDiscordId(id) ?: throw UserNotFoundException("User not found with Discord ID $id")

    /**
     * Save a user
     * @param user
     */
    fun saveUser(user: User): User = userRepository.save(user)

    /**
     * Delete a user
     * @param id
     */
    fun deleteUser(id: Long): HttpStatus {
        userRepository.getById(id)

        userRepository.deleteById(id.toString())
        return HttpStatus.OK
    }
}
