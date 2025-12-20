package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Player.Archetype
import com.nfcaab.backend.repositories.RangesRepository
import com.nfcaab.backend.util.ResultNotFoundException
import org.springframework.stereotype.Service

@Service
class RangesService(
    private val rangesRepository: RangesRepository,
) {
    /**
     * Get the result of a normal plate appearance
     * @param submissionType
     * @param batterArchetype
     * @param pitcherArchetype
     * @param difference
     */
    fun getResult(
        submissionType: SubmissionType,
        batterArchetype: Archetype,
        pitcherArchetype: Archetype,
        difference: Int,
    ) = rangesRepository.getNormalResult(
        submissionType,
        batterArchetype,
        pitcherArchetype,
        difference.toString(),
    ) ?: throw ResultNotFoundException()

    /**
     * Get the result of a bunt
     * @param difference
     */
    fun getBuntResult(difference: Int) = getResult(
        SubmissionType.BUNT,
        Archetype.NEUTRAL, // Default archetype for bunt
        Archetype.NEUTRAL,
        difference,
    )

    /**
     * Get the result of a steal attempt
     * @param difference
     */
    fun getStealResult(difference: Int) = getResult(
        SubmissionType.STEAL,
        Archetype.SPEEDY, // Default archetype for steal
        Archetype.NEUTRAL,
        difference,
    )
}
