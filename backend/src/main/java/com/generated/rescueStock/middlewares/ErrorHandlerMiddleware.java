package com.generated.rescueStock.middlewares;

import java.util.Map;
import com.generated.rescueStock.constants.ErrorCodes;
import com.generated.rescueStock.controllers.HandoverControllerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常出口：业务冲突统一 409，参数错误 400，其余 500；仅负责翻译，不在此吞业务规则 */
@RestControllerAdvice
public class ErrorHandlerMiddleware {

  @ExceptionHandler(HandoverControllerException.class)
  public ResponseEntity<Map<String, Object>> handleHandover(HandoverControllerException ex) {
    HttpStatus status = switch (ex.getCode()) {
      case ErrorCodes.HANDOVER_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case ErrorCodes.VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
      default -> HttpStatus.CONFLICT;
    };
    return ResponseEntity.status(status).body(Map.of(
        "code", ex.getCode(),
        "message", ex.getMessage(),
        "meta", ex.getMeta() == null ? Map.of() : ex.getMeta()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(Map.of(
        "code", ErrorCodes.VALIDATION_FAILED,
        "message", ex.getMessage(),
        "meta", Map.of()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
        "code", "INTERNAL_ERROR",
        "message", ex.getMessage() == null ? "internal error" : ex.getMessage(),
        "meta", Map.of()));
  }
}
