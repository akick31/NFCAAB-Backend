package com.nfcaab.backend.dto.website

import com.nfcaab.backend.model.User.Role

data class UserDTO(
    val id: Long,
    var username: String,
    var coachName: String,
    var discordTag: String,
    var discordId: String,
    var role: Role,
    var team: String?,
    var delayOfGameInstances: Int,
    var wins: Int,
    var losses: Int,
    var winPercentage: Float,
    var conferenceWins: Int,
    var conferenceLosses: Int,
    var conferenceWinPercentage: Float,
    var seriesWins: Int,
    var seriesLosses: Int,
    var seriesPushes: Int,
    var conferenceChampionships: Int,
    var tournamentAppearances: Int,
    var superRegionalAppearances: Int,
    var collegeWorldSeriesAppearances: Int,
    var championships: Int,
    var averageResponseTime: Double,
)

