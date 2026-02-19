# 홀덤 (Hold'em Poker)

친구들과 함께 즐기는 **최대 8명** 멀티플레이어 웹 홀덤 게임입니다.  
서버(Spring + WebSocket)와 웹 클라이언트(모바일 지원)로 구성됩니다.

## 프로젝트 구조

```
홀덤/
├── package.json          # 루트 (클라이언트/공통 패키지 관리)
├── README.md
├── server/               # Spring Boot + WebSocket 게임 서버
└── packages/
    ├── client/           # 웹 클라이언트 (반응형, 모바일 지원)
    └── shared/           # 서버·클라이언트 공통 타입/상수 (선택)
```

## 요구 사항

- **서버**: Java 17+, Spring Boot, WebSocket
- **클라이언트**: Node.js 18+, npm

## 실행 방법

### 서버 (Spring)

```bash
cd server
./mvnw spring-boot:run
# 또는
./mvnw clean package && java -jar target/*.jar
```

서버가 올라오면 기본적으로 `http://localhost:8080` (및 WebSocket 엔드포인트)에서 동작합니다.

### 클라이언트 (웹)

```bash
npm install
npm run dev
```

브라우저에서 표시되는 주소(예: `http://localhost:5173`)로 접속합니다.

## 배포 및 외부 접속

집에서 서버를 운영해 외부에서 접속하려면:

1. **공유기 포트 포워딩**: 서버 포트(예: 8080, 8443)를 내부 PC로 연결
2. **방화벽**: 해당 포트 인바운드 허용
3. **공인 IP 또는 DDNS**: 접속 주소 고정/공유
4. **보안**: 가능하면 HTTPS/WSS, 방 비밀번호, 요청 검증 적용

자세한 내용은 프로젝트 내 배포/보안 문서를 참고하세요.

## 라이선스

MIT
