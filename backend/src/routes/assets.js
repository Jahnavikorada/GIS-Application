const { Router } = require('express');

function createAssetRouter(controller) {
  const router = Router();
  router.get('/', controller.list);
  router.post('/', controller.create);
  router.patch('/:id', controller.updateStatus);
  return router;
}

module.exports = { createAssetRouter };
