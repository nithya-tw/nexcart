-- NexCart Inventory Service - Database Schema V1
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    available_quantity INTEGER GENERATED ALWAYS AS (quantity - reserved_quantity) STORED,
    reorder_level INTEGER NOT NULL DEFAULT 10,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_inventory_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_reserved_quantity CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_reserved_not_exceed CHECK (reserved_quantity <= quantity),
    CONSTRAINT chk_reorder_level CHECK (reorder_level >= 0)
);

CREATE TABLE stock_reservations (
    id BIGSERIAL PRIMARY KEY,
    inventory_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_reservations_inventory FOREIGN KEY (inventory_id) 
        REFERENCES inventory(id) ON DELETE CASCADE,
    CONSTRAINT chk_reservation_quantity CHECK (quantity > 0),
    CONSTRAINT chk_reservation_status CHECK (status IN ('RESERVED', 'CONFIRMED', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX idx_inventory_product_id ON inventory(product_id);
CREATE INDEX idx_inventory_available ON inventory(available_quantity) WHERE available_quantity > 0;
CREATE INDEX idx_reservations_inventory_id ON stock_reservations(inventory_id);
CREATE INDEX idx_reservations_order_id ON stock_reservations(order_id);
CREATE INDEX idx_reservations_status ON stock_reservations(status);

CREATE OR REPLACE FUNCTION update_inventory_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_inventory_updated_at
    BEFORE UPDATE ON inventory
    FOR EACH ROW
    EXECUTE FUNCTION update_inventory_timestamp();

CREATE OR REPLACE FUNCTION update_reservation_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_reservation_updated_at
    BEFORE UPDATE ON stock_reservations
    FOR EACH ROW
    EXECUTE FUNCTION update_reservation_timestamp();

INSERT INTO inventory (product_id, quantity, reserved_quantity, reorder_level) VALUES
(1, 100, 0, 10),
(2, 50, 0, 5),
(3, 200, 0, 20);
