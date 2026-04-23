package com.cargohub.backend.entity;

import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CargoAnalysisLogEntityMappingTest {

    @Test
    void tableName_matchesNewDomainNaming() {
        Table table = CargoAnalysisLog.class.getAnnotation(Table.class);

        assertNotNull(table);
        assertEquals("cargo_analysis_logs", table.name());
    }
}
