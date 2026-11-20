const PREFIXES = { Pole: 'P', Transformer: 'T', Substation: 'S' };

function createAssetService(db) {
  const selectAll = db.prepare('SELECT * FROM assets ORDER BY id');
  const selectById = db.prepare('SELECT * FROM assets WHERE id = ?');
  const insert = db.prepare(`
    INSERT INTO assets (id, name, asset_type, latitude, longitude, status)
    VALUES (@id, @name, @asset_type, @latitude, @longitude, @status)`);
  const updateStatus = db.prepare('UPDATE assets SET status = ? WHERE id = ?');
  const selectIdsWithPrefix = db.prepare('SELECT id FROM assets WHERE id LIKE ?');

  function nextId(assetType) {
    const prefix = PREFIXES[assetType];
    const used = selectIdsWithPrefix
      .all(`${prefix}%`)
      .map((row) => Number(row.id.slice(1)))
      .filter(Number.isInteger);
    return `${prefix}${String(Math.max(0, ...used) + 1).padStart(3, '0')}`;
  }

  return {
    list: () => selectAll.all(),

    get: (id) => selectById.get(id),

    // Returns null when the requested id is already taken.
    create(input) {
      return db.transaction(() => {
        const id = input.id ?? nextId(input.asset_type);
        if (selectById.get(id)) return null;
        insert.run({ ...input, id });
        return selectById.get(id);
      })();
    },

    setStatus(id, status) {
      const { changes } = updateStatus.run(status, id);
      return changes ? selectById.get(id) : null;
    },
  };
}

module.exports = { createAssetService };
