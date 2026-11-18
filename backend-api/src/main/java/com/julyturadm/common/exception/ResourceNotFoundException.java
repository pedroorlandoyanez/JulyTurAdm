package com.julyturadm.common.exception;

//---
// A classe ResourceNotFoundException, estendem RuntimeException e possue
// um construtor que recebe String message.
//---
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
