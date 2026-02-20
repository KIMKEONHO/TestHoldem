import { useEffect, useMemo, useRef, useState } from 'react';
import { APP_PREFIX, TOPIC_PREFIX, USER_QUEUE_TABLE_STATE } from '../config';
import type {
  ActionResult,
  TableSnapshot,
  GameActionType,
  HandStateSnapshot,
  SeatSnapshot,
} from '../types/game';


const SUIT_SYMBOL: Record<string, string> = {
  h: '♥',
  d: '♦',
  c: '♣',
  s: '♠',
};



type ParsedCard = { rank: number; suit: string };

type HandValue = {
  category: number;
  tiebreakers: number[];
  name: string;
};

const HAND_NAME: Record<number, string> = {
  9: '로열 플러시',
  8: '스트레이트 플러시',
  7: '포카드',
  6: '풀하우스',
  5: '플러시',
  4: '스트레이트',
  3: '트리플',
  2: '투페어',
  1: '원페어',
  0: '하이카드',
};

function parseCard(code: string): ParsedCard | null {
  const c = code.trim().toLowerCase();
  if (c.length < 2) return null;
  const suit = c.slice(-1);
  const rankRaw = c.slice(0, -1).toUpperCase();
  const rank = rankRaw === 'A' ? 14 : rankRaw === 'K' ? 13 : rankRaw === 'Q' ? 12 : rankRaw === 'J' ? 11 : rankRaw === 'T' ? 10 : Number(rankRaw);
  if (!['h', 'd', 'c', 's'].includes(suit) || Number.isNaN(rank) || rank < 2 || rank > 14) return null;
  return { rank, suit };
}

function findStraightTop(ranks: number[]): number | null {
  const uniq = [...new Set(ranks)].sort((a, b) => a - b);
  if (uniq.length !== 5) return null;
  const isWheel = uniq.join(',') === '2,3,4,5,14';
  if (isWheel) return 5;
  return uniq[4] - uniq[0] === 4 ? uniq[4] : null;
}

function evaluateFiveCodes(cards: string[]): HandValue {
  const parsed = cards.map(parseCard).filter((c): c is ParsedCard => c != null);
  if (parsed.length !== 5) return { category: 0, tiebreakers: [0], name: HAND_NAME[0] };

  const ranksDesc = parsed.map((c) => c.rank).sort((a, b) => b - a);
  const isFlush = parsed.every((c) => c.suit === parsed[0].suit);
  const straightTop = findStraightTop(parsed.map((c) => c.rank));

  const countMap = new Map<number, number>();
  for (const r of parsed.map((c) => c.rank)) countMap.set(r, (countMap.get(r) ?? 0) + 1);
  const byCountThenRank = [...countMap.entries()].sort((a, b) => (b[1] - a[1]) || (b[0] - a[0]));

  if (isFlush && straightTop === 14) return { category: 9, tiebreakers: [14], name: HAND_NAME[9] };
  if (isFlush && straightTop != null) return { category: 8, tiebreakers: [straightTop], name: HAND_NAME[8] };
  if (byCountThenRank[0][1] === 4) {
    const quad = byCountThenRank[0][0];
    const kicker = byCountThenRank.find((x) => x[0] !== quad)?.[0] ?? 0;
    return { category: 7, tiebreakers: [quad, kicker], name: HAND_NAME[7] };
  }
  if (byCountThenRank[0][1] === 3 && byCountThenRank[1][1] === 2) {
    return { category: 6, tiebreakers: [byCountThenRank[0][0], byCountThenRank[1][0]], name: HAND_NAME[6] };
  }
  if (isFlush) return { category: 5, tiebreakers: ranksDesc, name: HAND_NAME[5] };
  if (straightTop != null) return { category: 4, tiebreakers: [straightTop], name: HAND_NAME[4] };
  if (byCountThenRank[0][1] === 3) {
    const trip = byCountThenRank[0][0];
    const kickers = byCountThenRank.filter((x) => x[0] !== trip).map((x) => x[0]).sort((a, b) => b - a);
    return { category: 3, tiebreakers: [trip, ...kickers], name: HAND_NAME[3] };
  }
  if (byCountThenRank[0][1] === 2 && byCountThenRank[1][1] === 2) {
    const pairs = byCountThenRank.filter((x) => x[1] === 2).map((x) => x[0]).sort((a, b) => b - a);
    const kicker = byCountThenRank.find((x) => x[1] === 1)?.[0] ?? 0;
    return { category: 2, tiebreakers: [pairs[0], pairs[1], kicker], name: HAND_NAME[2] };
  }
  if (byCountThenRank[0][1] === 2) {
    const pair = byCountThenRank[0][0];
    const kickers = byCountThenRank.filter((x) => x[1] === 1).map((x) => x[0]).sort((a, b) => b - a);
    return { category: 1, tiebreakers: [pair, ...kickers], name: HAND_NAME[1] };
  }
  return { category: 0, tiebreakers: ranksDesc, name: HAND_NAME[0] };
}

function compareHand(a: HandValue, b: HandValue): number {
  if (a.category !== b.category) return a.category - b.category;
  const n = Math.max(a.tiebreakers.length, b.tiebreakers.length);
  for (let i = 0; i < n; i += 1) {
    const av = a.tiebreakers[i] ?? 0;
    const bv = b.tiebreakers[i] ?? 0;
    if (av !== bv) return av - bv;
  }
  return 0;
}

function chooseFive(cards: string[]): string[][] {
  const out: string[][] = [];
  for (let a = 0; a < cards.length - 4; a += 1) {
    for (let b = a + 1; b < cards.length - 3; b += 1) {
      for (let c = b + 1; c < cards.length - 2; c += 1) {
        for (let d = c + 1; d < cards.length - 1; d += 1) {
          for (let e = d + 1; e < cards.length; e += 1) {
            out.push([cards[a], cards[b], cards[c], cards[d], cards[e]]);
          }
        }
      }
    }
  }
  return out;
}

function getBestHandName(holeCards: string[], communityCards: string[]): string | null {
  const all = [...holeCards, ...communityCards].filter(Boolean);
  if (all.length < 5) return null;
  let best: HandValue | null = null;
  for (const combo of chooseFive(all)) {
    const evaluated = evaluateFiveCodes(combo);
    if (!best || compareHand(evaluated, best) > 0) best = evaluated;
  }
  return best?.name ?? null;
}

function formatCard(cardCode: string): string {
  if (!cardCode) return '';
  const trimmed = cardCode.trim();
  const suitKey = trimmed.slice(-1).toLowerCase();
  const rankPart = trimmed.slice(0, -1).toUpperCase();
  const suit = SUIT_SYMBOL[suitKey];
  if (!suit || !rankPart) return trimmed;
  return `${rankPart}${suit}`;
}

function parseCardForDisplay(cardCode: string): string {
  return formatCard(cardCode);
}

function getSeatPosition(seatIndex: number, totalSeats: number) {
  const count = Math.max(totalSeats, 2);
  const angle = ((seatIndex / count) * (Math.PI * 2)) - (Math.PI / 2);
  const radiusX = 44;
  const radiusY = 38;
  const x = 50 + Math.cos(angle) * radiusX;
  const y = 50 + Math.sin(angle) * radiusY;
  return { left: `${x}%`, top: `${y}%` };
}



type SeatActionBubble = {
  text: string;
  mine: boolean;
};

type TableNotice = {
  id: number;
  text: string;
};

const ACTION_LABEL: Partial<Record<GameActionType, string>> = {
  FOLD: '폴드',
  CHECK: '체크',
  CALL: '콜',
  BET: '베팅',
  RAISE: '레이즈',
  ALL_IN: '올인',
  START_HAND: '핸드 시작',
  LEAVE_TABLE: '퇴장',
  SIT_OUT: '잠시 비움',
  READY: '준비 완료',
};

function toActionBubbleText(result: ActionResult): string | null {
  if (!result.actionType) return null;
  const label = ACTION_LABEL[result.actionType];
  if (!label) return null;
  const amount = result.amount != null ? Number(result.amount) : null;
  if (amount != null && ['CALL', 'BET', 'RAISE', 'ALL_IN'].includes(result.actionType)) {
    return `${label} ${amount}`;
  }
  return label;
}

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
  const [seatBubbles, setSeatBubbles] = useState<Record<number, SeatActionBubble>>({});
  const [notices, setNotices] = useState<TableNotice[]>([]);
  const bubbleTimersRef = useRef<Record<number, ReturnType<typeof setTimeout>>>({});

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

  const syncMySeatIndex = (snapshot: TableSnapshot) => {
    const byHoleCards = snapshot.seats.find((seat) => (seat.player?.holeCards?.length ?? 0) > 0);
    if (byHoleCards) {
      setMySeatIndex(byHoleCards.seatIndex);
      return;
    }

    const byNickname = snapshot.seats.find((seat) => seat.player?.displayName === nickname);
    if (byNickname) {
      setMySeatIndex(byNickname.seatIndex);
    }
  };



  const addNotice = (text: string) => {
    const id = Date.now() + Math.floor(Math.random() * 1000);
    setNotices((prev) => [...prev, { id, text }].slice(-4));
    setTimeout(() => {
      setNotices((prev) => prev.filter((n) => n.id !== id));
    }, 2500);
  };

  const showActionBubble = (result: ActionResult) => {
    const text = toActionBubbleText(result);
    if (!text) return;

    const seatIndex = result.seatIndex != null
      ? Number(result.seatIndex)
      : (result.playerId != null
        ? (tableState?.seats?.find((seat) => seat.player?.id === result.playerId)?.seatIndex ?? null)
        : null);

    if (seatIndex == null || Number.isNaN(seatIndex)) return;

    const mine = result.playerId != null && result.playerId === mySeatSnapshot?.player?.id;
    if (mine) return;

    setSeatBubbles((prev) => ({
      ...prev,
      [seatIndex]: { text, mine },
    }));

    if (bubbleTimersRef.current[seatIndex]) {
      clearTimeout(bubbleTimersRef.current[seatIndex]);
    }
    bubbleTimersRef.current[seatIndex] = setTimeout(() => {
      setSeatBubbles((prev) => {
        const next = { ...prev };
        delete next[seatIndex];
        return next;
      });
      delete bubbleTimersRef.current[seatIndex];
    }, 2000);
  };
  useEffect(() => {
    if (!isConnected || !roomId || !nickname) return;

    setTableState(null);
    setMySeatIndex(null);

    const unsub = subscribe(`${TOPIC_PREFIX}/table/${roomId}`, (body) => {
      try {
        const res = typeof body === 'string' ? (JSON.parse(body) as ActionResult) : (body as ActionResult);
        showActionBubble(res);
        if (res.actionType === 'LEAVE_TABLE' && res.success) {
          const leavingName = res.seatIndex != null
            ? tableState?.seats?.find((seat) => seat.seatIndex === res.seatIndex)?.player?.displayName
            : null;
          addNotice(res.message ?? `${leavingName ?? '플레이어'} 님이 나갔습니다.`);
        }
        if (res.payload?.tableState) {
          setTableState(res.payload.tableState);
          syncMySeatIndex(res.payload.tableState);
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
        showActionBubble(res);
        if (res.payload?.tableState) {
          setTableState(res.payload.tableState);
          syncMySeatIndex(res.payload.tableState);
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
      Object.values(bubbleTimersRef.current).forEach((timer) => clearTimeout(timer));
      bubbleTimersRef.current = {};
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
  const mySeatSnapshot = tableState?.seats?.find((s) => s.seatIndex === mySeatIndex)
    ?? tableState?.seats?.find((s) => s.player?.displayName === nickname);
  const bestHandName = useMemo(() => {
    const holeCards = mySeatSnapshot?.player?.holeCards ?? [];
    const communityCards = tableState?.handState?.communityCards ?? [];
    return getBestHandName(holeCards, communityCards);
  }, [mySeatSnapshot?.player?.holeCards, tableState?.handState?.communityCards]);
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

      {notices.length > 0 && (
        <div className="table-notices">
          {notices.map((notice) => (
            <p key={notice.id} className="table-notice">{notice.text}</p>
          ))}
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
                      {parseCardForDisplay(code)}
                    </span>
                  ))}
                </div>
                <div className="phase">{tableState.handState?.phase ?? 'WAITING'}</div>
                {mySeatSnapshot?.player?.holeCards && mySeatSnapshot.player.holeCards.length > 0 && (
                  <div className="my-cards">
                    <span className="my-cards-label">내 패:</span>
                    {mySeatSnapshot.player.holeCards.map((code, i) => (
                      <span key={i} className="card my-card">
                        {parseCardForDisplay(code)}
                      </span>
                    ))}
                  </div>
                )}
                {bestHandName && (
                  <p className="best-hand">현재 최고 패: <strong>{bestHandName}</strong></p>
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
                          style={getSeatPosition(seat.seatIndex, tableState.seats.length)}
                        >
                          <span className="seat-name">시트 {seat.seatIndex + 1} · {seat.player.displayName}</span>
                          <span className="seat-stack">{Number(seat.player.stack)}</span>
                          {seatBubbles[seat.seatIndex] && (
                            <span className={`action-bubble ${seatBubbles[seat.seatIndex].mine ? 'mine' : ''}`}>
                              {seatBubbles[seat.seatIndex].text}
                            </span>
                          )}
                          {seat.seatIndex === mySeatIndex && seat.player.holeCards?.length ? (
                            <span className="hole-cards">
                              {seat.player.holeCards.map((code) => parseCardForDisplay(code)).join(' ')}
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
        .table-notices {
          padding: 8px 16px 0;
          display: flex;
          flex-direction: column;
          gap: 6px;
        }
        .table-notice {
          margin: 0;
          padding: 6px 10px;
          border-radius: var(--radius);
          background: rgba(15, 23, 42, 0.9);
          color: #f8fafc;
          font-size: 0.82rem;
          border: 1px solid rgba(255, 255, 255, 0.2);
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
          position: relative;
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
          position: relative;
          z-index: 2;
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
          position: absolute;
          inset: 0;
          z-index: 3;
        }
        .seat {
          position: absolute;
          transform: translate(-50%, -50%);
          min-width: 108px;
          padding: 6px 10px;
          background: rgba(15, 23, 42, 0.88);
          border-radius: var(--radius);
          font-size: 0.82rem;
          border: 1px solid rgba(255, 255, 255, 0.16);
        }
        .seat.me {
          border: 2px solid var(--accent);
        }
        .seat.folded {
          opacity: 0.6;
        }
        .seat-name { display: block; font-weight: 600; }
        .seat-stack { font-size: 0.8rem; opacity: 0.9; }
        .hole-cards { display: block; font-size: 0.75rem; margin-top: 2px; }
        .fold-label { font-size: 0.75rem; color: var(--text-muted); }
        .action-bubble {
          display: inline-block;
          margin-top: 4px;
          padding: 2px 8px;
          border-radius: 999px;
          background: rgba(15, 23, 42, 0.9);
          color: #f8fafc;
          font-size: 0.74rem;
          border: 1px solid rgba(255, 255, 255, 0.25);
          animation: pop-bubble 0.16s ease-out;
        }
        .action-bubble.mine {
          background: rgba(30, 64, 175, 0.9);
          border-color: rgba(147, 197, 253, 0.7);
        }
        @keyframes pop-bubble {
          from { transform: translateY(4px); opacity: 0; }
          to { transform: translateY(0); opacity: 1; }
        }
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
        .best-hand {
          margin: 0 0 10px 0;
          font-size: 0.9rem;
          color: #fde68a;
        }
        .best-hand strong {
          color: #fef3c7;
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
