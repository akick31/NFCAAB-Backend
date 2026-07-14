package com.nfcaab.backend.repositories

import com.nfcaab.backend.model.GameLineup
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameLineupRepository : CrudRepository<GameLineup, Int> {
    @Query(value = "SELECT * FROM game_lineups WHERE game_id = :gameId AND team = :team AND lineup_spot = :lineupSpot", nativeQuery = true)
    fun getBatterByLineupSpotAndTeam(
        gameId: Int,
        team: String,
        lineupSpot: Int,
    ): GameLineup?

    @Query(value = "SELECT * FROM game_lineups WHERE game_id = :gameId AND team = :team AND position = 'Pitcher' AND currently_playing = true", nativeQuery = true)
    fun getPitcherByTeam(
        gameId: Int,
        team: String,
    ): GameLineup?

    @Query(value = "SELECT * FROM game_lineups WHERE game_id = :gameId AND team = :team", nativeQuery = true)
    fun getAllLineupsByGameIdAndTeam(
        gameId: Int,
        team: String,
    ): List<GameLineup>

    @Query(
        value = "SELECT * FROM game_lineups WHERE game_id = :gameId AND team = :team AND uniform_number = :uniformNumber AND currently_playing = true",
        nativeQuery = true,
    )
    fun getCurrentLineupEntryByUniformNumber(
        gameId: Int,
        team: String,
        uniformNumber: Int,
    ): GameLineup?
}

