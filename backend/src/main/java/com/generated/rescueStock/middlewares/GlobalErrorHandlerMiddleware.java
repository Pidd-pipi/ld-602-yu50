package com.generated.rescueStock.middlewares;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局错误处理中间件：兜底所有未在 controller 包装的异常，
 * 避免在单一位置吞掉业务异常（业务异常已由各 controller 分别包装并携带错误码与原因）。
 */
@RestControllerAdvice
public class GlobalErrorHandlerMiddleware {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody("VALIDATION_FAILED", ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(errorBody("INTERNAL_ERROR", "服务暂时不可用，请稍后重试。"));
  }

  private Map<String, Object> errorBody(String code, String message) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("code", code);
    body.put("message", message);
    return body;
  }
}
