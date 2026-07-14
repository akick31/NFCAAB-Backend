package com.nfcaab.backend.repositories

import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Player.BatterArchetype
import com.nfcaab.backend.model.Player.PitcherArchetype
import com.nfcaab.backend.model.Ranges
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RangesRepository : CrudRepository<Ranges?, Int?> {
    @Query(
        value =
            "SELECT * FROM ranges WHERE submission_type = ? AND batter_archetype = ? " +
                "AND pitcher_archetype = ? AND ? BETWEEN low_range AND high_range;",
        nativeQuery = true,
    )
    fun getNormalResult(
        submissionType: SubmissionType?,
        batterArchetype: BatterArchetype?,
        pitcherArchetype: PitcherArchetype?,
        difference: String,
    ): Ranges?
}
