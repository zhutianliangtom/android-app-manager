/**
 * 斗地主游戏逻辑模块
 * 包含牌型判断、出牌比较、洗牌发牌等核心逻辑
 */

// 花色: ♠ ♥ ♣ ♦
const SUITS = ['spade', 'heart', 'club', 'diamond'];
const SUIT_SYMBOLS = { spade: '♠', heart: '♥', club: '♣', diamond: '♦' };
const SUIT_COLORS = { spade: 'black', heart: 'red', club: 'black', diamond: 'red' };

// 牌面值映射 (数值越大牌越大)
const RANK_VALUE = {
  '3': 3, '4': 4, '5': 5, '6': 6, '7': 7, '8': 8, '9': 9,
  '10': 10, 'J': 11, 'Q': 12, 'K': 13, 'A': 14, '2': 15,
  'small_joker': 16, 'big_joker': 17
};

const RANK_NAMES = {
  '3': '3', '4': '4', '5': '5', '6': '6', '7': '7', '8': '8', '9': '9',
  '10': '10', 'J': 'J', 'Q': 'Q', 'K': 'K', 'A': 'A', '2': '2',
  'small_joker': '小王', 'big_joker': '大王'
};

// 创建一副牌
function createDeck() {
  const deck = [];
  for (const suit of SUITS) {
    for (const rank of ['3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K', 'A', '2']) {
      deck.push({
        id: `${suit}_${rank}`,
        suit,
        rank,
        value: RANK_VALUE[rank],
        symbol: SUIT_SYMBOLS[suit],
        color: SUIT_COLORS[suit],
        name: RANK_NAMES[rank]
      });
    }
  }
  deck.push({ id: 'small_joker', suit: 'joker', rank: 'small_joker', value: 16, symbol: '🃏', color: 'black', name: '小王' });
  deck.push({ id: 'big_joker', suit: 'joker', rank: 'big_joker', value: 17, symbol: '🃏', color: 'red', name: '大王' });
  return deck;
}

// 洗牌 (Fisher-Yates)
function shuffle(deck) {
  const arr = [...deck];
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

// 排序手牌 (从大到小)
function sortCards(cards) {
  return [...cards].sort((a, b) => b.value - a.value);
}

// 发牌: 每人17张，留3张底牌
function dealCards(deck) {
  return {
    player0: sortCards(deck.slice(0, 17)),
    player1: sortCards(deck.slice(17, 34)),
    player2: sortCards(deck.slice(34, 51)),
    bottom: sortCards(deck.slice(51, 54))
  };
}

// ========== 牌型判断 ==========

// 统计每张牌的数量
function countCards(cards) {
  const count = {};
  for (const card of cards) {
    if (!count[card.value]) count[card.value] = [];
    count[card.value].push(card);
  }
  return count;
}

// 按值分组
function groupByValue(cards) {
  return countCards(cards);
}

// 判断牌型
function getCardType(cards) {
  const len = cards.length;
  const groups = groupByValue(cards);
  const values = Object.keys(groups).map(Number).sort((a, b) => a - b);
  const groupSizes = values.map(v => groups[v].length);

  if (len === 0) return null;

  // 火箭: 大王 + 小王
  if (len === 2 && cards.some(c => c.rank === 'big_joker') && cards.some(c => c.rank === 'small_joker')) {
    return { type: 'rocket', power: 1000 };
  }

  // 炸弹: 4张相同
  if (len === 4 && groupSizes.length === 1 && groupSizes[0] === 4) {
    return { type: 'bomb', power: 500 + values[0], value: values[0] };
  }

  // 单张
  if (len === 1) {
    return { type: 'single', power: values[0], value: values[0] };
  }

  // 对子
  if (len === 2 && groupSizes.length === 1 && groupSizes[0] === 2) {
    return { type: 'pair', power: values[0], value: values[0] };
  }

  // 三条
  if (len === 3 && groupSizes.length === 1 && groupSizes[0] === 3) {
    return { type: 'triple', power: values[0], value: values[0] };
  }

  // 三带一
  if (len === 4 && groupSizes.includes(3) && groupSizes.includes(1)) {
    const tripleValue = values[groupSizes.indexOf(3)];
    return { type: 'triple_one', power: tripleValue, value: tripleValue };
  }

  // 三带二
  if (len === 5 && groupSizes.includes(3) && groupSizes.includes(2)) {
    const tripleValue = values[groupSizes.indexOf(3)];
    return { type: 'triple_pair', power: tripleValue, value: tripleValue };
  }

  // 顺子 (5张及以上连续，不含2和王)
  if (len >= 5 && groupSizes.every(s => s === 1) && isConsecutive(values) && values[values.length - 1] <= 14) {
    return { type: 'straight', power: values[values.length - 1], value: values[values.length - 1], length: len };
  }

  // 连对 (3对及以上连续，不含2和王)
  if (len >= 6 && len % 2 === 0 && groupSizes.every(s => s === 2) && isConsecutive(values) && values[values.length - 1] <= 14) {
    return { type: 'pair_straight', power: values[values.length - 1], value: values[values.length - 1], length: len / 2 };
  }

  // 飞机 (连续三条，2组及以上，不含2和王)
  const tripleCount = groupSizes.filter(s => s === 3).length;
  const tripleValues = values.filter((v, i) => groupSizes[i] === 3);
  if (tripleCount >= 2 && isConsecutive(tripleValues) && tripleValues[tripleValues.length - 1] <= 14) {
    const remaining = values.filter((v, i) => groupSizes[i] !== 3);
    const remainingTotal = remaining.reduce((sum, v) => sum + groups[v].length, 0);

    // 纯飞机 (不带)
    if (remainingTotal === 0) {
      return { type: 'airplane', power: tripleValues[tripleValues.length - 1], value: tripleValues[tripleValues.length - 1], length: tripleCount };
    }
    // 飞机带单
    if (remainingTotal === tripleCount) {
      return { type: 'airplane_single', power: tripleValues[tripleValues.length - 1], value: tripleValues[tripleValues.length - 1], length: tripleCount };
    }
    // 飞机带对
    if (remainingTotal === tripleCount * 2 && remaining.every(v => groups[v].length === 2)) {
      return { type: 'airplane_pair', power: tripleValues[tripleValues.length - 1], value: tripleValues[tripleValues.length - 1], length: tripleCount };
    }
  }

  // 四带二 (两个单张或两个对子)
  if (len === 6 && groupSizes.includes(4)) {
    return { type: 'four_two', power: values[groupSizes.indexOf(4)], value: values[groupSizes.indexOf(4)] };
  }
  if (len === 8 && groupSizes.includes(4)) {
    return { type: 'four_two_pair', power: values[groupSizes.indexOf(4)], value: values[groupSizes.indexOf(4)] };
  }

  return null; // 无效牌型
}

// 判断是否连续
function isConsecutive(values) {
  for (let i = 1; i < values.length; i++) {
    if (values[i] - values[i - 1] !== 1) return false;
  }
  return true;
}

// 比较两副牌 (lastPlayed 是上家出的牌)
function canBeat(newCards, lastPlayed) {
  const newType = getCardType(newCards);
  const lastType = getCardType(lastPlayed);

  if (!newType) return false;
  if (!lastType) return true; // 自由出牌

  // 火箭可以打任何牌
  if (newType.type === 'rocket') return true;
  // 炸弹可以打非炸弹/火箭
  if (newType.type === 'bomb' && lastType.type !== 'bomb' && lastType.type !== 'rocket') return true;
  // 炸弹比较
  if (newType.type === 'bomb' && lastType.type === 'bomb') return newType.value > lastType.value;
  // 同类型比较
  if (newType.type === lastType.type && newType.length === lastType.length) {
    return newType.power > lastType.power;
  }
  return false;
}

// 获取所有可出的牌型提示
function getPlayableHints(hand, lastPlayed) {
  if (!lastPlayed || lastPlayed.length === 0) {
    return []; // 自由出牌，不提示
  }
  const lastType = getCardType(lastPlayed);
  if (!lastType) return [];

  const hints = [];
  const groups = groupByValue(hand);
  const values = Object.keys(groups).map(Number).sort((a, b) => a - b);

  if (lastType.type === 'rocket') return []; // 火箭无法被超越

  // 找火箭
  const hasBig = hand.some(c => c.rank === 'big_joker');
  const hasSmall = hand.some(c => c.rank === 'small_joker');
  if (hasBig && hasSmall) {
    hints.push(hand.filter(c => c.rank === 'big_joker' || c.rank === 'small_joker'));
  }

  // 找炸弹
  for (const v of values) {
    if (groups[v].length >= 4) {
      const bomb = groups[v].slice(0, 4);
      if (lastType.type === 'bomb') {
        if (v > lastType.value) hints.push(bomb);
      } else {
        hints.push(bomb);
      }
    }
  }

  // 同类型找更大的
  if (lastType.type === 'single') {
    for (const v of values) {
      if (v > lastType.value && groups[v].length >= 1) hints.push([groups[v][0]]);
    }
  } else if (lastType.type === 'pair') {
    for (const v of values) {
      if (v > lastType.value && groups[v].length >= 2) hints.push(groups[v].slice(0, 2));
    }
  } else if (lastType.type === 'triple') {
    for (const v of values) {
      if (v > lastType.value && groups[v].length >= 3) hints.push(groups[v].slice(0, 3));
    }
  } else if (lastType.type === 'triple_one') {
    for (const v of values) {
      if (v > lastType.value && groups[v].length >= 3) {
        const triple = groups[v].slice(0, 3);
        const remaining = hand.filter(c => c.value !== v);
        if (remaining.length >= 1) hints.push([...triple, remaining[0]]);
      }
    }
  } else if (lastType.type === 'triple_pair') {
    for (const v of values) {
      if (v > lastType.value && groups[v].length >= 3) {
        const triple = groups[v].slice(0, 3);
        const remaining = hand.filter(c => c.value !== v);
        const remainingGroups = groupByValue(remaining);
        for (const rv of Object.keys(remainingGroups).map(Number)) {
          if (remainingGroups[rv].length >= 2) {
            hints.push([...triple, ...remainingGroups[rv].slice(0, 2)]);
            break;
          }
        }
      }
    }
  } else if (lastType.type === 'straight') {
    const needLen = lastType.length;
    for (let start = lastType.value - needLen + 2; start <= 14 - needLen + 1; start++) {
      if (start + needLen - 1 <= 14) {
        const straight = [];
        let valid = true;
        for (let i = 0; i < needLen; i++) {
          const v = start + i;
          if (groups[v] && groups[v].length >= 1) {
            straight.push(groups[v][0]);
          } else {
            valid = false;
            break;
          }
        }
        if (valid && start + needLen - 1 > lastType.value) hints.push(straight);
      }
    }
  } else if (lastType.type === 'pair_straight') {
    const needLen = lastType.length;
    for (let start = lastType.value - needLen + 2; start <= 14 - needLen + 1; start++) {
      if (start + needLen - 1 <= 14) {
        const pairs = [];
        let valid = true;
        for (let i = 0; i < needLen; i++) {
          const v = start + i;
          if (groups[v] && groups[v].length >= 2) {
            pairs.push(...groups[v].slice(0, 2));
          } else {
            valid = false;
            break;
          }
        }
        if (valid && start + needLen - 1 > lastType.value) hints.push(pairs);
      }
    }
  }

  return hints;
}

// 判断手牌是否为空（胜利）
function isHandEmpty(hand) {
  return hand.length === 0;
}

module.exports = {
  createDeck,
  shuffle,
  sortCards,
  dealCards,
  getCardType,
  canBeat,
  getPlayableHints,
  isHandEmpty,
  RANK_VALUE,
  RANK_NAMES
};