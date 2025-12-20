package com.nfcaab.backend.enums.game

enum class TVChannel(val description: String) {
    ESPN("ESPN"),
    ESPN2("ESPN2"),
    ESPN3("ESPN3"),
    ESPNU("ESPNU"),
    ABC("ABC"),
    CBS("CBS"),
    FOX("FOX"),
    FS1("FS1"),
    FS2("FS2"),
    SEC_NETWORK("SEC Network"),
    ACC_NETWORK("ACC Network"),
    BIG_TEN_NETWORK("Big Ten Network"),
    PAC_12_NETWORK("Pac-12 Network"),
    LONGHORN_NETWORK("Longhorn Network"),
    ;

    companion object {
        fun fromString(description: String): TVChannel? {
            return entries.find { it.description == description }
        }
    }
}

