# 홀덤 게임 서버 (Spring Boot + WebSocket)

## 요구 사항

- **Java 17** 이상 (JDK 설치 확인: `java -version`)
- **Maven 3.6+** (또는 프로젝트 내 Maven Wrapper 사용)

## 빌드 및 실행

```bash
cd server

# Maven이 설치되어 있다면
mvn spring-boot:run

# 또는 JAR 빌드 후 실행
mvn clean package -DskipTests
java -jar target/holdup-server-0.1.0.jar
```

Maven Wrapper가 없다면 한 번만 생성:
```bash
mvn -N wrapper:wrapper
```
이후 `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`) 사용 가능.

## 접속 정보

- **HTTP**: http://localhost:8080
- **WebSocket (STOMP over SockJS)**: http://localhost:8080/ws

클라이언트에서 SockJS로 `/ws`에 연결한 뒤, STOMP로:
- **송신**: `/app/hello` (예시)
- **수신 구독**: `/topic/greetings`

## 설정

`src/main/resources/application.yml`:

| 항목 | 설명 | 기본값 |
|------|------|--------|
| `server.port` | 서버 포트 | 8080 |
| `holdup.game.max-players-per-room` | 방당 최대 인원 | 8 |
| `holdup.game.min-players-to-start` | 게임 시작 최소 인원 | 2 |

프로파일로 포트/설정 변경:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
```

## 프로젝트 구조

```
server/
├── pom.xml
├── src/main/java/com/holdup/server/
│   ├── HoldupApplication.java      # 진입점
│   ├── config/
│   │   └── WebSocketConfig.java    # STOMP 엔드포인트 /ws, 브로커 설정
│   └── controller/
│       └── GameController.java     # 게임 메시지 핸들러 (예시)
└── src/main/resources/
    └── application.yml
```

이후 `game`, `room`, `deck` 등 패키지를 추가해 게임 로직을 확장하면 됩니다.
