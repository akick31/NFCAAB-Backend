package com.nfcaab.backend.converter

import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.enums.team.Subdivision
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class SubdivisionConverter : AttributeConverter<Subdivision, String> {
    override fun convertToDatabaseColumn(attribute: Subdivision?): String? {
        return attribute?.description
    }

    override fun convertToEntityAttribute(dbData: String?): Subdivision? {
        return dbData?.let { Subdivision.fromString(it) }
    }
}

@Converter(autoApply = true)
class ResultConverter : AttributeConverter<Scenario, String> {
    override fun convertToDatabaseColumn(attribute: Scenario?): String? {
        return attribute?.description
    }

    override fun convertToEntityAttribute(dbData: String?): Scenario? {
        return dbData?.let { Scenario.fromString(it) }
    }
}
