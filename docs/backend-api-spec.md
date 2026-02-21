# Backend API spec (catalogue, orders, preorders)

Ce document décrit le contrat API cible pour aligner Front + Back ainsi que l’organisation BDD associée.

## Base paths

- Public: `/api/v1`
- Admin: `/api/v1/admin`

## 1) Catalogue

### `GET /api/v1/products`

Filtres supportés/cibles:
- `type` (`SPC` / `PANEL`)
- `page`, `size`
- `q` (recherche texte)
- `stockStatus` (`IN_STOCK`, `PREORDER`, `OUT_OF_STOCK`)
- `featured` (optionnel recommandé)
- `sort` (optionnel recommandé: `newest`, `price_asc`, `price_desc`, `name_asc`)

Réponse attendue: liste de produits avec `stockStatus` (et optionnellement `availableQty`, `eta`).

### `GET /api/v1/products/{slug}`

Conservé tel quel pour la fiche produit.

## 2) Orders (nouveau flux e-commerce)

### `POST /api/v1/orders`
Crée une commande avec statut initial: `PENDING_CONFIRMATION`.

Validations minimales:
- `phone` obligatoire
- `items.length >= 1`
- `qty > 0`
- produit existe et SKU cohérent
- produits commandés disponibles (`IN_STOCK`)
- contrôle transactionnel de stock (sinon `409 STOCK_INSUFFICIENT`)

### `GET /api/v1/orders/{orderId}`
Suivi client/admin.

### `PATCH /api/v1/orders/{orderId}/status` (admin)
Transitions:
- `PENDING_CONFIRMATION -> CONFIRMED | CANCELLED`
- `CONFIRMED -> FULFILLED | CANCELLED`

Règle stock:
- décrément au passage `CONFIRMED`.

## 3) Preorders (hors-stock, lead séparé)

### `POST /api/v1/preorders`
Crée un lead en `NEW`.

Validations minimales:
- téléphone obligatoire
- `acceptsDelay = true`
- `items.length >= 1`
- `qty > 0`
- produits existent
- produits hors-stock uniquement
- pas de décrément stock

### `GET /api/v1/preorders/{id}`
Suivi de la demande.

### `PATCH /api/v1/preorders/{id}/status` (admin)
Statuts métier recommandés:
`NEW`, `CONTACTED`, `RESERVED`, `CANCELLED`, `CONVERTED`.

## 4) Contact / devis pro

### `POST /api/v1/contact`
À garder pour demandes générales et devis pro (B2B), séparé des `orders` / `preorders`.

## 5) Stratégie stock recommandée

- `POST /orders`: validation transactionnelle + création en `PENDING_CONFIRMATION`.
- `PATCH /orders/{id}/status` vers `CONFIRMED`: revalidation stock + décrément réel + traçabilité dans `stock_movements`.
- `preorders`: lead commercial uniquement, sans décrément stock.

## 6) Devis pro BTP (quote)

`POST /api/v1/quotes` reste le canal dédié B2B/pro.

Structure BDD cible pour supporter les demandes avec packs et lignes produit:
- `quote_requests` enrichi (customer_type, project_type, city_zone, source, intent, channel, assigned_to, contacted_at, closed_at)
- `quote_request_items` pour les lignes produit (snapshot slug/sku, qty, unit)
- `quote_request_packs` pour les packs/services demandés (pack_code, pack_label_snapshot)

Cas d’usage: une demande de devis peut combiner des produits et des packs (ex. pose, accessoires, mixte SPC/panneaux).

