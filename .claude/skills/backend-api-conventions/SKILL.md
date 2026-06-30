---
name: backend-api-conventions
description: Spring Boot 백엔드의 REST API 설계 규약. 컨트롤러나 API 엔드포인트를 만들거나 수정할 때 사용한다. RESTful URL 설계, HTTP 메서드 사용 규칙, 수정 시 PUT 사용 규칙을 강제한다.
---

# 백엔드 API 설계 규약 (Java + Spring Boot)

REST API를 설계하거나 컨트롤러를 작성·수정할 때 아래 규약을 따른다.

## RESTful URL 규칙

- 자원(resource)은 복수형 명사로 표현한다. 예: `/users`, `/orders`
- URL에 동사를 쓰지 않는다. 행위는 HTTP 메서드로 표현한다.
  - 잘못: `GET /getUsers`, `POST /createUser`
  - 올바름: `GET /users`, `POST /users`
- 계층 관계는 경로로 표현한다. 예: `GET /users/{userId}/orders`
- 필터·정렬·페이징은 쿼리 파라미터로 표현한다. 예: `GET /users?status=ACTIVE&page=0&size=20`

## HTTP 메서드 규칙

| 메서드 | 용도 | 예시 |
| --- | --- | --- |
| GET | 조회 | `GET /users/{id}` |
| POST | 생성 | `POST /users` |
| PUT | 수정 | `PUT /users/{id}` |
| DELETE | 삭제 | `DELETE /users/{id}` |

- **수정은 무조건 PUT을 사용한다. PATCH는 사용하지 않는다.** 부분 수정이라도 PUT으로 처리한다.
- 생성은 POST, 성공 시 `201 Created` 를 반환한다.
- 조회 성공은 `200`, 본문 없는 성공(삭제 등)은 상황에 맞게 `200` 또는 `204` 를 사용한다.

## 상태 코드

- `200 OK`: 조회·수정 성공
- `201 Created`: 생성 성공
- `400 Bad Request`: 잘못된 요청
- `401 / 403`: 인증·인가 실패
- `404 Not Found`: 자원 없음
- `500 Internal Server Error`: 서버 오류

> 에러 응답 본문 형식은 `backend-api-response` Skill을 따른다.

## 컨트롤러 예시

```java
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ApiResponse.ok(userService.getUser(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreateRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    // 수정은 PUT 사용
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long id,
                                                @RequestBody @Valid UserUpdateRequest request) {
        return ApiResponse.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok();
    }
}
```

## 주의

- 컨트롤러는 Entity를 직접 반환하지 않는다. 요청·응답은 DTO를 사용한다 (`backend-jpa-persistence` Skill 참고).
- 공통 응답·예외 형식은 `backend-api-response` Skill을 따른다.
