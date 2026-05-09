-- V2__ingestion_tables.sql
-- Tables already created in V1, just add the missing column and indexes

ALTER TABLE api_specs ADD COLUMN IF NOT EXISTS original_filename VARCHAR(255);
ALTER TABLE api_specs ADD COLUMN IF NOT EXISTS error_message TEXT;
ALTER TABLE api_specs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_specs_tenant  ON api_specs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_specs_status  ON api_specs(status);
CREATE INDEX IF NOT EXISTS idx_chunks_spec   ON api_chunks(spec_id);
CREATE INDEX IF NOT EXISTS idx_chunks_tenant ON api_chunks(tenant_id);