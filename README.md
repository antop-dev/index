# Index

Nginx 설정 파일을 읽어 등록된 웹 서비스를 자동으로 수집하고, 썸네일과 함께 카드 형태로 보여주는 대시보드입니다.

## 기능

- Nginx `nginx.conf`의 `location` 블록을 파싱하여 서비스 URL 자동 수집
- Jsoup으로 각 페이지의 `<title>` 크롤링
- Playwright(Chromium)로 썸네일 스크린샷 자동 캡처
- 1시간마다 배치 실행 (앱 시작 1초 후 최초 실행)
- 이름 / URL 기준 실시간 검색

## 기술 스택

| 구분        | 사용 기술                   |
|-----------|-------------------------|
| Language  | Kotlin 1.9              |
| Framework | Spring Boot 3           |
| View      | Thymeleaf, Tailwind CSS |
| Database  | SQLite (Flyway 마이그레이션)  |
| Crawler   | Jsoup, Playwright       |
| Build     | Gradle                  |

## 실행

### 요구사항

- JDK 17 이상
- Chromium 실행 환경 (Playwright가 자동 설치)

### 환경 변수

| 변수                | 기본값                  | 설명               |
|-------------------|----------------------|------------------|
| `DB_PATH`         | `./index.db`         | SQLite DB 파일 경로  |
| `NGINX_CONF_PATH` | `./p.antop.ai`       | nginx.conf 파일 경로 |
| `THUMBNAILS_DIR`  | `./thumbnails`       | 스크린샷 저장 디렉토리     |
| `BASE_URL`        | `https://p.antop.ai` | 서비스 베이스 URL      |

### 빌드 및 실행

```bash
./gradlew bootJar

java -jar build/libs/index-*.jar --spring.profiles.active=prd
```
