package com.imkan.fincore.exception;

public class AccountAccessDeniedException extends RuntimeException {
    public AccountAccessDeniedException() {
        super("Access Denied!");
    }
}