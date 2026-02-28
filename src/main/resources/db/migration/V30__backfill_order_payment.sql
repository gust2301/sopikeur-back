-- V30 : Backfill new payment columns from legacy status values

-- 1. Backfill order_status from the old status column
UPDATE orders SET order_status = CASE status
  WHEN 'PENDING_CONFIRMATION' THEN 'SUBMITTED'
  WHEN 'CONFIRMED'            THEN 'CONFIRMED'
  WHEN 'PAID_DEPOSIT'         THEN 'CONFIRMED'
  WHEN 'FULFILLED'            THEN 'DELIVERED'
  WHEN 'CANCELLED'            THEN 'CANCELED'
  ELSE 'SUBMITTED'
END WHERE order_status IS NULL;

-- 2. Backfill payment_status from old status
UPDATE orders SET payment_status = CASE status
  WHEN 'PAID_DEPOSIT' THEN 'PARTIALLY_PAID'
  WHEN 'FULFILLED'    THEN 'PAID'
  ELSE 'UNPAID'
END;

-- 3. Backfill amount_total from order_items (sum of line totals)
UPDATE orders o
  INNER JOIN (
    SELECT order_id, SUM(line_total_snapshot) AS total
    FROM order_items
    GROUP BY order_id
  ) s ON o.id = s.order_id
  SET o.amount_total = s.total;

-- 4. amount_due = amount_total - amount_paid
UPDATE orders SET amount_due = COALESCE(amount_total, 0) - COALESCE(amount_paid, 0);

-- 5. Make order_status NOT NULL now that all rows are populated
ALTER TABLE orders MODIFY order_status VARCHAR(32) NOT NULL;
