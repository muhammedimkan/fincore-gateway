package com.imkan.fincore.exception;

public class AccountNotFoundException extends RuntimeException{
    public AccountNotFoundException(Long id){
        super("No account found with id: " + id);
    }
}
