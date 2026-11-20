const fs = require('fs');
const path = require('path');
const Database = require('better-sqlite3');
const config = require('../config');
const seedAssets = require('./seed-data.json');

const INSERT_SQL = `
  INSERT OR IGNORE INTO assets (id, name, asset_type, latitude, longitude, status)
  VALUES (@id, @name, @asset_type, @latitude, @longitude, @status)`;

function openDatabase(dbPath = config.dbPath) {
  if (dbPath !== ':memory:') fs.mkdirSync(path.dirname(dbPath), { recursive: true });
  const db = new Database(dbPath);
  db.pragma('journal_mode = WAL');
  db.exec(fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf8'));
  return db;
}

// Seeds only an empty table, so restarts never duplicate or overwrite edited rows.
function seedIfEmpty(db) {
  const { count } = db.prepare('SELECT COUNT(*) AS count FROM assets').get();
  if (count > 0) return 0;
  const insert = db.prepare(INSERT_SQL);
  db.transaction((rows) => rows.forEach((row) => insert.run(row)))(seedAssets);
  return seedAssets.length;
}

module.exports = { openDatabase, seedIfEmpty };
