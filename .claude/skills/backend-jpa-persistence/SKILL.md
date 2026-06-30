---
name: backend-jpa-persistence
description: Spring Boot 백엔드의 JPA 영속성 계층 규약. 엔티티·리포지토리·쿼리·DTO를 작성하거나 수정할 때 사용한다. JPA Auditing(생성·수정일), 기본 JPA·복잡 쿼리 시 JPQL/QueryDSL, Entity는 CRUD 계층에서만 쓰고 경계 밖에는 DTO를 쓰는 규칙을 강제한다.
---

# JPA 영속성 계층 규약 (Java + Spring Boot)

엔티티, 리포지토리, 쿼리, DTO를 작성할 때 아래 규약을 따른다.

## 1. 생성일 / 수정일 — JPA Auditing

`created_at`, `updated_at` 은 직접 세팅하지 않고 JPA Auditing 으로 자동 관리한다.

설정:

```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
```

공통 베이스 엔티티:

```java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

- `@CreatedDate` 는 최초 저장 시에만 기록되며 이후 변경되지 않는다 (`updatable = false`).
- `@LastModifiedDate` 는 **엔티티가 실제로 변경(dirty)되어 flush 될 때만** 갱신된다. 변경이 없으면 수정일도 바뀌지 않는다.
- 모든 엔티티는 `BaseEntity` 를 상속한다.

## 2. 기본은 JPA, 복잡하면 JPQL / QueryDSL

- 단순 CRUD·단건 조회는 Spring Data JPA 메서드를 사용한다.

  ```java
  public interface UserRepository extends JpaRepository<User, Long> {
      Optional<User> findByEmail(String email);
  }
  ```

- **SQL이 복잡하거나 join 구조가 복잡한 경우** JPQL 또는 QueryDSL 을 사용한다.
  - 정적인 복잡 쿼리: `@Query` (JPQL)
  - 동적 조건·복잡한 join: QueryDSL

  ```java
  // JPQL
  @Query("select new com.example.dto.UserSummary(u.id, u.name) " +
         "from User u join u.orders o where o.status = :status")
  List<UserSummary> findSummaries(@Param("status") OrderStatus status);
  ```

  ```java
  // QueryDSL (동적 조건)
  public List<User> search(UserSearchCondition cond) {
      return queryFactory.selectFrom(user)
          .where(
              nameEq(cond.getName()),
              statusEq(cond.getStatus())
          )
          .fetch();
  }
  ```

## 3. Entity는 CRUD에서만, 경계 밖은 DTO

- **Entity는 영속성(CRUD) 계층 내부에서만 사용한다.**
- 컨트롤러 요청·응답, 계층 간 데이터 전달에는 DTO를 사용하고 Entity를 직접 노출하지 않는다.

  ```java
  // 요청 DTO
  public record UserCreateRequest(@NotBlank String name, @Email String email) {}

  // 응답 DTO
  public record UserResponse(Long id, String name, String email, LocalDateTime createdAt) {
      public static UserResponse from(User user) {
          return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
      }
  }
  ```

- 조회 쿼리는 가능하면 DTO로 바로 projection 하여 불필요한 엔티티 로딩을 줄인다.

## 4. 수정 처리 (변경 감지)

- 수정은 setter 남발 대신 의미 있는 변경 메서드로 처리하고, 더티 체킹으로 반영한다.

  ```java
  @Transactional
  public UserResponse update(Long id, UserUpdateRequest request) {
      User user = userRepository.findById(id)
              .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
      user.changeProfile(request.name(), request.email()); // 변경된 필드만 수정
      return UserResponse.from(user);
  }
  ```

- 실제 값이 바뀔 때만 변경되므로, 변경이 없으면 `updated_at` 도 갱신되지 않는다.

## 규칙 요약

- `created_at` / `updated_at` 은 Auditing 으로 자동 관리, 수동 세팅 금지.
- 기본은 JPA, 복잡 쿼리는 JPQL / QueryDSL.
- Entity는 CRUD 계층에서만, 경계 밖은 DTO.
