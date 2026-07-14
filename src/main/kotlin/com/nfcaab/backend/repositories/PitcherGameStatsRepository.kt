package com.nfcaab.backend.repositories

import com.nfcaab.backend.model.PitcherGameStats
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PitcherGameStatsRepository : CrudRepository<PitcherGameStats, Int> {
    fun findByGameIdAndUniformNumberAndTeam(
        gameId: Int,
        uniformNumber: Int,
        team: String,
    ): PitcherGameStats?

    fun findByGameId(gameId: Int): List<PitcherGameStats>
}
