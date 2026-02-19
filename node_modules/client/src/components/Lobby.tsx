import { useState } from 'react';
import { APP_PREFIX, TOPIC_PREFIX } from '../config';

interface LobbyProps {
  isConnected: boolean;
  connectionState: string;
  onConnect: () => void;
  onDisconnect: () => void;
  onEnterRoom: (roomId: string, nickname: string) => void;
  send: (destination: string, body: object) => void;
  subscribe: (destination: string, onMessage: (body: unknown) => void) => () => void;
}

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
    const id = roomId.trim() || 'default-room';
    const name = nickname.trim() || 'Player';
    onEnterRoom(id, name);
  };

  return (
    <div className="lobby">
      <header className="lobby-header">
        <h1>홀덤</h1>
        <p className="subtitle">최대 8명 · 친구와 함께</p>
      </header>

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
        .lobby {
          max-width: 400px;
          margin: 0 auto;
          padding: 24px 16px;
          min-height: 100%;
        }
        .lobby-header {
          text-align: center;
          margin-bottom: 32px;
        }
        .lobby-header h1 {
          font-size: 1.75rem;
          margin: 0 0 4px 0;
          color: var(--text);
        }
        .subtitle {
          color: var(--text-muted);
          font-size: 0.9rem;
          margin: 0;
        }
        .connection-section {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 24px;
          flex-wrap: wrap;
        }
        .status {
          padding: 6px 12px;
          border-radius: var(--radius);
          font-size: 0.85rem;
        }
        .status-disconnected { background: var(--bg-card); color: var(--text-muted); }
        .status-connecting { background: #fef3c7; color: #92400e; }
        .status-connected { background: var(--success); color: #14532d; }
        .status-error { background: var(--danger); color: #7f1d1d; }
        .form-section label {
          display: block;
          margin-bottom: 16px;
        }
        .form-section label span {
          display: block;
          font-size: 0.85rem;
          color: var(--text-muted);
          margin-bottom: 4px;
        }
        .form-section input {
          width: 100%;
          padding: 12px;
          border-radius: var(--radius);
          border: 1px solid var(--bg-card);
          background: var(--bg-card);
          color: var(--text);
        }
        .form-section input::placeholder {
          color: var(--text-muted);
        }
        .btn {
          padding: 12px 20px;
          border-radius: var(--radius);
          font-weight: 600;
          transition: opacity 0.2s;
        }
        .btn:active { opacity: 0.9; }
        .btn-primary {
          background: var(--accent);
          color: var(--bg-dark);
        }
        .btn-secondary {
          background: var(--bg-card);
          color: var(--text);
          border: 1px solid var(--accent-dim);
        }
        .btn.full { width: 100%; margin-top: 8px; }
        .form-section { margin-bottom: 24px; }
        .test-section { padding-top: 16px; border-top: 1px solid var(--bg-card); }
        .test-section .btn { margin-bottom: 8px; }
        .greeting { color: var(--success); font-size: 0.9rem; margin: 8px 0 0 0; }
      `}</style>
    </div>
  );
}
