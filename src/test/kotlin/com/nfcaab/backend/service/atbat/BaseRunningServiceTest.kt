package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.BaseCondition
import com.nfcaab.backend.model.Game.InningHalf
import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.repositories.RangesRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BaseRunningServiceTest {
    private lateinit var rangesService: RangesService
    private lateinit var baseRunningService: BaseRunningService

    @BeforeEach
    fun setUp() {
        rangesService = RangesService(mockk<RangesRepository>())
        baseRunningService = BaseRunningService(rangesService)
    }

    @Test
    fun `resolveOutcome for a strikeout with two outs ends the inning`() {
        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.STRIKEOUT,
                2,
                InningHalf.TOP,
                BaseCondition.FIRST_SECOND,
                Player(),
                Player(),
                null,
                0,
                0,
            )

        assertEquals(ActualResult.STRIKEOUT, outcome.actualResult)
        assertEquals(3, outcome.outs)
        assertEquals(BaseCondition.EMPTY, outcome.baseConditionAfter)
        assertEquals(null, outcome.runnerOnFirstAfter)
    }

    @Test
    fun `resolveOutcome for a home run with bases loaded scores four runs`() {
        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.HOME_RUN,
                1,
                InningHalf.TOP,
                BaseCondition.BASED_LOADED,
                Player(),
                Player(),
                Player(),
                3,
                2,
            )

        assertEquals(ActualResult.HOME_RUN, outcome.actualResult)
        assertEquals(4, outcome.runsScored)
        assertEquals(6, outcome.awayScore)
        assertEquals(BaseCondition.EMPTY, outcome.baseConditionAfter)
    }

    @Test
    fun `resolveOutcome for a flyout with a speedy runner on second tags up to third`() {
        val speedyRunner = Player().apply { batterArchetype = Player.BatterArchetype.SPEEDY }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.FLYOUT,
                0,
                InningHalf.TOP,
                BaseCondition.SECOND,
                null,
                speedyRunner,
                null,
                0,
                0,
            )

        assertEquals(BaseCondition.THIRD, outcome.baseConditionAfter)
        assertEquals(speedyRunner, outcome.runnerOnThirdAfter)
        assertEquals(null, outcome.runnerOnSecondAfter)
    }

    @Test
    fun `resolveOutcome for a flyout with a non-speedy runner on second holds at second`() {
        val neutralRunner = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.FLYOUT,
                0,
                InningHalf.TOP,
                BaseCondition.SECOND,
                null,
                neutralRunner,
                null,
                0,
                0,
            )

        assertEquals(BaseCondition.SECOND, outcome.baseConditionAfter)
        assertEquals(neutralRunner, outcome.runnerOnSecondAfter)
    }

    @Test
    fun `resolveSteal delegates to RangesService and returns its result`() {
        val rangesRepository = mockk<RangesRepository>()
        val service = RangesService(rangesRepository)
        val runningService = BaseRunningService(service)
        val ranges = com.nfcaab.backend.model.Ranges().apply { result = Scenario.STEAL_SUCCESS }

        every {
            rangesRepository.getNormalResult(
                com.nfcaab.backend.model.AtBat.SubmissionType.STEAL,
                Player.BatterArchetype.SPEEDY,
                Player.PitcherArchetype.NEUTRAL,
                "5",
            )
        } returns ranges

        val result = runningService.resolveSteal(Player.BatterArchetype.SPEEDY, Player.PitcherArchetype.NEUTRAL, 5)

        assertEquals(Scenario.STEAL_SUCCESS, result)
    }
}
