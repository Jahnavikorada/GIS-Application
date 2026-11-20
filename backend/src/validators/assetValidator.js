const ASSET_TYPES = ['Pole', 'Transformer', 'Substation'];
const STATUSES = ['Active', 'Inactive'];
const MAX_TEXT_LENGTH = 100;

const isNonEmptyString = (value) => typeof value === 'string' && value.trim().length > 0;
const isNumberInRange = (value, min, max) =>
  typeof value === 'number' && Number.isFinite(value) && value >= min && value <= max;

function validateStatus(status, errors) {
  if (!STATUSES.includes(status)) {
    errors.push({ field: 'status', message: `status must be one of: ${STATUSES.join(', ')}` });
  }
}

function validateCreate(body) {
  const errors = [];
  const input = body && typeof body === 'object' ? body : {};

  if (!isNonEmptyString(input.name)) {
    errors.push({ field: 'name', message: 'name is required' });
  } else if (input.name.trim().length > MAX_TEXT_LENGTH) {
    errors.push({ field: 'name', message: `name must be at most ${MAX_TEXT_LENGTH} characters` });
  }

  if (input.asset_type === undefined || input.asset_type === null || input.asset_type === '') {
    errors.push({ field: 'asset_type', message: 'asset_type is required' });
  } else if (!ASSET_TYPES.includes(input.asset_type)) {
    errors.push({ field: 'asset_type', message: `asset_type must be one of: ${ASSET_TYPES.join(', ')}` });
  }

  if (input.latitude === undefined || input.latitude === null) {
    errors.push({ field: 'latitude', message: 'latitude is required' });
  } else if (!isNumberInRange(input.latitude, -90, 90)) {
    errors.push({ field: 'latitude', message: 'latitude must be a number between -90 and 90' });
  }

  if (input.longitude === undefined || input.longitude === null) {
    errors.push({ field: 'longitude', message: 'longitude is required' });
  } else if (!isNumberInRange(input.longitude, -180, 180)) {
    errors.push({ field: 'longitude', message: 'longitude must be a number between -180 and 180' });
  }

  const status = input.status === undefined ? 'Active' : input.status;
  validateStatus(status, errors);

  if (input.id !== undefined && (!isNonEmptyString(input.id) || input.id.trim().length > 40)) {
    errors.push({ field: 'id', message: 'id must be a non-empty string of at most 40 characters' });
  }

  return {
    errors,
    value: errors.length
      ? null
      : {
          id: input.id === undefined ? undefined : input.id.trim(),
          name: input.name.trim(),
          asset_type: input.asset_type,
          latitude: input.latitude,
          longitude: input.longitude,
          status,
        },
  };
}

function validateStatusUpdate(body) {
  const errors = [];
  const status = body && typeof body === 'object' ? body.status : undefined;
  if (status === undefined) {
    errors.push({ field: 'status', message: 'status is required' });
  } else {
    validateStatus(status, errors);
  }
  return { errors, value: errors.length ? null : { status } };
}

module.exports = { validateCreate, validateStatusUpdate, ASSET_TYPES, STATUSES };
