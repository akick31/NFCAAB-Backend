package com.nfcaab.backend.repositories

import com.nfcaab.backend.model.Player
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PlayerRepository : CrudRepository<Player, Int> {
    @Query(value = "SELECT * FROM players WHERE current_team = :team AND uniform_number = :uniformNumber", nativeQuery = true)
    fun getPlayerByNumberAndTeam(
        team: String,
        uniformNumber: Int?,
    ): Player?
}

