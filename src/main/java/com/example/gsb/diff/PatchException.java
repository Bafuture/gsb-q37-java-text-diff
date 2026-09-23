package com.example.gsb.diff;

/** Thrown when a patch cannot be applied because the base text does not match. */
public class PatchException extends RuntimeException {

    public PatchException(String message) {
        super(message);
    }
}
