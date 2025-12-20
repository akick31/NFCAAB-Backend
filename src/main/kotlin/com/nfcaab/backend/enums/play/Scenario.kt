package com.nfcaab.backend.enums.play

enum class Scenario(val description: String) {
    GAME_START("GAME START"),
    PLAY_RESULT("PLAY RESULT"),
    NORMAL_NUMBER_REQUEST("NORMAL NUMBER REQUEST"),
    DM_NUMBER_REQUEST("DM NUMBER REQUEST"),
    DELAY_OF_GAME_WARNING("DELAY OF GAME WARNING"),
    DELAY_OF_GAME_NOTIFICATION("DELAY OF GAME NOTIFICATION"),
    PREGAME_DELAY_OF_GAME_NOTIFICATION("PREGAME DELAY OF GAME NOTIFICATION"),
    DELAY_OF_GAME_HOME("DELAY OF GAME ON HOME TEAM"),
    DELAY_OF_GAME_AWAY("DELAY OF GAME ON AWAY TEAM"),
    GAME_OVER("GAME OVER"),
    PITCHER_CHANGE("PITCHER CHANGE"),
    BATTER_CHANGE("BATTER CHANGE"),
    STEAL_ATTEMPT("STEAL ATTEMPT"),
    STEAL_SUCCESS("STEAL SUCCESS"),
    STRIKEOUT("Strikeout"),
    WALK("Walk"),
    FLYOUT("Flyout"),
    LEFT_GROUNDOUT("Left Groundout"),
    RIGHT_GROUNDOUT("Right Groundout"),
    SINGLE("Single"),
    DOUBLE("Double"),
    TRIPLE("Triple"),
    HOME_RUN("Home Run"),
    ;

    companion object {
        fun fromString(description: String): Scenario? {
            return entries.find { it.description == description }
        }
    }
}

