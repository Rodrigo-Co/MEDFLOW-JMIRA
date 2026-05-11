package com.medflow.util;

import com.medflow.http.HttpException;

public final class Validation {

    private Validation() {
    }

    public static void require(boolean condition, int status, String message) {
        if (!condition) {
            throw new HttpException(status, message);
        }
    }

    public static void notBlank(String value, String message) {
        require(value != null && !value.isBlank(), 400, message);
    }

    public static void minLength(String value, int minLength, String message) {
        require(value != null && value.length() >= minLength, 400, message);
    }
}
