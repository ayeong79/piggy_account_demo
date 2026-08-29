package com.kakaobank.piggybank.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PiggyBankBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(PiggyBankBusinessException e) {
        return ResponseEntity.status(e.getHttpStatus()).body(ErrorResponse.of(e));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_FAILED", message));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleConcurrency(Exception e) {
        // 동시성 문제(예: 유니크 제약 위반)는 DB가 최후 방어선 — 신규가입 3단계 설명 참고.
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("DATA_INTEGRITY_VIOLATION",
                        "동시 요청으로 인한 제약조건 위반입니다. 잠시 후 다시 시도해주세요."));
    }
}
