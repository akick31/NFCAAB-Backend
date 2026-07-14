package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.model.AtBat.SubmissionType
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
    private val batter = Player().apply { uniformNumber = 99; batterArchetype = Player.BatterArchetype.NEUTRAL }

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
                batter,
            )

        assertEquals(ActualResult.STRIKEOUT, outcome.actualResult)
        assertEquals(3, outcome.outs)
        assertEquals(BaseCondition.EMPTY, outcome.baseConditionAfter)
        assertEquals(null, outcome.runnerOnFirstAfter)
    }

    @Test
    fun `resolveOutcome for a home run with bases loaded scores four runs`() {
        val runnerOnFirst = Player()
        val runnerOnSecond = Player()
        val runnerOnThird = Player()

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.HOME_RUN,
                1,
                InningHalf.TOP,
                BaseCondition.BASED_LOADED,
                runnerOnFirst,
                runnerOnSecond,
                runnerOnThird,
                3,
                2,
                null,
                batter,
            )

        assertEquals(ActualResult.HOME_RUN, outcome.actualResult)
        assertEquals(4, outcome.runsScored)
        assertEquals(6, outcome.awayScore)
        assertEquals(BaseCondition.EMPTY, outcome.baseConditionAfter)
        assertEquals(listOf(runnerOnFirst, runnerOnSecond, runnerOnThird, batter), outcome.scoringRunners)
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
                batter,
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
                batter,
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
    fun `resolveOutcome for a walk from an empty base places the batter on first`() {
        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.WALK,
                0,
                InningHalf.TOP,
                BaseCondition.EMPTY,
                null,
                null,
                null,
                0,
                0,
                null,
                batter,
            )

        assertEquals(BaseCondition.FIRST, outcome.baseConditionAfter)
        assertEquals(batter, outcome.runnerOnFirstAfter)
    }

    @Test
    fun `resolveOutcome for a single to left holds a runner on second at third regardless of archetype and places the batter on first`() {
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
                batter,
            )

        assertEquals(BaseCondition.FIRST_THIRD, outcome.baseConditionAfter)
        assertEquals(0, outcome.runsScored)
        assertEquals(speedyRunner, outcome.runnerOnThirdAfter)
        assertEquals(batter, outcome.runnerOnFirstAfter)
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
                batter,
            )

        assertEquals(BaseCondition.FIRST, outcome.baseConditionAfter)
        assertEquals(1, outcome.runsScored)
        assertEquals(null, outcome.runnerOnThirdAfter)
        assertEquals(batter, outcome.runnerOnFirstAfter)
        assertEquals(listOf(neutralRunner), outcome.scoringRunners)
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
                batter,
            )

        assertEquals(1, outcome.runsScored)
        assertEquals(BaseCondition.FIRST, outcome.baseConditionAfter)
        assertEquals(listOf(speedyRunner), outcome.scoringRunners)
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
                batter,
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
                batter,
            )

        assertEquals(1, outcome.runsScored)
        assertEquals(BaseCondition.FIRST, outcome.baseConditionAfter)
        assertEquals(listOf(neutralRunner), outcome.scoringRunners)
    }

    @Test
    fun `resolveOutcome for a double to left holds a runner on first at third and places the batter on second`() {
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
                batter,
            )

        assertEquals(0, outcome.runsScored)
        assertEquals(BaseCondition.SECOND_THIRD, outcome.baseConditionAfter)
        assertEquals(neutralRunner, outcome.runnerOnThirdAfter)
        assertEquals(batter, outcome.runnerOnSecondAfter)
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
                batter,
            )

        assertEquals(1, outcome.runsScored)
        assertEquals(BaseCondition.SECOND, outcome.baseConditionAfter)
        assertEquals(null, outcome.runnerOnThirdAfter)
        assertEquals(listOf(neutralRunner), outcome.scoringRunners)
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
                batter,
            )

        assertEquals(1, outcome.runsScored)
        assertEquals(1, outcome.awayScore)
        assertEquals(BaseCondition.SECOND_THIRD, outcome.baseConditionAfter)
        assertEquals(runnerOnFirst, outcome.runnerOnThirdAfter)
        assertEquals(listOf(runnerOnThird), outcome.scoringRunners)
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
                batter,
            )

        assertEquals(2, outcome.runsScored)
        assertEquals(2, outcome.awayScore)
        assertEquals(BaseCondition.SECOND, outcome.baseConditionAfter)
        assertEquals(null, outcome.runnerOnThirdAfter)
        assertEquals(listOf(runnerOnThird, runnerOnFirst), outcome.scoringRunners)
    }

    @Test
    fun `resolveOutcome for a triple with the bases empty places the batter on third`() {
        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.TRIPLE,
                0,
                InningHalf.TOP,
                BaseCondition.EMPTY,
                null,
                null,
                null,
                0,
                0,
                null,
                batter,
            )

        assertEquals(0, outcome.runsScored)
        assertEquals(BaseCondition.THIRD, outcome.baseConditionAfter)
        assertEquals(batter, outcome.runnerOnThirdAfter)
    }

    @Test
    fun `resolveOutcome for a left groundout with no runner on first is a routine out and runners hold`() {
        val runnerOnSecond = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.LEFT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.SECOND,
                null,
                runnerOnSecond,
                null,
                0,
                0,
                null,
                batter,
            )

        assertEquals(ActualResult.GROUNDOUT, outcome.actualResult)
        assertEquals(1, outcome.outs)
        assertEquals(BaseCondition.SECOND, outcome.baseConditionAfter)
        assertEquals(runnerOnSecond, outcome.runnerOnSecondAfter)
    }

    @Test
    fun `resolveOutcome for a left-side groundout with a runner on first turns a double play`() {
        val runnerOnFirst = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.LEFT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.FIRST,
                runnerOnFirst,
                null,
                null,
                0,
                0,
                null,
                batter,
            )

        assertEquals(ActualResult.DOUBLE_PLAY, outcome.actualResult)
        assertEquals(2, outcome.outs)
        assertEquals(BaseCondition.EMPTY, outcome.baseConditionAfter)
        assertEquals(null, outcome.runnerOnFirstAfter)
    }

    @Test
    fun `resolveOutcome for a right-side groundout with a runner on first is always a fielders choice, never a double play`() {
        val runnerOnFirst = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.RIGHT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.FIRST,
                runnerOnFirst,
                null,
                null,
                0,
                0,
                null,
                batter,
            )

        assertEquals(ActualResult.FIELDERS_CHOICE, outcome.actualResult)
        assertEquals(1, outcome.outs)
        assertEquals(BaseCondition.FIRST, outcome.baseConditionAfter)
        assertEquals(batter, outcome.runnerOnFirstAfter)
    }

    @Test
    fun `resolveOutcome for a left-side groundout does not attempt a double play with two outs already`() {
        val runnerOnFirst = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.LEFT_GROUNDOUT,
                2,
                InningHalf.TOP,
                BaseCondition.FIRST,
                runnerOnFirst,
                null,
                null,
                0,
                0,
                null,
                batter,
            )

        assertEquals(ActualResult.FIELDERS_CHOICE, outcome.actualResult)
        assertEquals(3, outcome.outs)
        assertEquals(BaseCondition.EMPTY, outcome.baseConditionAfter)
    }

    @Test
    fun `resolveOutcome for a right-side groundout with the bases loaded forces in a run on a fielders choice`() {
        val runnerOnFirst = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }
        val runnerOnSecond = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }
        val runnerOnThird = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.RIGHT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.BASED_LOADED,
                runnerOnFirst,
                runnerOnSecond,
                runnerOnThird,
                0,
                0,
                null,
                batter,
            )

        assertEquals(ActualResult.FIELDERS_CHOICE, outcome.actualResult)
        assertEquals(1, outcome.outs)
        assertEquals(1, outcome.runsScored)
        assertEquals(BaseCondition.FIRST_THIRD, outcome.baseConditionAfter)
        assertEquals(batter, outcome.runnerOnFirstAfter)
        assertEquals(runnerOnSecond, outcome.runnerOnThirdAfter)
        assertEquals(listOf(runnerOnThird), outcome.scoringRunners)
    }

    @Test
    fun `resolveOutcome for a right-side groundout with a runner on third scores them`() {
        val runnerOnThird = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.RIGHT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.THIRD,
                null,
                null,
                runnerOnThird,
                0,
                0,
                null,
                batter,
            )

        assertEquals(ActualResult.GROUNDOUT, outcome.actualResult)
        assertEquals(1, outcome.outs)
        assertEquals(1, outcome.runsScored)
        assertEquals(1, outcome.awayScore)
        assertEquals(BaseCondition.EMPTY, outcome.baseConditionAfter)
        assertEquals(listOf(runnerOnThird), outcome.scoringRunners)
    }

    @Test
    fun `resolveOutcome for a bunt to the right side with a runner on first always advances them and retires the batter`() {
        val runnerOnFirst = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.RIGHT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.FIRST,
                runnerOnFirst,
                null,
                null,
                0,
                0,
                null,
                batter,
                SubmissionType.BUNT,
            )

        assertEquals(ActualResult.SACRIFICE_BUNT, outcome.actualResult)
        assertEquals(1, outcome.outs)
        assertEquals(BaseCondition.SECOND, outcome.baseConditionAfter)
        assertEquals(runnerOnFirst, outcome.runnerOnSecondAfter)
        assertEquals(null, outcome.runnerOnFirstAfter)
    }

    @Test
    fun `resolveOutcome for a bunt to the left side with a runner on first is a fielders choice against the lead runner`() {
        val runnerOnFirst = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.LEFT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.FIRST,
                runnerOnFirst,
                null,
                null,
                0,
                0,
                null,
                batter,
                SubmissionType.BUNT,
            )

        assertEquals(ActualResult.FIELDERS_CHOICE, outcome.actualResult)
        assertEquals(1, outcome.outs)
        assertEquals(BaseCondition.FIRST, outcome.baseConditionAfter)
        assertEquals(batter, outcome.runnerOnFirstAfter)
    }

    @Test
    fun `resolveOutcome for a bunt with bases empty is a routine out with no sacrifice`() {
        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.LEFT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.EMPTY,
                null,
                null,
                null,
                0,
                0,
                null,
                batter,
                SubmissionType.BUNT,
            )

        assertEquals(ActualResult.GROUNDOUT, outcome.actualResult)
    }

    @Test
    fun `resolveOutcome for a bunt with a runner on second and no other runners holds the runner when it lands on the left side`() {
        val runnerOnSecond = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.LEFT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.SECOND,
                null,
                runnerOnSecond,
                null,
                0,
                0,
                null,
                batter,
                SubmissionType.BUNT,
            )

        assertEquals(ActualResult.SACRIFICE_BUNT, outcome.actualResult)
        assertEquals(1, outcome.outs)
        assertEquals(BaseCondition.SECOND, outcome.baseConditionAfter)
        assertEquals(runnerOnSecond, outcome.runnerOnSecondAfter)
    }

    @Test
    fun `resolveOutcome for a squeeze bunt to the right side scores the runner from third`() {
        val runnerOnThird = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.RIGHT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.THIRD,
                null,
                null,
                runnerOnThird,
                0,
                0,
                null,
                batter,
                SubmissionType.BUNT,
            )

        assertEquals(ActualResult.SACRIFICE_BUNT, outcome.actualResult)
        assertEquals(1, outcome.runsScored)
        assertEquals(1, outcome.awayScore)
        assertEquals(BaseCondition.EMPTY, outcome.baseConditionAfter)
        assertEquals(listOf(runnerOnThird), outcome.scoringRunners)
    }

    @Test
    fun `resolveOutcome for a squeeze bunt to the left side strands the runner and puts the batter on safely`() {
        val runnerOnThird = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.LEFT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.THIRD,
                null,
                null,
                runnerOnThird,
                0,
                0,
                null,
                batter,
                SubmissionType.BUNT,
            )

        assertEquals(ActualResult.FIELDERS_CHOICE, outcome.actualResult)
        assertEquals(0, outcome.runsScored)
        assertEquals(1, outcome.outs)
        assertEquals(BaseCondition.FIRST, outcome.baseConditionAfter)
        assertEquals(batter, outcome.runnerOnFirstAfter)
        assertEquals(null, outcome.runnerOnThirdAfter)
    }

    @Test
    fun `resolveOutcome for a failed squeeze with a runner on first forces them to second`() {
        val runnerOnFirst = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }
        val runnerOnThird = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.LEFT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.FIRST_THIRD,
                runnerOnFirst,
                null,
                runnerOnThird,
                0,
                0,
                null,
                batter,
                SubmissionType.BUNT,
            )

        assertEquals(ActualResult.FIELDERS_CHOICE, outcome.actualResult)
        assertEquals(BaseCondition.FIRST_SECOND, outcome.baseConditionAfter)
        assertEquals(batter, outcome.runnerOnFirstAfter)
        assertEquals(runnerOnFirst, outcome.runnerOnSecondAfter)
        assertEquals(null, outcome.runnerOnThirdAfter)
    }

    @Test
    fun `resolveOutcome for a failed squeeze with runners on first and second forces out the lead runner and advances the trailing runner to third`() {
        val runnerOnFirst = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }
        val runnerOnSecond = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.LEFT_GROUNDOUT,
                0,
                InningHalf.TOP,
                BaseCondition.FIRST_SECOND,
                runnerOnFirst,
                runnerOnSecond,
                null,
                0,
                0,
                null,
                batter,
                SubmissionType.BUNT,
            )

        assertEquals(ActualResult.FIELDERS_CHOICE, outcome.actualResult)
        assertEquals(BaseCondition.FIRST_THIRD, outcome.baseConditionAfter)
        assertEquals(batter, outcome.runnerOnFirstAfter)
        assertEquals(null, outcome.runnerOnSecondAfter)
        assertEquals(runnerOnSecond, outcome.runnerOnThirdAfter)
    }

    @Test
    fun `resolveOutcome for a bunt with the bases loaded ends the inning cleanly when it is the third out`() {
        val runnerOnFirst = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }
        val runnerOnSecond = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }
        val runnerOnThird = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.LEFT_GROUNDOUT,
                2,
                InningHalf.TOP,
                BaseCondition.BASED_LOADED,
                runnerOnFirst,
                runnerOnSecond,
                runnerOnThird,
                0,
                0,
                null,
                batter,
                SubmissionType.BUNT,
            )

        assertEquals(3, outcome.outs)
        assertEquals(BaseCondition.EMPTY, outcome.baseConditionAfter)
        assertEquals(0, outcome.runsScored)
    }

    @Test
    fun `resolveStealOutcome advances the lead runner one base on a successful steal`() {
        val runnerOnFirst = Player().apply { uniformNumber = 12; batterArchetype = Player.BatterArchetype.SPEEDY }

        val outcome =
            baseRunningService.resolveStealOutcome(
                Scenario.STEAL_SUCCESS,
                1,
                InningHalf.TOP,
                runnerOnFirst,
                null,
                null,
                0,
                0,
            )

        assertEquals(ActualResult.STOLEN_BASE, outcome.actualResult)
        assertEquals(1, outcome.outs)
        assertEquals(BaseCondition.SECOND, outcome.baseConditionAfter)
        assertEquals(runnerOnFirst, outcome.runnerOnSecondAfter)
        assertEquals(null, outcome.runnerOnFirstAfter)
    }

    @Test
    fun `resolveStealOutcome scores the runner when stealing home from third`() {
        val runnerOnThird = Player().apply { uniformNumber = 12; batterArchetype = Player.BatterArchetype.SPEEDY }

        val outcome =
            baseRunningService.resolveStealOutcome(
                Scenario.STEAL_SUCCESS,
                1,
                InningHalf.BOTTOM,
                null,
                null,
                runnerOnThird,
                2,
                2,
            )

        assertEquals(1, outcome.runsScored)
        assertEquals(3, outcome.homeScore)
        assertEquals(BaseCondition.EMPTY, outcome.baseConditionAfter)
        assertEquals(listOf(runnerOnThird), outcome.scoringRunners)
    }

    @Test
    fun `resolveStealOutcome performs a double steal when runners are on first and second`() {
        val runnerOnFirst = Player().apply { uniformNumber = 12 }
        val runnerOnSecond = Player().apply { uniformNumber = 15 }

        val outcome =
            baseRunningService.resolveStealOutcome(
                Scenario.STEAL_SUCCESS,
                0,
                InningHalf.TOP,
                runnerOnFirst,
                runnerOnSecond,
                null,
                0,
                0,
            )

        assertEquals(BaseCondition.SECOND_THIRD, outcome.baseConditionAfter)
        assertEquals(runnerOnSecond, outcome.runnerOnThirdAfter)
        assertEquals(runnerOnFirst, outcome.runnerOnSecondAfter)
        assertEquals(null, outcome.runnerOnFirstAfter)
    }

    @Test
    fun `resolveStealOutcome still advances the trailing runner when the lead runner is caught stealing`() {
        val runnerOnFirst = Player().apply { uniformNumber = 12 }
        val runnerOnSecond = Player().apply { uniformNumber = 15 }

        val outcome =
            baseRunningService.resolveStealOutcome(
                Scenario.STEAL_ATTEMPT,
                0,
                InningHalf.TOP,
                runnerOnFirst,
                runnerOnSecond,
                null,
                0,
                0,
            )

        assertEquals(ActualResult.CAUGHT_STEALING, outcome.actualResult)
        assertEquals(1, outcome.outs)
        assertEquals(BaseCondition.SECOND, outcome.baseConditionAfter)
        assertEquals(runnerOnFirst, outcome.runnerOnSecondAfter)
        assertEquals(null, outcome.runnerOnFirstAfter)
        assertEquals(null, outcome.runnerOnThirdAfter)
    }

    @Test
    fun `resolveStealOutcome performs a double steal of home with runners on first and third`() {
        val runnerOnFirst = Player().apply { uniformNumber = 12 }
        val runnerOnThird = Player().apply { uniformNumber = 15 }

        val outcome =
            baseRunningService.resolveStealOutcome(
                Scenario.STEAL_SUCCESS,
                0,
                InningHalf.TOP,
                runnerOnFirst,
                null,
                runnerOnThird,
                2,
                2,
            )

        assertEquals(1, outcome.runsScored)
        assertEquals(3, outcome.awayScore)
        assertEquals(BaseCondition.SECOND, outcome.baseConditionAfter)
        assertEquals(runnerOnFirst, outcome.runnerOnSecondAfter)
        assertEquals(null, outcome.runnerOnThirdAfter)
        assertEquals(listOf(runnerOnThird), outcome.scoringRunners)
    }

    @Test
    fun `resolveStealOutcome performs a double steal with runners on second and third`() {
        val runnerOnSecond = Player().apply { uniformNumber = 12 }
        val runnerOnThird = Player().apply { uniformNumber = 15 }

        val outcome =
            baseRunningService.resolveStealOutcome(
                Scenario.STEAL_SUCCESS,
                0,
                InningHalf.TOP,
                null,
                runnerOnSecond,
                runnerOnThird,
                2,
                2,
            )

        assertEquals(1, outcome.runsScored)
        assertEquals(BaseCondition.THIRD, outcome.baseConditionAfter)
        assertEquals(runnerOnSecond, outcome.runnerOnThirdAfter)
        assertEquals(listOf(runnerOnThird), outcome.scoringRunners)
    }

    @Test
    fun `resolveStealOutcome removes the runner and adds an out when caught stealing`() {
        val runnerOnFirst = Player().apply { uniformNumber = 12 }

        val outcome =
            baseRunningService.resolveStealOutcome(
                Scenario.STEAL_ATTEMPT,
                1,
                InningHalf.TOP,
                runnerOnFirst,
                null,
                null,
                0,
                0,
            )

        assertEquals(ActualResult.CAUGHT_STEALING, outcome.actualResult)
        assertEquals(2, outcome.outs)
        assertEquals(BaseCondition.EMPTY, outcome.baseConditionAfter)
        assertEquals(0, outcome.runsScored)
    }

    @Test
    fun `resolveStealOutcome ends the inning when caught stealing with two outs`() {
        val runnerOnFirst = Player().apply { uniformNumber = 12 }
        val runnerOnSecond = Player().apply { uniformNumber = 15 }

        val outcome =
            baseRunningService.resolveStealOutcome(
                Scenario.STEAL_ATTEMPT,
                2,
                InningHalf.TOP,
                runnerOnFirst,
                runnerOnSecond,
                null,
                0,
                0,
            )

        assertEquals(3, outcome.outs)
        assertEquals(BaseCondition.EMPTY, outcome.baseConditionAfter)
        assertEquals(null, outcome.runnerOnFirstAfter)
        assertEquals(null, outcome.runnerOnSecondAfter)
    }

    @Test
    fun `leadRunner prefers the runner closest to home`() {
        val runnerOnFirst = Player().apply { uniformNumber = 12 }
        val runnerOnThird = Player().apply { uniformNumber = 15 }

        assertEquals(runnerOnThird, baseRunningService.leadRunner(runnerOnFirst, null, runnerOnThird))
        assertEquals(runnerOnFirst, baseRunningService.leadRunner(runnerOnFirst, null, null))
    }
}
