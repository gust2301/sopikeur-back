-- V31 : Champs provider-agnostiques pour le support multi-provider (Wave, Orange Money)
-- Les colonnes Stripe existantes sont conservées pour backward-compat.

-- payment_intents : ajout colonnes provider + identifiants provider-agnostiques
ALTER TABLE payment_intents
  ADD COLUMN provider              VARCHAR(32)  NOT NULL DEFAULT 'STRIPE' AFTER id,
  ADD COLUMN provider_checkout_id  VARCHAR(255) NULL AFTER stripe_payment_intent_id,
  ADD COLUMN provider_payment_ref  VARCHAR(255) NULL AFTER provider_checkout_id;

-- stripe_session_id devient nullable (Wave/OM n'ont pas de stripe session id)
ALTER TABLE payment_intents
  MODIFY COLUMN stripe_session_id VARCHAR(255) NULL;

-- Backfill : pour les payment_intents Stripe existants
UPDATE payment_intents
  SET provider_checkout_id = stripe_session_id
  WHERE provider = 'STRIPE' AND stripe_session_id IS NOT NULL;

-- payment_events : ajout provider + provider_event_id pour idempotency générique
ALTER TABLE payment_events
  ADD COLUMN provider          VARCHAR(32)  NOT NULL DEFAULT 'STRIPE' AFTER id,
  ADD COLUMN provider_event_id VARCHAR(500) NULL AFTER stripe_event_id;

-- stripe_event_id devient nullable (Wave/OM n'ont pas de stripe event id)
ALTER TABLE payment_events
  MODIFY COLUMN stripe_event_id VARCHAR(255) NULL;

-- Backfill : pour les payment_events Stripe existants
UPDATE payment_events
  SET provider_event_id = stripe_event_id
  WHERE provider = 'STRIPE' AND stripe_event_id IS NOT NULL;

-- Index d'idempotency composite par provider
ALTER TABLE payment_events
  ADD UNIQUE KEY uq_payment_events_provider_event (provider, provider_event_id);
