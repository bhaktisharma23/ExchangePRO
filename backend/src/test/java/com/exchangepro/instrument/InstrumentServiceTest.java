package com.exchangepro.instrument;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InstrumentServiceTest {

    @Test
    void likeWildcardsInUserInputAreEscaped() {
        assertThat(InstrumentService.escapeLike("50%_off\\")).isEqualTo("50\\%\\_off\\\\");
        assertThat(InstrumentService.escapeLike("tata")).isEqualTo("tata");
    }

    @Test
    void symbolsAreNormalisedToUpperCase() {
        assertThat(InstrumentService.normalizeSymbol(" reliance ")).isEqualTo("RELIANCE");
        assertThat(InstrumentService.normalizeSymbol("m&m")).isEqualTo("M&M");
    }
}
