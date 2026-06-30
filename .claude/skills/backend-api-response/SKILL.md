---
name: backend-api-response
description: Spring Boot 백엔드의 공통 응답·에러 응답 포맷과 전역 예외 처리 규약. API 응답을 반환하거나 예외를 정의·처리할 때 사용한다. 모든 응답을 일관된 형태(ApiResponse / ErrorResponse)로 통일한다.
---

# 공통 응답 · 예외 처리 (Java + Spring Boot)

모든 API는 아래 공통 형식으로 응답한다. 컨트롤러·서비스에서 응답을 만들거나 예외를 처리할 때 이 규약을 따른다.

## 1. 공통 성공 응답 — ApiResponse

모든 정상 응답은 `ApiResponse<T>` 로 감싼다.

```java
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    private boolean success;   // 항상 true
    private T data;            // 실제 응답 데이터

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null);
    }
}
```

성공 응답 예시:

```json
{ "success": true, "data": { "id": 1, "name": "홍길동" } }
```

## 2. 공통 에러 응답 — ErrorResponse

모든 오류 응답은 `ErrorResponse` 로 통일한다.

```java
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {
    private boolean success;   // 항상 false
    private String code;       // 비즈니스 에러 코드
    private String message;    // 사용자 메시지

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage());
    }
}
```

에러 응답 예시:

```json
{ "success": false, "code": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다." }
```

## 3. 에러 코드 정의 — ErrorCode

에러는 enum 으로 관리해 코드·메시지·HTTP 상태를 한 곳에서 다룬다.

```java
@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "잘못된 요청입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

## 4. 비즈니스 예외 — BusinessException

도메인 예외는 공통 예외 클래스를 사용한다.

```java
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

사용:

```java
User user = userRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
```

## 5. 전역 예외 처리 — @RestControllerAdvice

예외는 컨트롤러마다 처리하지 않고 한 곳에서 공통 처리한다.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }
}
```

## 규칙 요약

- 정상 응답은 항상 `ApiResponse<T>`, 오류 응답은 항상 `ErrorResponse` 로 통일한다.
- 예외는 컨트롤러에서 try-catch 하지 않고 `@RestControllerAdvice` 에서 공통 처리한다.
- 새 오류는 `ErrorCode` enum에 추가해 코드·메시지를 일관되게 관리한다.
