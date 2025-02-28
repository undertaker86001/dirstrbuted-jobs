package com.sucheon.jobs.exception;

public class DistributeHttpException extends RuntimeException{
    public DistributeHttpException(String message){
        super(message);
    }

    public DistributeHttpException(String message, Throwable throwable){
        super(message,throwable);
    }
}
