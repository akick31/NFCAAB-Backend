package com.nfcaab.backend.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "players", schema = "porygon")
open class Player {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "first_name")
    open var firstName: String? = null

    @Column(name = "last_name")
    open var lastName: String? = null

    @Column(name = "uniform_number")
    open var uniformNumber: Int? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "college_year")
    open var collegeYear: CollegeYear? = null

    @Column(name = "active", nullable = false)
    open var active: Boolean = true

    @Column(name = "primary_position")
    open var primaryPosition: Position? = null

    @Column(name = "secondary_position")
    open var secondaryPosition: Position? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "batter_archetype")
    open var batterArchetype: BatterArchetype? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "pitcher_archetype")
    open var pitcherArchetype: PitcherArchetype? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "pitcher_role")
    open var pitcherRole: PitcherRole? = null

    @Column(name = "last_start_game_id")
    open var lastStartGameId: Int? = null

    @Column(name = "current_team")
    open var currentTeam: String? = null

    enum class CollegeYear(val description: String) {
        FRESHMAN("Freshman"),
        SOPHOMORE("Sophomore"),
        JUNIOR("Junior"),
        SENIOR("Senior"),
        GRADUATED("Graduated"),
        ;

        companion object {
            fun fromDescription(description: String): CollegeYear {
                return entries.first { it.description == description }
            }
        }
    }

    enum class BatterArchetype(val description: String) {
        POWER("Power"),
        SPEEDY("Speedy"),
        CONTACT("Contact"),
        NEUTRAL("Neutral"),
        ;

        companion object {
            fun fromDescription(description: String): BatterArchetype {
                return entries.first { it.description == description }
            }
        }
    }

    enum class PitcherArchetype(val description: String) {
        STRIKEOUT("Strikeout"),
        GROUND_BALL("Ground Ball"),
        FLY_BALL("Fly Ball"),
        CONTROL("Control"),
        NEUTRAL("Neutral"),
        ;

        companion object {
            fun fromDescription(description: String): PitcherArchetype {
                return entries.first { it.description == description }
            }
        }
    }

    enum class PitcherRole(val description: String) {
        STARTER("Starter"),
        RELIEVER("Reliever"),
        ;

        companion object {
            fun fromDescription(description: String): PitcherRole {
                return entries.first { it.description == description }
            }
        }
    }

    enum class Position(val description: String) {
        PITCHER("Pitcher"),
        CATCHER("Catcher"),
        FIRST_BASE("First Base"),
        SECOND_BASE("Second Base"),
        THIRD_BASE("Third Base"),
        SHORTSTOP("Shortstop"),
        LEFT_FIELD("Left Field"),
        CENTER_FIELD("Center Field"),
        RIGHT_FIELD("Right Field"),
        DESIGNATED_HITTER("Designated Hitter"),
        ;

        companion object {
            fun fromDescription(description: String): Position {
                return entries.first { it.description == description }
            }

            val FIELD_POSITIONS: Set<Position> = entries.toSet() - PITCHER
        }
    }
}
