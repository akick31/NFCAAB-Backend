package com.nfcaab.backend.repositories

import com.nfcaab.backend.model.BatterGameStats
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BatterGameStatsRepository : CrudRepository<BatterGameStats, Int> {
    fun findByGameIdAndUniformNumberAndTeam(
        gameId: Int,
        uniformNumber: Int,
        team: String,
    ): BatterGameStats?

    fun findByGameId(gameId: Int): List<BatterGameStats>
}
