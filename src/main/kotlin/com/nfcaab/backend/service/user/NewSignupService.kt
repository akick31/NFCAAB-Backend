package com.nfcaab.backend.service.user

import com.nfcaab.backend.converter.DTOConverter
import com.nfcaab.backend.model.NewSignup
import com.nfcaab.backend.model.User
import com.nfcaab.backend.model.User.Role.USER
import com.nfcaab.backend.dto.website.NewSignupDTO
import com.nfcaab.backend.repositories.NewSignupRepository
import com.nfcaab.backend.repositories.UserRepository
import com.nfcaab.backend.util.EmailNotFoundException
import com.nfcaab.backend.util.EncryptionUtils
import com.nfcaab.backend.util.Logger
import com.nfcaab.backend.util.NewSignupNotVerifiedException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NewSignupService(
    private val dtoConverter: DTOConverter,
    private val encryptionUtils: EncryptionUtils,
    private val userService: UserService,
    private val newSignupRepository: NewSignupRepository,
    private val userRepository: UserRepository,
) {
    /**
     * Create a new signup
     * @param newSignup
     */
    fun createNewSignup(newSignup: NewSignup): NewSignup {
        val passwordEncoder = BCryptPasswordEncoder()
        val verificationToken = UUID.randomUUID().toString()
        if (newSignup.email.isNullOrBlank()) {
            throw EmailNotFoundException("New signup email cannot be null or blank")
        }
        val existingSignup =
            userRepository.getUserByEmail(
                encryptionUtils.hash(
                    newSignup.email ?: throw EmailNotFoundException("New signup email cannot be null or blank"),
                ),
            )
        if (existingSignup != null) {
            throw EmailNotFoundException("Email already exists")
        }

        val createdSignup =
            NewSignup(
                newSignup.username,
                newSignup.coachName,
                newSignup.discordTag,
                newSignup.discordId,
                newSignup.teamChoiceOne,
                newSignup.teamChoiceTwo,
                newSignup.teamChoiceThree,
                encryptionUtils.encrypt(
                    newSignup.email
                        ?: throw EmailNotFoundException("New signup email cannot be null or blank"),
                ),
                encryptionUtils.hash(
                    newSignup.email
                        ?: throw EmailNotFoundException("New signup email cannot be null or blank"),
                ),
                passwordEncoder.encode(newSignup.password),
                verificationToken,
                false,
            )

        saveNewSignup(createdSignup)
        return createdSignup
    }

    /**
     * Approve a new signup
     * @param newSignup
     * @return Boolean
     */
    fun approveNewSignup(newSignup: NewSignup): Boolean {
        if (!newSignup.emailVerified) {
            throw NewSignupNotVerifiedException("New signup ${newSignup.id} has not verified their email yet")
        }
        try {
            newSignup.apply {
                approved = true
            }
            saveNewSignup(newSignup)
            val user = User()
            user.username = newSignup.username
            user.coachName = newSignup.coachName
            user.discordTag = newSignup.discordTag
            user.discordId = newSignup.discordId
            user.email = newSignup.email
            user.hashedEmail = newSignup.hashedEmail
            user.password = newSignup.password
            user.role = USER
            user.team = null
            user.delayOfGameInstances = 0
            user.wins = 0
            user.losses = 0
            user.winPercentage = 0.0f
            user.conferenceWins = 0
            user.conferenceLosses = 0
            user.conferenceWinPercentage = 0.0f
            user.conferenceChampionships = 0
            user.tournamentAppearances = 0
            user.superRegionalAppearances = 0
            user.collegeWorldSeriesAppearances = 0
            user.championships = 0
            user.averageResponseTime = 0.0
            user.resetToken = null
            user.resetTokenExpiration = null
            userService.saveUser(user)
            return true
        } catch (e: Exception) {
            Logger.error("Error approving new signup ${newSignup.id}: ${e.message}", e)
            throw e
        }
    }

    /**
     * Get a new signup by its id
     * @param id
     */
    fun getNewSignupById(id: Long) = newSignupRepository.getById(id)

    /**
     * Get a new signup by its Discord id
     */
    fun getNewSignupByDiscordId(discordId: String) = newSignupRepository.getByDiscordId(discordId)

    /**
     * Get a new signup by its verification token
     * @param token
     */
    fun getByVerificationToken(token: String) = newSignupRepository.getByVerificationToken(token)

    /**
     * Get all new signups
     */
    fun getNewSignups(): List<NewSignupDTO> {
        val userData = newSignupRepository.getNewSignups()
        return userData.map { dtoConverter.convertToNewSignupDTO(it) }
    }

    /**
     * Save a new signup
     * @param newSignup
     */
    fun saveNewSignup(newSignup: NewSignup) = newSignupRepository.save(newSignup)

    /**
     * Delete a new signup
     * @param id
     */
    fun deleteNewSignup(newSignup: NewSignup) = newSignupRepository.delete(newSignup)
}
