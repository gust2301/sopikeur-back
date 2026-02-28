-- V29 : Add payment model columns to orders table
ALTER TABLE orders
  ADD COLUMN order_status       VARCHAR(32),
  ADD COLUMN payment_status     VARCHAR(32) DEFAULT 'UNPAID',
  ADD COLUMN payment_plan       VARCHAR(32) DEFAULT 'CASH_ON_DELIVERY',
  ADD COLUMN payment_method_sel VARCHAR(32) DEFAULT 'NONE',
  ADD COLUMN amount_total       DECIMAL(12,2),
  ADD COLUMN amount_paid        DECIMAL(12,2) NOT NULL DEFAULT 0,
  ADD COLUMN amount_due         DECIMAL(12,2),
  ADD COLUMN deposit_amount     DECIMAL(12,2);
