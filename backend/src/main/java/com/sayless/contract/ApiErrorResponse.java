package com.sayless.contract;

public record ApiErrorResponse(ApiError error) {

    public record ApiError(String code, String message) {
    }
}
