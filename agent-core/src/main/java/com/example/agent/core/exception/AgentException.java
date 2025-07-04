package com.example.agent.core.exception;

/**
 * Agent 通用异常
 */
public class AgentException extends RuntimeException {
    public AgentException(String message) {
        super(message);
    }
    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
} 