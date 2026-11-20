const { ApiError, validationError } = require('../middleware/errors');
const { validateCreate, validateStatusUpdate } = require('../validators/assetValidator');

function createAssetController(service) {
  return {
    list(req, res) {
      res.status(200).json(service.list());
    },

    create(req, res) {
      const { errors, value } = validateCreate(req.body);
      if (errors.length) throw validationError(errors);

      const asset = service.create(value);
      if (!asset) throw new ApiError(409, 'DUPLICATE_ID', `Asset ${value.id} already exists`);
      res.status(201).json(asset);
    },

    updateStatus(req, res) {
      const { errors, value } = validateStatusUpdate(req.body);
      if (errors.length) throw validationError(errors);

      const asset = service.setStatus(req.params.id, value.status);
      if (!asset) throw new ApiError(404, 'NOT_FOUND', `Asset ${req.params.id} not found`);
      res.status(200).json(asset);
    },
  };
}

module.exports = { createAssetController };
