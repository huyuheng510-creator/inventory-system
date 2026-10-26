-- StockFlow Project 2 - Apache Derby embedded database schema.
-- Executed once by SchemaInitializer when the database is first created.
-- Money is DECIMAL(12,2); quantities are INTEGER; ids are UUID strings.

CREATE TABLE users (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    username      VARCHAR(20)  NOT NULL UNIQUE,
    password_hash VARCHAR(64)  NOT NULL,
    display_name  VARCHAR(60)  NOT NULL,
    role          VARCHAR(16)  NOT NULL
);

CREATE TABLE categories (
    id          VARCHAR(36) NOT NULL PRIMARY KEY,
    name        VARCHAR(40) NOT NULL UNIQUE,
    description VARCHAR(120)
);

CREATE TABLE suppliers (
    id           VARCHAR(36) NOT NULL PRIMARY KEY,
    name         VARCHAR(60) NOT NULL UNIQUE,
    contact_name VARCHAR(40),
    phone        VARCHAR(20),
    email        VARCHAR(80)
);

CREATE TABLE products (
    id            VARCHAR(36)   NOT NULL PRIMARY KEY,
    sku           VARCHAR(20)   NOT NULL UNIQUE,
    name          VARCHAR(60)   NOT NULL,
    category_id   VARCHAR(36)   REFERENCES categories(id),
    supplier_id   VARCHAR(36)   REFERENCES suppliers(id),
    unit_price    DECIMAL(12,2) NOT NULL,
    quantity      INTEGER       NOT NULL,
    reorder_level INTEGER       NOT NULL,
    unit          VARCHAR(20)   NOT NULL
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_supplier ON products(supplier_id);

CREATE TABLE stock_transactions (
    id                VARCHAR(36)   NOT NULL PRIMARY KEY,
    product_id        VARCHAR(36)   NOT NULL REFERENCES products(id),
    type              VARCHAR(20)   NOT NULL,
    quantity          INTEGER       NOT NULL,
    unit_price        DECIMAL(12,2) NOT NULL,
    operator_username VARCHAR(20)   NOT NULL,
    note              VARCHAR(120),
    occurred_at       TIMESTAMP     NOT NULL
);

CREATE INDEX idx_transactions_product ON stock_transactions(product_id);
CREATE INDEX idx_transactions_occurred ON stock_transactions(occurred_at);

CREATE TABLE purchase_orders (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    order_number  VARCHAR(24)  NOT NULL UNIQUE,
    supplier_id   VARCHAR(36)  NOT NULL REFERENCES suppliers(id),
    status        VARCHAR(24)  NOT NULL,
    created_by    VARCHAR(20)  NOT NULL,
    created_on    TIMESTAMP    NOT NULL,
    expected_date DATE,
    received_at   TIMESTAMP,
    note          VARCHAR(120)
);

CREATE INDEX idx_purchase_orders_supplier ON purchase_orders(supplier_id);
CREATE INDEX idx_purchase_orders_status ON purchase_orders(status);

CREATE TABLE purchase_order_lines (
    id                VARCHAR(36)   NOT NULL PRIMARY KEY,
    order_id          VARCHAR(36)   NOT NULL REFERENCES purchase_orders(id),
    product_id        VARCHAR(36)   NOT NULL REFERENCES products(id),
    quantity          INTEGER       NOT NULL,
    unit_cost         DECIMAL(12,2) NOT NULL,
    received_quantity INTEGER       NOT NULL DEFAULT 0
);

CREATE INDEX idx_purchase_order_lines_order ON purchase_order_lines(order_id);
