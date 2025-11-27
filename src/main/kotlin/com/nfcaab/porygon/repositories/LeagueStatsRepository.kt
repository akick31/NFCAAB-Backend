package com.nfcaab.porygon.repositories

import com.nfcaab.porygon.enums.team.Subdivision
import com.nfcaab.porygon.model.LeagueStats
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueStatsRepository : CrudRepository<LeagueStats, Int>, JpaSpecificationExecutor<LeagueStats> {
    fun findAllByOrderBySeasonNumberDescSubdivisionAsc(): List<LeagueStats>

    fun findBySubdivisionAndSeasonNumber(
        subdivision: Subdivision,
        seasonNumber: Int,
    ): LeagueStats?

    fun findBySubdivisionOrderBySeasonNumberDesc(subdivision: Subdivision): List<LeagueStats>

    fun findBySeasonNumberOrderBySubdivisionAsc(seasonNumber: Int): List<LeagueStats>

    fun existsBySubdivisionAndSeasonNumber(
        subdivision: Subdivision,
        seasonNumber: Int,
    ): Boolean
}

