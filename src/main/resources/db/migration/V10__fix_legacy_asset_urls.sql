-- V10__fix_legacy_asset_urls.sql
-- Corrige les anciens assets dont l'url est un chemin relatif (/assets/...)
-- hérité de la migration V5/V6 (avant l'adoption du CDN absolu).
-- Après cette migration, tous les assets ont une URL absolue vers assets.sopikeur.sn.

-- 1) Backfille le champ path (vide pour les anciens assets) depuis l'url relative
UPDATE media_assets
SET path = SUBSTRING(url, 9)           -- supprime le préfixe '/assets/' (8 chars)
WHERE url LIKE '/assets/%'
  AND (path IS NULL OR path = '');

-- 2) Convertit les urls relatives en URL CDN absolues
UPDATE media_assets
SET url = CONCAT('https://assets.sopikeur.sn/', SUBSTRING(url, 9))
WHERE url LIKE '/assets/%';
