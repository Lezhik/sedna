package io.sedna.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void okCarriesValue() {
        Result<String, SemanticError> result = Result.ok("dna");
        assertTrue(result.isOk());
        assertEquals("dna", result.value());
    }

    @Test
    void errCarriesError() {
        SemanticError error = SemanticError.global(ErrorCode.NOT_IMPLEMENTED, "stub");
        Result<String, SemanticError> result = Result.err(error);
        assertFalse(result.isOk());
        assertEquals(error, result.error());
    }

    @Test
    void valueOnErrThrows() {
        assertThrows(IllegalStateException.class, () -> Result.err(SemanticError.global(ErrorCode.INTERNAL, "x")).value());
    }
}
