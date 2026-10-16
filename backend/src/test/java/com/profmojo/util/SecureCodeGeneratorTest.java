package com.profmojo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecureCodeGenerator Unit Tests")
class SecureCodeGeneratorTest {

    @Test
    @DisplayName("generate4DigitCode: Returns a non-null 4-digit zero-padded numeric string")
    void generate4DigitCode_ReturnsValidFormat() {
        for (int i = 0; i < 100; i++) {
            String code = SecureCodeGenerator.generate4DigitCode();
            assertThat(code)
                    .isNotNull()
                    .hasSize(4)
                    .matches("^[0-9]{4}$");
            int val = Integer.parseInt(code);
            assertThat(val).isBetween(0, 9999);
        }
    }

    @Test
    @DisplayName("generate4DigitCode: Produces varied codes across successive invocations")
    void generate4DigitCode_ProducesVariedOutputs() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            generated.add(SecureCodeGenerator.generate4DigitCode());
        }
        assertThat(generated.size())
                .as("50 random codes should produce substantial diversity")
                .isGreaterThan(30);
    }
}
