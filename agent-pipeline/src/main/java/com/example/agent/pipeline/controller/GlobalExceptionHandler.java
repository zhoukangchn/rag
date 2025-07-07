package com.example.agent.pipeline.controller;

import com.example.agent.core.exception.AgentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理REST API的错误响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理Agent业务异常
     */
    @ExceptionHandler(AgentException.class)
    public ResponseEntity<ErrorResponse> handleAgentException(AgentException e) {
        logger.error("Agent业务异常: {}", e.getMessage(), e);
        
        ErrorResponse response = new ErrorResponse();
        response.setError("AGENT_ERROR");
        response.setMessage(e.getMessage());
        response.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        logger.warn("参数验证异常: {}", e.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse response = new ErrorResponse();
        response.setError("VALIDATION_ERROR");
        response.setMessage("参数验证失败");
        response.setDetails(errors);
        response.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        logger.warn("绑定异常: {}", e.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse response = new ErrorResponse();
        response.setError("BIND_ERROR");
        response.setMessage("数据绑定失败");
        response.setDetails(errors);
        response.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        logger.warn("参数类型不匹配异常: {}", e.getMessage());
        
        ErrorResponse response = new ErrorResponse();
        response.setError("TYPE_MISMATCH");
        response.setMessage(String.format("参数 '%s' 类型不匹配，期望类型: %s", 
            e.getName(), e.getRequiredType().getSimpleName()));
        response.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("非法参数异常: {}", e.getMessage());
        
        ErrorResponse response = new ErrorResponse();
        response.setError("ILLEGAL_ARGUMENT");
        response.setMessage(e.getMessage());
        response.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException e) {
        logger.error("空指针异常", e);
        
        ErrorResponse response = new ErrorResponse();
        response.setError("NULL_POINTER");
        response.setMessage("内部服务错误");
        response.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        logger.error("运行时异常: {}", e.getMessage(), e);
        
        ErrorResponse response = new ErrorResponse();
        response.setError("RUNTIME_ERROR");
        response.setMessage("运行时异常: " + e.getMessage());
        response.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        logger.error("未知异常: {}", e.getMessage(), e);
        
        ErrorResponse response = new ErrorResponse();
        response.setError("UNKNOWN_ERROR");
        response.setMessage("未知错误: " + e.getMessage());
        response.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 错误响应类
     */
    public static class ErrorResponse {
        private String error;
        private String message;
        private Map<String, String> details;
        private long timestamp;
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public Map<String, String> getDetails() {
            return details;
        }
        
        public void setDetails(Map<String, String> details) {
            this.details = details;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
} 