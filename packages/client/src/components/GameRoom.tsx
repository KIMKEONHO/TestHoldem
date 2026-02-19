import { useEffect, useState } from 'react';
import { APP_PREFIX, TOPIC_PREFIX, USER_QUEUE_TABLE_STATE } from '../config';
import type {
  ActionResult,
  TableSnapshot,
  GameActionType,
  HandStateSnapshot,
  SeatSnapshot,
} from '../types/game';

interface GameRoomProps {
  roomId: string;
  nickname: string;
  isConnected: boolean;
  onLeave: () => void;
  send: (destination: string, body: object) => void;
  subscribe: (destination: string, onMessage: (body: unknown) => void) => () => void;
}

export function GameRoom({
  roomId,
  nickname,
  isConnected,
  onLeave,
  send,
  subscribe,
}: GameRoomProps) {
  const [tableState, setTableState] = useState<TableSnapshot | null>(null);
  const [joinError, setJoinError] = useState<string | null>(null);
  const [mySeatIndex, setMySeatIndex] = useState<number | null>(null);
  const [joinTimeout, setJoinTimeout] = useState(false);

  const sendJoin = () => {
    if (!isConnected || !roomId || !nickname) return;
    setJoinError(null);
    setJoinTimeout(false);
    send(`${APP_PREFIX}/action`, {
      actionType: 'JOIN_TABLE',
      tableId: roomId,
      playerId: nickname,
    });
  };

  useEffect(() => {
    if (!isConnected || !roomId || !nickname) return;

    setTableState(null);
    setMySeatIndex(null);

    const unsub = subscribe(`${TOPIC_PREFIX}/table/${roomId}`, (body) => {
      try {
        const res = typeof body === 'string' ? (JSON.parse(body) as ActionResult) : (body as ActionResult);
        // 내 시트 번호는 입장(JOIN_TABLE) 응답에서만 설정. 다른 액션 응답의 seatIndex로 덮어쓰지 않음.
        if (res.actionType === 'JOIN_TABLE' && res.seatIndex != null) {
          setMySeatIndex(res.seatIndex);
        }
        if (res.payload?.tableState) {
          setTableState(res.payload.tableState);
          setJoinError(null);
          setJoinTimeout(false);
        } else if (res.success && res.tableId) {
          // 서버가 tableState 없이 보낸 경우(또는 직렬화 차이) 최소 상태로 화면 전환
          setTableState({
            tableId: res.tableId,
            tableName: `Table-${res.tableId}`,
            seats: [],
            handState: {
              phase: 'WAITING',
              communityCards: [],
              pot: 0,
              currentBet: 0,
              actingSeatIndex: null,
              minRaise: 0,
            },
            smallBlindAmount: 1,
            bigBlindAmount: 2,
          });
          setJoinError(null);
          setJoinTimeout(false);
        }
        if (!res.success && res.message) {
          setJoinError(res.message);
        }
      } catch (e) {
        setJoinError('응답 처리 실패');
      }
    });

    // 본인 전용: 홀카드가 포함된 테이블 상태 수신
    const unsubUser = subscribe(USER_QUEUE_TABLE_STATE, (body) => {
      try {
        const res = typeof body === 'string' ? (JSON.parse(body) as ActionResult) : (body as ActionResult);
        if (res.payload?.tableState) {
          setTableState(res.payload.tableState);
          setJoinError(null);
          setJoinTimeout(false);
        }
      } catch {
        // ignore
      }
    });

    // 구독이 서버에 반영된 뒤에 JOIN 전송 (Strict Mode 이중 마운트 대비해 지연)
    const joinTimer = setTimeout(sendJoin, 600);
    const timeoutTimer = setTimeout(() => setJoinTimeout(true), 3500);
    // 응답 없으면 한 번 더 전송 (이미 구독된 상태라 수신 가능)
    const retryTimer = setTimeout(() => {
      setTableState((prev) => {
        if (prev == null) sendJoin();
        return prev;
      });
    }, 2500);

    return () => {
      clearTimeout(joinTimer);
      clearTimeout(timeoutTimer);
      clearTimeout(retryTimer);
      // 구독 해제를 약간 지연해 Strict Mode에서 즉시 UNSUB 되는 것 방지
      setTimeout(() => {
        unsub();
        unsubUser();
      }, 150);
    };
  }, [isConnected, roomId, nickname, subscribe, send]);

  const handleLeave = () => {
    if (isConnected && roomId) {
      send(`${APP_PREFIX}/action`, {
        actionType: 'LEAVE_TABLE',
        tableId: roomId,
        playerId: nickname,
      });
    }
    onLeave();
  };

  const sendAction = (actionType: GameActionType, amount?: number) => {
    if (!isConnected || !roomId) return;
    const body: Record<string, unknown> = {
      actionType,
      tableId: roomId,
      playerId: nickname,
    };
    if (amount != null) body.amount = amount;
    send(`${APP_PREFIX}/action`, body);
  };

  const handState: HandStateSnapshot | undefined = tableState?.handState;
  const actingSeat = handState?.actingSeatIndex != null ? Number(handState.actingSeatIndex) : null;
  const mySeat = mySeatIndex != null ? Number(mySeatIndex) : null;
  const isMyTurn =
    actingSeat != null &&
    mySeat != null &&
    actingSeat === mySeat;

  // 내 시트 정보 (홀카드 표시용)
  const mySeatSnapshot = tableState?.seats?.find((s) => s.seatIndex === mySeatIndex);
  const amIFolded = mySeatSnapshot?.player?.folded ?? false;
  const myStack = Number(mySeatSnapshot?.player?.stack ?? 0);
  const myBetThisStreet = Number(mySeatSnapshot?.player?.currentBetThisStreet ?? 0);
  const currentBet = Number(handState?.currentBet ?? 0);
  const minRaise = Math.max(1, Number(handState?.minRaise ?? 1));
  const toCall = Math.max(0, currentBet - myBetThisStreet);
  const canCheck = toCall === 0;
  const canCall = toCall > 0 && myStack > 0;
  const canRaise = myStack > toCall + minRaise;

  return (
    <div className="game-room">
      <header className="room-header">
        <button type="button" className="back-btn" onClick={handleLeave} aria-label="나가기">
          ←
        </button>
        <div>
          <h1>방: {roomId}</h1>
          <p className="player-name">
            {nickname}
            {mySeatIndex != null && ` · 시트 ${mySeatIndex + 1}`}
          </p>
        </div>
      </header>

      {joinError && (
        <div className="join-error">
          {joinError}
        </div>
      )}

      <div className="table-container">
        <div className="poker-table">
          <div className="table-surface">
            {tableState ? (
              <>
                <p className="table-name">{tableState.tableName}</p>
                <div className="pot">팟: {Number(tableState.handState?.pot ?? 0)}</div>
                <div className="community-cards">
                  {(tableState.handState?.communityCards ?? []).map((code, i) => (
                    <span key={i} className="card">
                      {code}
                    </span>
                  ))}
                </div>
                <div className="phase">{tableState.handState?.phase ?? 'WAITING'}</div>
                {mySeatSnapshot?.player?.holeCards && mySeatSnapshot.player.holeCards.length > 0 && (
                  <div className="my-cards">
                    <span className="my-cards-label">내 패:</span>
                    {mySeatSnapshot.player.holeCards.map((code, i) => (
                      <span key={i} className="card my-card">
                        {code}
                      </span>
                    ))}
                  </div>
                )}
                {handState?.actingSeatIndex != null && (
                  <p className="turn-info">
                    {isMyTurn ? (
                      <strong className="my-turn">당신의 턴입니다</strong>
                    ) : (
                      <>
                        현재 턴: 시트 {handState.actingSeatIndex + 1}
                        {tableState.seats?.find((s) => s.seatIndex === handState.actingSeatIndex)?.player && (
                          <> ({tableState.seats.find((s) => s.seatIndex === handState.actingSeatIndex)?.player?.displayName})</>
                        )}
                      </>
                    )}
                  </p>
                )}
                <div className="seats">
                  {(tableState.seats ?? []).map(
                    (seat: SeatSnapshot) =>
                      seat.player && (
                        <div
                          key={seat.seatIndex}
                          className={`seat ${seat.seatIndex === mySeatIndex ? 'me' : ''} ${seat.player.folded ? 'folded' : ''}`}
                        >
                          <span className="seat-name">{seat.player.displayName}</span>
                          <span className="seat-stack">{Number(seat.player.stack)}</span>
                          {seat.player.holeCards?.length ? (
                            <span className="hole-cards">
                              {seat.player.holeCards.join(' ')}
                            </span>
                          ) : (
                            seat.player.folded && <span className="fold-label">폴드</span>
                          )}
                        </div>
                      )
                  )}
                </div>
                {handState?.phase !== 'WAITING' && (
                  <>
                    {isMyTurn && !amIFolded ? (
                      <div className="actions">
                        <button type="button" className="act-btn" onClick={() => sendAction('FOLD')}>
                          폴드
                        </button>
                        <button type="button" className="act-btn" disabled={!canCheck} onClick={() => sendAction('CHECK')}>
                          체크
                        </button>
                        <button type="button" className="act-btn" disabled={!canCall} onClick={() => sendAction('CALL')}>
                          콜 {canCall ? toCall : ''}
                        </button>
                        <button
                          type="button"
                          className="act-btn"
                          disabled={!canRaise}
                          onClick={() => sendAction('RAISE', toCall + minRaise)}
                        >
                          최소 레이즈 {toCall + minRaise}
                        </button>
                        <button type="button" className="act-btn" disabled={myStack <= 0} onClick={() => sendAction('ALL_IN')}>
                          올인
                        </button>
                      </div>
                    ) : (
                      <p className="waiting-turn">
                        {amIFolded ? '폴드하셨습니다.' : '다른 플레이어의 턴을 기다리는 중…'}
                      </p>
                    )}
                  </>
                )}
                {handState?.phase === 'WAITING' && mySeatIndex != null && (
                  <button
                    type="button"
                    className="btn start-hand"
                    onClick={() => sendAction('START_HAND')}
                  >
                    핸드 시작
                  </button>
                )}
                {tableState && (!tableState.seats || tableState.seats.length === 0) && (
                  <button type="button" className="btn start-hand" onClick={sendJoin}>
                    상태 새로고침
                  </button>
                )}
              </>
            ) : (
              <>
                <p className="table-label">홀덤 테이블</p>
                <p className="table-hint">
                  {joinTimeout ? '입장 응답이 없습니다. 서버가 켜져 있는지 확인하고 재시도해 주세요.' : '입장 중…'}
                </p>
                {joinTimeout && (
                  <button type="button" className="btn start-hand" onClick={sendJoin}>
                    입장 재시도
                  </button>
                )}
              </>
            )}
          </div>
        </div>
      </div>

      {!isConnected && (
        <div className="connection-lost">
          서버와 연결이 끊겼습니다. 로비에서 다시 연결해 주세요.
        </div>
      )}

      <style>{`
        .game-room {
          display: flex;
          flex-direction: column;
          min-height: 100%;
          padding-bottom: var(--safe-bottom);
        }
        .room-header {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 12px 16px;
          background: var(--bg-card);
          border-bottom: 1px solid var(--bg-table);
        }
        .back-btn {
          width: 44px;
          height: 44px;
          border-radius: var(--radius);
          background: var(--bg-dark);
          color: var(--text);
          font-size: 1.25rem;
        }
        .room-header h1 {
          font-size: 1.1rem;
          margin: 0 0 2px 0;
        }
        .player-name {
          font-size: 0.85rem;
          color: var(--text-muted);
          margin: 0;
        }
        .join-error {
          padding: 8px 16px;
          background: var(--danger);
          color: #fff;
          font-size: 0.9rem;
        }
        .table-container {
          flex: 1;
          overflow: auto;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 16px;
        }
        .poker-table {
          width: 100%;
          max-width: 520px;
          min-height: 320px;
          border-radius: 50%;
          background: var(--bg-table);
          border: 12px solid #0a3d5c;
          box-shadow: inset 0 0 40px rgba(0,0,0,0.3), 0 8px 24px rgba(0,0,0,0.4);
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .table-surface {
          text-align: center;
          color: rgba(255,255,255,0.95);
          padding: 16px;
        }
        .table-name, .table-label {
          font-size: 1rem;
          font-weight: 600;
          margin: 0 0 8px 0;
        }
        .table-hint {
          font-size: 0.85rem;
          opacity: 0.8;
          margin: 0;
        }
        .pot {
          font-size: 1rem;
          margin-bottom: 8px;
        }
        .community-cards {
          display: flex;
          gap: 4px;
          justify-content: center;
          flex-wrap: wrap;
          margin-bottom: 8px;
        }
        .card {
          display: inline-block;
          padding: 4px 8px;
          background: #fff;
          color: #222;
          border-radius: 4px;
          font-size: 0.9rem;
        }
        .phase {
          font-size: 0.8rem;
          opacity: 0.9;
          margin-bottom: 4px;
        }
        .turn-info {
          font-size: 0.9rem;
          margin: 0 0 12px 0;
        }
        .turn-info .my-turn {
          color: var(--success);
        }
        .seats {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;
          justify-content: center;
          margin-bottom: 12px;
        }
        .seat {
          padding: 6px 10px;
          background: var(--bg-card);
          border-radius: var(--radius);
          font-size: 0.85rem;
        }
        .seat.me {
          border: 2px solid var(--accent);
        }
        .seat.folded {
          opacity: 0.6;
        }
        .seat-name { display: block; font-weight: 600; }
        .seat-stack { font-size: 0.8rem; opacity: 0.9; }
        .hole-cards { font-size: 0.75rem; margin-left: 4px; }
        .fold-label { font-size: 0.75rem; color: var(--text-muted); }
        .actions {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;
          justify-content: center;
        }
        .act-btn {
          padding: 8px 14px;
          border-radius: var(--radius);
          background: var(--accent);
          color: var(--bg-dark);
          font-weight: 600;
          font-size: 0.85rem;
        }
        .act-btn:active { opacity: 0.9; }
        .act-btn:disabled { opacity: 0.45; cursor: not-allowed; }
        .my-cards {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          margin-bottom: 12px;
        }
        .my-cards-label {
          font-size: 0.9rem;
          font-weight: 600;
        }
        .card.my-card {
          background: linear-gradient(135deg, #fff 0%, #e2e8f0 100%);
          padding: 8px 12px;
          font-size: 1rem;
          font-weight: 600;
        }
        .waiting-turn {
          font-size: 0.9rem;
          opacity: 0.9;
          margin: 8px 0 0 0;
        }
        .btn.start-hand {
          padding: 10px 20px;
          border-radius: var(--radius);
          background: var(--success);
          color: #0f172a;
          font-weight: 600;
        }
        .connection-lost {
          padding: 12px 16px;
          background: var(--danger);
          color: #fff;
          text-align: center;
          font-size: 0.9rem;
        }
      `}</style>
    </div>
  );
}
