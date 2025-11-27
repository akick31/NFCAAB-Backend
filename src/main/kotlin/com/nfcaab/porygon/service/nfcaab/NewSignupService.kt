package com.nfcaab.porygon.service.nfcaab

import com.nfcaab.porygon.converter.DTOConverter
import com.nfcaab.porygon.model.NewSignup
import com.nfcaab.porygon.model.User
import com.nfcaab.porygon.model.User.Role.USER
import com.nfcaab.porygon.dto.website.NewSignupDTO
import com.nfcaab.porygon.repositories.NewSignupRepository
import com.nfcaab.porygon.repositories.UserRepository
import com.nfcaab.porygon.service.fcfb.UserService
import com.nfcaab.porygon.util.EmailNotFoundException
import com.nfcaab.porygon.util.EncryptionUtils
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
        val salt = passwordEncoder.encode(newSignup.password)
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
                salt,
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
        try {
            newSignup.apply {
                approved = true
            }
            saveNewSignup(newSignup)
            userService.saveUser(
                User(
                    username = newSignup.username,
                    coachName = newSignup.coachName,
                    discordTag = newSignup.discordTag,
                    discordId = newSignup.discordId,
                    email = newSignup.email,
                    hashedEmail = newSignup.hashedEmail,
                    password = newSignup.password,
                    role = USER,
                    salt = newSignup.salt,
                    team = null,
                    delayOfGameInstances = 0,
                    wins = 0,
                    losses = 0,
                    winPercentage = 0.0f,
                    conferenceWins = 0,
                    conferenceLosses = 0,
                    conferenceWinPercentage = 0.0f,
                    conferenceChampionships = 0,
                    tournamentAppearances = 0,
                    superRegionalAppearances = 0,
                    collegeWorldSeriesAppearances = 0,
                    championships = 0,
                    averageResponseTime = 0.0,
                    resetToken = null,
                    resetTokenExpiration = null,
                ),
            )
            return true
        } catch (e: Exception) {
            return false
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
