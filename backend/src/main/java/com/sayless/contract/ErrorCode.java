package com.sayless.contract;

public enum ErrorCode {
    INVALID_JSON(400, "Send a JSON object with message and tone as strings."),
    VALIDATION_ERROR(400, "Enter a message of 1–2000 characters and choose a supported tone."),
    UNSUPPORTED_MEDIA_TYPE(415, "Send the request as application/json."),
    METHOD_NOT_ALLOWED(405, "Use POST to rewrite a message."),
    AI_NOT_CONFIGURED(503, "Rewriting is not available yet. Please try again later."),
    AI_RATE_LIMITED(503, "Rewriting is busy right now. Please wait and try again."),
    AI_UNAVAILABLE(503, "Rewriting is temporarily unavailable. Please try again later."),
    AI_TIMEOUT(504, "Rewriting took too long. Please try again."),
    AI_REFUSED(422, "We couldn’t rewrite that message. Try rephrasing it."),
    AI_INVALID_RESPONSE(502, "We couldn’t create three rewrite options. Please try again."),
    INTERNAL_ERROR(500, "Something went wrong. Please try again.");

    private final int httpStatus;
    private final String publicMessage;

    ErrorCode(int httpStatus, String publicMessage) {
        this.httpStatus = httpStatus;
        this.publicMessage = publicMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String publicMessage() {
        return publicMessage;
    }
}
