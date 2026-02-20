import { useCallback, useEffect, useMemo, useState } from 'react';
import { API_BASE_URL, APP_PREFIX, TOPIC_PREFIX } from '../config';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
function isValidEmailFormat(value: string): boolean {
  return EMAIL_REGEX.test((value || '').trim());
}
function getPasswordError(value: string): string | null {
  if (!value || value.length < 8) return '비밀번호는 최소 8자 이상이어야 합니다.';
  if (!/[a-z]/.test(value)) return '비밀번호에 영어 소문자를 포함해 주세요.';
  if (!/[0-9]/.test(value)) return '비밀번호에 숫자를 포함해 주세요.';
  return null;
}

interface LobbyProps {
  isConnected: boolean;
  connectionState: string;
  onConnect: () => void;
  onDisconnect: () => void;
  onEnterRoom: (roomId: string, nickname: string) => void;
  send: (destination: string, body: object) => void;
  subscribe: (destination: string, onMessage: (body: unknown) => void) => () => void;
}

type AuthMode = 'login' | 'signup';

type AuthUser = {
  username: string;
  displayName: string;
  email?: string;
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
  const [mode, setMode] = useState<AuthMode>('login');
  const [nickname, setNickname] = useState('');
  const [roomId, setRoomId] = useState('');
  const [greeting, setGreeting] = useState<string | null>(null);
  const [authUser, setAuthUser] = useState<AuthUser | null>(null);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [authMessage, setAuthMessage] = useState<string | null>(null);
  const [emailCheckStatus, setEmailCheckStatus] = useState<'idle' | 'checking' | 'taken' | 'available'>('idle');

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

  const signupErrors = useMemo(() => {
    if (mode !== 'signup') return { username: null, password: null, email: null, displayName: null };
    const u = (username || '').trim();
    const e = (email || '').trim();
    const d = (displayName || '').trim();
    return {
      username: !u ? '아이디를 입력해 주세요.' : null,
      password: getPasswordError(password),
      email: !e ? '이메일을 입력해 주세요.' : !isValidEmailFormat(e) ? '올바른 이메일 형식이 아닙니다.' : emailCheckStatus === 'taken' ? '이미 사용 중인 이메일입니다.' : null,
      displayName: !d ? '표시 닉네임을 입력해 주세요.' : null,
    };
  }, [mode, username, password, email, displayName, emailCheckStatus]);

  const checkEmailAvailable = useCallback(async (emailVal: string) => {
    const trimmed = (emailVal || '').trim().toLowerCase();
    if (!trimmed || !isValidEmailFormat(trimmed)) {
      setEmailCheckStatus('idle');
      return;
    }
    setEmailCheckStatus('checking');
    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/check-email?email=${encodeURIComponent(trimmed)}`);
      const data = (await res.json()) as { taken?: boolean };
      setEmailCheckStatus(data.taken ? 'taken' : 'available');
    } catch {
      setEmailCheckStatus('idle');
    }
  }, []);

  useEffect(() => {
    if (mode !== 'signup') {
      setEmailCheckStatus('idle');
      return;
    }
    const trimmed = (email || '').trim();
    if (!trimmed || !isValidEmailFormat(trimmed)) {
      setEmailCheckStatus('idle');
      return;
    }
    const t = setTimeout(() => checkEmailAvailable(email), 400);
    return () => clearTimeout(t);
  }, [mode, email, checkEmailAvailable]);

  const saveAuth = (user: AuthUser) => {
    setAuthUser(user);
    setNickname(user.displayName);
    localStorage.setItem('holdup-auth', JSON.stringify(user));
  };

  const requestLogin = async () => {
    setAuthMessage(null);
    const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    const body = (await res.json()) as {
      success?: boolean;
      message?: string;
      token?: string;
      username?: string;
      displayName?: string;
      email?: string;
    };

    if (!res.ok || !body.success || !body.token || !body.username || !body.displayName) {
      setAuthMessage(body.message ?? '로그인 실패');
      return;
    }
    saveAuth({ token: body.token, username: body.username, displayName: body.displayName, email: body.email });
    setAuthMessage('로그인 완료');
  };

  const requestSignup = async () => {
    setAuthMessage(null);
    if (signupErrors.username || signupErrors.password || signupErrors.email || signupErrors.displayName) return;
    const res = await fetch(`${API_BASE_URL}/api/auth/signup`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({
        username,
        password,
        displayName: (displayName || username).trim(),
        email,
      }),
    });
    const body = (await res.json()) as {
      success?: boolean;
      message?: string;
      token?: string;
      username?: string;
      displayName?: string;
      email?: string;
    };

    if (!res.ok || !body.success || !body.token || !body.username || !body.displayName) {
      setAuthMessage(body.message ?? '회원가입 실패');
      return;
    }

    saveAuth({ token: body.token, username: body.username, displayName: body.displayName, email: body.email });
    setAuthMessage('회원가입 및 로그인 완료');
    setMode('login');
  };

  const requestFindId = async () => {
    setAuthMessage(null);
    if (!email.trim()) {
      setAuthMessage('ID 찾기를 위해 이메일을 입력해 주세요.');
      return;
    }
    const res = await fetch(`${API_BASE_URL}/api/auth/find-id`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ email }),
    });
    const body = (await res.json()) as { success?: boolean; message?: string };
    setAuthMessage(body.message ?? (res.ok ? 'ID 조회 완료' : 'ID 조회 실패'));
  };

  const requestResetPassword = async () => {
    setAuthMessage(null);
    if (!username.trim() || !email.trim() || !password.trim()) {
      setAuthMessage('비밀번호 재설정을 위해 아이디/이메일/새 비밀번호를 입력해 주세요.');
      return;
    }
    const res = await fetch(`${API_BASE_URL}/api/auth/reset-password`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({
        username,
        email,
        newPassword: password,
      }),
    });
    const body = (await res.json()) as { success?: boolean; message?: string };
    setAuthMessage(body.message ?? (res.ok ? '비밀번호 재설정 완료' : '비밀번호 재설정 실패'));
  };

  const handleLogout = () => {
    setAuthUser(null);
    setUsername('');
    setPassword('');
    setDisplayName('');
    setEmail('');
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

  const generateRandomRoomCode = () => {
    return String(100000 + Math.floor(Math.random() * 900000));
  };

  const handleEnterRoom = () => {
    if (!authUser) {
      setAuthMessage('먼저 로그인해 주세요.');
      return;
    }
    const id = roomId.trim() || generateRandomRoomCode();
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
        <h2>{mode === 'login' ? '로그인' : '회원가입'}</h2>

        {authUser ? (
          <>
            <p className="auth-ok">{authUser.displayName} ({authUser.username}) 로그인됨</p>
            <button type="button" className="btn btn-secondary" onClick={handleLogout}>로그아웃</button>
          </>
        ) : (
          <>
            <label>
              <span>아이디</span>
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="아이디"
                className={mode === 'signup' && signupErrors.username ? 'input-error' : ''}
              />
              {mode === 'signup' && signupErrors.username && <span className="field-error">{signupErrors.username}</span>}
            </label>
            <label>
              <span>비밀번호</span>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="비밀번호"
                className={mode === 'signup' && signupErrors.password ? 'input-error' : ''}
              />
              {mode === 'signup' && signupErrors.password && <span className="field-error">{signupErrors.password}</span>}
            </label>
            {mode === 'signup' && (
              <label>
                <span>이메일</span>
                <input
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="email@example.com"
                  className={signupErrors.email ? 'input-error' : ''}
                />
                {signupErrors.email && <span className="field-error">{signupErrors.email}</span>}
                {emailCheckStatus === 'checking' && <span className="field-hint">이메일 확인 중…</span>}
                {emailCheckStatus === 'available' && !signupErrors.email && <span className="field-ok">사용 가능한 이메일입니다.</span>}
              </label>
            )}
            {mode === 'signup' && (
              <label>
                <span>표시 닉네임</span>
                <input
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="테이블 닉네임"
                  className={signupErrors.displayName ? 'input-error' : ''}
                />
                {signupErrors.displayName && <span className="field-error">{signupErrors.displayName}</span>}
              </label>
            )}

            <div className="auth-actions">
              {mode === 'login' ? (
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={requestLogin}
                  disabled={!username.trim() || !password}
                >
                  로그인
                </button>
              ) : (
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={requestSignup}
                  disabled={!!(signupErrors.username || signupErrors.password || signupErrors.email || signupErrors.displayName)}
                >
                  회원가입 완료
                </button>
              )}
              <button type="button" className="btn btn-secondary" onClick={() => setMode(mode === 'login' ? 'signup' : 'login')}>
                {mode === 'login' ? '회원가입 페이지로 이동' : '로그인 페이지로 이동'}
              </button>
            </div>

            {mode === 'login' && (
              <div className="helper-actions">
                <button type="button" className="link-btn" onClick={requestFindId}>ID 찾기</button>
                <button type="button" className="link-btn" onClick={requestResetPassword}>PW 찾기/재설정</button>
              </div>
            )}
          </>
        )}

        {authMessage && <p className="auth-msg">{authMessage}</p>}
      </section>

      {authUser && (
        <>
          <section className="connection-section">
            <div className={`status status-${connectionState}`}>
              {connectionState === 'disconnected' && '연결 안 됨'}
              {connectionState === 'connecting' && '연결 중…'}
              {connectionState === 'connected' && '연결됨'}
              {connectionState === 'error' && '연결 실패'}
            </div>
            {!isConnected ? (
              <button type="button" className="btn btn-primary" onClick={handleConnect}>서버 연결</button>
            ) : (
              <button type="button" className="btn btn-secondary" onClick={onDisconnect}>연결 끊기</button>
            )}
          </section>

          {isConnected && (
            <>
              <section className="form-section">
            <label>
              <span>닉네임</span>
              <input type="text" value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="이름 입력" maxLength={20} />
            </label>
            <label>
              <span>방 코드 (없으면 새 방)</span>
              <input
                type="text"
                inputMode="numeric"
                autoComplete="off"
                value={roomId}
                onChange={(e) => setRoomId(e.target.value.replace(/\D/g, ''))}
                placeholder="숫자만 입력 또는 비워두기"
                maxLength={12}
              />
            </label>
            <button type="button" className="btn btn-primary full" onClick={handleEnterRoom}>입장하기</button>
          </section>

              <section className="test-section">
                <button type="button" className="btn btn-secondary" onClick={handleTestHello}>서버 테스트 (Hello)</button>
                {greeting && <p className="greeting">{greeting}</p>}
              </section>
            </>
          )}
        </>
      )}

      <style>{`
        .lobby { max-width: 440px; margin: 0 auto; padding: 24px 16px; min-height: 100%; }
        .lobby-header { text-align: center; margin-bottom: 24px; }
        .lobby-header h1 { font-size: 1.75rem; margin: 0 0 4px 0; color: var(--text); }
        .subtitle { color: var(--text-muted); font-size: 0.9rem; margin: 0; }
        .auth-section { margin-bottom: 20px; padding: 12px; border: 1px solid var(--bg-card); border-radius: var(--radius); }
        .auth-section h2 { margin: 0 0 8px 0; font-size: 1rem; }
        .auth-actions { display: flex; gap: 8px; flex-wrap: wrap; }
        .helper-actions { margin-top: 8px; display: flex; gap: 10px; }
        .link-btn { background: transparent; color: #93c5fd; text-decoration: underline; font-size: 0.85rem; }
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
        input.input-error { border-color: var(--danger); box-shadow: 0 0 0 1px var(--danger); }
        input::placeholder { color: var(--text-muted); }
        .field-error { display: block; font-size: 0.8rem; color: #fca5a5; margin-top: 4px; }
        .field-hint { display: block; font-size: 0.8rem; color: var(--text-muted); margin-top: 4px; }
        .field-ok { display: block; font-size: 0.8rem; color: var(--success); margin-top: 4px; }
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
