package com.nfcaab.backend.enums.team

enum class Conference(val description: String) {
    ACC("ACC"),
    AMERICAN("American"),
    BIG_12("Big 12"),
    BIG_TEN("Big Ten"),
    BIG_EAST("Big East"),
    BIG_WEST("Big West"),
    COLONIAL("Colonial"),
    CONFERENCE_USA("Conference USA"),
    HORIZON("Horizon"),
    IVY_LEAGUE("Ivy League"),
    METRO_ATLANTIC("Metro Atlantic"),
    MID_AMERICAN("Mid-American"),
    MISSOURI_VALLEY("Missouri Valley"),
    MOUNTAIN_WEST("Mountain West"),
    NORTHEAST("Northeast"),
    OHIO_VALLEY("Ohio Valley"),
    PAC_12("Pac-12"),
    PATRIOT("Patriot"),
    SEC("SEC"),
    SOCON("SoCon"),
    SOUTHERN("Southern"),
    SOUTHLAND("Southland"),
    SUMMIT("Summit"),
    SUN_BELT("Sun Belt"),
    SWAC("SWAC"),
    WAC("WAC"),
    WEST_COAST("West Coast"),
    ;

    companion object {
        fun fromString(description: String): Conference? {
            return entries.find { it.description == description }
        }
    }
}

