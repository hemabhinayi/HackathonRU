package com.sayless.contract;

import java.util.Objects;

/** Carries only a safe error code; never provider text, prompts, or user input. */
public final class RewriteException extends RuntimeException {

    private final ErrorCode code;

    public RewriteException(ErrorCode code) {
        super(Objects.requireNonNull(code, "code").name());
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
