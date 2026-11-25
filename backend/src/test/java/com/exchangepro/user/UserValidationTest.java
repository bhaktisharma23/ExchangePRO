package com.exchangepro.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserValidationTest {

    private static final Pattern DISPLAY_NAME = Pattern.compile(UserValidation.DISPLAY_NAME_PATTERN);

    @ParameterizedTest
    @ValueSource(strings = {"Asha Trader", "O'Neil-2", "आशा", "प्रिया शर्मा", "ராஜ்", "José_Ñ"})
    void acceptsNamesInIndianAndOtherScripts(String name) {
        assertThat(DISPLAY_NAME.matcher(name).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"<script>", "a\nb", "name;drop", "x@y"})
    void rejectsMarkupControlCharactersAndSymbols(String name) {
        assertThat(DISPLAY_NAME.matcher(name).matches()).isFalse();
    }
}
