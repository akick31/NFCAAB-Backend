package com.nfcaab.porygon.enums.team

enum class Subdivision(val description: String) {
    NFCAAB("NFCAAB"),
    ;

    companion object {
        fun fromString(description: String): Subdivision? {
            return entries.find { it.description == description }
        }
    }
}

