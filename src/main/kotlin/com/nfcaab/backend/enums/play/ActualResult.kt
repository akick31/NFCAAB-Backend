package com.nfcaab.backend.enums.play

enum class ActualResult(val description: String) {
    STRIKEOUT("Strikeout"),
    WALK("Walk"),
    FLYOUT("Fly out"),
    SACRIFICE_FLY("Sacrifice Fly"),
    GROUNDOUT("Ground out"),
    DOUBLE_PLAY("Double Play"),
    FIELDERS_CHOICE("Fielder's Choice"),
    SINGLE("Single"),
    DOUBLE("Double"),
    TRIPLE("Triple"),
    HOME_RUN("Home Run"),
    DELAY_OF_GAME("Delay of Game"),
    ;

    companion object {
        fun fromString(description: String): ActualResult? {
            return entries.find { it.description == description }
        }
    }
}

