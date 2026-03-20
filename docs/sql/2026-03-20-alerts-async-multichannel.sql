BEGIN;

SET search_path TO public;

LOCK TABLE user_sport_follows IN ACCESS EXCLUSIVE MODE;

DELETE FROM user_sport_follows
WHERE follow_type = 'venue'
   OR venue_id IS NOT NULL;

DROP INDEX IF EXISTS user_sport_follows_venue_uniq;

ALTER TABLE user_sport_follows
  DROP CONSTRAINT IF EXISTS user_sport_follows_follow_type_check,
  DROP CONSTRAINT IF EXISTS user_sport_follows_exactly_one_target;

ALTER TABLE user_sport_follows
  DROP COLUMN IF EXISTS venue_id;

ALTER TABLE user_sport_follows
  ADD CONSTRAINT user_sport_follows_follow_type_check
    CHECK (follow_type IN ('competition', 'participant')),
  ADD CONSTRAINT user_sport_follows_exactly_one_target
    CHECK (
      (competition_id IS NOT NULL)::int +
      (participant_id IS NOT NULL)::int = 1
    );

LOCK TABLE user_event_alerts IN ACCESS EXCLUSIVE MODE;

ALTER TABLE user_event_alerts
  DROP CONSTRAINT IF EXISTS user_event_alerts_channel_check,
  DROP CONSTRAINT IF EXISTS user_event_alerts_status_check;

ALTER TABLE user_event_alerts
  ADD CONSTRAINT user_event_alerts_channel_check
    CHECK (channel IN ('telegram', 'discord', 'email')),
  ADD CONSTRAINT user_event_alerts_status_check
    CHECK (
      status IN (
        'scheduled',
        'waiting_artifact',
        'queued',
        'processing',
        'sent',
        'failed_retryable',
        'failed_permanent',
        'cancelled'
      )
    );

ALTER TABLE user_event_alerts
  ADD COLUMN IF NOT EXISTS idempotency_key text,
  ADD COLUMN IF NOT EXISTS attempts int NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS max_attempts int NOT NULL DEFAULT 6,
  ADD COLUMN IF NOT EXISTS next_retry_at_utc timestamptz NULL,
  ADD COLUMN IF NOT EXISTS queued_at_utc timestamptz NULL,
  ADD COLUMN IF NOT EXISTS processing_started_at_utc timestamptz NULL,
  ADD COLUMN IF NOT EXISTS dispatched_at_utc timestamptz NULL,
  ADD COLUMN IF NOT EXISTS sent_at_utc timestamptz NULL,
  ADD COLUMN IF NOT EXISTS stream_name text NULL,
  ADD COLUMN IF NOT EXISTS stream_message_id text NULL,
  ADD COLUMN IF NOT EXISTS provider_message_id text NULL,
  ADD COLUMN IF NOT EXISTS worker_id text NULL,
  ADD COLUMN IF NOT EXISTS last_error text NULL,
  ADD COLUMN IF NOT EXISTS last_error_code text NULL,
  ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now(),
  ADD COLUMN IF NOT EXISTS artifact_required boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS artifact_id bigint NULL;

UPDATE user_event_alerts
SET idempotency_key = user_id::text || ':' || event_id::text || ':' || channel || ':' || lead_time_minutes::text
WHERE idempotency_key IS NULL;

DELETE FROM user_event_alerts a
USING user_event_alerts b
WHERE a.id > b.id
  AND a.user_id = b.user_id
  AND a.event_id = b.event_id
  AND a.channel = b.channel
  AND a.lead_time_minutes = b.lead_time_minutes;

ALTER TABLE user_event_alerts
  ALTER COLUMN idempotency_key SET NOT NULL;

DROP INDEX IF EXISTS user_event_alerts_due_idx;

CREATE UNIQUE INDEX IF NOT EXISTS user_event_alerts_idempotency_uniq
  ON user_event_alerts(idempotency_key);

CREATE INDEX IF NOT EXISTS user_event_alerts_due_claim_idx
  ON user_event_alerts (status, COALESCE(next_retry_at_utc, send_at_utc), id)
  WHERE status IN ('scheduled', 'failed_retryable');

CREATE INDEX IF NOT EXISTS user_event_alerts_waiting_artifact_idx
  ON user_event_alerts (channel, status, send_at_utc, id)
  WHERE status = 'waiting_artifact';

CREATE INDEX IF NOT EXISTS user_event_alerts_channel_status_idx
  ON user_event_alerts (channel, status, send_at_utc);

DROP TRIGGER IF EXISTS trg_user_event_alerts_updated_at ON user_event_alerts;
CREATE TRIGGER trg_user_event_alerts_updated_at
BEFORE UPDATE ON user_event_alerts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS alert_artifacts (
  id bigserial PRIMARY KEY,
  event_id bigint NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  artifact_type text NOT NULL CHECK (artifact_type IN ('image')),
  storage_provider text NOT NULL CHECK (storage_provider IN ('cloudinary')),
  storage_key text NULL,
  asset_url text NOT NULL,
  render_context_hash text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NULL,
  UNIQUE (event_id, artifact_type, render_context_hash)
);

CREATE INDEX IF NOT EXISTS alert_artifacts_event_idx
  ON alert_artifacts(event_id, artifact_type, created_at DESC);

ALTER TABLE user_event_alerts
  DROP CONSTRAINT IF EXISTS user_event_alerts_artifact_fk;

ALTER TABLE user_event_alerts
  ADD CONSTRAINT user_event_alerts_artifact_fk
    FOREIGN KEY (artifact_id) REFERENCES alert_artifacts(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS user_sport_notification_channels (
  user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  sport_id bigint NOT NULL REFERENCES sports(id) ON DELETE CASCADE,
  channel text NOT NULL CHECK (channel IN ('telegram', 'discord', 'email')),
  enabled boolean NOT NULL DEFAULT true,
  default_lead_time_minutes int NOT NULL DEFAULT 30 CHECK (default_lead_time_minutes > 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, sport_id, channel)
);

CREATE INDEX IF NOT EXISTS usnc_lookup_idx
  ON user_sport_notification_channels(user_id, sport_id, enabled, channel);

CREATE TABLE IF NOT EXISTS user_follow_notification_channels (
  follow_id bigint NOT NULL REFERENCES user_sport_follows(id) ON DELETE CASCADE,
  channel text NOT NULL CHECK (channel IN ('telegram', 'discord', 'email')),
  enabled boolean NOT NULL DEFAULT true,
  override_lead_time_minutes int NULL CHECK (override_lead_time_minutes > 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (follow_id, channel)
);

CREATE INDEX IF NOT EXISTS ufnc_follow_idx
  ON user_follow_notification_channels(follow_id, enabled, channel);

ALTER TABLE user_sport_settings
  DROP COLUMN IF EXISTS default_channel,
  DROP COLUMN IF EXISTS default_lead_time_minutes;

ALTER TABLE user_sport_follows
  DROP COLUMN IF EXISTS override_channel,
  DROP COLUMN IF EXISTS override_lead_time_minutes;

CREATE TABLE IF NOT EXISTS alert_delivery_attempts (
  id bigserial PRIMARY KEY,
  alert_id bigint NOT NULL REFERENCES user_event_alerts(id) ON DELETE CASCADE,
  attempt_no int NOT NULL CHECK (attempt_no > 0),
  channel text NOT NULL CHECK (channel IN ('telegram', 'discord', 'email')),
  worker_id text NULL,
  stream_name text NULL,
  stream_message_id text NULL,
  started_at timestamptz NOT NULL DEFAULT now(),
  finished_at timestamptz NULL,
  outcome text NOT NULL CHECK (outcome IN ('success', 'retryable_failure', 'permanent_failure')),
  error_code text NULL,
  error_message text NULL,
  http_status int NULL,
  provider_message_id text NULL,
  latency_ms int NULL CHECK (latency_ms IS NULL OR latency_ms >= 0)
);

CREATE INDEX IF NOT EXISTS alert_delivery_attempts_alert_idx
  ON alert_delivery_attempts(alert_id, attempt_no DESC);

CREATE INDEX IF NOT EXISTS alert_delivery_attempts_outcome_idx
  ON alert_delivery_attempts(outcome, started_at DESC);

COMMIT;
