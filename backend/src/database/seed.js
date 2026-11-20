const { openDatabase, seedIfEmpty } = require('./db');

const db = openDatabase();
const inserted = seedIfEmpty(db);
console.log(inserted ? `Seeded ${inserted} assets.` : 'Assets table already has data; nothing seeded.');
db.close();
