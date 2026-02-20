import { useEffect, useState } from 'react';
import { API_BASE_URL, APP_PREFIX, TOPIC_PREFIX } from '../config';

interface LobbyProps {
  isConnected: boolean;
  connectionState: string;
  onConnect: () => void;
  onDisconnect: () => void;
  onEnterRoom: (roomId: string, nickname: string) => void;
  send: (destination: string, body: object) => void;
  subscribe: (destination: string, onMessage: (body: unknown) => void) => () => void;
}

type AuthUser = {
  username: string;
  displayName: string;
  token: string;
};

export function Lobby({
  isConnected,
  connectionState,
  onConnect,
  onDisconnect,
  onEnterRoom,
  send,
  subscribe,
}: LobbyProps) {
  const [nickname, setNickname] = useState('');
  const [roomId, setRoomId] = useState('');
  const [greeting, setGreeting] = useState<string | null>(null);
  const [authUser, setAuthUser] = useState<AuthUser | null>(null);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [authMessage, setAuthMessage] = useState<string | null>(null);

  useEffect(() => {
    const raw = localStorage.getItem('holdup-auth');
    if (!raw) return;
    try {
      const parsed = JSON.parse(raw) as AuthUser;
      if (parsed?.token && parsed?.displayName) {
        setAuthUser(parsed);
        setNickname(parsed.displayName);
      }
    } catch {
      localStorage.removeItem('holdup-auth');
    }
  }, []);

  const saveAuth = (user: AuthUser) => {
    setAuthUser(user);
    setNickname(user.displayName);
    localStorage.setItem('holdup-auth', JSON.stringify(user));
  };

  const requestAuth = async (path: '/signup' | '/login') => {
    setAuthMessage(null);
    const res = await fetch(`${API_BASE_URL}/api/auth${path}`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({
        username,
        password,
        displayName: displayName || username,
      }),
    });
    const body = (await res.json()) as {
      success?: boolean;
      message?: string;
      token?: string;
      username?: string;
      displayName?: string;
    };

    if (!res.ok || !body.success || !body.token || !body.username || !body.displayName) {
      setAuthMessage(body.message ?? '인증 요청 실패');
      return;
    }
    saveAuth({ token: body.token, username: body.username, displayName: body.displayName });
    setAuthMessage(path === '/signup' ? '회원가입 및 로그인 완료' : '로그인 완료');
  };

  const handleLogout = () => {
    setAuthUser(null);
    setUsername('');
    setPassword('');
    setDisplayName('');
    localStorage.removeItem('holdup-auth');
    setAuthMessage('로그아웃되었습니다.');
  };

  const handleConnect = () => {
    onConnect();
  };

  const handleTestHello = () => {
    subscribe(`${TOPIC_PREFIX}/greetings`, (body) => {
      const msg = body as { message?: string };
      setGreeting(msg?.message ?? JSON.stringify(body));
    });
    send(`${APP_PREFIX}/hello`, { name: nickname || 'Guest' });
  };

  const handleEnterRoom = () => {
    if (!authUser) {
      setAuthMessage('먼저 회원가입/로그인을 진행해 주세요.');
      return;
    }
    const id = roomId.trim() || 'default-room';
    const name = (nickname.trim() || authUser.displayName).trim();
    onEnterRoom(id, name);
  };

  return (
    <div className="lobby">
      <header className="lobby-header">
        <h1>홀덤</h1>
        <p className="subtitle">최대 8명 · 친구와 함께</p>
      </header>

      <section className="auth-section">
        <h2>계정</h2>
        {authUser ? (
          <>
            <p className="auth-ok">{authUser.displayName} ({authUser.username}) 로그인됨</p>
            <button type="button" className="btn btn-secondary" onClick={handleLogout}>로그아웃</button>
          </>
        ) : (
          <>
            <label>
              <span>아이디</span>
              <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="아이디" />
            </label>
            <label>
              <span>비밀번호</span>
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="비밀번호" />
            </label>
            <label>
              <span>표시 닉네임</span>
              <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="테이블에서 보일 이름" />
            </label>
            <div className="auth-actions">
              <button type="button" className="btn btn-primary" onClick={() => requestAuth('/signup')}>회원가입</button>
              <button type="button" className="btn btn-secondary" onClick={() => requestAuth('/login')}>로그인</button>
            </div>
          </>
        )}
        {authMessage && <p className="auth-msg">{authMessage}</p>}
      </section>

      <section className="connection-section">
        <div className={`status status-${connectionState}`}>
          {connectionState === 'disconnected' && '연결 안 됨'}
          {connectionState === 'connecting' && '연결 중…'}
          {connectionState === 'connected' && '연결됨'}
          {connectionState === 'error' && '연결 실패'}
        </div>
        {!isConnected ? (
          <button type="button" className="btn btn-primary" onClick={handleConnect}>
            서버 연결
          </button>
        ) : (
          <button type="button" className="btn btn-secondary" onClick={onDisconnect}>
            연결 끊기
          </button>
        )}
      </section>

      {isConnected && (
        <>
          <section className="form-section">
            <label>
              <span>닉네임</span>
              <input
                type="text"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                placeholder="이름 입력"
                maxLength={20}
              />
            </label>
            <label>
              <span>방 코드 (없으면 새 방)</span>
              <input
                type="text"
                value={roomId}
                onChange={(e) => setRoomId(e.target.value)}
                placeholder="방 ID 또는 비움"
              />
            </label>
            <button type="button" className="btn btn-primary full" onClick={handleEnterRoom}>
              입장하기
            </button>
          </section>

          <section className="test-section">
            <button type="button" className="btn btn-secondary" onClick={handleTestHello}>
              서버 테스트 (Hello)
            </button>
            {greeting && <p className="greeting">{greeting}</p>}
          </section>
        </>
      )}

      <style>{`
        .lobby { max-width: 420px; margin: 0 auto; padding: 24px 16px; min-height: 100%; }
        .lobby-header { text-align: center; margin-bottom: 24px; }
        .lobby-header h1 { font-size: 1.75rem; margin: 0 0 4px 0; color: var(--text); }
        .subtitle { color: var(--text-muted); font-size: 0.9rem; margin: 0; }
        .auth-section { margin-bottom: 20px; padding: 12px; border: 1px solid var(--bg-card); border-radius: var(--radius); }
        .auth-section h2 { margin: 0 0 8px 0; font-size: 1rem; }
        .auth-actions { display: flex; gap: 8px; }
        .auth-msg { margin: 8px 0 0 0; font-size: 0.85rem; color: #fef08a; }
        .auth-ok { margin: 0 0 8px 0; font-size: 0.9rem; color: var(--success); }
        .connection-section { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; flex-wrap: wrap; }
        .status { padding: 6px 12px; border-radius: var(--radius); font-size: 0.85rem; }
        .status-disconnected { background: var(--bg-card); color: var(--text-muted); }
        .status-connecting { background: #fef3c7; color: #92400e; }
        .status-connected { background: var(--success); color: #14532d; }
        .status-error { background: var(--danger); color: #7f1d1d; }
        label { display: block; margin-bottom: 12px; }
        label span { display: block; font-size: 0.85rem; color: var(--text-muted); margin-bottom: 4px; }
        input { width: 100%; padding: 12px; border-radius: var(--radius); border: 1px solid var(--bg-card); background: var(--bg-card); color: var(--text); }
        input::placeholder { color: var(--text-muted); }
        .btn { padding: 10px 16px; border-radius: var(--radius); font-weight: 600; transition: opacity 0.2s; }
        .btn:active { opacity: 0.9; }
        .btn-primary { background: var(--accent); color: var(--bg-dark); }
        .btn-secondary { background: var(--bg-card); color: var(--text); border: 1px solid var(--accent-dim); }
        .btn.full { width: 100%; margin-top: 8px; }
        .form-section { margin-bottom: 24px; }
        .test-section { padding-top: 16px; border-top: 1px solid var(--bg-card); }
        .test-section .btn { margin-bottom: 8px; }
        .greeting { color: var(--success); font-size: 0.9rem; margin: 8px 0 0 0; }
      `}</style>
    </div>
  );
}
