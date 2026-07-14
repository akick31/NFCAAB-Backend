package com.nfcaab.backend.repositories

import com.nfcaab.backend.model.RunEvent
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RunEventRepository : CrudRepository<RunEvent, Int> {
    fun countByGameIdAndScoringPlayerUniformNumberAndScoringTeam(
        gameId: Int,
        scoringPlayerUniformNumber: Int,
        scoringTeam: String,
    ): Int

    fun countByGameIdAndChargedPitcherUniformNumberAndChargedPitcherTeam(
        gameId: Int,
        chargedPitcherUniformNumber: Int,
        chargedPitcherTeam: String,
    ): Int

    fun findByGameId(gameId: Int): List<RunEvent>
}
