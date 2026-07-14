package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.BattedBallType
import com.nfcaab.backend.model.Game.HitDirection
import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.model.Player.BatterArchetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HitLocationServiceTest {
    private val hitLocationService = HitLocationService()

    @Test
    fun `determine returns nulls for scenarios with no ball in play`() {
        val result = hitLocationService.determine(Scenario.STRIKEOUT, BatterArchetype.NEUTRAL, 55, 5)

        assertNull(result.direction)
        assertNull(result.battedBallType)
        assertNull(result.fielderPosition)
    }

    @Test
    fun `determine returns nulls for a walk`() {
        val result = hitLocationService.determine(Scenario.WALK, BatterArchetype.NEUTRAL, 55, 5)

        assertNull(result.direction)
        assertNull(result.battedBallType)
        assertNull(result.fielderPosition)
    }

    @Test
    fun `determine is deterministic for the same inputs`() {
        val first = hitLocationService.determine(Scenario.SINGLE, BatterArchetype.NEUTRAL, 123, 45)
        val second = hitLocationService.determine(Scenario.SINGLE, BatterArchetype.NEUTRAL, 123, 45)

        assertEquals(first.direction, second.direction)
        assertEquals(first.battedBallType, second.battedBallType)
        assertEquals(first.fielderPosition, second.fielderPosition)
    }

    @Test
    fun `determine changes direction when the difference changes even if the batters number stays fixed`() {
        val fixedBattersNumber = 500

        val directions =
            (0..99).map { difference ->
                hitLocationService.determine(Scenario.SINGLE, BatterArchetype.NEUTRAL, fixedBattersNumber, difference).direction
            }.toSet()

        assertNotEquals(1, directions.size)
    }

    @Test
    fun `determine always returns ground for a left groundout`() {
        val result = hitLocationService.determine(Scenario.LEFT_GROUNDOUT, BatterArchetype.NEUTRAL, 5, 10)

        assertEquals(BattedBallType.GROUND, result.battedBallType)
        assertEquals(HitDirection.LEFT, result.direction)
        assertEquals(5, result.fielderPosition)
    }

    @Test
    fun `determine always returns ground for a right groundout`() {
        val result = hitLocationService.determine(Scenario.RIGHT_GROUNDOUT, BatterArchetype.NEUTRAL, 85, 0)

        assertEquals(BattedBallType.GROUND, result.battedBallType)
        assertEquals(HitDirection.RIGHT, result.direction)
        assertEquals(3, result.fielderPosition)
    }

    @Test
    fun `determine returns only fly or line for a flyout`() {
        val battedBallTypes =
            (0..99).map { seed ->
                hitLocationService.determine(Scenario.FLYOUT, BatterArchetype.NEUTRAL, seed, 0).battedBallType
            }.toSet()

        assertEquals(setOf(BattedBallType.FLY, BattedBallType.LINE), battedBallTypes)
    }

    @Test
    fun `determine never returns a ground ball or popup for a home run`() {
        val battedBallTypes =
            (0..99).map { seed ->
                hitLocationService.determine(Scenario.HOME_RUN, BatterArchetype.NEUTRAL, seed, 0).battedBallType
            }.toSet()

        assertEquals(setOf(BattedBallType.LINE, BattedBallType.FLY), battedBallTypes)
    }

    @Test
    fun `determine returns a pull-heavy skew for power hitters`() {
        val leftCount =
            (0..99).count { seed ->
                hitLocationService.determine(Scenario.SINGLE, BatterArchetype.POWER, seed, 0).direction == HitDirection.LEFT
            }

        assertEquals(35, leftCount)
    }

    @Test
    fun `determine spreads direction evenly for neutral hitters`() {
        val counts =
            HitDirection.entries.associateWith { direction ->
                (0..99).count { seed ->
                    hitLocationService.determine(Scenario.SINGLE, BatterArchetype.NEUTRAL, seed, 0).direction == direction
                }
            }

        counts.values.forEach { count -> assertEquals(20, count) }
    }

    @Test
    fun `buildFieldingNotation returns null when there is no fielder position`() {
        assertNull(hitLocationService.buildFieldingNotation(ActualResult.GROUNDOUT, null, BattedBallType.GROUND))
    }

    @Test
    fun `buildFieldingNotation marks a first baseman groundout as unassisted`() {
        assertEquals("3U", hitLocationService.buildFieldingNotation(ActualResult.GROUNDOUT, 3, BattedBallType.GROUND))
    }

    @Test
    fun `buildFieldingNotation renders a routine groundout as fielder-to-first`() {
        assertEquals("6-3", hitLocationService.buildFieldingNotation(ActualResult.GROUNDOUT, 6, BattedBallType.GROUND))
    }

    @Test
    fun `buildFieldingNotation renders a fielders choice as fielder-to-coverer`() {
        assertEquals("6-4", hitLocationService.buildFieldingNotation(ActualResult.FIELDERS_CHOICE, 6, BattedBallType.GROUND))
        assertEquals("4-6", hitLocationService.buildFieldingNotation(ActualResult.FIELDERS_CHOICE, 4, BattedBallType.GROUND))
    }

    @Test
    fun `buildFieldingNotation renders a double play as the classic three-fielder sequence`() {
        assertEquals("6-4-3", hitLocationService.buildFieldingNotation(ActualResult.DOUBLE_PLAY, 6, BattedBallType.GROUND))
        assertEquals("4-6-3", hitLocationService.buildFieldingNotation(ActualResult.DOUBLE_PLAY, 4, BattedBallType.GROUND))
        assertEquals("5-6-3", hitLocationService.buildFieldingNotation(ActualResult.DOUBLE_PLAY, 5, BattedBallType.GROUND))
    }

    @Test
    fun `buildFieldingNotation renders air outs with the fly or line prefix`() {
        assertEquals("F8", hitLocationService.buildFieldingNotation(ActualResult.FLYOUT, 8, BattedBallType.FLY))
        assertEquals("L7", hitLocationService.buildFieldingNotation(ActualResult.FLYOUT, 7, BattedBallType.LINE))
        assertEquals("F9", hitLocationService.buildFieldingNotation(ActualResult.SACRIFICE_FLY, 9, BattedBallType.FLY))
    }

    @Test
    fun `buildFieldingNotation returns null for outcomes with no fielding sequence`() {
        assertNull(hitLocationService.buildFieldingNotation(ActualResult.SINGLE, 7, BattedBallType.LINE))
        assertNull(hitLocationService.buildFieldingNotation(ActualResult.STRIKEOUT, null, null))
    }
}
