package ru.cinimex;

public class KorusException extends Exception {
    private int errorCode;


    KorusException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

}
