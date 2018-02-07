package com.dk.ethereumwallet.core;

/**
 * Created by ayrton on 2/7/18.
 */

public class ValidationException extends RuntimeException {
    public ValidationException(String msg) {
        super(msg);
    }
}
