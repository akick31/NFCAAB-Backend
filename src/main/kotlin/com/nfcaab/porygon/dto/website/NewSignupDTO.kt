package com.nfcaab.porygon.dto.website

data class NewSignupDTO(
    val id: Long,
    var username: String,
    var coachName: String,
    var discordTag: String,
    var discordId: String,
    var teamChoiceOne: String,
    var teamChoiceTwo: String,
    var teamChoiceThree: String,
    var approved: Boolean,
)

