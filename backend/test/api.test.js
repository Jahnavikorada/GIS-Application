const test = require('node:test');
const assert = require('node:assert');
const { openDatabase, seedIfEmpty } = require('../src/database/db');
const { createApp } = require('../src/app');

let server;
let base;

test.before(async () => {
  const db = openDatabase(':memory:');
  seedIfEmpty(db);
  server = createApp(db).listen(0);
  base = `http://127.0.0.1:${server.address().port}`;
});
test.after(() => server.close());

const call = async (method, path, body, raw) => {
  const res = await fetch(base + path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: raw ?? (body === undefined ? undefined : JSON.stringify(body)),
  });
  return { status: res.status, json: await res.json() };
};
const valid = { name: 'Pole X', asset_type: 'Pole', latitude: 26.2, longitude: 91.8 };

test('GET returns the three seeded assets', async () => {
  const { status, json } = await call('GET', '/api/assets');
  assert.strictEqual(status, 200);
  assert.deepStrictEqual(json.map((a) => a.id), ['P001', 'S001', 'T001']);
});

test('POST creates an asset with a generated id and default status', async () => {
  const { status, json } = await call('POST', '/api/assets', valid);
  assert.strictEqual(status, 201);
  assert.strictEqual(json.id, 'P002');
  assert.strictEqual(json.status, 'Active');
});

test('POST rejects missing fields, bad types and out-of-range coordinates', async () => {
  const cases = [
    [{}, ['name', 'asset_type', 'latitude', 'longitude']],
    [{ ...valid, latitude: 91 }, ['latitude']],
    [{ ...valid, latitude: -90.5 }, ['latitude']],
    [{ ...valid, longitude: 181 }, ['longitude']],
    [{ ...valid, longitude: '91.8' }, ['longitude']],
    [{ ...valid, asset_type: 'Tower' }, ['asset_type']],
    [{ ...valid, status: 'Broken' }, ['status']],
    [{ ...valid, name: '   ' }, ['name']],
  ];
  for (const [body, fields] of cases) {
    const { status, json } = await call('POST', '/api/assets', body);
    assert.strictEqual(status, 400);
    assert.strictEqual(json.error.code, 'VALIDATION_ERROR');
    assert.deepStrictEqual(json.error.details.map((d) => d.field), fields);
  }
});

test('POST accepts boundary coordinates', async () => {
  const { status } = await call('POST', '/api/assets', { ...valid, latitude: 90, longitude: -180 });
  assert.strictEqual(status, 201);
});

test('POST rejects malformed JSON and duplicate ids', async () => {
  assert.strictEqual((await call('POST', '/api/assets', undefined, '{bad')).status, 400);
  const dup = await call('POST', '/api/assets', { ...valid, id: 'P001' });
  assert.strictEqual(dup.status, 409);
});

test('PATCH updates status and persists it', async () => {
  const patched = await call('PATCH', '/api/assets/T001', { status: 'Active' });
  assert.strictEqual(patched.status, 200);
  assert.strictEqual(patched.json.status, 'Active');
  const list = await call('GET', '/api/assets');
  assert.strictEqual(list.json.find((a) => a.id === 'T001').status, 'Active');
});

test('PATCH validates status and returns 404 for unknown ids', async () => {
  assert.strictEqual((await call('PATCH', '/api/assets/T001', { status: 'Nope' })).status, 400);
  assert.strictEqual((await call('PATCH', '/api/assets/T001', {})).status, 400);
  const missing = await call('PATCH', '/api/assets/ZZZ', { status: 'Active' });
  assert.strictEqual(missing.status, 404);
  assert.strictEqual(missing.json.error.code, 'NOT_FOUND');
});

test('unknown routes return a JSON 404', async () => {
  const { status, json } = await call('GET', '/api/nothing');
  assert.strictEqual(status, 404);
  assert.strictEqual(json.error.code, 'NOT_FOUND');
});
