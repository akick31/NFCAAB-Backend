package com.nfcaab.backend.service.lineup

import com.nfcaab.backend.model.LineupToken
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.repositories.LineupTokenRepository
import com.nfcaab.backend.util.GameNotFoundException
import com.nfcaab.backend.util.InvalidLineupTokenException
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class LineupTokenService(
    private val lineupTokenRepository: LineupTokenRepository,
    private val gameRepository: GameRepository,
) {
    companion object {
        private const val TOKEN_VALID_HOURS = 48L
    }

    fun generateToken(
        gameId: Int,
        team: String,
    ): LineupToken {
        val game = gameRepository.getGameById(gameId)
            ?: throw GameNotFoundException("No game found with ID: $gameId")
        if (game.homeTeam != team && game.awayTeam != team) {
            throw InvalidLineupTokenException("Team $team is not part of game $gameId")
        }

        val now = LocalDateTime.now()
        val lineupToken =
            LineupToken().apply {
                this.token = UUID.randomUUID().toString()
                this.gameId = gameId
                this.team = team
                this.createdAt = now
                this.expiresAt = now.plusHours(TOKEN_VALID_HOURS)
                this.used = false
            }
        return lineupTokenRepository.save(lineupToken)
    }

    fun validateToken(token: String): LineupToken {
        val lineupToken = lineupTokenRepository.getByToken(token)
            ?: throw InvalidLineupTokenException("Lineup token not found")
        if (lineupToken.used) {
            throw InvalidLineupTokenException("Lineup token has already been used")
        }
        if (lineupToken.expiresAt?.isBefore(LocalDateTime.now()) == true) {
            throw InvalidLineupTokenException("Lineup token has expired")
        }
        return lineupToken
    }

    fun consumeToken(lineupToken: LineupToken): LineupToken {
        lineupToken.used = true
        return lineupTokenRepository.save(lineupToken)
    }

    fun getActiveTokensForTeam(team: String): List<LineupToken> =
        lineupTokenRepository.findByTeamAndUsedFalseAndExpiresAtAfter(team, LocalDateTime.now())
}
