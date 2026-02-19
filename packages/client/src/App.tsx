import { useState } from 'react';
import { useStomp } from './hooks/useStomp';
import { Lobby } from './components/Lobby';
import { GameRoom } from './components/GameRoom';

type View = 'lobby' | 'room';

export default function App() {
  const { connectionState, connect, disconnect, send, subscribe, isConnected } = useStomp();
  const [view, setView] = useState<View>('lobby');
  const [roomId, setRoomId] = useState('');
  const [nickname, setNickname] = useState('');

  const handleEnterRoom = (id: string, name: string) => {
    setRoomId(id);
    setNickname(name);
    setView('room');
  };

  const handleLeaveRoom = () => {
    setView('lobby');
    setRoomId('');
    setNickname('');
  };

  if (view === 'room') {
    return (
      <GameRoom
        roomId={roomId}
        nickname={nickname}
        isConnected={isConnected}
        onLeave={handleLeaveRoom}
        send={send}
        subscribe={subscribe}
      />
    );
  }

  return (
    <Lobby
      isConnected={isConnected}
      connectionState={connectionState}
      onConnect={connect}
      onDisconnect={disconnect}
      onEnterRoom={handleEnterRoom}
      send={send}
      subscribe={subscribe}
    />
  );
}
