/**
 * 개발: Vite 프록시로 /ws, /app, /topic → localhost:8080
 * 배포: VITE_WS_URL로 실제 서버 URL (예: https://your-server.com/ws)
 * SockJS는 ws:/wss:가 아니라 http:/https: URL만 허용합니다.
 */
const protocol = typeof location !== 'undefined' && location.protocol === 'https:' ? 'https:' : 'http:';
const host = typeof location !== 'undefined' ? location.host : 'localhost:5173';

export const WS_URL =
  import.meta.env.VITE_WS_URL ?? `${protocol}//${host}/ws`;

export const APP_PREFIX = '/app';
export const TOPIC_PREFIX = '/topic';
/** 본인 전용 (홀카드 포함 상태) - Spring이 /user/queue/... 로 라우팅 */
export const USER_QUEUE_TABLE_STATE = '/user/queue/table-state';
