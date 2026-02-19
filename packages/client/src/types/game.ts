export type GamePhase = 'waiting' | 'preflop' | 'flop' | 'turn' | 'river' | 'showdown';

export interface Player {
  id: string;
  nickname: string;
  chips: number;
  isDealer?: boolean;
  isSmallBlind?: boolean;
  isBigBlind?: boolean;
  isFolded?: boolean;
  isAllIn?: boolean;
  hand?: string[];
}

export interface Room {
  id: string;
  name: string;
  players: Player[];
  maxPlayers: number;
  phase: GamePhase;
  pot: number;
  communityCards: string[];
  smallBlind: number;
  bigBlind: number;
}

export interface GreetingMessage {
  message: string;
}

/** 서버 액션 타입 (GameActionType enum과 동일) */
export type GameActionType =
  | 'JOIN_TABLE'
  | 'LEAVE_TABLE'
  | 'SIT'
  | 'SIT_OUT'
  | 'READY'
  | 'FOLD'
  | 'CHECK'
  | 'CALL'
  | 'BET'
  | 'RAISE'
  | 'ALL_IN'
  | 'START_HAND'
  | 'TIMEOUT'
  | 'CHAT';

/** 서버 → 클라이언트 액션 결과 */
export interface ActionResult {
  success: boolean;
  message?: string;
  actionType?: GameActionType;
  playerId?: string;
  tableId?: string;
  seatIndex?: number;
  amount?: number;
  payload?: {
    tableState?: TableSnapshot;
    [key: string]: unknown;
  };
}

/** 서버 테이블 스냅샷 */
export interface TableSnapshot {
  tableId: string;
  tableName: string;
  seats: SeatSnapshot[];
  handState: HandStateSnapshot;
  smallBlindAmount: number;
  bigBlindAmount: number;
}

export interface SeatSnapshot {
  seatIndex: number;
  player: PlayerSnapshot | null;
}

export interface PlayerSnapshot {
  id: string;
  displayName: string;
  stack: number;
  folded: boolean;
  allIn: boolean;
  currentBetThisStreet: number;
  holeCards?: string[] | null;
}

export interface HandStateSnapshot {
  phase: string;
  communityCards: string[];
  pot: number;
  currentBet: number;
  actingSeatIndex: number | null;
  minRaise: number;
}
