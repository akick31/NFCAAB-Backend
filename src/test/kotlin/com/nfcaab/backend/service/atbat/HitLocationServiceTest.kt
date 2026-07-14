package com.nfcaab.backend.service.atbat

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
    fun `determine always returns fly for a flyout`() {
        val result = hitLocationService.determine(Scenario.FLYOUT, BatterArchetype.NEUTRAL, 500, 30)

        assertEquals(BattedBallType.FLY, result.battedBallType)
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
}
