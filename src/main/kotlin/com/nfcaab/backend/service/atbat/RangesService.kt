package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Player.BatterArchetype
import com.nfcaab.backend.model.Player.PitcherArchetype
import com.nfcaab.backend.repositories.RangesRepository
import com.nfcaab.backend.util.ResultNotFoundException
import org.springframework.stereotype.Service

@Service
class RangesService(
    private val rangesRepository: RangesRepository,
) {
    fun getResult(
        submissionType: SubmissionType,
        batterArchetype: BatterArchetype,
        pitcherArchetype: PitcherArchetype,
        difference: Int,
    ) = rangesRepository.getNormalResult(
        submissionType,
        batterArchetype,
        pitcherArchetype,
        difference.toString(),
    ) ?: throw ResultNotFoundException()

    fun getBuntResult(
        batterArchetype: BatterArchetype,
        pitcherArchetype: PitcherArchetype,
        difference: Int,
    ) = getResult(
        SubmissionType.BUNT,
        batterArchetype,
        pitcherArchetype,
        difference,
    )

    fun getStealResult(
        batterArchetype: BatterArchetype,
        pitcherArchetype: PitcherArchetype,
        difference: Int,
    ) = getResult(
        SubmissionType.STEAL,
        batterArchetype,
        pitcherArchetype,
        difference,
    )
}
