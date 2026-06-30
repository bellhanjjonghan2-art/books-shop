# books-shop

도서 판매 사이트 초기 골격. `shop-back`(Spring Boot) + `shop-front`(React/Vite) 멀티 모듈 프로젝트.

## 환경

- JDK 21
- Node.js v24.15.0
- Gradle (Wrapper 포함, 별도 설치 불필요)

## 개발 모드 실행

두 모듈을 각각 별도 프로세스로 실행한다.

### 백엔드 (포트 8080)

```bash
./gradlew :shop-back:bootRun
```

### 프론트엔드 (포트 5173)

```bash
cd shop-front
npm install
npm run dev
```

`http://localhost:5173` 접속 시 `/api` 요청은 Vite dev 서버 proxy를 통해 `http://localhost:8080`으로 전달된다.

## 통합 빌드

루트에서 다음 명령 한 번으로 프론트엔드(`npm ci` + `npm run build`)와 백엔드를 함께 빌드하고,
`shop-front/dist` 결과물을 `shop-back/src/main/resources/static`으로 복사하여 단일 실행 jar를 만든다.

```bash
./gradlew build
```

빌드된 jar 실행:

```bash
java -jar shop-back/build/libs/shop-back-0.0.1-SNAPSHOT.jar
```

`http://localhost:8080/api/health` 호출 시 `{"status":"ok"}` 가 반환된다.
