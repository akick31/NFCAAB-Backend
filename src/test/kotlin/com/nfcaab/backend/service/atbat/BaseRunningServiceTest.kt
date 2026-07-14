package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.BaseCondition
import com.nfcaab.backend.model.Game.HitDirection
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
                null,
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
                null,
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
                null,
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
                null,
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

    @Test
    fun `resolveOutcome for a single to left holds a runner on second at third regardless of archetype`() {
        val speedyRunner = Player().apply { batterArchetype = Player.BatterArchetype.SPEEDY }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.SINGLE,
                0,
                InningHalf.TOP,
                BaseCondition.SECOND,
                null,
                speedyRunner,
                null,
                0,
                0,
                HitDirection.LEFT,
            )

        assertEquals(BaseCondition.FIRST_THIRD, outcome.baseConditionAfter)
        assertEquals(0, outcome.runsScored)
        assertEquals(speedyRunner, outcome.runnerOnThirdAfter)
    }

    @Test
    fun `resolveOutcome for a single to right scores a runner from second regardless of archetype`() {
        val neutralRunner = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.SINGLE,
                0,
                InningHalf.TOP,
                BaseCondition.SECOND,
                null,
                neutralRunner,
                null,
                0,
                0,
                HitDirection.RIGHT,
            )

        assertEquals(BaseCondition.FIRST, outcome.baseConditionAfter)
        assertEquals(1, outcome.runsScored)
        assertEquals(null, outcome.runnerOnThirdAfter)
    }

    @Test
    fun `resolveOutcome for a single up the middle scores a speedy runner from second`() {
        val speedyRunner = Player().apply { batterArchetype = Player.BatterArchetype.SPEEDY }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.SINGLE,
                0,
                InningHalf.TOP,
                BaseCondition.SECOND,
                null,
                speedyRunner,
                null,
                0,
                0,
                HitDirection.CENTER,
            )

        assertEquals(1, outcome.runsScored)
        assertEquals(BaseCondition.FIRST, outcome.baseConditionAfter)
    }

    @Test
    fun `resolveOutcome for a single up the middle holds a non-speedy runner with fewer than two outs`() {
        val neutralRunner = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.SINGLE,
                0,
                InningHalf.TOP,
                BaseCondition.SECOND,
                null,
                neutralRunner,
                null,
                0,
                0,
                HitDirection.CENTER,
            )

        assertEquals(0, outcome.runsScored)
        assertEquals(BaseCondition.FIRST_THIRD, outcome.baseConditionAfter)
    }

    @Test
    fun `resolveOutcome for a single up the middle sends a non-speedy runner with two outs`() {
        val neutralRunner = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.SINGLE,
                2,
                InningHalf.TOP,
                BaseCondition.SECOND,
                null,
                neutralRunner,
                null,
                0,
                0,
                HitDirection.CENTER,
            )

        assertEquals(1, outcome.runsScored)
        assertEquals(BaseCondition.FIRST, outcome.baseConditionAfter)
    }

    @Test
    fun `resolveOutcome for a double to left holds a runner on first at third`() {
        val neutralRunner = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.DOUBLE,
                0,
                InningHalf.TOP,
                BaseCondition.FIRST,
                neutralRunner,
                null,
                null,
                0,
                0,
                HitDirection.LEFT,
            )

        assertEquals(0, outcome.runsScored)
        assertEquals(BaseCondition.SECOND_THIRD, outcome.baseConditionAfter)
        assertEquals(neutralRunner, outcome.runnerOnThirdAfter)
    }

    @Test
    fun `resolveOutcome for a double to right scores a runner from first`() {
        val neutralRunner = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.DOUBLE,
                0,
                InningHalf.TOP,
                BaseCondition.FIRST,
                neutralRunner,
                null,
                null,
                0,
                0,
                HitDirection.RIGHT,
            )

        assertEquals(1, outcome.runsScored)
        assertEquals(BaseCondition.SECOND, outcome.baseConditionAfter)
        assertEquals(null, outcome.runnerOnThirdAfter)
    }

    @Test
    fun `resolveOutcome for a double with runners on first and third to left only scores the runner from third`() {
        val runnerOnFirst = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }
        val runnerOnThird = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.DOUBLE,
                0,
                InningHalf.TOP,
                BaseCondition.FIRST_THIRD,
                runnerOnFirst,
                null,
                runnerOnThird,
                0,
                0,
                HitDirection.LEFT,
            )

        assertEquals(1, outcome.runsScored)
        assertEquals(1, outcome.awayScore)
        assertEquals(BaseCondition.SECOND_THIRD, outcome.baseConditionAfter)
        assertEquals(runnerOnFirst, outcome.runnerOnThirdAfter)
    }

    @Test
    fun `resolveOutcome for a double with runners on first and third to right scores both runners`() {
        val runnerOnFirst = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }
        val runnerOnThird = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.DOUBLE,
                0,
                InningHalf.TOP,
                BaseCondition.FIRST_THIRD,
                runnerOnFirst,
                null,
                runnerOnThird,
                0,
                0,
                HitDirection.RIGHT,
            )

        assertEquals(2, outcome.runsScored)
        assertEquals(2, outcome.awayScore)
        assertEquals(BaseCondition.SECOND, outcome.baseConditionAfter)
        assertEquals(null, outcome.runnerOnThirdAfter)
    }
}
