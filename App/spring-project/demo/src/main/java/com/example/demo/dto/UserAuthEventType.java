package com.example.demo.dto;

public enum UserAuthEventType {
    SIGNUP,
    LOGIN,
    LOGOUT,
    PASSWORD_UPDATE,
    UNKNOWN;

    public static UserAuthEventType fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return UserAuthEventType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
