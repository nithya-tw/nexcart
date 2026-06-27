-- Orders table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    shipping_address TEXT NOT NULL,
    payment_method VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT orders_total_amount_positive CHECK (total_amount >= 0),
    CONSTRAINT orders_status_valid CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

-- Order items table
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT order_items_unit_price_positive CHECK (unit_price >= 0),
    CONSTRAINT order_items_subtotal_positive CHECK (subtotal >= 0)
);

-- Indexes for performance
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- Trigger for updating updated_at timestamp
CREATE OR REPLACE FUNCTION update_order_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER orders_updated_at_trigger
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_order_updated_at();

CREATE TRIGGER order_items_updated_at_trigger
    BEFORE UPDATE ON order_items
    FOR EACH ROW
    EXECUTE FUNCTION update_order_updated_at();

-- Comments
COMMENT ON TABLE orders IS 'Main orders table storing customer orders';
COMMENT ON TABLE order_items IS 'Order line items with price snapshots';
COMMENT ON COLUMN orders.order_number IS 'Unique order identifier in format ORD-YYYYMMDD-XXXX';
COMMENT ON COLUMN orders.total_amount IS 'Total order amount calculated from order items';
COMMENT ON COLUMN order_items.subtotal IS 'Item subtotal (quantity * unit_price)';
