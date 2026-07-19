/**
 * 斗地主游戏服务器
 * 使用 Express + Socket.IO 实现局域网棋牌游戏
 */
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const { v4: uuidv4 } = require('uuid');
const path = require('path');
const { usersDB } = require('./database');
const gameLogic = require('./game-logic');

const app = express();
const server = http.createServer(app);
const io = new Server(server);

// 静态文件服务
app.use(express.static(path.join(__dirname, 'public')));

// ========== 游戏状态管理 ==========

// 在线用户: socketId -> { id, nickname, avatar, socketId, status }
const onlineUsers = new Map();

// 匹配队列: [socketId, ...]
const matchQueue = [];

// 活跃房间: roomId -> { id, players: [{socketId, cards, role}], state, currentTurn, lastPlayed, ... }
const activeRooms = new Map();

// ========== Socket.IO 事件处理 ==========

io.on('connection', (socket) => {
  console.log(`[连接] ${socket.id}`);

  // 用户注册/登录
  socket.on('login', (data, callback) => {
    const { nickname, avatar } = data;
    if (!nickname || nickname.trim().length === 0) {
      return callback({ error: '昵称不能为空' });
    }

    // 检查是否已存在该用户
    let user = usersDB.findOne(u => u.nickname === nickname.trim());
    if (user) {
      user.avatar = avatar || user.avatar;
      usersDB.put(user.id, user);
    } else {
      user = {
        id: uuidv4(),
        nickname: nickname.trim(),
        avatar: avatar || '',
        createdAt: Date.now()
      };
      usersDB.put(user.id, user);
    }

    // 如果之前已登录，先踢下线
    for (const [sid, ou] of onlineUsers) {
      if (ou.id === user.id) {
        io.to(sid).emit('kicked', { message: '您的账号在其他设备登录' });
        onlineUsers.delete(sid);
        removeFromQueue(sid);
        leaveActiveRoom(sid);
      }
    }

    onlineUsers.set(socket.id, {
      socketId: socket.id,
      id: user.id,
      nickname: user.nickname,
      avatar: user.avatar
    });

    socket.userId = user.id;
    socket.nickname = user.nickname;

    callback({ success: true, user });
    broadcastOnlineUsers();
  });

  // 开始匹配
  socket.on('start_match', () => {
    const user = onlineUsers.get(socket.id);
    if (!user) return;

    // 检查是否已在匹配中
    if (matchQueue.includes(socket.id)) {
      socket.emit('match_status', { status: 'waiting', message: '正在匹配中，请等待...' });
      return;
    }

    // 检查是否已在房间中
    if (getUserRoom(socket.id)) {
      socket.emit('match_status', { status: 'in_room', message: '您已在游戏中' });
      return;
    }

    matchQueue.push(socket.id);
    socket.emit('match_status', { status: 'waiting', message: `匹配中...当前排队人数: ${matchQueue.length}` });

    // 广播排队人数
    broadcastMatchQueueCount();

    // 尝试匹配 (满3人)
    tryMatch();
  });

  // 取消匹配
  socket.on('cancel_match', () => {
    removeFromQueue(socket.id);
    socket.emit('match_status', { status: 'idle', message: '已取消匹配' });
    broadcastMatchQueueCount();
  });

  // 叫地主
  socket.on('bid_landlord', (data) => {
    const room = getUserRoom(socket.id);
    if (!room) return;
    handleBidLandlord(room, socket.id, data.bid);
  });

  // 出牌
  socket.on('play_cards', (data) => {
    const room = getUserRoom(socket.id);
    if (!room) return;
    handlePlayCards(room, socket.id, data.cards);
  });

  // 不出
  socket.on('pass', () => {
    const room = getUserRoom(socket.id);
    if (!room) return;
    handlePass(room, socket.id);
  });

  // 断线
  socket.on('disconnect', () => {
    console.log(`[断开] ${socket.id}`);
    const user = onlineUsers.get(socket.id);
    if (user) {
      removeFromQueue(socket.id);
      handleDisconnect(socket.id);
      onlineUsers.delete(socket.id);
      broadcastOnlineUsers();
      broadcastMatchQueueCount();
    }
  });

  // 心跳
  socket.on('heartbeat', () => {
    const user = onlineUsers.get(socket.id);
    if (user) {
      user.lastHeartbeat = Date.now();
    }
  });
});

// ========== 匹配逻辑 ==========

function tryMatch() {
  if (matchQueue.length >= 3) {
    const playerIds = matchQueue.splice(0, 3);
    const roomId = uuidv4().slice(0, 8);

    const players = playerIds.map((sid, index) => ({
      socketId: sid,
      playerIndex: index,
      cards: [],
      role: null, // 'landlord' | 'farmer'
      ready: true
    }));

    const room = {
      id: roomId,
      players,
      state: 'bidding', // bidding | playing | finished
      landlordIndex: -1,
      currentTurn: 0,
      bidTurn: 0,
      bidCount: 0,
      highestBid: 0,
      highestBidder: -1,
      lastPlayed: null,
      lastPlayedBy: -1,
      passCount: 0,
      bottomCards: [],
      roundHistory: [],
      createdAt: Date.now()
    };

    activeRooms.set(roomId, room);

    // 通知所有玩家
    for (const player of players) {
      const sid = player.socketId;
      const user = onlineUsers.get(sid);
      if (user) {
        io.to(sid).emit('match_success', {
          roomId,
          playerIndex: player.playerIndex,
          players: players.map(p => {
            const u = onlineUsers.get(p.socketId);
            return {
              playerIndex: p.playerIndex,
              nickname: u ? u.nickname : '未知',
              avatar: u ? u.avatar : '',
              cardCount: 17
            };
          })
        });
      }
    }

    // 开始发牌
    startGame(room);
  }
}

function removeFromQueue(socketId) {
  const idx = matchQueue.indexOf(socketId);
  if (idx !== -1) matchQueue.splice(idx, 1);
}

function getUserRoom(socketId) {
  for (const [roomId, room] of activeRooms) {
    if (room.players.some(p => p.socketId === socketId)) {
      return room;
    }
  }
  return null;
}

// ========== 游戏流程 ==========

function startGame(room) {
  const deck = gameLogic.shuffle(gameLogic.createDeck());
  const dealt = gameLogic.dealCards(deck);

  // 分配手牌
  room.players[0].cards = dealt.player0;
  room.players[1].cards = dealt.player1;
  room.players[2].cards = dealt.player2;
  room.bottomCards = dealt.bottom;

  // 发送手牌给每个玩家
  for (const player of room.players) {
    io.to(player.socketId).emit('deal_cards', {
      cards: player.cards,
      cardCount: 17
    });
  }

  // 开始叫地主阶段，从随机玩家开始
  room.bidTurn = Math.floor(Math.random() * 3);
  room.state = 'bidding';

  sendBidRequest(room);
}

function sendBidRequest(room) {
  const player = room.players[room.bidTurn];
  io.to(player.socketId).emit('bid_request', {
    currentBid: room.highestBid,
    canBid: room.highestBid < 3
  });

  // 通知所有玩家当前叫地主的人
  broadcastToRoom(room, 'bid_turn', {
    playerIndex: room.bidTurn,
    bidCount: room.bidCount
  });
}

function handleBidLandlord(room, socketId, bid) {
  const player = room.players.find(p => p.socketId === socketId);
  if (!player) return;
  if (player.playerIndex !== room.bidTurn) return;

  room.bidCount++;

  if (bid > room.highestBid) {
    room.highestBid = bid;
    room.highestBidder = player.playerIndex;
  }

  broadcastToRoom(room, 'bid_result', {
    playerIndex: player.playerIndex,
    bid: bid,
    highestBid: room.highestBid
  });

  // 检查叫地主是否结束
  // 有人叫3分或已经轮完一圈
  if (room.highestBid === 3 || room.bidCount >= 3) {
    finishBidding(room);
    return;
  }

  // 下一人叫地主
  room.bidTurn = (room.bidTurn + 1) % 3;
  sendBidRequest(room);
}

function finishBidding(room) {
  if (room.highestBidder === -1) {
    // 没人叫地主，重新开始
    broadcastToRoom(room, 'game_message', { message: '没人叫地主，重新发牌' });
    setTimeout(() => startGame(room), 1500);
    return;
  }

  room.landlordIndex = room.highestBidder;
  room.players[room.landlordIndex].role = 'landlord';
  room.players[room.landlordIndex].cards = gameLogic.sortCards([
    ...room.players[room.landlordIndex].cards,
    ...room.bottomCards
  ]);

  // 农民
  for (let i = 0; i < 3; i++) {
    if (i !== room.landlordIndex) {
      room.players[i].role = 'farmer';
    }
  }

  // 通知所有玩家
  broadcastToRoom(room, 'landlord_decided', {
    landlordIndex: room.landlordIndex,
    bottomCards: room.bottomCards
  });

  // 发送更新后的手牌给地主
  io.to(room.players[room.landlordIndex].socketId).emit('update_cards', {
    cards: room.players[room.landlordIndex].cards
  });

  // 开始游戏，地主先出
  room.state = 'playing';
  room.currentTurn = room.landlordIndex;
  room.lastPlayed = null;
  room.lastPlayedBy = -1;
  room.passCount = 0;

  sendTurnRequest(room);
}

function sendTurnRequest(room) {
  const player = room.players[room.currentTurn];
  const canPass = room.lastPlayed !== null && room.lastPlayedBy !== player.playerIndex;
  io.to(player.socketId).emit('your_turn', {
    lastPlayed: room.lastPlayed,
    lastPlayedBy: room.lastPlayedBy,
    canPass: canPass
  });

  broadcastToRoom(room, 'turn_change', {
    currentTurn: room.currentTurn
  });
}

function handlePlayCards(room, socketId, cards) {
  const player = room.players.find(p => p.socketId === socketId);
  if (!player) return;
  if (player.playerIndex !== room.currentTurn) return;

  // 从手牌中移除出的牌
  const cardIds = new Set(cards.map(c => c.id));
  const playedCards = player.cards.filter(c => cardIds.has(c.id));
  player.cards = player.cards.filter(c => !cardIds.has(c.id));

  // 检查是否可以出牌
  if (room.lastPlayed && room.lastPlayedBy !== player.playerIndex) {
    if (!gameLogic.canBeat(playedCards, room.lastPlayed)) {
      // 不能出，退回手牌
      player.cards = gameLogic.sortCards([...player.cards, ...playedCards]);
      io.to(socketId).emit('invalid_play', { message: '出的牌打不过上家，请重新出牌' });
      return;
    }
  }

  room.lastPlayed = playedCards;
  room.lastPlayedBy = player.playerIndex;
  room.passCount = 0;
  room.roundHistory.push({
    playerIndex: player.playerIndex,
    cards: playedCards
  });

  broadcastToRoom(room, 'cards_played', {
    playerIndex: player.playerIndex,
    cards: playedCards,
    cardCount: player.cards.length
  });

  // 更新该玩家手牌
  io.to(socketId).emit('update_cards', { cards: player.cards });

  // 检查胜利
  if (gameLogic.isHandEmpty(player.cards)) {
    endGame(room, player.playerIndex);
    return;
  }

  // 下一轮
  room.currentTurn = (room.currentTurn + 1) % 3;
  sendTurnRequest(room);
}

function handlePass(room, socketId) {
  const player = room.players.find(p => p.socketId === socketId);
  if (!player) return;
  if (player.playerIndex !== room.currentTurn) return;
  if (room.lastPlayed === null) return; // 不能不出
  if (room.lastPlayedBy === player.playerIndex) return; // 自己是上家

  room.passCount++;

  broadcastToRoom(room, 'player_pass', {
    playerIndex: player.playerIndex,
    passCount: room.passCount
  });

  // 如果两个人都pass，新回合开始
  if (room.passCount >= 2) {
    room.lastPlayed = null;
    room.lastPlayedBy = -1;
    room.passCount = 0;
    room.currentTurn = room.lastPlayedBy >= 0 ? room.lastPlayedBy : (room.currentTurn + 1) % 3;
    broadcastToRoom(room, 'new_round', { message: '新回合开始' });
  } else {
    room.currentTurn = (room.currentTurn + 1) % 3;
  }

  sendTurnRequest(room);
}

function endGame(room, winnerIndex) {
  room.state = 'finished';
  const winner = room.players[winnerIndex];
  const isLandlordWin = winner.role === 'landlord';

  broadcastToRoom(room, 'game_over', {
    winnerIndex,
    winnerRole: winner.role,
    message: isLandlordWin ? '地主获胜！' : '农民获胜！',
    playerCards: room.players.map(p => ({
      playerIndex: p.playerIndex,
      cards: p.cards
    }))
  });

  // 清理房间
  setTimeout(() => {
    for (const player of room.players) {
      io.to(player.socketId).emit('return_to_lobby');
    }
    activeRooms.delete(room.id);
  }, 10000);
}

function handleDisconnect(socketId) {
  const room = getUserRoom(socketId);
  if (room) {
    const player = room.players.find(p => p.socketId === socketId);
    if (player) {
      broadcastToRoom(room, 'player_disconnect', {
        playerIndex: player.playerIndex,
        message: `${onlineUsers.get(socketId)?.nickname || '玩家'} 已断开连接`
      });

      // 结束游戏
      room.state = 'finished';
      broadcastToRoom(room, 'game_over', {
        disconnect: true,
        message: '有玩家断开连接，游戏结束'
      });

      setTimeout(() => {
        for (const p of room.players) {
          if (p.socketId !== socketId) {
            io.to(p.socketId).emit('return_to_lobby');
          }
        }
        activeRooms.delete(room.id);
      }, 5000);
    }
  }
}

function leaveActiveRoom(socketId) {
  // 已在 handleDisconnect 中处理
}

// ========== 广播工具 ==========

function broadcastToRoom(room, event, data) {
  for (const player of room.players) {
    io.to(player.socketId).emit(event, data);
  }
}

function broadcastOnlineUsers() {
  const users = Array.from(onlineUsers.values()).map(u => ({
    id: u.id,
    nickname: u.nickname,
    avatar: u.avatar,
    online: true
  }));
  io.emit('online_users', users);
}

function broadcastMatchQueueCount() {
  io.emit('match_queue_count', { count: matchQueue.length });
}

// 心跳检测 (每30秒检查一次)
setInterval(() => {
  const now = Date.now();
  for (const [sid, user] of onlineUsers) {
    if (user.lastHeartbeat && now - user.lastHeartbeat > 60000) {
      console.log(`[超时] ${sid}`);
      io.to(sid).emit('kicked', { message: '连接超时' });
      removeFromQueue(sid);
      handleDisconnect(sid);
      onlineUsers.delete(sid);
    }
  }
  broadcastOnlineUsers();
}, 30000);

// ========== 启动服务器 ==========

const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`========================================`);
  console.log(`  斗地主游戏服务器已启动`);
  console.log(`  局域网地址: http://<你的IP>:${PORT}`);
  console.log(`  本地地址: http://localhost:${PORT}`);
  console.log(`========================================`);
});