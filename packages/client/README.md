# 홀덤 클라이언트 (웹)

React + TypeScript + Vite 기반 웹 클라이언트. 모바일 반응형, 서버와 STOMP over SockJS로 통신합니다.

## 요구 사항

- Node.js 18+
- 서버(`server`)가 `http://localhost:8080`에서 실행 중이어야 함

## 실행

```bash
# 루트에서
npm install
npm run dev

# 또는 client 폴더에서
cd packages/client
npm install
npm run dev
```

브라우저에서 http://localhost:5173 접속.  
Vite 프록시로 `/ws`, `/app`, `/topic` 요청이 서버(8080)로 전달됩니다.

## 빌드

```bash
npm run build
```

결과물은 `packages/client/dist`에 생성됩니다. 정적 호스팅 또는 서버에서 서빙하면 됩니다.

## 환경 변수

- `VITE_WS_URL`: WebSocket URL (기본값: 현재 호스트 기준 `/ws`). 배포 시 서버 주소로 설정.

## 구조

```
packages/client/
├── src/
│   ├── components/   # Lobby, GameRoom
│   ├── hooks/        # useStomp (WebSocket)
│   ├── types/        # game 타입
│   ├── config.ts     # WS_URL 등
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
├── index.html
├── vite.config.ts
└── package.json
```
