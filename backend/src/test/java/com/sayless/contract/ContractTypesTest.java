package com.sayless.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ContractTypesTest {

    @Test
    void toneWireValuesRoundTripExactly() {
        for (Tone tone : Tone.values()) {
            assertThat(Tone.fromWireValue(tone.wireValue())).isSameAs(tone);
        }
        assertThat(Tone.PROFESSIONAL.wireValue()).isEqualTo("professional");
        assertThat(Tone.FUNNY.wireValue()).isEqualTo("funny");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Professional", "CASUAL", " casual", "casual ", "sarcastic", "\tfunny"})
    void toneRejectsAnythingButExactLowercase(String value) {
        assertThatThrownBy(() -> Tone.fromWireValue(value))
                .isInstanceOfSatisfying(RewriteException.class,
                        ex -> assertThat(ex.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void errorCodesMatchTheContractTable() {
        assertThat(ErrorCode.values()).hasSize(11);
        assertThat(ErrorCode.INVALID_JSON.httpStatus()).isEqualTo(400);
        assertThat(ErrorCode.VALIDATION_ERROR.httpStatus()).isEqualTo(400);
        assertThat(ErrorCode.UNSUPPORTED_MEDIA_TYPE.httpStatus()).isEqualTo(415);
        assertThat(ErrorCode.METHOD_NOT_ALLOWED.httpStatus()).isEqualTo(405);
        assertThat(ErrorCode.AI_NOT_CONFIGURED.httpStatus()).isEqualTo(503);
        assertThat(ErrorCode.AI_RATE_LIMITED.httpStatus()).isEqualTo(503);
        assertThat(ErrorCode.AI_UNAVAILABLE.httpStatus()).isEqualTo(503);
        assertThat(ErrorCode.AI_TIMEOUT.httpStatus()).isEqualTo(504);
        assertThat(ErrorCode.AI_REFUSED.httpStatus()).isEqualTo(422);
        assertThat(ErrorCode.AI_INVALID_RESPONSE.httpStatus()).isEqualTo(502);
        assertThat(ErrorCode.INTERNAL_ERROR.httpStatus()).isEqualTo(500);
        assertThat(ErrorCode.VALIDATION_ERROR.publicMessage())
                .isEqualTo("Enter a message of 1–2000 characters and choose a supported tone.");
    }

    @Test
    void rewriteExceptionCarriesOnlyTheCode() {
        RewriteException ex = new RewriteException(ErrorCode.AI_TIMEOUT);

        assertThat(ex.code()).isEqualTo(ErrorCode.AI_TIMEOUT);
        assertThat(ex.getMessage()).isEqualTo("AI_TIMEOUT");
        assertThat(ex.getCause()).isNull();
    }
}
