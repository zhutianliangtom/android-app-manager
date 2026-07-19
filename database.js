/**
 * 纯 JavaScript 文件数据库模块
 * 使用 JSON 文件存储数据，无需外部数据库依赖
 */
const fs = require('fs');
const path = require('path');

const DB_DIR = path.join(__dirname, 'data');

// 确保数据目录存在
if (!fs.existsSync(DB_DIR)) {
  fs.mkdirSync(DB_DIR, { recursive: true });
}

class Database {
  constructor(name) {
    this.filePath = path.join(DB_DIR, `${name}.json`);
    this.data = {};
    this._load();
  }

  // 从文件加载数据
  _load() {
    try {
      if (fs.existsSync(this.filePath)) {
        const raw = fs.readFileSync(this.filePath, 'utf-8');
        this.data = JSON.parse(raw);
      }
    } catch (e) {
      this.data = {};
    }
  }

  // 保存数据到文件
  _save() {
    try {
      fs.writeFileSync(this.filePath, JSON.stringify(this.data, null, 2), 'utf-8');
    } catch (e) {
      console.error(`[DB] 保存失败: ${this.filePath}`, e.message);
    }
  }

  // 获取所有记录
  all() {
    return Object.values(this.data);
  }

  // 根据 ID 获取
  get(id) {
    return this.data[id] || null;
  }

  // 插入或更新
  put(id, record) {
    this.data[id] = { ...record, id, _updated: Date.now() };
    this._save();
    return this.data[id];
  }

  // 删除
  delete(id) {
    delete this.data[id];
    this._save();
  }

  // 条件查询
  find(predicate) {
    return Object.values(this.data).filter(predicate);
  }

  // 查询单条
  findOne(predicate) {
    return Object.values(this.data).find(predicate) || null;
  }

  // 计数
  count() {
    return Object.keys(this.data).length;
  }

  // 清空
  clear() {
    this.data = {};
    this._save();
  }
}

// 导出数据库实例
const usersDB = new Database('users');
const roomsDB = new Database('rooms');

module.exports = { usersDB, roomsDB, Database };