package com.nfcaab.porygon.repositories

import com.nfcaab.porygon.model.PlateAppearance.SubmissionType
import com.nfcaab.porygon.model.Player.Archetype
import com.nfcaab.porygon.model.Ranges
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
        batterArchetype: Archetype?,
        pitcherArchetype: Archetype?,
        difference: String,
    ): Ranges?
}
