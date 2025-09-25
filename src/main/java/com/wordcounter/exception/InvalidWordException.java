package com.wordcounter.exception;

public class InvalidWordException extends Exception {

    private final String invalidWord;


    public InvalidWordException(String message, String invalidWord) {
        super(message);
        this.invalidWord = invalidWord;
    }


    public String getInvalidWord() {
        return invalidWord;
    }
}