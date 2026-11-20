require('dotenv').config();
const path = require('path');

module.exports = {
  port: Number(process.env.PORT) || 3000,
  host: process.env.HOST || '0.0.0.0',
  dbPath: path.resolve(process.env.DB_PATH || path.join(__dirname, '../../data/assets.db')),
};
