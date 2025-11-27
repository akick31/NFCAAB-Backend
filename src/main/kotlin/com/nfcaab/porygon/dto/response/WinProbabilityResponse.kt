package com.nfcaab.porygon.dto.response

data class PlayWinProbabilityResponse(
    val playNumber: Int,
    val inning: Int,
    val inningHalf: String,
    val homeScore: Int,
    val awayScore: Int,
    val homeTeamWinProbability: Double,
    val awayTeamWinProbability: Double,
)

data class GameWinProbabilitiesResponse(
    val gameId: Int,
    val totalPlays: Int,
    val plays: List<PlayWinProbabilityResponse>,
)

data class SinglePlayWinProbabilityResponse(
    val playId: Int,
    val playNumber: Int,
    val inning: Int,
    val inningHalf: String,
    val homeScore: Int,
    val awayScore: Int,
    val winProbability: Double,
    val winProbabilityAdded: Double,
    val possession: String,
    val possessionTeam: String,
    val homeElo: Double,
    val awayElo: Double,
)

data class SingleGameWinProbabilitiesResponse(
    val gameId: Int,
    val homeTeam: String,
    val awayTeam: String,
    val totalPlays: Int,
    val processedPlays: Int,
    val plays: List<SinglePlayWinProbabilityResponse>,
)

data class ProcessedGameResult(
    val gameId: Int,
    val homeTeam: String,
    val awayTeam: String,
    val playsProcessed: Int,
)

data class WinProbabilitiesForAllGamesResponse(
    val totalGames: Int,
    val gamesProcessed: Int,
    val totalPlaysProcessed: Int,
    val processedGames: List<ProcessedGameResult>,
)

data class EloRatingResponse(
    val teamId: Int,
    val teamName: String,
    val currentElo: Double,
    val overallElo: Double,
)

data class GameSpreadResult(
    val gameId: Int,
    val homeTeam: String,
    val awayTeam: String,
    val homeElo: Double,
    val awayElo: Double,
    val homeSpread: Double,
    val awaySpread: Double,
)

data class UpdateSpreadsResponse(
    val message: String,
    val season: Int,
    val week: Int,
    val totalGames: Int,
    val updatedGames: Int,
    val results: List<GameSpreadResult>,
)

