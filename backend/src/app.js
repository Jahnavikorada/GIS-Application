const express = require('express');
const { createAssetService } = require('./services/assetService');
const { createAssetController } = require('./controllers/assetController');
const { createAssetRouter } = require('./routes/assets');
const { notFoundHandler, errorHandler } = require('./middleware/errors');

function createApp(db) {
  const app = express();
  app.use(express.json());

  app.get('/health', (req, res) => res.json({ status: 'ok' }));
  app.use('/api/assets', createAssetRouter(createAssetController(createAssetService(db))));

  app.use(notFoundHandler);
  app.use(errorHandler);
  return app;
}

module.exports = { createApp };
