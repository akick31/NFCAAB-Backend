package com.nfcaab.backend.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "new_signup", schema = "porygon")
class NewSignup {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0

    @Column(name = "username")
    lateinit var username: String

    @Column(name = "coach_name")
    lateinit var coachName: String

    @Column(name = "discord_tag")
    lateinit var discordTag: String

    @Column(name = "discord_id")
    lateinit var discordId: String

    @Column(name = "team_choice_one")
    lateinit var teamChoiceOne: String

    @Column(name = "team_choice_two")
    lateinit var teamChoiceTwo: String

    @Column(name = "team_choice_three")
    lateinit var teamChoiceThree: String

    @Column(name = "email")
    lateinit var email: String

    @Column(name = "hashed_email")
    var hashedEmail: String? = null

    @Column(name = "password")
    lateinit var password: String

    @Column(name = "verification_token")
    var verificationToken: String? = null

    @Column(name = "approved")
    var approved: Boolean = false

    @Column(name = "email_verified")
    var emailVerified: Boolean = false

    // Default constructor
    constructor()

    // Constructor with parameters
    constructor(
        username: String,
        coachName: String,
        discordTag: String,
        discordId: String,
        teamChoiceOne: String,
        teamChoiceTwo: String,
        teamChoiceThree: String,
        email: String,
        hashedEmail: String?,
        password: String,
        verificationToken: String?,
        approved: Boolean,
    ) {
        this.username = username
        this.coachName = coachName
        this.discordTag = discordTag
        this.discordId = discordId
        this.teamChoiceOne = teamChoiceOne
        this.teamChoiceTwo = teamChoiceTwo
        this.teamChoiceThree = teamChoiceThree
        this.email = email
        this.hashedEmail = hashedEmail
        this.password = password
        this.verificationToken = verificationToken
        this.approved = approved
    }
}
