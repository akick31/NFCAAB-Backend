package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.model.Player.BatterArchetype
import org.springframework.stereotype.Service

data class HitLocation(
    val direction: Game.HitDirection?,
    val battedBallType: Game.BattedBallType?,
    val fielderPosition: Int?,
    val assistSequence: String? = null,
)

@Service
class HitLocationService {
    companion object {
        private val BATTED_BALL_SCENARIOS =
            setOf(
                Scenario.FLYOUT,
                Scenario.LEFT_GROUNDOUT,
                Scenario.RIGHT_GROUNDOUT,
                Scenario.SINGLE,
                Scenario.DOUBLE,
                Scenario.TRIPLE,
                Scenario.HOME_RUN,
            )

        private val DIRECTION_BOUNDARIES: Map<BatterArchetype, IntArray> =
            mapOf(
                BatterArchetype.POWER to intArrayOf(35, 55, 70, 85, 100),
                BatterArchetype.CONTACT to intArrayOf(15, 35, 65, 85, 100),
                BatterArchetype.SPEEDY to intArrayOf(15, 30, 50, 70, 100),
                BatterArchetype.NEUTRAL to intArrayOf(20, 40, 60, 80, 100),
            )

        private val HIT_BATTED_BALL_WEIGHTS: Map<Scenario, IntArray> =
            mapOf(
                Scenario.SINGLE to intArrayOf(40, 75, 90, 100),
                Scenario.DOUBLE to intArrayOf(30, 70, 95, 100),
                Scenario.TRIPLE to intArrayOf(25, 65, 100, 100),
                Scenario.HOME_RUN to intArrayOf(0, 30, 100, 100),
            )
    }

    fun determine(
        scenario: Scenario,
        batterArchetype: BatterArchetype,
        battersNumber: Int,
        difference: Int,
    ): HitLocation {
        if (scenario !in BATTED_BALL_SCENARIOS) {
            return HitLocation(null, null, null)
        }

        val direction = determineDirection(batterArchetype, battersNumber, difference)
        val battedBallType = determineBattedBallType(scenario, battersNumber, difference)
        val fielderPosition = determineFielderPosition(battedBallType, direction)
        return HitLocation(direction, battedBallType, fielderPosition)
    }

    private fun determineDirection(
        batterArchetype: BatterArchetype,
        battersNumber: Int,
        difference: Int,
    ): Game.HitDirection {
        val seed = (battersNumber + difference) % 100
        val boundaries = DIRECTION_BOUNDARIES.getValue(batterArchetype)
        return when {
            seed < boundaries[0] -> Game.HitDirection.LEFT
            seed < boundaries[1] -> Game.HitDirection.LEFT_CENTER
            seed < boundaries[2] -> Game.HitDirection.CENTER
            seed < boundaries[3] -> Game.HitDirection.RIGHT_CENTER
            else -> Game.HitDirection.RIGHT
        }
    }

    private fun determineBattedBallType(
        scenario: Scenario,
        battersNumber: Int,
        difference: Int,
    ): Game.BattedBallType {
        if (scenario == Scenario.FLYOUT) return Game.BattedBallType.FLY
        if (scenario == Scenario.LEFT_GROUNDOUT || scenario == Scenario.RIGHT_GROUNDOUT) return Game.BattedBallType.GROUND

        val seed = (battersNumber * 7 + difference * 3) % 100
        val boundaries = HIT_BATTED_BALL_WEIGHTS.getValue(scenario)
        return when {
            seed < boundaries[0] -> Game.BattedBallType.GROUND
            seed < boundaries[1] -> Game.BattedBallType.LINE
            seed < boundaries[2] -> Game.BattedBallType.FLY
            else -> Game.BattedBallType.POPUP
        }
    }

    private fun determineFielderPosition(
        battedBallType: Game.BattedBallType,
        direction: Game.HitDirection,
    ): Int =
        if (battedBallType == Game.BattedBallType.GROUND) {
            when (direction) {
                Game.HitDirection.LEFT -> 5
                Game.HitDirection.LEFT_CENTER -> 6
                Game.HitDirection.CENTER -> 4
                Game.HitDirection.RIGHT_CENTER -> 4
                Game.HitDirection.RIGHT -> 3
            }
        } else {
            when (direction) {
                Game.HitDirection.LEFT -> 7
                Game.HitDirection.LEFT_CENTER -> 8
                Game.HitDirection.CENTER -> 8
                Game.HitDirection.RIGHT_CENTER -> 8
                Game.HitDirection.RIGHT -> 9
            }
        }
}
