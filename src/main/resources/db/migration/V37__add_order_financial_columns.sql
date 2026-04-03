SET @has_payment_status = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'payment_status'
);

SET @add_payment_status_sql = IF(
    @has_payment_status = 0,
    'ALTER TABLE orders ADD COLUMN payment_status VARCHAR(30) NULL AFTER status',
    'SELECT 1'
);
PREPARE stmt_add_payment_status FROM @add_payment_status_sql;
EXECUTE stmt_add_payment_status;
DEALLOCATE PREPARE stmt_add_payment_status;

SET @has_payment_plan = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'payment_plan'
);

SET @add_payment_plan_sql = IF(
    @has_payment_plan = 0,
    'ALTER TABLE orders ADD COLUMN payment_plan VARCHAR(30) NULL AFTER payment_status',
    'SELECT 1'
);
PREPARE stmt_add_payment_plan FROM @add_payment_plan_sql;
EXECUTE stmt_add_payment_plan;
DEALLOCATE PREPARE stmt_add_payment_plan;

SET @has_payment_method_sel = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'payment_method_sel'
);

SET @add_payment_method_sel_sql = IF(
    @has_payment_method_sel = 0,
    'ALTER TABLE orders ADD COLUMN payment_method_sel VARCHAR(50) NULL AFTER payment_plan',
    'SELECT 1'
);
PREPARE stmt_add_payment_method_sel FROM @add_payment_method_sel_sql;
EXECUTE stmt_add_payment_method_sel;
DEALLOCATE PREPARE stmt_add_payment_method_sel;

SET @has_amount_total = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'amount_total'
);

SET @add_amount_total_sql = IF(
    @has_amount_total = 0,
    'ALTER TABLE orders ADD COLUMN amount_total DECIMAL(15,2) NULL AFTER payment_method_sel',
    'SELECT 1'
);
PREPARE stmt_add_amount_total FROM @add_amount_total_sql;
EXECUTE stmt_add_amount_total;
DEALLOCATE PREPARE stmt_add_amount_total;

SET @has_amount_paid = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'amount_paid'
);

SET @add_amount_paid_sql = IF(
    @has_amount_paid = 0,
    'ALTER TABLE orders ADD COLUMN amount_paid DECIMAL(15,2) NULL AFTER amount_total',
    'SELECT 1'
);
PREPARE stmt_add_amount_paid FROM @add_amount_paid_sql;
EXECUTE stmt_add_amount_paid;
DEALLOCATE PREPARE stmt_add_amount_paid;

SET @has_amount_due = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'amount_due'
);

SET @add_amount_due_sql = IF(
    @has_amount_due = 0,
    'ALTER TABLE orders ADD COLUMN amount_due DECIMAL(15,2) NULL AFTER amount_paid',
    'SELECT 1'
);
PREPARE stmt_add_amount_due FROM @add_amount_due_sql;
EXECUTE stmt_add_amount_due;
DEALLOCATE PREPARE stmt_add_amount_due;

SET @has_deposit_amount = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'deposit_amount'
);

SET @add_deposit_amount_sql = IF(
    @has_deposit_amount = 0,
    'ALTER TABLE orders ADD COLUMN deposit_amount DECIMAL(15,2) NULL AFTER amount_due',
    'SELECT 1'
);
PREPARE stmt_add_deposit_amount FROM @add_deposit_amount_sql;
EXECUTE stmt_add_deposit_amount;
DEALLOCATE PREPARE stmt_add_deposit_amount;
