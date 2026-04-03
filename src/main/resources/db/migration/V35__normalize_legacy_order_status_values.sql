UPDATE orders
SET status = 'CANCELLED'
WHERE UPPER(TRIM(status)) = 'CANCELED';

UPDATE orders
SET status = 'PENDING_CONFIRMATION'
WHERE UPPER(TRIM(status)) IN ('SUBMITTED', 'DRAFT_PENDING_PAYMENT');

UPDATE orders
SET status = 'FULFILLED'
WHERE UPPER(TRIM(status)) = 'DELIVERED';

UPDATE orders
SET cancelled_at = COALESCE(cancelled_at, updated_at, created_at)
WHERE UPPER(TRIM(status)) = 'CANCELLED'
  AND cancelled_at IS NULL;
