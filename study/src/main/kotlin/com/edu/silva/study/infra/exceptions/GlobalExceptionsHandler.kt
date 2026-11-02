package com.edu.silva.study.infra.exceptions

import com.edu.silva.common.DefaultResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionsHandler {

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(ex: EntityNotFoundException): ResponseEntity<DefaultResponse<Void>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(DefaultResponse<Void>(ex.message))
    }

    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequest(ex: InvalidRequestException): ResponseEntity<DefaultResponse<Void>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DefaultResponse<Void>(ex.message))
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleGeneric(ex: RuntimeException): ResponseEntity<DefaultResponse<Void>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(DefaultResponse<Void>(ex.message))
    }
}
