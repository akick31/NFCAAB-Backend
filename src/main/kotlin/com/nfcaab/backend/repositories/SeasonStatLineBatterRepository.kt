package com.nfcaab.backend.repositories

import com.nfcaab.backend.model.SeasonStatLineBatter
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SeasonStatLineBatterRepository : CrudRepository<SeasonStatLineBatter, Int> {
    fun findByPlayerIdAndSeason(
        playerId: String,
        season: Int,
    ): SeasonStatLineBatter?
}
