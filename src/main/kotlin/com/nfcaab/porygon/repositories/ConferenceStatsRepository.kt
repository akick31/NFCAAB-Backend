package com.nfcaab.porygon.repositories

import com.nfcaab.porygon.enums.team.Conference
import com.nfcaab.porygon.enums.team.Subdivision
import com.nfcaab.porygon.model.ConferenceStats
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ConferenceStatsRepository : CrudRepository<ConferenceStats, Int>, JpaSpecificationExecutor<ConferenceStats> {
    fun findAllByOrderBySeasonNumberDescSubdivisionAsc(): List<ConferenceStats>

    fun findBySubdivisionAndConferenceAndSeasonNumber(
        subdivision: Subdivision,
        conference: Conference,
        seasonNumber: Int,
    ): ConferenceStats?

    fun findBySubdivisionAndSeasonNumber(
        subdivision: Subdivision,
        seasonNumber: Int,
    ): List<ConferenceStats>

    fun findByConferenceAndSeasonNumber(
        conference: Conference,
        seasonNumber: Int,
    ): List<ConferenceStats>

    fun findBySubdivisionOrderBySeasonNumberDesc(subdivision: Subdivision): List<ConferenceStats>

    fun findByConferenceOrderBySeasonNumberDesc(conference: Conference): List<ConferenceStats>

    fun findBySeasonNumberOrderBySubdivisionAsc(seasonNumber: Int): List<ConferenceStats>

    fun existsBySubdivisionAndConferenceAndSeasonNumber(
        subdivision: Subdivision,
        conference: Conference,
        seasonNumber: Int,
    ): Boolean
}

