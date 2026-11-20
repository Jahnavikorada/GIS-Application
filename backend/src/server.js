const config = require('./config');
const { openDatabase, seedIfEmpty } = require('./database/db');
const { createApp } = require('./app');

const db = openDatabase();
const seeded = seedIfEmpty(db);
if (seeded) console.log(`Seeded ${seeded} initial assets`);

const server = createApp(db).listen(config.port, config.host, () => {
  console.log(`API listening on http://${config.host}:${config.port} (db: ${config.dbPath})`);
});

function shutdown() {
  server.close(() => {
    db.close();
    process.exit(0);
  });
}
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
