package com.project.cybershop.shared.exception;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.DisconnectedClientHelper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Hidden
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        HttpStatus status = ex.getHttpStatus();
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", ex.getMessage());
        // Lớp con (ví dụ BuildRuleException của pcbuild) gắn mã lỗi và dữ liệu cho FE
        if (ex.getCode() != null) body.put("code", ex.getCode());
        if (ex.getData() != null) body.put("data", ex.getData());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (ex.getRetryAfterSeconds() != null) {
            response.header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        }
        return response.body(body);
    }

    // Nếu thiếu hai hàm dưới, lỗi chưa đăng nhập và không đủ quyền sẽ rơi vào handleGeneralException thành 500.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthenticated(AuthenticationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Error");

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        body.put("validationErrors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // JSON hỏng, sai kiểu dữ liệu, thiếu tham số là lỗi của phía gọi. Không trả ex.getMessage() vì nó lộ tên lớp Java.
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, Object>> handleMalformedRequest(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Dữ liệu gửi lên không hợp lệ.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Ràng buộc DB (trùng số điện thoại, trùng serial...) khi hai yêu cầu chạy đua qua được bước kiểm tra trước đó.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataConflict(DataIntegrityViolationException ex) {
        return errorBody(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc xung đột với dữ liệu đã có.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        // Lỗi chuẩn của Spring (sai URL, sai phương thức, thiếu header...) đã tự biết mã HTTP đúng.
        if (ex instanceof ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            String reason = ex instanceof ResponseStatusException rse ? rse.getReason() : null;
            return errorBody(status, reason != null ? reason : messageFor(status));
        }
        // Trình duyệt rời trang giữa chừng: không còn ai nhận phản hồi, cũng không phải lỗi máy chủ.
        if (DisconnectedClientHelper.isClientDisconnectedException(ex)) {
            return null;
        }
        // Lỗi thật của máy chủ: ghi log để điều tra, không trả chi tiết nội bộ cho client.
        log.error("Lỗi không mong đợi", ex);
        return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống đang gặp lỗi. Vui lòng thử lại sau.");
    }

    private static String messageFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Không tìm thấy đường dẫn yêu cầu.";
            case METHOD_NOT_ALLOWED -> "Phương thức này không được hỗ trợ.";
            case UNSUPPORTED_MEDIA_TYPE -> "Định dạng dữ liệu gửi lên không được hỗ trợ.";
            case NOT_ACCEPTABLE -> "Không trả được dữ liệu theo định dạng yêu cầu.";
            default -> status.is4xxClientError() ? "Yêu cầu không hợp lệ." : "Hệ thống đang gặp lỗi. Vui lòng thử lại sau.";
        };
    }

    private static ResponseEntity<Map<String, Object>> errorBody(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
