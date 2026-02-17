-- Align schema with V5 seed script (path/name)
ALTER TABLE media_assets
    RENAME COLUMN url TO path,
    RENAME COLUMN alt TO name;
