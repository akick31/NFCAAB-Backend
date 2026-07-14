package com.nfcaab.backend.repositories

import com.nfcaab.backend.model.SeasonStatLinePitcher
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SeasonStatLinePitcherRepository : CrudRepository<SeasonStatLinePitcher, Int> {
    fun findByPlayerIdAndSeason(
        playerId: String,
        season: Int,
    ): SeasonStatLinePitcher?
}
