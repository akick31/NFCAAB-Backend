package com.nfcaab.porygon.enums.game

import com.fasterxml.jackson.annotation.JsonCreator

enum class GameType(val description: String) {
    OUT_OF_CONFERENCE("Out of Conference"),
    CONFERENCE_GAME("Conference Game"),
    CONFERENCE_TOURNAMENT("Conference Tournament"),
    CONFERENCE_CHAMPIONSHIP("Conference Championship"),
    REGIONAL("Regional"),
    REGIONAL_ELIMINATION("Regional Elimination"),
    SUPER_REGIONAL("Super Regional"),
    COLLEGE_WORLD_SERIES("College World Series"),
    COLLEGE_WORLD_SERIES_ELIMINATION("College World Series Elimination"),
    COLLEGE_WORLD_SERIES_CHAMPIONSHIP("College World Series Championship"),
    SCRIMMAGE("Scrimmage"),
    ;

    companion object {
        @JsonCreator
        fun fromDescription(description: String): GameType =
            entries.find { it.description.equals(description, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown game type: $description")
    }
}

