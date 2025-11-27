package com.nfcaab.porygon.model

import javax.persistence.Column
import javax.persistence.Entity
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

    @Column(name = "college_year")
    open var collegeYear: Int? = null

    @Column(name = "primary_position")
    open var primaryPosition: Position? = null

    @Column(name = "secondary_position")
    open var secondaryPosition: Position? = null

    @Column(name = "archetype")
    open var archetype: Archetype? = null

    @Column(name = "current_team")
    open var currentTeam: String? = null

    enum class Archetype(val description: String) {
        POWER("Power"),
        SPEEDY("Speedy"),
        NEUTRAL("Neutral"),
        STRIKEOUT("Strikeout"),
        GROUND_BALL("Ground Ball"),
        FLY_BALL("Fly Ball"),
        ;

        companion object {
            fun fromDescription(description: String): Archetype {
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
        ;

        companion object {
            fun fromDescription(description: String): Position {
                return entries.first { it.description == description }
            }
        }
    }
}
