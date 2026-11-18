package com.julyturadm.common.exception;

//---
// A classe BusinessException, estendem RuntimeException e possue
// um construtor que recebe String message.
//---
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}

