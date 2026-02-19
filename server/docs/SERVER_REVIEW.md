# 홀덤 서버 구성 검토

## 1. 현재 디렉터리 구조

```
com.holdup.server
├── action          # 플레이어 액션 (FOLD, CALL, BET, JOIN_TABLE 등)
│   ├── dto         # PlayerActionRequest, ActionResult
│   └── handler     # 액션별 핸들러 (스켈레톤)
├── card            # 카드·패 등급 (Suit, Rank, Card, HandRank, HandEvaluation, HandEvaluator, Cards)
├── config          # WebSocketConfig (STOMP /app, /topic, /ws)
├── deck            # Deck, DeckFactory, ShuffleStrategy
├── gamestate       # GamePhase, HandState (한 핸드 진행 상태)
├── handevaluator   # WinnerResolver, SeatHandResult, ParticipantHand, HandEvaluatorConfig
├── player          # Player (스택, 홀카드, 폴드/올인 상태)
├── table           # Seat, Table (시트, 핸드상태, 덱, 블라인드)
├── service         # TableManager (테이블 생성·조회)
├── controller      # GameController (/app/action, /app/hello)
└── HoldupApplication
```

## 2. 잘 갖춰진 부분

| 영역 | 상태 | 비고 |
|------|------|------|
| **카드/덱** | ✅ | Card, Suit, Rank, Deck, DeckFactory, 셔플 전략 분리 |
| **패 평가** | ✅ | HandEvaluator, DefaultHandEvaluator, HandRank, 7장 평가·동점 비교 |
| **승자 판정** | ✅ | WinnerResolver(participants + communityCards → 순위), HandEvaluatorConfig 빈 등록 |
| **도메인 모델** | ✅ | Player(스택, 홀카드, 폴드/올인), Seat, Table, HandState, GamePhase |
| **액션 파이프라인** | ✅ | GameActionType, ActionDispatcher, 액션별 핸들러 등록 |
| **WebSocket** | ✅ | STOMP /app, /topic, /queue, /ws+SockJS |
| **테이블 저장소** | ✅ | TableManager (테이블 생성·조회) 추가됨 |

## 3. 구현 완료된 연결

### 3.1 액션 핸들러 ↔ 테이블/게임 상태 ✅

- **ActionValidation**: 테이블 조회·플레이어 착석·현재 턴 검증 공통 처리.
- **FOLD/CHECK/CALL/BET/RAISE/ALL_IN**: TableManager·GameFlow 연동, 턴 검증 후 상태 갱신.
- **필요**:  
  - `TableManager.getTable(request.getTableId())` 로 테이블 조회  
  - 해당 테이블의 `HandState`, `Seat`/`Player`를 사용해  
    - 턴 검증(actingSeatIndex vs 요청 플레이어 시트)  
    - FOLD/CALL/BET/RAISE/ALL_IN 시 플레이어·팟·currentBet 등 갱신  

### 3.2 핸드 진행(게임 플로우) 서비스

- **현재**: 덱 생성·딜링·블라인드·스트릿 전환·쇼다운을 한 곳에서 orchestration 하는 서비스 없음.
- **필요**:  
  - 예: `GameFlowService` 또는 `HandRunner`  
    - 새 핸드 시작: 덱 셔플, 딜러/SB/BB 결정, 홀카드 2장 딜링, HandState phase = PREFLOP  
    - 플랍/턴/리버: 커뮤니티 카드 딜링, phase 전환, betPerSeat 초기화  
    - 쇼다운: 폴드 제외 참가자로 `WinnerResolver.evaluateWinners` 호출, 팟 분배, Player 스택 갱신  
  - 액션 핸들러는 “이 테이블에서 이 액션만 검증·적용”하고, “스트릿 종료/핸드 종료” 판단 및 다음 단계 진행은 GameFlow에서 처리하는 식으로 분리 가능.

### 3.3 테이블별 구독

- **현재**: `GameController`가 액션 결과를 `/topic/actions`로만 전송 → 모든 구독자가 모든 테이블 메시지를 받음.
- **권장**: 테이블별 이벤트는 `/topic/table/{tableId}` 로 보내고, 클라이언트는 참가한 테이블만 구독하도록 구성하면 확장·보안에 유리함.

### 3.4 인증·세션

- **현재**: `Principal`로 playerId 사용, 미인증 시 "anonymous".
- **권장**: 실제 서비스에서는 STOMP 연결 시 인증(세션/JWT 등)으로 playerId를 고정하고, 테이블 입장 시 해당 playerId가 그 테이블의 어떤 시트에 앉았는지 매핑해 두면, 액션 검증(내 턴인지, 해당 시트가 나인지)이 명확해짐.

## 4. 요약

- **도메인·인프라**: 카드/덱/패평가/승자판정/테이블/플레이어/핸드상태/액션 라우팅/WebSocket/테이블 저장소까지 홀덤 서버에 필요한 뼈대는 잘 갖춰져 있음.
- **한 판 흐름**: JOIN_TABLE(tableId) → (선택) SIT(tableId, seatIndex) → START_HAND(tableId) → FOLD/CHECK/CALL/BET/RAISE/ALL_IN 반복 → 쇼다운 후 WAITING. 클라이언트는 `/topic/table/{tableId}` 구독으로 해당 테이블 이벤트만 수신.

이 문서는 서버 구성을 한 번에 점검할 때 참고용으로 두면 됩니다.
