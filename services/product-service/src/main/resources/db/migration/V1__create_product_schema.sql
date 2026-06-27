-- =====================================================
-- Flyway Migration: V1 - Create Product Service Schema
-- =====================================================
-- Description: Creates brands, categories, products, and product_images tables
-- Author: NexCart Team
-- Date: 2026-06-27
-- =====================================================

-- =====================================================
-- Table: brands
-- =====================================================
-- Stores product brands/manufacturers (e.g., Apple, Nike, Samsung)
-- Independent table with no foreign keys
-- =====================================================

CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    logo_url VARCHAR(500),
    website_url VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_brand_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

-- Index for active brands filtering
CREATE INDEX idx_brands_active ON brands(is_active);

-- Index for brand name search
CREATE INDEX idx_brands_name ON brands(name);

COMMENT ON TABLE brands IS 'Product brands and manufacturers';
COMMENT ON COLUMN brands.is_active IS 'Soft delete flag - inactive brands are hidden from catalog';

-- =====================================================
-- Table: categories
-- =====================================================
-- Hierarchical product categories (e.g., Electronics > Phones > Smartphones)
-- Self-referencing for parent-child relationship
-- =====================================================

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    parent_id BIGINT,
    image_url VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Self-referencing foreign key for hierarchy
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) 
        REFERENCES categories(id) ON DELETE CASCADE,
    
    -- Ensure category cannot be its own parent
    CONSTRAINT chk_category_not_self_parent CHECK (id != parent_id),
    
    CONSTRAINT chk_category_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_category_slug_not_empty CHECK (LENGTH(TRIM(slug)) > 0)
);

-- Index for hierarchical queries
CREATE INDEX idx_categories_parent ON categories(parent_id);

-- Index for active categories
CREATE INDEX idx_categories_active ON categories(is_active);

-- Index for category ordering
CREATE INDEX idx_categories_display_order ON categories(display_order);

COMMENT ON TABLE categories IS 'Hierarchical product categories with parent-child relationships';
COMMENT ON COLUMN categories.slug IS 'URL-friendly identifier (e.g., electronics-smartphones)';
COMMENT ON COLUMN categories.parent_id IS 'Reference to parent category for hierarchy (NULL for root categories)';
COMMENT ON COLUMN categories.display_order IS 'Sort order for displaying categories (lower numbers first)';

-- =====================================================
-- Table: products
-- =====================================================
-- Core product catalog with brand and category associations
-- Uses JSONB for flexible attributes per category
-- =====================================================

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(300) NOT NULL UNIQUE,
    description TEXT,
    short_description VARCHAR(500),
    price NUMERIC(10, 2) NOT NULL,
    compare_at_price NUMERIC(10, 2),
    cost_price NUMERIC(10, 2),
    brand_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    attributes JSONB,
    meta_title VARCHAR(200),
    meta_description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_featured BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) 
        REFERENCES brands(id) ON DELETE RESTRICT,
    
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) 
        REFERENCES categories(id) ON DELETE RESTRICT,
    
    -- Business rules
    CONSTRAINT chk_product_price_positive CHECK (price >= 0),
    CONSTRAINT chk_product_compare_price_valid CHECK (compare_at_price IS NULL OR compare_at_price >= price),
    CONSTRAINT chk_product_cost_price_positive CHECK (cost_price IS NULL OR cost_price >= 0),
    CONSTRAINT chk_product_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_product_sku_not_empty CHECK (LENGTH(TRIM(sku)) > 0),
    CONSTRAINT chk_product_slug_not_empty CHECK (LENGTH(TRIM(slug)) > 0)
);

-- Indexes for common queries
CREATE INDEX idx_products_brand ON products(brand_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_active ON products(is_active);
CREATE INDEX idx_products_featured ON products(is_featured);
CREATE INDEX idx_products_created ON products(created_at DESC);

-- Composite index for category + price filtering (common in e-commerce)
CREATE INDEX idx_products_category_price ON products(category_id, price);

-- GIN index for JSONB attributes (enables fast queries on attributes)
CREATE INDEX idx_products_attributes ON products USING GIN (attributes);

COMMENT ON TABLE products IS 'Product catalog with brand and category associations';
COMMENT ON COLUMN products.sku IS 'Stock Keeping Unit - unique product identifier (e.g., ELEC-IPHONE15-256-BLK)';
COMMENT ON COLUMN products.slug IS 'URL-friendly identifier for SEO (e.g., iphone-15-pro-256gb-black)';
COMMENT ON COLUMN products.price IS 'Current selling price';
COMMENT ON COLUMN products.compare_at_price IS 'Original price for showing discounts (must be >= price)';
COMMENT ON COLUMN products.cost_price IS 'Cost to acquire product (for margin calculations)';
COMMENT ON COLUMN products.attributes IS 'Flexible JSONB field for category-specific attributes (e.g., {"color": "Black", "storage": "256GB"})';
COMMENT ON COLUMN products.is_featured IS 'Featured products shown on homepage';

-- =====================================================
-- Table: product_images
-- =====================================================
-- Product images with support for multiple images per product
-- One image marked as primary for main display
-- =====================================================

CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key
    CONSTRAINT fk_product_image_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE,
    
    -- Business rules
    CONSTRAINT chk_product_image_url_not_empty CHECK (LENGTH(TRIM(url)) > 0)
);

-- Index for retrieving images by product
CREATE INDEX idx_product_images_product ON product_images(product_id);

-- Index for finding primary images quickly
CREATE INDEX idx_product_images_primary ON product_images(product_id, is_primary);

-- Index for ordering images
CREATE INDEX idx_product_images_display_order ON product_images(product_id, display_order);

COMMENT ON TABLE product_images IS 'Product images with primary image support';
COMMENT ON COLUMN product_images.url IS 'CDN/S3 URL to image (images not stored in database)';
COMMENT ON COLUMN product_images.alt_text IS 'Accessibility text for screen readers';
COMMENT ON COLUMN product_images.display_order IS 'Sort order for displaying images (lower numbers first)';
COMMENT ON COLUMN product_images.is_primary IS 'Primary image shown in product listings';

-- =====================================================
-- Trigger: Update timestamps automatically
-- =====================================================
-- Automatically updates updated_at column when row is modified
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to brands table
CREATE TRIGGER trg_brands_updated_at
    BEFORE UPDATE ON brands
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to categories table
CREATE TRIGGER trg_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to products table
CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON FUNCTION update_updated_at_column() IS 'Automatically updates updated_at timestamp on row modification';
